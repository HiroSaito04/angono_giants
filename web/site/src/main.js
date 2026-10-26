import {LevelConfig} from './levelConfig.js';
import {GameTimer,formatSeconds} from './gameTimer.js';
import {EnemyManager} from './enemyManager.js';
import {ScoreManager} from './scoreManager.js';
import {GameRenderer} from './renderer.js';
import {AudioManager} from './audioManager.js';
import {guestProgress,setGuestName,recordGuestResult} from './storage.js';
import {FirebaseService} from './firebase.js';

const $=id=>document.getElementById(id);
const A='./assets/images/';
const PAGES=['splash-screen','menu-screen','loading-screen','game-screen','leaderboard-screen','about-screen'];
const INTRO_MID_MS=1070; // Original GIF: after frame 12 of 25, from its own frame durations.
const INTRO_END_MS=1510;
const RESULT_DELAY_MS=5000;
const SAVE_SCORE_LABEL=v=>`Score: ${Number(v).toLocaleString('en-US')}`;
const im=(filename)=>`${A}${filename}`;
function restartGif(id,filename){$(id).src=im(filename)+`?run=${Date.now()}`;}

function scaleStage(){
  const stage=$('android-stage'), portrait=window.innerHeight>window.innerWidth;
  const k=portrait?window.innerWidth/360:Math.min(window.innerWidth/360,window.innerHeight/800);
  const height=portrait?Math.round(window.innerHeight/k):800;
  stage.style.height=`${height}px`;stage.style.transform=`scale(${k})`;
  // Match Android's match_parent behavior on mobile rather than letterboxing.
  // The Canvas is resized before an active game starts.
  const app=window.AngonoGiantsDebug;
  if(!app?.enemies){$('gameView').height=height;if(app?.renderer)app.renderer.height=height;}
}
function hide(el){$(el).classList.add('is-hidden');}
function show(el){$(el).classList.remove('is-hidden');}
function animateButton(button){
  if(!button?.dataset?.first)return;
  const child=button.querySelector('img');const first=button.dataset.first,second=button.dataset.second;
  child.src=im(second);setTimeout(()=>{if(child.isConnected)child.src=im(first);},200);
}
function waitImage(url){return new Promise(resolve=>{const pic=new Image();pic.onload=pic.onerror=()=>resolve(pic);pic.src=url;});}

class AngonoGiants{
  constructor(){
    this.phase='splash';this.audio=new AudioManager();this.firebase=new FirebaseService();
    this.cloud=false;this.cloudName='';this.cloudLevel=1;
    this.player='Guest';this.level=1;this.lives=3;this.completedSeconds=0;this.completedWaves=0;
    this.enemies=null;this.score=null;this.timer=null;this.result=null;
    this.canvas=$('gameView');this.renderer=null;this.images={};this.imageReady=null;
    this.lastFrame=0;this.musicReadyAt=0;this.firstIntroSound=false;
    this.introTimeouts=[];this.resultTimeout=null;this.timeoutRemaining=RESULT_DELAY_MS;this.timeoutStarted=0;
    this.authMode='login';this.pendingName=null;this.loadLaunched=false;this.page='splash-screen';
  }
  async init(){
    scaleStage();window.addEventListener('resize',scaleStage);
    this.bind();this.imageReady=this.loadGameplaySprites();
    this.firebase.init().then(()=>this.updateAccountStatus()).catch(err=>console.warn('Firebase not configured:',err));
    requestAnimationFrame(t=>this.frame(t));
    await new Promise(resolve=>setTimeout(resolve,1000)); // SplashActivity.SPLASH_DELAY
    this.showMenu();
  }
  async loadGameplaySprites(){
    const files=['red_warning','stall01','stall02','stall03','stall_destroyed','heart_full','heart_two','heart_one','heart_empty'];
    for(let n=1;n<=7;n++){files.push(`higantes${n}top`,`higantes${n}bottom`);}
    await Promise.all(files.map(async key=>{
      const picture=await waitImage(im(key+'.png'));if(picture.complete&&picture.naturalWidth)this.images[key]=picture;
    }));
    this.renderer=new GameRenderer(this.canvas,this.images);
  }
  pageOnly(id){for(const p of PAGES)$(p).classList.toggle('is-hidden',p!==id);this.page=id;}
  gameViewVisible(showIt){$('game-screen').classList.toggle('is-hidden',!showIt);}
  setGameHud(active){$('game-hud').classList.toggle('is-hidden',!active);}
  showMenu(){
    this.cancelTransitions();this.phase='menu';this.pageOnly('menu-screen');this.hideOutcome();this.clearPauseButtons();
    restartGif('menu-screen-bg','mainmenubg01.gif');const guest=guestProgress();$('player-name').value=guest.name;$('level-status').textContent=this.cloud?`${this.cloudName} - LEVEL ${this.cloudLevel}`:`GUEST - LEVEL ${guest.highestLevel}`;
    $('player-name').disabled=this.cloud;if(this.cloud)$('player-name').value=this.cloudName;
    $('account-button').textContent=this.cloud?'SIGN OUT':'SIGN IN';
    this.audio.resume();this.audio.musicSet('menu');
  }
  updateAccountStatus(){if(this.firebase.user&&!this.cloud)this.loadCloudUser().catch(console.warn);else if(this.phase==='menu')this.showMenu();}
  async loadCloudUser(){
    if(!this.firebase.user)return;
    const profile=await this.firebase.readProfile(),progress=await this.firebase.readProgress();
    if(!profile?.playerName){this.cloud=false;this.showMenu();this.authMode='profile';this.renderAuth();show('auth-overlay');return;}
    this.cloud=true;this.cloudName=profile.playerName;this.cloudLevel=Math.max(1,progress?.highestUnlockedLevel||1);
    if(this.phase==='menu')this.showMenu();
  }
  async startGame(){
    if(this.phase!=='menu')return;
    if(this.cloud){this.player=this.cloudName;this.level=this.cloudLevel;}
    else{
      this.player=($('player-name').value.trim()||'Guest').slice(0,20);setGuestName(this.player);
      this.level=guestProgress().highestLevel;
    }
    this.completedSeconds=0;this.completedWaves=0;this.score=new ScoreManager(score=>$('ScoreCounter').textContent=SAVE_SCORE_LABEL(score));
    this.audio.musicSet(null);await this.startLoading();
  }
  async startLoading(){
    this.phase='loading';this.loadLaunched=false;this.pageOnly('loading-screen');
    const video=$('loading-video');video.pause();try{video.currentTime=0;}catch{}
    video.muted=false;const play=video.play();if(play?.catch)play.catch(()=>{video.muted=true;video.play().catch(()=>this.launchGame());});
  }
  async launchGame(){
    if(this.phase!=='loading'||this.loadLaunched)return;this.loadLaunched=true;
    const video=$('loading-video');video.pause();
    await this.imageReady; // All GameRenderer sprites loaded before the GIF starts.
    this.resetLevel(this.level,true);
    this.pageOnly('game-screen');this.hideOutcome();this.setGameHud(true);
    await Promise.all(['tilelayerView','treelayerView'].map(async id=>{const img=$(id);if(!img.complete)await new Promise(resolve=>{img.onload=img.onerror=resolve;});try{await img.decode();}catch{}}));
    this.startIntro();
  }
  resetLevel(level,preserveScore){
    this.level=level;this.lives=3;
    if(!preserveScore||!this.score)this.score=new ScoreManager(score=>{$('ScoreCounter').textContent=SAVE_SCORE_LABEL(score);});
    this.score.clearPopups();
    this.enemies=new EnemyManager(360,this.canvas.height,{
      onWaveChanged:wave=>{$('waveCounter').textContent=`Level ${this.level} - Wave ${wave}`;},
      onFirstEnemySpawned:()=>{if(this.phase==='playing')this.timer.start();},
      onGrowlNeeded:(first,done)=>this.audio.playGrowl(first,done),
      onEnemyReached:e=>this.enemyReached(e),
    });
    this.enemies.resetForLevel(level);
    this.renderer.reset();this.canvas.style.filter='none';
    this.timer=new GameTimer(LevelConfig.durationSeconds(level),s=>{$('timerText').textContent=formatSeconds(s);},()=>this.levelComplete());
    $('timerText').textContent=formatSeconds(LevelConfig.durationSeconds(level));
    $('ScoreCounter').textContent=SAVE_SCORE_LABEL(this.score.score);
    $('waveCounter').textContent=`Level ${level} - Wave 1`;
    this.setPauseImage(false);this.frameOnce();
  }
  startIntro(){
    this.phase='intro';this.firstIntroSound=false;
    const curtain=$('introCurtain'),gif=$('gifOverlay');show('introCurtain');show('gifOverlay');
    this.introTimeouts.forEach(clearTimeout);this.introTimeouts=[];
    gif.onload=()=>{
      if(this.phase!=='intro')return;
      if(!this.firstIntroSound){this.firstIntroSound=true;this.audio.play('intro');}
      this.introTimeouts.push(setTimeout(()=>{if(this.phase==='intro')hide('introCurtain');},INTRO_MID_MS));
      this.introTimeouts.push(setTimeout(()=>this.endIntro(),INTRO_END_MS));
    };
    gif.onerror=()=>this.endIntro();
    // Reload the GIF on each fresh run so animation begins at frame zero.
    gif.src=im('gametransition3.gif')+`?run=${Date.now()}`;
  }
  endIntro(){if(this.phase!=='intro')return;
    this.introTimeouts.forEach(clearTimeout);this.introTimeouts=[];
    hide('introCurtain');hide('gifOverlay');this.phase='playing';this.lastFrame=0;
    this.musicReadyAt=performance.now()+5000;this.updateGameMusic();
  }
  frame(now){
    const actualDelta=this.lastFrame?Math.max(0,now-this.lastFrame):20;const delta=Math.min(50,actualDelta);this.lastFrame=now;
    if(this.enemies&&this.score&&this.renderer){
      if(this.phase==='playing'){
        this.enemies.update(delta);
        if(this.phase==='playing'){this.score.update(delta);this.timer.update(actualDelta);}
        this.updateGameMusic(now);
      }
      this.renderer.setLives(this.lives,now);
      if(this.phase==='intro'||this.phase==='playing'||this.phase==='paused')this.renderer.draw(this.enemies.enemies,this.score.popups,this.lives,now);
    }
    requestAnimationFrame(t=>this.frame(t));
  }
  frameOnce(){if(this.renderer&&this.enemies)this.renderer.draw(this.enemies.enemies,this.score.popups,this.lives,performance.now());}
  tap(event){
    if(this.phase!=='playing')return;event.preventDefault();
    const r=this.canvas.getBoundingClientRect();const x=(event.clientX-r.left)/r.width*360,y=(event.clientY-r.top)/r.height*800;
    const enemy=this.enemies.hitTest(x,y);if(!enemy)return;
    this.audio.hit(enemy.type);this.score.hit(enemy,this.enemies.targetX,this.enemies.targetY);
  }
  enemyReached(e){
    if(this.phase!=='playing')return;
    if(e.type===6){if(this.lives<3){this.lives++;this.enemies.lives=this.lives;this.audio.play('heal');}return;}
    this.lives=Math.max(0,this.lives-1);this.enemies.lives=this.lives;
    this.score.stallDamaged(this.enemies.targetX,this.enemies.targetY);this.audio.play('hurt');
    if(this.lives===0)this.gameLost();else this.updateGameMusic();
  }
  updateGameMusic(now=performance.now()){
    if(this.phase!=='playing')return;
    if(this.lives===1){this.audio.musicSet('low');return;}
    if(now<this.musicReadyAt){this.audio.musicSet(null);return;}
    this.audio.musicSet('normal');
  }
  levelComplete(){if(this.phase!=='playing')return;
    this.completedSeconds+=this.timer.elapsedSeconds();this.completedWaves+=this.enemies.getActualWaveCount();
    this.result={victory:true,level:this.level,nextLevel:LevelConfig.nextLevel(this.level),score:this.score.score,waves:this.completedWaves,seconds:this.completedSeconds};
    this.enemies.stop();this.audio.cancelGrowl();this.saveResult(this.result);this.showOutcome(true);
  }
  gameLost(){if(this.phase!=='playing')return;
    this.result={victory:false,level:this.level,nextLevel:this.level,score:this.score.score,waves:this.completedWaves+this.enemies.getActualWaveCount(),seconds:this.completedSeconds+this.timer.elapsedSeconds()};
    this.renderer.lost=true;this.canvas.style.filter='grayscale(1)';this.enemies.stop();this.audio.cancelGrowl();
    $('tilelayerView').style.filter='grayscale(1)';$('treelayerView').style.filter='grayscale(1)';
    this.saveResult(this.result);this.showOutcome(false);
  }
  saveResult(r){
    if(this.cloud&&this.firebase.user){
      this.firebase.saveResult(this.player,r.victory?r.nextLevel:r.level,r.score,r.waves,r.seconds).catch(e=>console.error('Cloud save failed:',e));
      if(r.victory)this.cloudLevel=Math.max(this.cloudLevel,r.nextLevel);
    }else recordGuestResult(r.victory?r.nextLevel:r.level,r.score,r.waves,r.seconds);
  }
  showOutcome(victory){
    this.phase='outcome';this.setGameHud(false);this.clearPauseButtons();this.hideOutcome();
    this.audio.musicSet(victory?'winIntro':'lostIntro');
    if(victory){
      restartGif('gameThanksImage','gamewonthanks_background.gif');show('gameThanksImage');
      $('gameThanksText').textContent=`THANKS FOR SAVING OUR VILLAGE!\n\nLEVEL ${this.level} IS COMPLETE!\nLEVEL ${LevelConfig.nextLevel(this.level)} UNLOCKED!\n\nGIANTS WON'T ATTACK FOR THE MEANTIME.`;
      show('gameThanksText');
    }else show('gameOverImage');
    this.timeoutRemaining=RESULT_DELAY_MS;this.timeoutStarted=performance.now();
    this.resultTimeout=setTimeout(()=>this.showResultScreen(),RESULT_DELAY_MS);
  }
  showResultScreen(){if(this.phase!=='outcome')return;this.phase='results';this.hideOutcome();const r=this.result;
    if(r.victory){restartGif('gameWonImage','wonfinalscore.gif');show('gameWonImage');this.audio.musicSet(null);}else{show('lostfinalscoreiv');this.audio.musicSet('lostResult');}
    $('playerNameText').textContent=this.player;
    $('finalLevelTV').textContent=r.victory?`LEVEL ${r.level} COMPLETE!`:`LEVEL ${r.level} REACHED`;
    $('resultSubtitle').textContent=r.victory?`LEVEL ${r.nextLevel} UNLOCKED`:'THE VILLAGE NEEDS YOUR HELP!';
    $('finalScoreTimer').textContent=formatSeconds(r.seconds);
    $('finalScoreTV').textContent=r.score.toLocaleString('en-US');
    $('finalwaveTV').textContent=r.waves.toLocaleString('en-US');
    show('finalScoreContainer');show('finalActionRow');
  }
  hideOutcome(){for(const id of ['gameOverImage','lostfinalscoreiv','gameThanksImage','gameWonImage','gameThanksText','finalScoreContainer','finalActionRow'])hide(id);}
  clearPauseButtons(){for(const id of ['restartButton','menuButton','exitButton'])hide(id);}
  setPauseImage(resume){const b=$('btnPauseResume');b.dataset.first=resume?'resume_button01.png':'pause_button01.png';b.dataset.second=resume?'resume_button02.png':'pause_button02.png';b.querySelector('img').src=im(b.dataset.first);}
  pause(){if(this.phase!=='playing')return;this.phase='paused';this.setPauseImage(true);this.audio.pause();
    for(const id of ['restartButton','menuButton','exitButton'])show(id);
  }
  resume(){if(this.phase!=='paused')return;this.phase='playing';this.setPauseImage(false);this.clearPauseButtons();this.audio.resume();this.lastFrame=0;
    this.musicReadyAt=performance.now()+Math.max(0,this.musicReadyAt-performance.now());
  }
  async restart(){if(this.phase!=='paused'&&this.phase!=='results')return;
    this.completedSeconds=0;this.completedWaves=0;this.score=null;this.hideOutcome();this.clearPauseButtons();
    this.audio.resume();this.audio.musicSet(null);await this.startLoading();
  }
  nextLevel(){if(this.phase!=='results')return;
    if(!this.result.victory){this.restart();return;}
    this.audio.musicSet(null);this.resetLevel(LevelConfig.nextLevel(this.level),true);this.hideOutcome();
    $('tilelayerView').style.filter='none';$('treelayerView').style.filter='none';
    this.setGameHud(true);this.phase='playing';this.musicReadyAt=performance.now()+5000;
  }
  cancelTransitions(){clearTimeout(this.resultTimeout);this.introTimeouts.forEach(clearTimeout);this.introTimeouts=[];}
  mainMenu(){this.cancelTransitions();this.hideOutcome();this.clearPauseButtons();this.enemies?.stop();
    hide('gifOverlay');hide('introCurtain');$('tilelayerView').style.filter='none';$('treelayerView').style.filter='none';
    this.showMenu();
  }
  async showBoard(){
    this.phase='board';this.pageOnly('leaderboard-screen');this.audio.musicSet('leaderboard');
    $('leaderboardListView').replaceChildren();$('emptyLeaderboardText').textContent='Loading leaderboard...';show('emptyLeaderboardText');
    let list=[];
    if(this.firebase.ready){try{list=await this.firebase.readLeaderboard()||[];}catch(err){console.warn(err);}}
    else list=[]; // Android guest scores are not placed in the global leaderboard.
    list.sort((a,b)=>(b.highestLevel||0)-(a.highestLevel||0)||(b.score||0)-(a.score||0)||String(a.playerName).localeCompare(String(b.playerName)));
    if(this.phase!=='board')return;
    if(!list.length){$('emptyLeaderboardText').textContent='No scores yet. Be the first to climb the levels!';return;}
    hide('emptyLeaderboardText');const root=$('leaderboardListView');
    list.forEach((row,index)=>{
      const div=document.createElement('div');div.className='leaderboard-entry';div.setAttribute('role','listitem');
      if(index<3){const medal=document.createElement('img');medal.className='medal';medal.src=im(['medal1gold.png','medal2silver.png','medal3bronze.png'][index]);medal.alt=['Gold','Silver','Bronze'][index];div.append(medal);}
      const info=document.createElement('div');info.className='entry-info';
      for(const value of [`#${index+1}  ${row.playerName}`,`LEVEL ${row.highestLevel||1}`,`SCORE: ${(row.score||0).toLocaleString('en-US')}`,`WAVES: ${row.wavesSurvived||0}`,`TIME: ${row.timePlayed||'Unknown'}`]){
        const child=document.createElement('div');child.textContent=value;info.append(child);
      }div.append(info);root.append(div);
    });
  }
  showAbout(){this.phase='about';this.pageOnly('about-screen');this.audio.musicSet(null);}
  authOpen(){this.authMode='login';this.renderAuth();show('auth-overlay');}
  renderAuth(){
    const signup=this.authMode==='signup',profile=this.authMode==='profile';
    $('auth-title').textContent=profile?'FINISH PROFILE':signup?'CREATE ACCOUNT':'WELCOME BACK';
    $('auth-subtitle').textContent=profile?'Choose your username':signup?'Create an account to save progress online':'Sign in to save progress online';
    $('auth-submit').textContent=profile?'SAVE USERNAME':signup?'SIGN UP':'LOGIN';
    $('auth-switch').textContent=signup?'ALREADY HAVE AN ACCOUNT?':profile?'SIGN OUT':'CREATE NEW ACCOUNT';
    $('auth-username').classList.toggle('is-hidden',!(signup||profile));
    $('auth-email').classList.toggle('is-hidden',profile);$('auth-password').classList.toggle('is-hidden',profile);
    $('auth-reset').classList.toggle('is-hidden',profile);hide('auth-error');
  }
  async submitAuth(){
    const name=$('auth-username').value.trim(),email=$('auth-email').value.trim(),password=$('auth-password').value;
    const error=$('auth-error');hide('auth-error');$('auth-submit').disabled=true;
    try{
      if(this.authMode==='login')await this.firebase.signIn(email,password);
      else if(this.authMode==='signup'){
        if(!/^[a-zA-Z0-9_]{3,20}$/.test(name))throw Error('Username must be 3–20 characters: letters, numbers or underscore.');
        await this.firebase.signUp(email,password);await this.makeProfile(name);
      }else{if(!/^[a-zA-Z0-9_]{3,20}$/.test(name))throw Error('Enter a valid username (3–20 letters, numbers or underscore).');await this.makeProfile(name);}
      await this.loadCloudUser();if(this.cloud)hide('auth-overlay');
    }catch(e){error.textContent=e?.message||String(e);show('auth-error');}
    finally{$('auth-submit').disabled=false;}
  }
  async makeProfile(name){
    if(!this.firebase.ready||!this.firebase.user)throw Error('Firebase is not connected.');
    const {ref,set,get}=this.firebase.mods.db,uid=this.firebase.user.uid,db=this.firebase.db;
    const key=name.toLowerCase(),existing=(await get(ref(db,`usernames/${key}`))).val();
    if(existing&&existing!==uid)throw Error('Username is already taken.');
    await set(ref(db,`usernames/${key}`),uid);
    await set(ref(db,`playerProfiles/${uid}`),{uid,playerName:name,normalizedName:key});
  }
  async authReset(){try{await this.firebase.resetPassword($('auth-email').value.trim());$('auth-error').textContent='Password reset email sent.';show('auth-error');}catch(e){$('auth-error').textContent=e?.message||String(e);show('auth-error');}}
  bind(){
    $('gameView').addEventListener('pointerdown',e=>this.tap(e));
    $('start-button').addEventListener('click',async()=>{this.audio.play('click');animateButton($('start-button'));await this.startGame();});
    $('skip-loading').addEventListener('click',()=>{this.audio.play('click');this.launchGame();});
    $('loading-video').addEventListener('ended',()=>this.launchGame());
    $('loading-video').addEventListener('error',()=>this.launchGame());
    $('btnPauseResume').addEventListener('click',()=>{this.audio.play('click');if(this.phase==='playing')this.pause();else if(this.phase==='paused')this.resume();});
    $('restartButton').addEventListener('click',()=>{this.audio.play('click');animateButton($('restartButton'));this.restart();});
    $('menuButton').addEventListener('click',()=>{this.audio.play('click');animateButton($('menuButton'));this.mainMenu();});
    $('exitButton').addEventListener('click',()=>{this.audio.play('click');animateButton($('exitButton'));window.close();if(!window.closed)this.mainMenu();});
    $('defendAgainButton').addEventListener('click',()=>{this.audio.play('click');animateButton($('defendAgainButton'));this.nextLevel();});
    $('mainMenuButton').addEventListener('click',()=>{this.audio.play('click');animateButton($('mainMenuButton'));this.mainMenu();});
    $('leaderboard-open').addEventListener('click',()=>{this.audio.play('click');this.showBoard();});
    $('mainMenuLButton').addEventListener('click',()=>{this.audio.play('click');this.mainMenu();});
    $('about-open').addEventListener('click',()=>{this.audio.play('click');this.showAbout();});
    $('about-back').addEventListener('click',()=>{this.audio.play('click');this.mainMenu();});
    $('account-button').addEventListener('click',async()=>{this.audio.play('click');if(this.cloud){await this.firebase.signOut();this.cloud=false;this.cloudName='';this.showMenu();}else this.authOpen();});
    $('auth-back').addEventListener('click',()=>hide('auth-overlay'));
    $('auth-submit').addEventListener('click',()=>this.submitAuth());
    $('auth-switch').addEventListener('click',()=>{if(this.authMode==='profile'){this.firebase.signOut().catch(()=>{});this.authMode='login';}else this.authMode=this.authMode==='signup'?'login':'signup';this.renderAuth();});
    $('auth-reset').addEventListener('click',()=>this.authReset());
    document.addEventListener('keydown',e=>{if(e.key==='Escape'||e.key.toLowerCase()==='p'){
      if(this.phase==='playing')this.pause();else if(this.phase==='paused')this.resume();else if(!$('auth-overlay').classList.contains('is-hidden'))hide('auth-overlay');
    }});
    document.addEventListener('visibilitychange',()=>{
      if(document.hidden){if(this.phase==='playing')this.pause();if(this.phase==='outcome'&&this.resultTimeout){clearTimeout(this.resultTimeout);this.timeoutRemaining=Math.max(0,this.timeoutRemaining-(performance.now()-this.timeoutStarted));this.resultTimeout=null;}this.audio.pause();}
      else{this.audio.resume();if(this.phase==='outcome'&&!this.resultTimeout){this.timeoutStarted=performance.now();this.resultTimeout=setTimeout(()=>this.showResultScreen(),this.timeoutRemaining);}}
    });
  }
}

const app=new AngonoGiants();
app.init().catch(err=>console.error('Angono Giants could not start',err));
// Developer diagnostics, not part of the game UI.
window.AngonoGiantsDebug=app;
