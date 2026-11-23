package com.bargeboss.game;

/** Public radio gestures, physical mouth gate, continuous lift and instructor milestone waits. */
public final class CanalRadioTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Game scene(float width){Game root=new Game();root.configureCanvas(width,600);root.startTutorial();Game g=root.activeGame();g.tutorial.lesson=Tutorial.CHANNEL;g.tutorial.setup(g);return root;}
    private static void ticks(Game g,int n){for(int i=0;i<n;i++)g.step();}
    private static void contact(Game root){Game g=root.activeGame();TutorialCanalBridge b=g.tutorial.canal;
        check(b.contactable(g),"bridge house is visible and available at staging point");
        root.pointerDown(73,b.houseX(g),b.houseY(g));root.pointerUp(73,b.houseX(g),b.houseY(g));
    }
    private static void answer(Game root,int choice){Game g=root.activeGame();check(g.dialogue.ready(),"operator waits for a real reply");Layout.Box b=g.dialogue.choiceBox(g,choice);root.pointerDown(81,b.centerX(),b.centerY());root.pointerUp(81,b.centerX(),b.centerY());ReplayInput.finishMenu(root);}
    public static void run(){
        for(float width:new float[]{960,1333,1600}){
            Game root=scene(width),g=root.activeGame();TutorialCanalBridge b=g.tutorial.canal;
            ticks(root,720);check(!b.authorized && g.passages.position[0]==0,"approach and waiting never open the mouth bridge");
            Body hull=new Body(0,-1,TutorialCanalBridge.X-15,TutorialCanalBridge.Y,0);hull.vx=80;
            g.passages.collide(hull,null);check(hull.x<TutorialCanalBridge.X-30 && hull.vx<=0,"closed span physically blocks a tug");
            float sx=b.houseX(g),sy=b.houseY(g);
            root.pointerDown(73,sx,sy);root.pointerCancel(73);root.pointerUp(73,sx,sy);check(b.phase==0,"cancelled house tap cannot transmit");
            root.pointerDown(73,sx,sy);root.pointerMove(73,sx+100,sy+100);root.pointerUp(73,sx,sy);check(b.phase==0,"house drag does not count as radio tap");
            root.pointerDown(73,sx,sy);root.pointerUp(74,sx,sy);check(b.phase==0,"second finger cannot transmit another finger's call");root.pointerCancel(73);
            contact(root);check(b.phase==TutorialCanalBridge.CALLING && !g.pointer && !g.navigating,"house tap captures input without steering");
            check(b.content(g).waiting && b.content(g).header.startsWith("YOU"),"outgoing call identifies player and waits with star");
            int tick=b.ticks;g.background();ticks(root,100);check(b.ticks==tick,"pause freezes pending operator response");g.pause();
            ticks(root,80);check(g.dialogue.active && g.dialogue.content().waiting && g.dialogue.content().header.startsWith("CANAL BRIDGE"),"operator identity and waiting state reach dialogue");
            check(g.radio.transmitter()==RadioAudio.BRIDGE && !g.radio.transmitting(g,g.teacherBoat()),"bridge reception is not assigned to the teacher tug");
            answer(root,1);ticks(root,30);check(!b.authorized && g.passages.position[0]==0,"stand-by response leaves span locked");
            ticks(root,130);contact(root);ticks(root,80);answer(root,0);
            check(!g.dialogue.content().waiting,"chosen reply stops waiting indicator");ticks(root,30);
            check(b.authorized && !g.dialogue.active && !g.passages.passable(0),"request authorizes a timed opening, not instant passage");
            LandmarkBridge bridge=g.passages.bridges[0];float previous=-1;int moving=0;
            for(int i=0;i<400;i++){
                int before=g.passages.position[0];root.step();int now=g.passages.position[0];
                check(now>=before && now-before<=1,"opening advances continuously one tick at a time");
                float[] point=bridge.pose(g.passages.amount(0)).point(bridge.leaves[0],50,0,0);
                if(now>before){check(point[2]>previous,"moving leaf tip advances every active tick");moving++;}previous=point[2];
            }
            check(moving>250 && g.passages.passable(0),"full continuous travel opens physical gate");
            check(b.escort.leading && b.escort.content(g).header.startsWith("INSTRUCTOR"),"instructor takes over the radio and enters first");
            int seen=0;
            for(int i=0;i<7000 && g.trainingChannel();i++){
                ReplayInput.channel(root);root.step();
                if(b.escort.waiting){int gate=b.escort.gate;seen|=1<<gate;
                    check(g.teacherBoat().x>TrainingChannel.X[gate]+30,"instructor waits beyond the milestone line");
                }
                check(g.inWater(g.teacherBoat().x,g.teacherBoat().y),"leader remains in the navigable canal");
            }
            check(g.tutorial.lesson==Tutorial.CONNECT && seen==255,"leader waits beyond every canal milestone before cargo lesson: "+seen);
            g.tutorial.lesson=Tutorial.CHANNEL;g.tutorial.setup(g);check(!b.authorized && g.passages.position[0]==0,"explicit canal reset closes the gate and clears radio authorization");
        }
        System.out.println(checks+" mouth bridge, radio choice, continuous motion and leader milestone assertions passed");
    }
    public static void main(String[] args){run();}
}
