package com.bargeboss.game;

/** The entire support footprint must lie on the permanent map, through every crew gesture. */
public final class DockWorkersTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void run() {
        for(int stage=0;stage<Challenges.COUNT;stage++) {
            Game g=new Game();g.challengeId=stage;g.reset();g.configureCanvas(1333,750);
            for(int id=0;id<6;id++) {
                float[] at=new float[2];DockWorkers.position(g,id,at);
                // Independent dense support samples catch water at both ends of each boot/apron.
                for(int dx=-7;dx<=7;dx++)for(int dy=-7;dy<=7;dy++)
                    check(!CalumetMap.contains(at[0]+dx,at[1]+dy),"crew support intersects actual water stage="+stage+" id="+id);
                for(int tick:new int[]{0,23,120,234,711}) {
                    g.visualTicks=tick;g.celebrationTicks=60;g.celebrationY=g.dockY(id/2);
                    float[] moved=new float[2];DockWorkers.position(g,id,moved);
                    check(at[0]==moved[0] && at[1]==moved[1],"gesture cannot translate boots off the quay");
                    float expected=g.layout.camera.screenY(at[0],at[1])-18*g.layout.camera.scale;
                    check(Math.abs(DockWorkers.footY(g,id)-expected)<.001f,"feet project to actual raised ground plane");
                }
                DockWorkers.shoreAnchor(g,id/2,0,16,at);
                for(int dx=-16;dx<=16;dx+=2)for(int dy=-16;dy<=16;dy+=2)
                    check(!CalumetMap.contains(at[0]+dx,at[1]+dy),"crane base support stays dry");
            }
            int[] ids=DockWorkers.ordered(g);float last=-Float.MAX_VALUE;
            for(int id:ids){check(g.berthDisplayEnabled(id/2),"only active berth crew draw");float d=DockWorkers.depth(g,id);check(d>=last,"crew order follows unraised ground depth");last=d;}
            if(g.montroseStage())check(ids.length==0,"no Calumet crew in marina");
        }
        System.out.println("DockWorkersTest: "+checks+" checks");
    }
    public static void main(String[] args){run();}
}
