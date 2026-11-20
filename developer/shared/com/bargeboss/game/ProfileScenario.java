package com.bargeboss.game;

/** Debug-only fixtures reached through real tutorial input; no alternate gameplay implementation. */
public final class ProfileScenario {
    public static final String[] NAMES={"calumet-offload","calumet-bridge","calumet-skyway","indiana-bridges","calumet-inlet","calumet-basin","shared-slip-wide","montrose-raft","montrose-swimmers","tutorial-entrance","seawall-corner","buoy-complete","tow-tight-circle","indiana-harbor","indiana-hick","splash-impact","basin-entry","canal-entry","canal-middle","barge-arrival","barge-push","harbor-close","barge-tow","barge-bridge","montrose-inlet","montrose-docks","montrose-overview","montrose-green-shore"};
    private static final java.util.WeakHashMap<Game,Boolean> CIRCLES=new java.util.WeakHashMap<Game,Boolean>();
    static float circleX(){return IndianaHarbor.START_X;}
    static float circleY(){return IndianaHarbor.START_Y;}
    private ProfileScenario(){}
    private static void bridgeApproach(Game g,LandmarkBridge b,float[][] route){
        float mid=(b.right-b.left)*.5f,tx=b.x+b.dx*mid-b.dy*140,ty=b.y+b.dy*mid+b.dx*140,best=Float.MAX_VALUE;
        for(int i=1;i<route.length;i++){
            float ax=route[i-1][0],ay=route[i-1][1],dx=route[i][0]-ax,dy=route[i][1]-ay;
            float t=Math.max(0,Math.min(1,((tx-ax)*dx+(ty-ay)*dy)/(dx*dx+dy*dy))),x=ax+t*dx,y=ay+t*dy,d=(x-tx)*(x-tx)+(y-ty)*(y-ty);
            if(d<best){best=d;g.tug.x=x;g.tug.y=y;g.tug.angle=(float)StrictMath.atan2(b.y-y,b.x-x);}
        }
    }
    /** Developer-only fixed camera; same anchor and projection on every host. */
    public static void cameraZoom(Game root,float width,float height,float zoom) {
        if(!Float.isFinite(zoom) || zoom<.25f || zoom>5)throw new IllegalArgumentException("Profile zoom requires .25..5");
        Game g=root.activeGame();RiverCamera c=g.layout.camera;float x=width*.5f,y=height*.52f;
        g.layout=Layout.focused(width,height,c.worldX(x,y),c.worldY(x,y),zoom,x,y);
    }
    public static void prepare(Game root,String name,float width,float height) {
        boolean valid=false;for(String n:NAMES)if(n.equals(name))valid=true;
        if(!valid)throw new IllegalArgumentException("Unknown profile scene: "+name);
        CIRCLES.remove(root);
        root.configureCanvas(width,height);
        if(name.startsWith("calumet-") || name.equals("shared-slip-wide")){
            root.challengeId=0;root.reset();
            if(name.equals("shared-slip-wide")){for(Body b:root.cargo)if(b!=null)b.deliveryRecorded=true;root.voyage.step(root);root.tug.x=root.dockX(2);root.tug.y=root.dockY(2)+180;}
            if(name.equals("calumet-offload")){
                Body b=root.cargo[0];for(Body other:root.cargo)if(other!=null && other!=b)other.active=false;
                int dock=b.destination;DockView.Crane c=root.dockCranes[dock];
                b.x=c.pickX;b.y=c.pickY;b.angle=root.voyage.heading(dock);Mooring.attach(b);
                root.tug.x=b.x-root.voyage.normalX(dock)*100;root.tug.y=b.y-root.voyage.normalY(dock)*100;
                root.layout=Layout.focused(width,height,(b.x+c.x)*.5f,(b.y+c.y)*.5f,1.6f,width*.5f,height*.72f);return;
            }
            if(name.equals("calumet-basin")){float[] at=CalumetRiver.ROUTE[CalumetRiver.ROUTE.length-1];root.tug.x=at[0];root.tug.y=at[1];}
            if(name.equals("calumet-bridge") || name.equals("calumet-skyway")){
                LandmarkBridge b=LocationBridges.calumet()[name.equals("calumet-skyway")?5:7];bridgeApproach(root,b,CalumetRiver.ROUTE);
                root.layout=Layout.focused(width,height,b.x,b.y,name.equals("calumet-skyway")?.65f:1.6f,width*.5f,height*.62f);return;
            }
            root.layout=Layout.focused(width,height,root.tug.x,root.tug.y,name.equals("shared-slip-wide")?.65f:1.4f,width*.5f,height*.52f);return;
        }
        if(name.equals("tow-tight-circle")){
            root.selectChallenge(Challenges.INDIANA);root.reset();
            // Open basin isolates turning/tow work from shore and moored-fleet collisions.
            root.tug.x=circleX()+110;root.tug.y=circleY();root.tug.angle=(float)(StrictMath.PI/2);
            Body b=root.cargo[0];b.x=circleX()+110;b.y=circleY()-120;b.angle=root.tug.angle;
            root.layout=Layout.focused(width,height,circleX()+50,circleY()-30,1.4f,width*.5f,height*.52f);
            ReplayInput.down(root,b.x,b.y);ReplayInput.move(root,root.tug.x,root.tug.y);
            ReplayInput.up(root,root.tug.x,root.tug.y);
            for(int i=0;i<90 && !root.tow.connected(b,root.tug);i++)root.step();
            if(!root.tow.connected(b,root.tug))throw new AssertionError("Circle fixture failed to catch tow");
            CIRCLES.put(root,Boolean.TRUE);return;
        }
        if(name.startsWith("indiana-")){
            root.selectChallenge(Challenges.INDIANA);root.reset();
            if(name.equals("indiana-bridges")){LandmarkBridge b=LocationBridges.indiana()[1];bridgeApproach(root,b,IndianaHarbor.ROUTE);root.layout=Layout.focused(width,height,b.x,b.y,1.25f,width*.5f,height*.62f);return;}
            if(name.equals("indiana-hick")){float[] at=IndianaHarbor.ROUTE[IndianaHarbor.ROUTE.length/2];root.tug.x=at[0];root.tug.y=at[1];root.cargo[0].x=at[0]+110;root.cargo[0].y=at[1];}
            root.layout=Layout.focused(width,height,root.tug.x,root.tug.y,1.4f,width*.5f,height*.52f);return;
        }
        if(name.equals("splash-impact")){
            root.resetPracticeYard();for(Feeder f:root.feeders){f.phase=Feeder.IDLE;f.load=null;}
            for(int i=0;i<root.cargo.length;i++)root.cargo[i]=null;
            root.tug.x=270;root.tug.y=500;root.tug.angle=0;root.tug.vx=65;root.addCargo(0,330,500);root.cargo[0].angle=0;
            for(int tick=0;tick<16;tick++)root.step();
            root.layout=Layout.focused(width,height,300,500,3.2f,width*.5f,height*.58f);
            if(root.splashes.emitted==0)throw new AssertionError("Impact fixture missed contact");return;
        }
        if(name.equals("montrose-raft") || name.equals("montrose-swimmers")){
            root.challengeId=Challenges.MONTROSE;root.reset();root.challengeId=name.equals("montrose-raft")?Challenges.RAFT_RESCUE:Challenges.SWIMMER_RESCUE;for(int i=0;i<root.cargo.length;i++)root.cargo[i]=null;root.rescue.setup(root);
            if(name.equals("montrose-swimmers")){root.tug.x=660;root.tug.y=640;root.tug.angle=0;root.layout=Layout.focused(width,height,660,590,3.6f,width*.5f,height*.52f);}
            return;
        }
        if(name.startsWith("montrose-")){
            root.selectChallenge(Challenges.MONTROSE);root.reset();
            if(name.equals("montrose-docks")){
                for(int tick=0;tick<18000;tick++){
                    if(Physics.length(root.tug.x-MontroseHarbor.SLIP_X,root.tug.y-MontroseHarbor.SLIP_Y)<200)break;
                    ReplayInput.work(root);root.step();
                }
            }else for(int tick=0;tick<120;tick++)root.step();
            if(name.equals("montrose-overview"))root.layout=MontroseHarbor.overview(width,height);
            if(name.equals("montrose-green-shore"))root.layout=Layout.focused(width,height,MontroseHarbor.mapX(275),MontroseHarbor.mapY(1250),2.4f,width*.5f,height*.53f);
            return;
        }
        root.startTutorial();Game g=root.activeGame();
        if(name.equals("tutorial-entrance"))return;
        if(name.equals("seawall-corner")){
            g.tutorial.entering=false;g.tutorial.lesson=Tutorial.CHANNEL;g.tutorial.channelGate=1;
            g.tug.x=-1118;g.tug.y=437;g.tug.angle=0;
            g.layout=Layout.focused(width,height,g.tug.x,g.tug.y,2.2f,width*.52f,height*.56f);return;
        }
        int channelTicks=0;
        for(int tick=0;tick<24000;tick++) {
            int lesson=g.tutorial.lesson;
            if(lesson==Tutorial.CHANNEL)channelTicks++;
            if(name.equals("buoy-complete") && g.tutorial.driveMarker==1 && g.tutorial.buoyClearTicks[0]>0 && g.tutorial.buoyClearTicks[0]<=84
                    || name.equals("basin-entry")
                    || name.equals("canal-entry") && channelTicks==1
                    || name.equals("canal-middle") && channelTicks==700
                    || name.equals("barge-arrival") && lesson==Tutorial.HOLD
                    || name.equals("barge-push") && lesson==Tutorial.HOLD && g.pushing && g.manualSteering && Physics.touching(g.tug,g.cargo[0],5)
                    || name.equals("harbor-close") && lesson==Tutorial.STEER
                    || name.equals("barge-tow") && lesson==Tutorial.PULL
                    || name.equals("barge-bridge") && lesson==Tutorial.BRIDGE)return;
            ReplayInput.tutorial(root);root.step();
        }
        throw new IllegalStateException("Profile fixture not reached: "+name);
    }
    public static void replayTick(Game root) {
        Game g=root.activeGame();
        if(CIRCLES.containsKey(root)){
            // Look ahead on a 110-world-unit circle; the real steering and tow solver
            // determine motion. Never reposition bodies or prescribe their angles here.
            double angle=StrictMath.atan2(g.tug.y-circleY(),g.tug.x-circleX())+.65;
            float dx=circleX()+110*(float)StrictMath.cos(angle)-g.tug.x;
            float dy=circleY()+110*(float)StrictMath.sin(angle)-g.tug.y;
            float len=Math.max(1,Physics.length(dx,dy));
            if(!g.pointer)ReplayInput.down(root,g.tug.x,g.tug.y);
            ReplayInput.move(root,g.downX+dx/len*90,g.downY+dy/len*90);
        }else if(g.tutorial!=null && g.tutorial.lesson<Tutorial.DONE)ReplayInput.tutorial(root);
        else if(g.montroseStage())ReplayInput.work(g);
        root.step();
    }
}
