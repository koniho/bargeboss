package com.bargeboss.game;

public final class ChannelTest {
    private static int checks;
    private static void check(boolean ok,String message) { checks++;if(!ok) throw new AssertionError(message); }
    private static Game course(float w,float h) {
        Game r=new Game();r.configureCanvas(w,h);r.startTutorial();Game g=r.activeGame();
        g.tutorial.lesson=Tutorial.CHANNEL;g.tutorial.setup(g);return r;
    }
    public static void run() {
        geometry();
        bankSideGates();
        for(float[] size:new float[][]{{960,600},{1333,600},{960,720}}) {
            Game root=course(size[0],size[1]),g=root.activeGame();
            Body originalTug=g.tug,originalLoad=g.cargo[0];
            float original=g.layout.camera.worldRight;
            check(g.layout.camera.screenY(TrainingChannel.X[7],TrainingChannel.Y[7])<0
                    && g.layout.camera.screenX(TrainingChannel.X[7],TrainingChannel.Y[7])>g.layout.width,
                    "long winding course continues upward beyond the initial isometric view");
            for(int i=0;i<120;i++)root.step();
            check(g.tutorial.channelGate==1 && g.tutorial.lesson==Tutorial.CHANNEL,"waiting never passes a gate");
            int seen=0;boolean scrolled=false;
            for(int i=0;i<6500 && g.trainingChannel();i++) {
                if(i%20==0) ReplayInput.channel(root);
                boolean touching=g.pointer;RiverCamera c=g.layout.camera;
                float px=g.pointerX,py=g.pointerY;
                float ax=g.downX,ay=g.downY;
                seen|=1<<g.tutorial.channelGate;root.step();
                if(g.trainingChannel()) {
                    check(g.inWater(g.tug.x,g.tug.y),"tug remains within the winding waterway");
                    check(!touching || !g.pointer || Math.abs(g.downX-ax)<.002f
                            && Math.abs(g.downY-ay)<.002f
                            && Math.abs(g.pointerX-px)<.002f
                            && Math.abs(g.pointerY-py)<.002f,"scroll preserves physical touch anchors");
                    scrolled|=g.layout.camera.worldRight>original+700;
                    float x=g.layout.camera.screenX(g.tug.x,g.tug.y),y=g.layout.camera.screenY(g.tug.x,g.tug.y);
                    check(x>100 && x<g.layout.width-100 && y>100 && y<g.layout.height-100,
                            "camera keeps tug clear of every screen edge throughout course");
                }
            }
            check(scrolled && seen==254 && g.tutorial.lesson==Tutorial.CONNECT,"seven ordered gates lead into cargo practice");
            check(g.tug==originalTug && g.cargo[0]==originalLoad && originalLoad.awaitingPickup,
                    "course exit preserves the tug and waiting harbor load");
            check(Math.abs(g.tug.x-TrainingChannel.X[7])<30,
                    "last gate leads into the same harbor without teleporting the tug");
        }
        // Separate short gestures reproduce the native test's lift/coast cadence
        // as well as the continuous steering gesture exercised above.
        Game pulsed=course(960,600),pulsedGame=pulsed.activeGame();int attempts=0;
        while(pulsedGame.trainingChannel() && attempts++<150) {
            ReplayInput.channel(pulsed);
            for(int i=0;i<24 && pulsedGame.trainingChannel();i++) pulsed.step();
            if(pulsedGame.trainingChannel()) ReplayInput.up(pulsed,pulsedGame.pointerX,pulsedGame.pointerY);
            for(int i=0;i<30 && pulsedGame.trainingChannel();i++) pulsed.step();
        }
        check(pulsedGame.tutorial.lesson==Tutorial.CONNECT && pulsedGame.tutorial.channelGate==8,
                "short drag-and-release gestures also complete every right-angle reach within the native attempt budget");
        Game root=course(960,600),g=root.activeGame();
        g.tug.x=TrainingChannel.X[7];g.tug.y=TrainingChannel.Y[7];g.tutorial.update(g);
        check(g.tutorial.lesson==Tutorial.CHANNEL && g.tutorial.channelGate==1,"finish cannot bypass earlier gates");
        root=course(960,600);g=root.activeGame();ReplayInput.throttle(root,0,100);
        for(int i=0;i<300;i++)root.step();
        check(g.tutorial.channelGate==1 && g.inWater(g.tug.x,g.tug.y),"driving into a solid bank cannot shortcut the course");
        root.background();float x=g.tug.x;int ticks=g.ticks;root.advance(1);
        check(g.tug.x==x && g.ticks==ticks,"course pauses on background");
        root.configureCanvas(1333,600);g.pause();ReplayInput.tap(root,Layout.HELP);
        check(g.trainingChannel() && g.tutorial.channelGate==1 && g.tug.x==TrainingChannel.MIN-160,"reset restarts channel and responsive camera");
        check(Parity.frame(root)==Parity.frame(root),"channel rendering has no side effects");
        Game low=course(960,600),high=course(960,600);
        for(int second=0;second<12;second++) {
            ReplayInput.channel(low);ReplayInput.channel(high);
            for(int i=0;i<30;i++) low.advance(1.0/30);
            for(int i=0;i<144;i++) high.advance(1.0/144);
        }
        Game a=low.activeGame(),b=high.activeGame();
        check(a.tug.x==b.tug.x && a.tug.y==b.tug.y && a.tug.angle==b.tug.angle
                && a.layout.camera.worldRight==b.layout.camera.worldRight && a.layout.camera.worldTop==b.layout.camera.worldTop && a.tutorial.channelGate==b.tutorial.channelGate,
                "course physics and camera match at 30 and 144 Hz");
        System.out.println(checks+" winding course, solid banks, camera, gates and lifecycle assertions passed");
    }
    private static void bankSideGates() {
        // Reproduce the screenshot: a player can reach the harbor while the old
        // 23-unit center circle leaves the HUD waiting forever at gate one.
        for(int side:new int[]{-1,1}) {
            Game root=course(1333,600),g=root.activeGame();
            g.tutorial.canal.authorized=true;g.passages.position[0]=BridgePassage.OPEN_TICKS;
            for(int gate=1;gate<TrainingChannel.X.length;gate++) {
                float x=TrainingChannel.X[gate],y=TrainingChannel.Y[gate]+side*36;
                g.cancelInputs();g.tug.x=x-30;g.tug.y=y;g.tug.angle=0;
                g.tug.vx=g.tug.vy=g.tug.spin=0;root.step();
                check(g.tutorial.channelGate==gate,"approaching a gate does not award a later gate");
                g.tug.x=x;root.step();
                check(g.tutorial.channelGate==gate+1,"passing beside either bank counts across the full gate width");
                check(g.tutorial.clearedGate==gate && g.tutorial.gateClearTicks==90,"each crossing triggers its gate burst once");
                long frame=Parity.frame(root);
                check(Parity.frame(root)==frame && g.tutorial.gateClearTicks==90,"drawing a gate burst does not advance it");
                root.step();
                check(g.tutorial.gateClearTicks==89,"remaining at a cleared gate does not restart the effect");
            }
            check(g.tutorial.lesson==Tutorial.CONNECT && g.cargo[0]!=null,
                    "bank-side navigation reaches the harbor and starts barge practice");
            root.background();root.advance(1);
            check(g.tutorial.gateClearTicks==89,"gate celebration pauses with the game");
            g.tutorial.setup(g);
            check(g.tutorial.gateClearTicks==0,"lesson reset clears an old celebration");
        }
        for(int gate=1;gate<TrainingChannel.X.length;gate++) {
            float x=TrainingChannel.X[gate],y=TrainingChannel.Y[gate];
            check(TrainingChannel.reachedGate(gate,x-35,y+36,x+35,y+36),"swept crossing catches a gate between samples");
            check(!TrainingChannel.reachedGate(gate,x-35,y+70,x+35,y+70),"crossing dry land gives no gate credit");
            check(!TrainingChannel.reachedGate(gate,x+35,y,x+50,y),"being beyond a gate is not a crossing");
        }
    }
    private static void geometry() {
        for(int side:new int[]{-1,1}) {
            float[] bank=TrainingChannel.bank(side);
            check(bank[0]==TrainingChannel.MIN && bank[bank.length-2]==TrainingChannel.MAX,"rectilinear banks close against the same course endcaps");
            for(int i=0;i<bank.length-2;i+=2) {
                float dx=bank[i+2]-bank[i],dy=bank[i+3]-bank[i+1];
                check((dx==0 || dy==0) && (dx!=0 || dy!=0),"every quay edge is nondegenerate and axis aligned");
                float length=Physics.length(dx,dy),nx=side*dy/length,ny=-side*dx/length;
                float x=(bank[i]+bank[i+2])*.5f,y=(bank[i+1]+bank[i+3])*.5f;
                check(TrainingChannel.contains(x+nx,y+ny) && !TrainingChannel.contains(x-nx,y-ny),
                        "drawn bank edge separates water from solid land in the exact collision union");
                for(int heading=0;heading<8;heading++) {
                    Body boat=new Body(0,-1,x+nx*5,y+ny*5,heading*(float)Math.PI/4);
                    boat.vx=-nx*40;boat.vy=-ny*40;TrainingChannel.collide(boat);
                    for(int disc=0;disc<3;disc++) for(int sample=0;sample<16;sample++) {
                        double a=sample*Math.PI/8;float radius=boat.radius*.999f;
                        check(TrainingChannel.contains(boat.discX(disc)+radius*(float)StrictMath.cos(a),
                                boat.discY(disc)+radius*(float)StrictMath.sin(a)),
                                "rotated hull discs resolve fully inside rectilinear banks and corners");
                    }
                    check(boat.vx*nx+boat.vy*ny>=-.001f,"bank collision removes velocity into the quay");
                }
            }
        }
        for(int gate=1;gate<TrainingChannel.X.length;gate++) {
            float turn=(TrainingChannel.X[gate-1]+TrainingChannel.X[gate])*.5f;
            float low=Math.min(TrainingChannel.Y[gate-1],TrainingChannel.Y[gate]),high=Math.max(TrainingChannel.Y[gate-1],TrainingChannel.Y[gate]);
            for(float y=low;y<=high;y+=10) check(TrainingChannel.contains(turn,y),"vertical jog joins both horizontal reaches without a seam");
            check(TrainingChannel.contains(TrainingChannel.X[gate],TrainingChannel.Y[gate]),"every ordered gate remains in navigable water");
        }
    }

}
