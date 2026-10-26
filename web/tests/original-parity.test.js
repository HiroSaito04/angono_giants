import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync,existsSync} from 'node:fs';
import {LevelConfig} from '../site/src/levelConfig.js';
import {GameTimer} from '../site/src/gameTimer.js';
import {EnemyManager} from '../site/src/enemyManager.js';
import {ScoreManager} from '../site/src/scoreManager.js';

test('original Android LevelConfig values are preserved',()=>{
  assert.deepEqual([1,2,3,4,10,39].map(LevelConfig.durationSeconds),[30,45,60,75,165,600]);
  assert.equal(LevelConfig.waveIntervalMs(1),3200);
  assert.equal(LevelConfig.maxEnemiesPerWave(1),2);
  assert.equal(LevelConfig.speedMultiplier(1),.6);
  assert.equal(LevelConfig.nextLevel(2147483647),2147483647);
});
test('timer does not count down before the first spawn callback',()=>{
  let remaining=0,finished=0;
  const timer=new GameTimer(30,s=>remaining=s,()=>finished++);
  timer.update(15000);assert.equal(remaining,30);
  timer.start();timer.update(31000);assert.equal(remaining,0);assert.equal(finished,1);
});
test('Android first wave growl, first enemy timing, wave count',()=>{
  let firstSpawns=0;
  const enemy=new EnemyManager(360,800,{
    onGrowlNeeded:(_,done)=>done(),
    onFirstEnemySpawned:()=>firstSpawns++,
  });
  enemy.update(1499);assert.equal(enemy.waveNumber,0);
  enemy.update(1);assert.equal(enemy.waveNumber,1);assert.equal(firstSpawns,0);
  enemy.update(999);assert.equal(firstSpawns,0);
  enemy.update(1);assert.equal(firstSpawns,1);assert.equal(enemy.enemies.length,1);
});
test('Android hit-test gives regular giant priority over healing giant',()=>{
  const enemy=new EnemyManager(360,800);
  const healing={x:180,y:400,type:6};const regular={x:181,y:400,type:0};
  enemy.enemies=[healing,regular];
  assert.equal(enemy.hitTest(180,400),regular);
  assert.equal(enemy.enemies.length,1);
});
test('original -1000 penalty and type scores',()=>{
  const scores=new ScoreManager(()=>{});
  scores.hit({type:6,x:0,y:0},180,400);assert.equal(scores.score,-1000);
  scores.stallDamaged(180,400);assert.equal(scores.score,-2000);
  scores.hit({type:1,x:180,y:400},180,400);assert.ok(scores.score>=-1755&&scores.score<=-1696);
});
test('original graphics and screen IDs are used, without redesign markup',()=>{
  const site='site/assets/images/';
  for(const key of ['mainmenubg01.gif','splashart.png','gametransition3.gif','wonfinalscore.gif','gamewonthanks_background.gif','gameoverskull_background.png','lostfinalscore_background.png','stall01.png','heart_full.png','leaderboard_background.png','defendagain_button01.png','menu_button01.png']){
    assert.ok(existsSync(site+key),key);
  }
  const html=readFileSync('site/index.html','utf8');
  for(const id of ['introCurtain','gifOverlay','gameView','ScoreCounter','timerText','waveCounter','btnPauseResume','finalScoreContainer','finalActionRow','defendAgainButton','mainMenuButton','gameThanksImage','gameOverImage','playerNameText']){
    assert.ok(html.includes(`id="${id}"`),id);
  }
  assert.ok(!html.includes('DEFEND OUR VILLAGE</p>'));
  assert.ok(!html.includes('popup-panel'));
});
