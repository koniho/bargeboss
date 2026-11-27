package com.bargeboss.game;

/** Debug-only fixtures reached through real tutorial input; no alternate gameplay implementation. */
public final class ProfileScenario {
    public static final String[] NAMES={"montrose-powerup-review","montrose-markers-review","montrose-boost-review","damage-sink-review","damage-impact-review","montrose-pavilion-study","montrose-concession-study","islands-cottage-study","islands-channel-study","islands-lift-study","training-spotlight","montrose-tow-one","montrose-tow-two","sink-demo","montrose-chas","montrose-storm-return","montrose-racer","montrose-police","montrose-police-radio","montrose-arrival","land-bridge-lit","land-workers-lit","montrose-moorings-lit","montrose-swimmers-lit","montrose-moorings-day","montrose-moorings-dusk","montrose-bimini-day","montrose-bimini-dusk","montrose-bimini-night","montrose-dock-i","montrose-dispatch","montrose-haulout","montrose-lift-lowering","montrose-lift-carry","montrose-lift-water","tutorial-fireworks","tutorial-firefight","seawall-audio","barge-abilities","instructor-teach","instructor-call","instructor-choice","tutorial-yard","calumet-offload","calumet-bridge","calumet-skyway","indiana-bridges","calumet-inlet","calumet-basin","shared-slip-wide","montrose-raft","montrose-swimmers","tutorial-entrance","canal-radio","canal-operator","canal-opening","canal-follow","canal-milestone","seawall-corner","buoy-complete","tow-tight-circle","indiana-harbor","indiana-hick","chicago-start","chicago-downtown","chicago-wolf","splash-impact","basin-entry","canal-entry","canal-middle","barge-arrival","barge-push","harbor-close","barge-tow","barge-bridge","montrose-inlet","montrose-lights","montrose-docks","montrose-overview","montrose-green-shore","montrose-beach","montrose-beacon","montrose-marina","towing-challenge","towing-challenge-wide","tutorial-six-fires"};
    private static final java.util.WeakHashMap<Game,Boolean> RECEIVERS=new java.util.WeakHashMap<Game,Boolean>();
    private static final java.util.WeakHashMap<Game,Boolean> CIRCLES=new java.util.WeakHashMap<Game,Boolean>();
    private static final java.util.WeakHashMap<Game,Boolean> MARINA_TOWS=new java.util.WeakHashMap<Game,Boolean>();
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
        g.changeCamera(Layout.focused(width,height,c.worldX(x,y),c.worldY(x,y),zoom,x,y));
    }
    public static void prepare(Game root,String name,float width,float height) {
        if(name.equals("montrose-markers-review") || name.equals("montrose-boost-review") || name.equals("montrose-powerup-review")){
            root.configureCanvas(width,height);root.selectChallenge(Challenges.MONTROSE);root.reset();root.montroseArrival.clear();root.dialogue.close(root);root.changeLighting(2);
            float x=MontroseHarbor.mapX(453),y=MontroseHarbor.mapY(1274),zoom=1.6f;
            if(name.equals("montrose-boost-review")){int k=3;x=MontroseTransit.BUOYS[k*2];y=MontroseTransit.BUOYS[k*2+1];root.tug.x=x;root.tug.y=y;root.tug.vx=90;root.tug.angle=0;root.step();zoom=2.1f;}
            if(name.equals("montrose-powerup-review")){int k=3;x=MontroseTransit.BUOYS[k*2];y=MontroseTransit.BUOYS[k*2+1];root.tug.x=x+90;root.tug.y=y+20;root.tug.angle=0;root.visualTicks=67;zoom=2.1f;}
            root.layout=Layout.focused(width,height,x,y,zoom,width*.5f,height*.58f);RECEIVERS.put(root,Boolean.TRUE);return;
        }
        if(name.equals("damage-sink-review") || name.equals("damage-impact-review")){
            root.configureCanvas(width,height);root.resetPracticeYard();for(Feeder f:root.feeders){f.phase=Feeder.IDLE;f.load=null;}
            for(int i=0;i<root.cargo.length;i++)root.cargo[i]=null;
            root.tug.x=260;root.tug.y=500;root.addCargo(0,330,500);Body b=root.cargo[0];b.angle=0;
            if(name.equals("damage-sink-review")){b.health=1;root.ticks+=HullDamage.COOLDOWN;HullDamage.contact(root,root.tug,b,100);}
            root.splashes.contact(root.tug,b,300,500,100,0,0,0,100);root.changeLighting(2);
            root.layout=Layout.focused(width,height,315,500,2.4f,width*.5f,height*.6f);return;
        }
        if(name.endsWith("-study")){
            boolean islands=name.startsWith("islands-");root.configureCanvas(width,height);
            root.selectChallenge(islands?Challenges.ISLAND_TAG:Challenges.MONTROSE);root.reset();root.dialogue.close(root);
            float x,y,zoom;
            if(islands){boolean cottage=name.equals("islands-cottage-study");x=cottage?2200:1090;y=cottage?330:840;zoom=cottage?2.1f:1.15f;
                root.tug.x=x-140;root.tug.y=y+160;
                if(name.equals("islands-lift-study")){root.tug.x=root.arena.course.gateX(1);root.tug.y=root.arena.course.gateY(1)+65;}
            }else{root.montroseArrival.clear();boolean pavilion=name.equals("montrose-pavilion-study");
                x=MontroseHarbor.mapX(pavilion?1080:1280);y=MontroseHarbor.mapY(pavilion?-545:-465);zoom=pavilion?2.2f:2.8f;
                root.tug.x=x+150;root.tug.y=y-180;
            }
            root.changeLighting(2);root.visualTicks=120;root.layout=Layout.focused(width,height,x,y,zoom,width*.5f,height*.57f);return;
        }

        boolean valid=false;for(String n:NAMES)if(n.equals(name))valid=true;
        if(!valid)throw new IllegalArgumentException("Unknown profile scene: "+name);
        if(name.equals("sink-demo")){
            // Health bars and the sinking sequence: a towed cruiser on its last hit takes a hard contact.
            prepare(root,"montrose-tow-two",width,height);Game g=root.activeGame();
            Body b=null;for(Body c:g.cargo)if(c!=null && c.active)b=c;
            if(b==null)throw new IllegalStateException("sink-demo needs a cruiser");
            g.hull=20;b.health=HullDamage.percent(100,HullDamage.hp(b));g.ticks+=HullDamage.COOLDOWN;
            g.splashes.contact(g.tug,b,(g.tug.x+b.x)*.5f,(g.tug.y+b.y)*.5f,100,0,0,0,100);return;
        }
        if(name.startsWith("land-")){
            boolean bridge=name.equals("land-bridge-lit");prepare(root,bridge?"canal-opening":"tutorial-firefight",width,height);
            Game g=root.activeGame();g.changeLighting(3);g.abilities.cancel();
            if(bridge){LandmarkBridge b=g.passages.bridges[0];g.layout=Layout.focused(width,height,b.x,b.y,1.7f,width*.5f,height*.65f);}
            else {float[] a=new float[2];TrainingYardCrew.position(TrainingYardCrew.count()-4,a);g.layout=Layout.focused(width,height,a[0],a[1],2.2f,width*.5f,height*.58f);}
            RECEIVERS.put(root,Boolean.TRUE);return;
        }
        if(name.equals("seawall-audio")){
            root.configureCanvas(width,height);root.resetPracticeYard();
            root.tug.x=-1600;root.tug.y=TrainingChannel.BASIN_TOP+root.tug.half+root.tug.radius+16;
            root.tug.angle=-(float)Math.PI/2;root.tug.vy=-60;
            root.layout=Layout.focused(width,height,root.tug.x,root.tug.y,1.6f,width*.5f,height*.52f);return;
        }
        CIRCLES.remove(root);RECEIVERS.remove(root);
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
        if(name.equals("montrose-tow-one") || name.equals("montrose-tow-two")){
            root.selectChallenge(Challenges.MONTROSE);root.reset();root.montroseArrival.clear();root.challengeId=Challenges.RAFT_RESCUE;
            for(int i=0;i<root.cargo.length;i++)root.cargo[i]=null;
            root.rescue.setup(root);root.rescue.rafted=false;root.dialogue.close(root);
            root.tug.x=MontroseHarbor.mapX(1100);root.tug.y=MontroseHarbor.mapY(550);root.tug.angle=(float)StrictMath.PI;
            for(int i=0;i<2;i++){Body b=root.cargo[i];b.x=root.tug.x+90;b.y=root.tug.y+(i==0?-30:30);b.angle=root.tug.angle;
                b.tiedX=b.x;b.tiedY=b.y;b.tiedAngle=b.angle;
                if(i==0 || name.endsWith("two")){if(!root.tow.attach(root,b,root.tug))throw new AssertionError("Marina tow fixture failed");}}
            root.layout=Layout.focused(width,height,root.tug.x-70,root.tug.y,2.4f,width*.5f,height*.52f);
            MARINA_TOWS.put(root,true);return;
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
        if(name.startsWith("chicago-")){
            // Navy Pier start (fire, lock approach), downtown towers at Michigan Ave, and the Wolf Point confluence.
            root.selectChallenge(Challenges.CHICAGO);root.reset();
            if(name.equals("chicago-downtown")){root.tug.x=ChicagoRiver.north(41.88855);root.tug.y=ChicagoRiver.east(-87.62550);root.tug.angle=(float)(StrictMath.PI/2);}
            if(name.equals("chicago-wolf")){root.tug.x=ChicagoRiver.north(41.88690);root.tug.y=ChicagoRiver.east(-87.63700);root.tug.angle=-(float)(StrictMath.PI/2);}
            root.layout=Layout.focused(width,height,root.tug.x,root.tug.y,Game.TRANSIT_ZOOM,width*.5f,height*.52f);return;
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
        if(name.equals("montrose-chas") || name.equals("montrose-storm-return")){
            root.challengeId=Challenges.MONTROSE;root.reset();root.montroseArrival.clear();root.challengeId=Challenges.RAFT_RESCUE;
            for(int i=0;i<root.cargo.length;i++)root.cargo[i]=null;root.rescue.setup(root);root.rescue.distress(root);
            if(name.equals("montrose-storm-return")){root.dialogue.close(root);root.rescue.rafted=false;
                root.weather.eveningTicks=StageWeather.EVENING_TICKS;root.weather.stormTicks=StageWeather.STORM_TICKS;root.weather.towTicks=StageWeather.TOW_TICKS;
                root.tow.attach(root,root.cargo[0],root.tug);}
            root.layout=Layout.focused(width,height,MontroseRescue.RAFT_X-70,MontroseRescue.RAFT_Y+20,2.4f,width*.5f,height*.55f);return;
        }
        if(name.equals("montrose-raft") || name.equals("montrose-swimmers") || name.equals("montrose-swimmers-lit")){
            root.challengeId=Challenges.MONTROSE;root.reset();root.montroseArrival.clear();root.challengeId=name.equals("montrose-raft")?Challenges.RAFT_RESCUE:Challenges.SWIMMER_RESCUE;for(int i=0;i<root.cargo.length;i++)root.cargo[i]=null;root.rescue.setup(root);root.dialogue.close(root);
            if(name.startsWith("montrose-swimmers")){
                Body barge=root.rescue.barge.boat;barge.x=MontroseHarbor.mapX(1300);barge.y=MontroseHarbor.mapY(-1600);barge.angle=0;barge.awaitingPickup=false;
                root.tug.x=barge.x-90;root.tug.y=barge.y+100;root.tug.angle=-.9f;root.rescue.barge.towed=true;
                root.spotlight.aim(root,-.9f);
                for(int i=0;i<MontroseRescue.PEOPLE;i++){root.rescue.found[i]=true;
                    if(i<12){root.rescue.state[i]=4;root.delivered++;}
                    else{root.rescue.x[i]=barge.x+40+(i-12)*16;root.rescue.y[i]=barge.y-100;}}
                root.rescue.barge.step(root);root.rescue.barge.launch(root,root.rescue.x[12],root.rescue.y[12]);
                root.layout=Layout.focused(width,height,barge.x,barge.y-30,2.8f,width*.48f,height*.55f);
            }
            return;
        }
        if(name.startsWith("montrose-police")){
            root.selectChallenge(Challenges.MONTROSE);root.reset();root.montroseArrival.clear();root.challengeId=Challenges.WAKE_PATROL;
            for(int i=0;i<root.cargo.length;i++)root.cargo[i]=null;
            root.job=MontroseJob.create(Challenges.WAKE_PATROL);root.job.setup(root);root.dialogue.close(root);
            Body b=((WakePatrolJob)root.job).police;root.tug.x=b.x-65;root.tug.y=b.y+65;
            root.changeLighting(2);root.layout=Layout.focused(width,height,b.x,b.y,3,width*.5f,height*.56f);
            if(name.endsWith("radio")){WakePatrolJob job=(WakePatrolJob)root.job;job.radio.lassoed(0);for(int i=0;i<24;i++){root.radio.step();job.radio.step(root);}}
            return;
        }
        if(name.startsWith("montrose-")){
            root.selectChallenge(Challenges.MONTROSE);root.reset();
            if(name.equals("montrose-arrival"))return;
            root.montroseArrival.clear();root.mission.setup(root);
            if(name.equals("montrose-dispatch")){MontroseDockmaster.dispatch(root);for(int i=0;i<24;i++)root.step();return;}
            root.dialogue.close(root);
            if(name.equals("montrose-racer")){Body b=root.cargo[0];root.tug.x=b.x-50;root.tug.y=b.y+70;
                root.layout=Layout.focused(width,height,b.x,b.y,3.5f,width*.5f,height*.55f);return;}
            if(name.startsWith("montrose-moorings-")){
                root.tug.x=MontroseHarbor.mapX(350);root.tug.y=MontroseHarbor.mapY(650);
                root.changeLighting(name.endsWith("day")?1:name.endsWith("lit")?3:2);
                root.layout=Layout.focused(width,height,MontroseHarbor.mapX(285),MontroseHarbor.mapY(565),2.1f,width*.5f,height*.52f);
                if(name.endsWith("lit")){float[] m=MontroseHarbor.MOORINGS[8];root.tug.x=m[0]+35;root.tug.y=m[1]+35;RECEIVERS.put(root,true);
                    root.layout=Layout.focused(width,height,m[0],m[1],4,width*.5f,height*.42f);}
                return;
            }
            if(name.startsWith("montrose-bimini-")){
                root.changeLighting(name.endsWith("day")?1:name.endsWith("night")?3:2);
                root.layout=Layout.focused(width,height,MontroseHarbor.mapX(1080),MontroseHarbor.mapY(920),2.6f,width*.5f,height*.52f);return;
            }
            if(name.equals("montrose-dock-i")){
                root.layout=Layout.focused(width,height,MontroseHarbor.mapX(1642),MontroseHarbor.mapY(240),2.3f,width*.5f,height*.58f);return;
            }
            if(name.equals("montrose-haulout") || name.startsWith("montrose-lift-")){
                if(name.startsWith("montrose-lift-")){
                    for(int i=0;i<150 && !root.haulout.requested;i++){ReplayInput.work(root);root.step();}
                    if(name.equals("montrose-lift-water"))while(root.haulout.liftTicks<175)root.step();
                    if(name.equals("montrose-lift-carry")){
                        root.dialogue.close(root);root.haulout.liftTicks=MontroseHaulout.LOWER_TICKS;root.haulout.rig.update(1);
                        Body boat=root.cargo[0];boat.x=boat.tiedX=MontroseHaulout.X;boat.y=boat.tiedY=MontroseHaulout.Y;boat.angle=boat.tiedAngle=MontroseHaulout.ANGLE;boat.vx=boat.vy=boat.spin=0;
                        root.haulout.towed=root.haulout.pushed=root.haulout.warping=true;root.haulout.settled=59;root.haulout.lineCrew.start(boat);root.haulout.lineCrew.age=DockLineCrew.CATCH;root.haulout.step(root);
                        for(int i=0;i<MontroseHaulout.HOIST_TICKS;i++)root.haulout.step(root);root.step();root.dialogue.close(root);
                    }
                }
                root.layout=Layout.focused(width,height,MontroseHaulout.X,MontroseHarbor.mapY(240),2.4f,width*.5f,height*.56f);return;
            }
            root.challengeId=Challenges.MONTROSE;root.mission.setup(root);
            if(name.equals("montrose-lights")){
                root.tug.x=MontroseHarbor.mapX(500);root.tug.y=MontroseHarbor.mapY(1320);root.visualTicks=15;
                root.layout=Layout.focused(width,height,MontroseHarbor.mapX(458),MontroseHarbor.mapY(1274),1.4f,width*.5f,height*.52f);return;
            }
            if(name.equals("montrose-docks")){
                for(int tick=0;tick<18000;tick++){
                    if(Physics.length(root.tug.x-MontroseHarbor.SLIP_X,root.tug.y-MontroseHarbor.SLIP_Y)<200)break;
                    ReplayInput.work(root);root.step();
                }
            }else for(int tick=0;tick<120;tick++)root.step();
            if(name.equals("montrose-beach") || name.equals("montrose-beacon")){
                boolean beacon=name.equals("montrose-beacon");float mx=beacon?2200:1600,my=beacon?-1380:-600;
                root.tug.x=MontroseHarbor.mapX(beacon?2400:1700);root.tug.y=MontroseHarbor.mapY(beacon?-1400:-850);
                root.layout=Layout.focused(width,height,MontroseHarbor.mapX(mx),MontroseHarbor.mapY(my),beacon?1.1f:.55f,width*.5f,height*.52f);return;
            }
            if(name.equals("montrose-overview") || name.equals("montrose-marina"))root.layout=MontroseHarbor.overview(width,height,!name.equals("montrose-marina"));
            if(name.equals("montrose-green-shore"))root.layout=Layout.focused(width,height,MontroseHarbor.mapX(275),MontroseHarbor.mapY(1250),2.4f,width*.5f,height*.53f);
            return;
        }
        root.startTutorial();Game g=root.activeGame();
        if(name.equals("training-spotlight")){
            g.tutorial.lesson=Tutorial.MOVE;g.tutorial.setup(g);g.tutorial.entering=false;g.dialogue.close(g);
            g.tug.x=-1500;g.tug.y=600;g.tug.angle=(float)Math.PI;g.spotlight.aim(g,g.tug.angle);
            g.layout=Layout.focused(width,height,g.tug.x,g.tug.y,2.4f,width*.56f,height*.36f);
            g.abilities.reveal(g,g.tug);for(int i=0;i<20;i++)g.abilities.step(g);return;
        }

        if(name.equals("tutorial-six-fires")){
            // Worst case for the extinguish phase: all six fires burning while the monitor sprays,
            // with the tow moved north between the east and north quays so the live camera frames
            // four burning piles and every group's red wash and smoke column reaches the screen.
            prepare(root,"tutorial-firefight",width,height);g=root.activeGame();
            Body barge=g.cargo[3];float dy=-1300-barge.y;
            barge.y+=dy;barge.tiedY+=dy;g.tug.y+=dy;g.fitTutorialCamera();return;
        }
        if(name.equals("tutorial-fireworks") || name.equals("tutorial-firefight") || name.equals("barge-abilities")){
            boolean spray=name.equals("tutorial-firefight") || name.equals("barge-abilities");
            g.tutorial.lesson=spray?Tutorial.EXTINGUISH:Tutorial.SHOW;g.tutorial.setup(g);
            for(int i=0;i<100;i++)g.step();
            for(int i=0;i<(spray?80:90);i++){ReplayInput.abilityLesson(root);root.step();}
            return;
        }
        if(name.startsWith("instructor-")){
            for(int i=0;i<3000 && !g.tutorial.instructorRadio.teach;i++){ReplayInput.tutorial(root);root.step();}
            if(!g.tutorial.instructorRadio.teach)throw new IllegalStateException("Radio teaching not reached");
            for(int i=0;i<90;i++)root.step();
            if(name.equals("instructor-teach"))return;
            InstructorRadio radio=g.tutorial.instructorRadio;ReplayInput.tap(root,radio.x(g),radio.y(g));
            for(int i=0;i<(name.equals("instructor-call")?35:100);i++)root.step();return;
        }
        if(name.equals("tutorial-yard")){
            g.tutorial.lesson=Tutorial.HOLD;g.tutorial.setup(g);g.visualTicks=180;
            g.layout=Layout.focused(width,height,-800,160,1.4f,width*.5f,height*.65f);return;
        }
        if(name.equals("tutorial-entrance"))return;
        if(name.equals("towing-challenge") || name.equals("towing-challenge-wide")){
            // Twenty seconds into a real run: public lasso/tow/tie gestures, ten barges in the basin.
            g.tutorial.lesson=Tutorial.CHALLENGE;g.tutorial.setup(g);g.dialogue.close(g);
            for(int i=0;i<3000 && g.tutorial.challenge.remaining>TowingChallenge.TICKS-20*60;i++){ReplayInput.tutorial(root);root.step();}
            if(!g.tutorial.challenge.running())throw new IllegalStateException("Towing challenge fixture not reached");
            // Wide: every challenge barge, both quays and the far berths in one view.
            if(name.equals("towing-challenge-wide"))g.layout=TrainingScenery.constrain(Layout.focused(width,height,
                    (TowingChallenge.SPAWN_X+TowingChallenge.BERTH_X)*.5f,(TowingChallenge.rowY(0)+TowingChallenge.rowY(TowingChallenge.BARGES-1))*.5f,.65f,width*.5f,height*.52f));
            return;
        }
        if(name.equals("canal-radio") || name.equals("canal-operator") || name.equals("canal-opening") || name.equals("canal-follow") || name.equals("canal-milestone")){
            g.tutorial.lesson=Tutorial.CHANNEL;g.tutorial.setup(g);
            for(int i=0;i<90;i++)root.step();
            if(name.equals("canal-radio"))return;
            TutorialCanalBridge b=g.tutorial.canal;ReplayInput.tap(root,b.houseX(g),b.houseY(g));
            for(int i=0;i<140;i++)root.step();
            if(name.equals("canal-operator"))return;
            Layout.Box choice=g.dialogue.choiceBox(g,0);ReplayInput.tap(root,choice.centerX(),choice.centerY());
            for(int i=0;i<1400;i++){
                root.step();
                if(name.equals("canal-opening") && g.passages.position[0]==150)return;
                if(name.equals("canal-follow") && b.escort.leading && b.escort.radioTicks==150)return;
                if(name.equals("canal-milestone") && b.escort.waiting){for(int j=0;j<90;j++)root.step();return;}
            }
            throw new IllegalStateException("Canal radio fixture not reached: "+name);
        }
        if(name.equals("seawall-corner")){
            g.tutorial.entering=false;g.tutorial.lesson=Tutorial.CHANNEL;g.tutorial.channelGate=1;
            g.tug.x=-1118;g.tug.y=437;g.tug.angle=0;
            g.layout=Layout.focused(width,height,g.tug.x,g.tug.y,2.2f,width*.52f,height*.56f);return;
        }
        int channelTicks=0;
        for(int tick=0;tick<27000;tick++) {
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
        if(RECEIVERS.containsKey(root)){root.step();return;}
        if(MARINA_TOWS.containsKey(root)){ReplayInput.throttle(root,-70,0);root.step();return;}
        if(CIRCLES.containsKey(root)){
            // Look ahead on a 110-world-unit circle; the real steering and tow solver
            // determine motion. Never reposition bodies or prescribe their angles here.
            double angle=StrictMath.atan2(g.tug.y-circleY(),g.tug.x-circleX())+.65;
            float dx=circleX()+110*(float)StrictMath.cos(angle)-g.tug.x;
            float dy=circleY()+110*(float)StrictMath.sin(angle)-g.tug.y;
            float len=Math.max(1,Physics.length(dx,dy));
            ReplayInput.throttle(root,dx/len*90,dy/len*90);
        }else if(g.tutorial!=null && g.tutorial.lesson<Tutorial.DONE)ReplayInput.tutorial(root);
        else if(g.montroseStage())ReplayInput.work(g);
        root.step();
    }
}
