package com.bargeboss.game;

/** Every catalog level and practice use the same rigid rig on a dry raised footing. */
public final class DockCraneTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void run(){
        int count=0;
        for(int level=-1;level<Challenges.COUNT;level++){
            Game root=new Game();Game g=root;
            if(level<0){root.startTutorial();g=root.activeGame();}else{g.challengeId=level;g.reset();}
            if(g.montroseStage()){check(DockView.ordered(g).length==0,"Montrose keeps its surveyed service crane");continue;}
            for(int berth=0;berth<3;berth++)if(g.berthDisplayEnabled(berth)){
                count++;DockView.Crane c=g.dockCranes[berth];
                check(c.model.nodeCount()==MontroseBoatHoist.MODEL.nodeCount(),"Same rigid crane nodes in level "+level);
                check(c.model.sourceFaceCount()<350 && c.motion.fragmentCount()>0,"Bounded prepared cargo crane geometry in level "+level);
                check(c.model.parent(c.model.nodeId(MontroseBoatHoist.LIFT_ID))==c.model.nodeId(MontroseBoatHoist.BOOM_ID),"Hook parent is rigid boom");
                for(int x=-1;x<=1;x+=2)for(int y=-1;y<=1;y+=2){float xx=c.x+x*16,yy=c.y+y*16;check(g.indianaStage()?!IndianaHarbor.contains(xx,yy):!CalumetMap.contains(xx,yy),"Footing dry level "+level);}
                g.layout=Layout.focused(960,600,c.x,c.y,2,480,350);
                boolean found=false;float previous=-Float.MAX_VALUE;
                for(int id:DockView.ordered(g)){found|=id==berth;float d=DockView.depth(g,id);check(d>=previous,"Crane world depth order");previous=d;}
                check(found,"Enabled berth has crane in level "+level);
                CargoTransfer.Pose state=new CargoTransfer.Pose(g,c,berth);check(Math.abs(DockView.depth(g,berth)-g.layout.camera.screenY(state.hookX(),state.hookY())-.04f)<.001f,"Hook depth uses ground anchor, not height");
            }
        }
        check(count>=8,"Cover all cargo berth variants");
        System.out.println("Shared Montrose crane placement checked across practice and all "+Challenges.COUNT+" levels ("+count+" cargo berths)");
    }
}
