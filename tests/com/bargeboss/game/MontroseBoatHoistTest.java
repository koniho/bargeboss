package com.bargeboss.game;
/** Placement and rigid-anchor regression for the scenery-only haulout crane. */
public final class MontroseBoatHoistTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static final class Capture implements Painter {
        int faces;boolean finite=true;
        public void poly(float[] q,int c){faces++;for(float v:q)finite&=Float.isFinite(v);}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float blend,float alpha){}
        public void rect(float x,float y,float w,float h,int c){}public void circle(float x,float y,float r,int c){}public void line(float x,float y,float xx,float yy,float w,int c){}
    }
    public static void run(){ArticulatedModel model=MontroseBoatHoist.MODEL;
        int boom=model.nodeId(MontroseBoatHoist.BOOM_ID),lift=model.nodeId(MontroseBoatHoist.LIFT_ID);
        check(boom>=0 && lift>=0 && model.parent(lift)==boom,"Lifting anchor inherits separate rigid boom");
        check(model.nodeCount()==3 && model.sourceFaceCount()<300,"Bounded static crane geometry");
        for(int x=-1;x<=1;x+=2)for(int y=-1;y<=1;y+=2)check(!MontroseHarbor.openWater(MontroseBoatHoist.X+x*11,MontroseBoatHoist.Y+y*11),"Crane footing stays entirely on land");
        float hx=MontroseBoatHoist.X,hy=MontroseBoatHoist.Y;float[] footprint={hx-11,hy-11,hx+11,hy-11,hx+11,hy+11,hx-11,hy+11};
        boolean clear=true;for(float[] walkway:MontroseShoreArt.PROMENADE)if(Math.abs(PolygonWater.area(PolygonWater.clip(footprint,walkway)))>.01f)clear=false;
        check(clear,"Hoist footing does not obstruct continuous promenade");
        check(MontroseBoatHoist.MAP_X<823 && MontroseBoatHoist.MAP_X>725,"Crane lies between launch area and Dock O");
        ArticulatedModel.Pose still=model.pose(0),raised=model.pose(.5f);
        float[] first=still.point(lift,1.5f,0,-42),second=raised.point(lift,1.5f,0,-42);
        check(Physics.length(second[0]-first[0],second[1]-first[1])>1 && Math.abs(second[2]-first[2])>1,"Future boom articulation transforms lifting anchor");
        float[] anchor=MontroseBoatHoist.liftingAnchor();for(int i=0;i<3;i++)check(anchor[i]==first[i],"Public anchor uses visible static pose");
        check(MontroseHarbor.openWater(anchor[0],anchor[1]),"Hook reaches launch water without changing its collision geometry");
        Game g=new Game();g.challengeId=Challenges.MONTROSE;g.reset();g.layout=Layout.focused(1333,750,MontroseBoatHoist.X,MontroseBoatHoist.Y,2,666,400);
        float x=g.tug.x,y=g.tug.y;int ticks=g.visualTicks,score=g.score;Capture capture=new Capture();MontroseBoatHoist.draw(g,capture);
        check(capture.faces>0 && capture.finite,"Crane renders finite shared geometry");check(g.tug.x==x && g.tug.y==y && g.visualTicks==ticks && g.score==score,"Scenery drawing leaves gameplay unchanged");
        check(MontroseBoatHoist.depth(g)==g.layout.camera.screenY(MontroseBoatHoist.X,MontroseBoatHoist.Y),"Hoist merges at unraised ground depth");
        g.challengeId=0;Capture elsewhere=new Capture();MontroseBoatHoist.draw(g,elsewhere);check(elsewhere.faces==0,"Hoist is Montrose-only");
        System.out.println(checks+" Montrose boat-hoist placement, rigid-anchor and render checks passed");
    }
    public static void main(String[] args){run();}
}
