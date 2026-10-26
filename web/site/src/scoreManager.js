export class ScoreManager {
  constructor(onChange) { this.score=0; this.popups=[]; this.onChange=onChange; }
  add(points,x,y) {
    this.score+=points;
    this.popups.push({x,y,points,red:points<0,life:1000,maxLife:1000});
    this.onChange?.(this.score);
  }
  hit(enemy,targetX,targetY) {
    if (enemy.type===6) {this.add(-1000,enemy.x,enemy.y);return;}
    const base=[100,250,400,100,250,400][enemy.type];
    const distance=Math.hypot(enemy.x-targetX,enemy.y-targetY)/.68;
    const factor=1-Math.max(0,Math.min(1,(distance-100)/700));
    const bonus=Math.trunc(base*0.2*factor);
    const random=Math.floor(Math.random()*10)-5;
    this.add(base+bonus+random,enemy.x,enemy.y);
  }
  stallDamaged(x,y){this.add(-1000,x,y);}
  update(ms){
    for(const p of this.popups){p.life-=ms;p.y-=2*.68*(ms/20);}
    this.popups=this.popups.filter(p=>p.life>0);
  }
  clearPopups(){this.popups=[];}
}
