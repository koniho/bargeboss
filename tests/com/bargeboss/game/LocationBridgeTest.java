package com.bargeboss.game;

/** Landmark mechanisms, vessel safety, lifecycle and depth-fragment regression. */
public final class LocationBridgeTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static float distance(float[] a,float[] b){return (float)StrictMath.sqrt((a[0]-b[0])*(a[0]-b[0])+(a[1]-b[1])*(a[1]-b[1])+(a[2]-b[2])*(a[2]-b[2]));}
    public static void run(){
        for(int location=0;location<2;location++){
            Game g=new Game();if(location==1){g.challengeId=Challenges.INDIANA;g.reset();}
            for(Body b:g.cargo)if(b!=null)b.active=false;
            for(int i=0;i<g.passages.bridges.length;i++){
                LandmarkBridge b=g.passages.bridges[i];
                check(b.left+b.right>=25 && b.left+b.right<400,"survey crossing runs across water, not along the river: "+b.id);
                check(b.lamps.length>0,"landmark has named effect anchors");
                check(!b.movable() || b.motionFragments()>0,"part-local motion topology prepared before play");
                for(int leaf:b.leaves){
                    float[] origin=b.pose(0).point(leaf,0,0,0),end=b.pose(0).point(leaf,20,0,0);
                    for(float a:new float[]{0,.25f,.5f,.75f,1}){
                        float[] p=b.pose(a).point(leaf,0,0,0),q=b.pose(a).point(leaf,20,0,0);
                        check(Math.abs(distance(p,q)-distance(origin,end))<.01f,"rigid span never stretches");
                        if(b.type==LandmarkBridge.BASCULE)check(distance(p,origin)<.01f,"hinge fixed throughout motion");
                        if(b.type==LandmarkBridge.LIFT)check(Math.abs(p[0]-origin[0])<.01f && Math.abs(p[1]-origin[1])<.01f,"lift stays registered to tower shafts");
                    }
                }
                for(int a=0;a<b.lamps.length;a++)for(int c=0;c<a;c++)check(!b.lamps[a].id.equals(b.lamps[c].id),"stable unique lighting anchors");
                if(!b.movable()){check((b.type==LandmarkBridge.SKYWAY || b.type==LandmarkBridge.FIXED) && b.height>=180,"Skyway remains high and fixed");continue;}
                // Every simulation tick and fractional phase uses exact rigid transforms, not 33 slots.
                ArticulatedModel.Pose live=b.pose(0);int topology=b.motionFragments();
                int probe=b.leaves[0];float[] previous=live.point(probe,20,0,0);
                for(int tick=1;tick<=BridgePassage.OPEN_TICKS;tick++){
                    float amount=tick/(float)BridgePassage.OPEN_TICKS;
                    check(b.pose(amount)==live,"bounded reusable live pose buffers");
                    float[] current=live.point(probe,20,0,0);
                    check(distance(previous,current)>.001f,"motion advances every tick: "+b.id);
                    previous=current;
                }
                for(float amount:new float[]{.12345f,.5001f,.4999f,.12345f,0,1,0}){
                    ArticulatedModel.Pose actual=b.pose(amount),exact=b.moving.pose(amount);
                    for(int leaf:b.leaves){
                        check(distance(actual.point(leaf,20,3,7),exact.point(leaf,20,3,7))<.001f,"continuous transform agrees with exact model");
                        check(distance(actual.normal(leaf,0,0,1),exact.normal(leaf,0,0,1))<.0001f,"continuous normal agrees with exact model");
                    }
                    for(LandmarkBridge.Lamp lamp:b.lamps)if(lamp.moving)
                        check(distance(actual.point(lamp.node,lamp.x,lamp.y,lamp.z),exact.point(lamp.node,lamp.x,lamp.y,lamp.z))<.001f,"lamp follows exact live transform");
                    check(b.motionFragments()==topology,"no new topology during motion or reversal");
                }
                g.passages=new BridgePassage(g);g.tug.x=b.x-b.dy*250;g.tug.y=b.y+b.dx*250;
                for(int t=0;t<BridgePassage.OPEN_TICKS+BridgePassage.WAIT_TICKS+1;t++)g.passages.step(g);
                check(g.passages.passable(i),"approach opens crossing: "+b.id);
                g.tug.x=b.x;g.tug.y=b.y;g.tug.vx=g.tug.vy=0;
                for(int t=0;t<BridgePassage.HOLD_TICKS+10;t++)g.passages.step(g);
                check(g.passages.passable(i),"occupied crossing cannot close");
                float x=g.tug.x,y=g.tug.y;g.passages.collide(g.tug,null);check(Physics.length(x-g.tug.x,y-g.tug.y)<.01f,"open bridge has no invisible gate");
                g.tug.x=50000;g.tug.y=50000;
                for(int t=0;t<BridgePassage.HOLD_TICKS+BridgePassage.OPEN_TICKS+1;t++)g.passages.step(g);
                check(g.passages.position[i]==0,"empty crossing closes");
                check(g.passages.blocksLine(b.x+b.dy*80,b.y-b.dx*80,b.x-b.dy*80,b.y+b.dx*80),"closed bridges block a tow throw through the deck");
                g.tow.leader=g.tug;g.tug.x=b.x+b.dy*900;g.tug.y=b.y-b.dx*900;
                g.tow.cargo=new Body(999,0,b.x-b.dy*900,b.y+b.dx*900,0);
                check(g.passages.requested(g,i),"long connected tow crossing holds bridge even with both ends far away");g.tow=new Towline();
                for(int k=0;k<g.passages.position.length;k++)if(k!=i)g.passages.position[k]=BridgePassage.OPEN_TICKS;
                g.tug.x=b.x;g.tug.y=b.y;g.tug.angle=(float)StrictMath.atan2(b.dx,-b.dy);g.passages.collide(g.tug,null);
                check(Math.abs(b.across(g.tug.x,g.tug.y))>=b.width*.5f+g.tug.radius-.02f,"closed gate blocks hull: "+b.id+" distance="+b.across(g.tug.x,g.tug.y));
            }
            g.reset();check(g.passages.position[0]==0,"reset owns fresh mechanism state");
            g.mode=Game.PAUSE;int ticks=g.passages.position[0];g.step();check(g.passages.position[0]==ticks,"pause freezes bridge state");
        }
        System.out.println(checks+" geographic bridge rigidity, anchor, request and collision assertions passed");
    }
    public static void main(String[] args){run();}
}
