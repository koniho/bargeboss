package com.bargeboss.game;
public final class CanalLeadTest {
    public static void run(){
        Game root=new Game();root.startTutorial();Game g=root.activeGame();g.tutorial.lesson=Tutorial.CHANNEL;g.tutorial.setup(g);
        g.tutorial.canal.authorized=true;g.passages.position[0]=BridgePassage.OPEN_TICKS;
        // Test the overtaken guide in a straight reach without moving the player.
        Body teacher=g.teacherBoat();teacher.x=-1205;teacher.y=500;teacher.angle=0;teacher.vx=teacher.vy=0;
        g.tug.x=-1135;g.tug.y=500;g.tug.vx=g.tug.vy=0;
        CanalEscort e=g.tutorial.canal.escort;e.leading=true;e.gate=1;
        float px=g.tug.x,py=g.tug.y,maxX=teacher.x;
        for(int i=0;i<900;i++){
            e.step(g,teacher);Physics.collide(teacher,g.tug);maxX=Math.max(maxX,teacher.x);
            if(!g.inWater(teacher.x,teacher.y))throw new AssertionError("Passing guide leaves water");
        }
        if(maxX<=g.tug.x+40)throw new AssertionError("Guide remained behind stationary player: "+maxX);
        if(g.tug.x!=px || g.tug.y!=py)throw new AssertionError("Guide must not teleport/displace player");
        System.out.println("Overtaken canal instructor recovers lead without moving player");
    }
    public static void main(String[] args){run();}
}
