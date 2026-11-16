package com.bargeboss.game;

/** Landscape harbor cabinet, with the same shared scene and controls on every host. */
public final class ScreenView {
    private static final int INK=VisualStyle.OUTLINE, NAVY=VisualStyle.NAVY, PAPER=VisualStyle.CREAM,
            GOLD=VisualStyle.GOLD, MUTED=VisualStyle.MUTED;
    private ScreenView() {}
    private static void text(Painter p,String s,float x,float y,float size,int c) { Lettering.draw(p,s,x,y,size,c); }
    private static void center(Painter p,String s,float x,float y,float size,int c) { Lettering.center(p,s,x,y,size,c); }
    public static void hud(Game g,Painter p) {
        Layout layout=g.layout;
        boolean lockWindow=g.tutorial==null && g.lockStage() && (g.lock.phase==Lock.ENTRY || g.lock.phase==Lock.EXIT);
        int seconds=lockWindow?g.lock.secondsRemaining():0;
        String progress=g.tutorial!=null?"FREE":lockWindow?seconds+"S":g.goalProgress()+"/"+g.quota();
        VisualStyle.panel(p,12,8,316,48,0xed101c30);
        text(p,g.tutorial!=null?"PRACTICE - INDUSTRIAL HARBOR":"LEVEL "+(g.challengeId+1)+" - "+g.challengeTitle(),24,18,12,PAPER);
        String status=g.trainingChannel()?"GATE "+g.tutorial.channelGate+" / "+(TrainingChannel.X.length-1)+" - HEAD UPSTREAM":g.tutorial!=null?"LESSON "+Math.min(Tutorial.DONE,g.tutorial.lesson+1)+" / "+Tutorial.DONE+" - NO TIME LIMIT":g.goalStatus()+" / HULL "+g.hull+"%";
        text(p,status,24,39,Math.min(9,292*7f/Math.max(1,status.length()*6-1)),GOLD);
        float timerX=layout.width/2-130;
        VisualStyle.panel(p,timerX,8,104,48,0xed101c30);
        if(g.tutorial!=null)center(p,progress,timerX+52,20,23,PAPER);
        else {
            center(p,lockWindow?(g.lock.phase==Lock.ENTRY?"ENTRY CLOSES":"EXIT CLOSES"):"GOAL",timerX+52,14,8,MUTED);
            center(p,progress,timerX+52,30,18,lockWindow && seconds<=10?GOLD:PAPER);
        }
        VisualStyle.panel(p,timerX+112,8,124,48,0xed101c30);
        int rating=g.goalComplete()?Challenges.stars(g.challengeId,g.goalProgress(),g.lost,g.hull):0;
        if(g.lockStage()) {
            float level=g.lock.waterLevel();
            center(p,"WATER "+Math.round(level*100)+"%",timerX+174,14,9,PAPER);
            p.rect(timerX+124,30,100,8,0xff233f4c);
            p.rect(timerX+124,30,100*level,8,0xff71cfca);
            for(int mark=0;mark<=4;mark++)p.rect(timerX+124+mark*25,29,1,10,0x998bd0c5);
            center(p,g.lock.phase==Lock.FILLING?"RISING":level>=1?"HIGH / MATCHED":"LOW",timerX+174,44,7,GOLD);
        } else for(int i=0;i<3;i++) star(p,timerX+138+i*35,31,13,i<rating?GOLD:0xff465569);
        VisualStyle.panel(p,12,layout.height-48,170,40,0xed101c30);
        text(p,"SCORE",24,layout.height-39,8,MUTED);
        text(p,""+g.score,81,layout.height-37,18,PAPER);
        haptics(g,p);
        Layout.Box pause=layout.box(Layout.PAUSE);
        VisualStyle.panel(p,pause.x,pause.y,pause.w,pause.h,0xff293a4a);
        p.rect(pause.centerX()-10,pause.y+10,6,20,PAPER); p.rect(pause.centerX()+4,pause.y+10,6,20,PAPER);
    }
    private static void haptics(Game g,Painter p) {
        Layout.Box setup=g.layout.box(Layout.SETTINGS);
        VisualStyle.panel(p,setup.x,setup.y,setup.w,setup.h,0xff203047);
        center(p,"SETTINGS",setup.centerX(),setup.y+15,10,PAPER);
        Layout.Box b=g.layout.box(Layout.HAPTICS);
        VisualStyle.panel(p,b.x,b.y,b.w,b.h,0xff203047);
        center(p,"HAPTICS",b.centerX(),b.y+7,9,MUTED);
        center(p,g.hapticsEnabled?"ON":"OFF",b.centerX(),b.y+23,11,g.hapticsEnabled?GOLD:PAPER);
    }
    public static void title(Game g,Painter p) {
        TitleView.background(g,p); TitleView.voyage(g,p); haptics(g,p);
        // Title anchors use the full safe canvas instead of centering a fixed
        // 960-wide poster inside a wide phone screen.
        Painter sign=scaled(p,24,-14,.74f);
        sign.poly(new float[]{44,132,125,110,143,66,533,66,492,238,133,270,151,240,32,246,78,169},INK);
        sign.poly(new float[]{55,132,133,117,150,74,521,74,484,231,143,258,163,231,47,237,91,169},0xffa92d37);
        sign.poly(new float[]{70,134,143,125,158,82,509,82,476,223,156,246,173,223,62,227,103,169},NAVY);
        logo(sign,"BARGE",76,93,84,PAPER); logo(sign,"BOSS",155,181,93,GOLD);
        float lamp=1+(float)StrictMath.sin(g.visualTicks*.065)*.25f;
        sign.rect(99,263,15*lamp,4,GOLD); sign.rect(496,91,5,19*lamp,PAPER);
        Painter menu=offset(p,g.layout.width-Layout.WIDTH,g.layout.offsetY);
        VisualStyle.panel(menu,608,180,324,368,0xf5101c30);
        center(menu,"LEVEL 0"+(g.challengeId+1),770,200,11,GOLD);
        center(menu,g.challengeTitle(),770,230,22,PAPER);
        center(menu,goalSummary(g.challengeId),770,274,12,GOLD);
        button(g,menu,Layout.LEARN,"LEARN TO DRIVE",false);
        button(g,menu,Layout.START,"START LEVEL",true);
        button(g,menu,Layout.CHALLENGES,"CHALLENGES",false);
        Painter rank=offset(p,0,g.layout.height-Layout.HEIGHT);
        VisualStyle.panel(rank,28,536,545,40,0xe8101c30);
        captainBadge(rank,52,556,.37f,g.career.stripes());
        text(rank,g.career.rankTitle(),77,548,13,PAPER);
        text(rank,g.career.totalStars()+" / "+Challenges.maxStars()+" CAREER STARS",367,549,10,GOLD);
    }

    private static void logo(Painter p,String word,float x,float y,float size,int fill) {
        Painter italic=new Transform(p,x,y,1,-.18f);
        Lettering.draw(italic,word,4,6,size,INK); Lettering.draw(italic,word,-2,-2,size,INK);
        Lettering.draw(italic,word,0,0,size,fill);
        p.line(x+1,y-1,x+Lettering.width(word,size)-5,y-1,2,fill==GOLD?0xffffef8a:0xffffffff);
    }
    private static void harbor(Game g,Painter p) {
        int[] sky={0xff7467a8,0xff8c6ea7,0xffae7aa5,0xffd98d9c,0xfff5aa8f,0xffffc487};
        float horizon=312+g.layout.offsetY;
        for(int i=0;i<sky.length;i++) p.rect(0,i*horizon/6,g.layout.width,horizon/6+1,sky[i]);
        p.circle(550+g.layout.offsetX,258+g.layout.offsetY,65,0xffffd8a0);
        for(int i=0;i<(int)(g.layout.width/72)+2;i++) {
            float x=(i*83+g.visualTicks/35f)%(g.layout.width+140)-100,y=46+(i%4)*49;
            p.rect(x,y,42+i%3*17,9,0x66f4a8a0); p.rect(x+15,y-7,45,8,0x55ffc2a0);
        }
        for(int i=0;i<(int)(g.layout.width/38)+2;i++) {
            int x=i*38-10,h=35+(i*31)%111;
            p.rect(x,horizon+1-h,31,h,0xff555877); p.rect(x+5,horizon-8-h,20,9,0xff555877);
            if(i%4==0) p.line(x+15,horizon-8-h,x+15,horizon-32-h,2,0xff555877);
            for(int row=0;row<h/14;row++) for(int col=0;col<3;col++)
                if((row+col+i)%3==0) p.rect(x+5+col*8,horizon+10-h+row*13,3,4,0xffbd9b9c);
        }
        p.rect(0,horizon,g.layout.width,g.layout.height-horizon,VisualStyle.TEAL); p.rect(0,horizon,g.layout.width,6,0xfff1b682);
        for(int side=0;side<2;side++) {
            float x=side==0?5:g.layout.width-5,dir=side==0?1:-1;
            p.poly(new float[]{x,horizon+1,x+dir*146,horizon-34,x+dir*151,horizon-15,x+dir*4,horizon+20},0xff27364e);
            p.line(x,horizon-2,x+dir*146,horizon-37,6,0xffb08782);
            for(int i=1;i<7;i++) {
                float xx=x+dir*i*21,yy=horizon-2-i*5;
                p.line(xx,yy,xx+dir*14,yy+12,3,0xff727281);
                p.rect(xx-2,yy-6,4,6,GOLD); p.rect(xx-4,yy+24,8,2,0x88eebc70);
            }
        }
        for(int row=0;row<(g.layout.height-horizon)/17;row++) for(int col=0;col<g.layout.width/72+1;col++) {
            float x=col*72+(row%2)*23,y=horizon+14+row*17;
            float drift=(float)StrictMath.sin(g.visualTicks*.025+row*.73+col)*8;
            int color=col>5 && col<10 && row<10?0x99ffc276:row%3==0?0x88578aab:0x6657b8be;
            p.rect(x+drift,y,15+(row+col)%4*7,2+(row%2),color);
        }
        p.poly(new float[]{0,g.layout.height-70,73,g.layout.height-95,73,g.layout.height,0,g.layout.height},0xff1b2b40);
        p.poly(new float[]{g.layout.width,g.layout.height-73,g.layout.width-67,g.layout.height-96,g.layout.width-67,g.layout.height,g.layout.width,g.layout.height},0xff1b2b40);
        for(int i=0;i<3;i++) { p.rect(0,g.layout.height-59+i*22,45,10,0xff293b50); p.rect(g.layout.width-45,g.layout.height-63+i*22,45,10,0xff293b50); }
    }
    public static void help(Game g,Painter p) {
        p=menu(g,p);
        overlay(p,105,411);
        center(p,"CAPTAIN'S FIELD GUIDE",480,133,24,GOLD);
        instruction(p,1,185,"DRAG OR USE JOYSTICK","Pull farther for thrust. Release to coast.");
        instruction(p,2,251,"DRAG THE CARGO","Press, drag and hold. Center for assist.");
        instruction(p,3,317,"AIM, THEN RELEASE","Drag cargo toward a bollard. Release.");
        instruction(p,4,383,"LINK A BARGE TRAIN","Drag cargo to tug or barge. Release to link.");
        text(p,"TAP THE CONNECTED BARGE TO LET GO.",159,447,10,GOLD);
        button(g,p,Layout.LEARN,"PRACTICE CONTROLS",false);
        button(g,p,Layout.START,"RESUME LEVEL",true);
        center(p,"YOUR LEVEL WAITS HERE.",770,231,12,MUTED);
        center(p,"COMPLETE THE GOAL TO ADVANCE.",770,257,11,MUTED);
    }
    private static void instruction(Painter p,int index,float y,String title,String detail) {
        VisualStyle.panel(p,106,y-3,39,39,0xff344453); center(p,""+index,126,y+6,20,GOLD);
        text(p,title,159,y,16,PAPER); text(p,detail,159,y+28,10,MUTED);
    }
    public static void settings(Game g,Painter p) {
        p.rect(0,0,g.layout.width,g.layout.height,NAVY);p=menu(g,p);overlay(p,100,405);
        center(p,"SETTINGS",480,138,28,GOLD);
        center(p,"JOYSTICK POSITION",480,208,18,PAPER);
        center(p,"TAP-AND-DRAG DRIVING REMAINS AVAILABLE.",480,244,11,MUTED);
        int side=g.settings.joystickSide();
        button(g,p,Layout.JOYSTICK_LEFT,"LEFT",side==GameSettings.LEFT);
        button(g,p,Layout.JOYSTICK_RIGHT,"RIGHT",side==GameSettings.RIGHT);
        button(g,p,Layout.JOYSTICK_OFF,"OFF",side==GameSettings.OFF);
        center(p,"POINT YOUR THUMB WHERE YOU WANT TO GO.",480,365,12,PAPER);
        center(p,"PUSH FARTHER FOR THRUST. RELEASE TO COAST.",480,387,10,MUTED);
        button(g,p,Layout.SETTINGS_BACK,"BACK",true);
    }
    public static void pause(Game g,Painter p) {
        p=menu(g,p);
        overlay(p,113,370);
        p.rect(451,142,18,37,GOLD); p.rect(492,142,18,37,GOLD);
        center(p,"LEVEL PAUSED",480,203,28,PAPER);
        center(p,"LINES HELD. ENGINES QUIET.",480,253,12,MUTED);
        button(g,p,Layout.RESUME,"RESUME",true); button(g,p,Layout.RESTART,"RESTART LEVEL",false);
    }
    public static void finish(Game g,Painter p) {
        p=menu(g,p);
        overlay(p,82,454);
        boolean complete=g.goalComplete() && g.hull>0;
        center(p,g.hull<=0?"TUG OUT OF SERVICE":complete?"LEVEL COMPLETE":"TRY AGAIN, CAPTAIN",480,106,26,PAPER);
        center(p,g.challengeTitle(),480,148,12,MUTED);
        int earned=g.earnedStars;
        for(int i=0;i<3;i++) star(p,400+i*80,200+(earned>i?2*(float)StrictMath.sin(g.visualTicks*.05+i):0),25,earned>i?GOLD:0xff465366);
        center(p,earned==3?"GOLD STRIPES":earned==2?"SILVER STRIPES":earned==1?"BRONZE STRIPES":"KEEP PRACTICING",480,238,11,earned>0?GOLD:MUTED);
        center(p,""+g.score,480,271,38,GOLD);
        center(p,g.goalProgress()+" / "+g.quota()+(g.lockStage()?" THROUGH LOCK":" DELIVERED")+" / HULL "+g.hull+"%",480,323,12,PAPER);
        if(g.lockStage() && g.lock.failed())center(p,g.lock.failure(),480,349,11,GOLD);
        if(g.rankPromoted) for(int i=0;i<6;i++) {
            double angle=i*Math.PI/3+g.visualTicks*.008;
            star(p,174+(float)StrictMath.cos(angle)*70,211+(float)StrictMath.sin(angle)*65,
                    3+2*(float)StrictMath.sin(g.visualTicks*.06+i),GOLD);
        }
        captainBadge(p,174,211,1.1f,g.career.stripes());
        center(p,g.career.rankTitle(),174,280,14,PAPER);
        center(p,g.rankPromoted?"PROMOTED, CAPTAIN!":"CAPTAIN'S RANK",174,306,10,g.rankPromoted?GOLD:MUTED);
        center(p,g.career.totalStars()+" / "+Challenges.maxStars()+" CAREER STARS",174,329,10,GOLD);
        center(p,"PERSONAL BEST",784,183,11,MUTED);
        center(p,""+g.career.bestScore(g.challengeId),784,207,26,GOLD);
        for(int i=0;i<3;i++) star(p,748+i*36,264,12,g.career.bestStars(g.challengeId)>i?GOLD:0xff465366);
        center(p,g.rankPromoted?"NEW STRIPES. SAME GOOD TUG.":complete?"GOOD WORK, CAPTAIN.":"TRY THE GOAL AGAIN.",480,378,14,PAPER);
        boolean next=g.challengeId+1<Challenges.COUNT && g.career.unlocked(g.challengeId+1);
        if(next) center(p,"NEXT: "+Challenges.title(g.challengeId+1),480,409,11,GOLD);
        else center(p,"REPLAY TO IMPROVE YOUR STARS AND BEST SCORE",480,409,10,MUTED);
        button(g,p,Layout.FINISH_CHALLENGES,"CHALLENGES",false);
        button(g,p,Layout.AGAIN,"REPLAY LEVEL",!next);
        if(next) button(g,p,Layout.NEXT_CHALLENGE,"NEXT LEVEL",true);
    }
    private static String goalSummary(int id) {
        return Challenges.summary(id);
    }
    /** Persistent career board; levels finish when their displayed goal is complete. */
    public static void challenges(Game g,Painter p) {
        harbor(g,p); p.rect(0,0,g.layout.width,g.layout.height,0xdd0b1728);
        Painter screen=p; p=centered(g,p);
        center(p,"CAPTAIN'S CHALLENGES",480,37,28,PAPER);
        center(p,Challenges.COUNT+" HARBOR CHALLENGES. PICK YOUR NEXT JOB.",480,79,11,MUTED);
        captainBadge(p,85,130,.7f,g.career.stripes());
        text(p,g.career.rankTitle(),125,109,19,GOLD);
        text(p,g.career.totalStars()+" / "+Challenges.maxStars()+" UNIQUE STARS",126,140,11,PAPER);
        text(p,"BRONZE OPENS THE NEXT LEVEL",529,113,12,GOLD);
        text(p,"BETTER STARS EARN CAPTAIN STRIPES",529,140,10,MUTED);
        int page=Math.max(0,Math.min(Challenges.pageCount()-1,g.challengePage));
        for(int slot=0;slot<Layout.CHALLENGE_CARDS.length;slot++) {
            int i=page*Challenges.PAGE_SIZE+slot;
            if(!Challenges.valid(i))continue;
            Layout.Box card=Layout.CHALLENGE_CARDS[slot];
            boolean open=g.career.unlocked(i),selected=g.challengeId==i;
            VisualStyle.panel(p,card.x,card.y,card.w,card.h,selected?0xff294656:open?0xff1c3045:0xff162334);
            p.rect(card.x+5,card.y+5,card.w-10,4,selected?GOLD:open?0xff678197:0xff3b4859);
            center(p,"0"+(i+1)+(open?" / CALUMET":" / LOCKED"),card.centerX(),card.y+20,10,open?MUTED:0xff6b798a);
            center(p,Challenges.title(i),card.centerX(),card.y+47,20,open?PAPER:MUTED);
            String goal=Challenges.cardGoal(i);
            float goalSize=Math.min(12,12*(card.w-20)/Lettering.width(goal,12));
            center(p,goal,card.centerX(),card.y+84,goalSize,open?GOLD:MUTED);
            center(p,Challenges.cardHint(i),card.centerX(),card.y+105,8,MUTED);
            if(open) {
                for(int j=0;j<3;j++) star(p,card.centerX()-42+j*42,card.y+132,17,g.career.bestStars(i)>j?GOLD:0xff465366);
                center(p,"BEST "+g.career.bestScore(i),card.centerX(),card.y+165,12,MUTED);
                center(p,selected?"READY TO SAIL":"TAP TO SELECT",card.centerX(),card.y+205,12,selected?GOLD:PAPER);
                if(selected) p.rect(card.x+19+(g.visualTicks%120)/120f*(card.w-60),card.y+230,20,2,PAPER);
            } else {
                lock(p,card.centerX(),card.y+132);
                center(p,"EARN BRONZE IN",card.centerX(),card.y+170,11,MUTED);
                center(p,Challenges.title(i-1),card.centerX(),card.y+193,12,PAPER);
            }
        }
        boolean selectionHere=Challenges.pageFor(g.challengeId)==page;
        center(p,selectionHere?Challenges.description(g.challengeId):"EARN BRONZE TO OPEN THE NEXT HARBOR JOB.",480,444,13,PAPER);
        center(p,"BRONZE: GOAL / SILVER: NO LOST LOADS OR 80% HULL / GOLD: BOTH",480,475,10,MUTED);
        button(g,p,Layout.BACK,"BACK",false);
        button(g,p,Layout.CHALLENGE_START,selectionHere?"START LEVEL":"SELECT LEVEL",selectionHere);
        if(Challenges.pageCount()>1){
            if(page>0)button(g,p,Layout.CHALLENGE_PREV_PAGE,"PREV",false);
            if(page+1<Challenges.pageCount())button(g,p,Layout.CHALLENGE_NEXT_PAGE,"NEXT",false);
            center(p,"PAGE "+(page+1)+" / "+Challenges.pageCount(),480,539,11,MUTED);
        }
        haptics(g,screen);
    }
    private static void lock(Painter p,float x,float y) {
        ControlView.ring(p,x,y-6,13,1,0xff718092,5);
        VisualStyle.panel(p,x-19,y-5,38,27,0xff43566a);
        p.circle(x,y+7,3,GOLD); p.rect(x-1.5f,y+7,3,7,GOLD);
    }
    /** A tiny uniform badge: a cheerful duck above braided gold captain bands. */
    private static void captainBadge(Painter p,float x,float y,float scale,int stripes) {
        p=new Transform(p,x,y,scale,0);
        p.poly(new float[]{-38,-45,38,-45,35,29,0,47,-35,29},INK);
        p.poly(new float[]{-34,-41,34,-41,31,26,0,42,-31,26},GOLD);
        p.poly(new float[]{-30,-37,30,-37,27,24,0,37,-27,24},NAVY);
        p.circle(-5,-18,12,PAPER); p.circle(7,-25,9,PAPER);
        p.poly(new float[]{14,-28,25,-23,14,-19},0xfff39b38);
        p.circle(10,-27,1.8f,INK);
        p.poly(new float[]{-17,-20,-24,-26,-21,-14,-9,-7,7,-9,13,-17},PAPER);
        p.rect(-2,-37,18,5,PAPER); p.rect(-3,-32,20,3,INK); p.rect(4,-37,5,3,GOLD);
        if(stripes==0) { p.circle(0,15,8,0xff617287); p.circle(0,15,4,NAVY); }
        for(int i=0;i<stripes;i++) {
            float yy=1+i*7;
            p.rect(-23,yy,46,4,GOLD); p.rect(-23,yy,46,1,0xffffec96);
            for(int j=0;j<8;j++) p.line(-22+j*6,yy+4,-18+j*6,yy,1,0xffb17a27);
        }
    }
    public static void star(Painter p,float x,float y,float r,int color) {
        float[] points=new float[20];
        for(int i=0;i<10;i++) {
            double a=-Math.PI/2+i*Math.PI/5; float rr=i%2==0?r:r*.45f;
            points[i*2]=x+(float)StrictMath.cos(a)*rr; points[i*2+1]=y+(float)StrictMath.sin(a)*rr;
        }
        float[] shadow=points.clone(); for(int i=1;i<shadow.length;i+=2) shadow[i]+=4;
        p.poly(shadow,INK); p.poly(points,color);
        p.line(x,y-r+5,x-r*.34f,y-r*.05f,2,color==GOLD?0xffffef96:0xff647489);
    }
    public static void overlay(Painter p,float top,float height) {
        p.rect(0,Layout.HEADER_HEIGHT,Layout.WIDTH,Layout.HEIGHT-Layout.HEADER_HEIGHT,0xcc07101f);
        VisualStyle.panel(p,28,top,904,height,NAVY);
        for(int i=0;i<2;i++) { VisualStyle.bolt(p,40+i*880,top+14); VisualStyle.bolt(p,40+i*880,top+height-14); }
        p.rect(56,top+3,848,3,GOLD);
    }
    public static void button(Game g,Painter p,Layout.Box box,String label,boolean primary) { button(g,p,box.x,box.y,box.w,box.h,label,primary); }
    public static void button(Game g,Painter p,float x,float y,float w,float h,String label,boolean primary) {
        p.rect(x+2,y+5,w,h,INK); VisualStyle.panel(p,x,y,w,h,primary?GOLD:0xff2b4156);
        if(primary) {
            p.rect(x+7,y+5,w-14,3,0xffffe891); p.rect(x+7,y+h-7,w-14,3,0xffbb791d);
            float pulse=2*(float)StrictMath.sin(g.visualTicks*.075);
            p.poly(new float[]{x+16+pulse,y+h/2-6,x+25+pulse,y+h/2,x+16+pulse,y+h/2+6},NAVY);
        }
        float size=Math.min(label.length()>18?14:18,(w-62)*7/Math.max(1,label.length()*6-1));
        center(p,label,x+w/2,y+(h-size)/2,size,primary?INK:PAPER);
    }
    public static Painter scaled(Painter p,float x,float y,float scale) { return new Transform(p,x,y,scale,0); }
    public static Painter offset(Painter p,float x,float y) { return x==0 && y==0?p:new Transform(p,x,y,1,0); }
    public static Painter centered(Game g,Painter p) { return offset(p,g.layout.offsetX,g.layout.offsetY); }
    public static Painter menu(Game g,Painter p) {
        p.rect(0,0,g.layout.width,g.layout.height,0x8807101f);
        return centered(g,p);
    }
    /** Scale/shear for vector lettering; atlas sprites retain an unskewed projection. */
    private static final class Transform implements Painter {
        private final Painter p; private final float x,y,scale,shear;
        Transform(Painter p,float x,float y,float scale,float shear) { this.p=p; this.x=x; this.y=y; this.scale=scale; this.shear=shear; }
        private float tx(float xx,float yy) { return x+(xx+shear*yy)*scale; }
        private float ty(float yy) { return y+yy*scale; }
        public void pushClipOutside(float[] a) {
            float[] b=new float[a.length];for(int i=0;i<a.length;i+=2){b[i]=tx(a[i],a[i+1]);b[i+1]=ty(a[i+1]);}p.pushClipOutside(b);
        }
        public void popClip(){p.popClip();}
        public void rect(float xx,float yy,float w,float h,int c) {
            if(shear!=0) { xx-=.3f; yy-=.3f; w+=.6f; h+=.6f; }
            poly(new float[]{xx,yy,xx+w,yy,xx+w,yy+h,xx,yy+h},c);
        }
        public void circle(float xx,float yy,float r,int c) { p.circle(tx(xx,yy),ty(yy),r*scale,c); }
        public void poly(float[] a,int c) { float[] b=new float[a.length]; for(int i=0;i<a.length;i+=2) { b[i]=tx(a[i],a[i+1]); b[i+1]=ty(a[i+1]); } p.poly(b,c); }
        public void line(float xx,float yy,float bx,float by,float w,int c) { p.line(tx(xx,yy),ty(yy),tx(bx,by),ty(by),w*scale,c); }
        public boolean prefersPolygonBatches(){return p.prefersPolygonBatches();}
        public boolean preparesLayers(){return shear==0 && p.preparesLayers();}
        public void prepareLayer(StaticLayer layer,float size){if(shear==0)p.prepareLayer(layer,size*scale);}
        public void layer(StaticLayer layer,float xx,float yy,float size,float alpha) {
            if(shear==0)p.layer(layer,x+xx*scale,y+yy*scale,size*scale,alpha);
            else layer.draw(this,xx,yy,size,alpha);
        }
        public void waterFoam(float xx,float yy,float ax,float ay,float bx,float by,float phase,int kind,float alpha){p.waterFoam(tx(xx,yy),ty(yy),(ax+shear*ay)*scale,ay*scale,(bx+shear*by)*scale,by*scale,phase,kind,alpha);}
    public void material(float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color){p.material(tx(x,y),ty(y),(ax+shear*ay)*scale,ay*scale,(bx+shear*by)*scale,by*scale,kind,detail,amount,color);}
    public void waterReflection(float x,float y,float ax,float ay,float bx,float by,float phase,int color){p.waterReflection(tx(x,y),ty(y),(ax+shear*ay)*scale,ay*scale,(bx+shear*by)*scale,by*scale,phase,color);}
    public void ropeSegment(float x,float y,float xx,float yy,float width,float from,float to,int color){p.ropeSegment(tx(x,y),ty(y),tx(xx,yy),ty(yy),width*scale,from,to,color);}
    public void smokePuff(float x,float y,float radius,float phase,int color){p.smokePuff(tx(x,y),ty(y),radius*scale,phase,color);}
    public void spriteAffine(int a,int f,float xx,float yy,float ax,float ay,float bx,float by,float alpha){p.spriteAffine(a,f,tx(xx,yy),ty(yy),(ax+shear*ay)*scale,ay*scale,(bx+shear*by)*scale,by*scale,alpha);}
        public void sprite(int atlas,int frame,float cx,float cy,float width,float height,float rotation,float alpha) {
            p.sprite(atlas,frame,x+cx*scale,y+cy*scale,width*scale,height*scale,rotation,alpha);
        }
        public void spriteBlend(int atlas,int frameA,int frameB,float cx,float cy,float width,float height,float rotation,float blend,float alpha) {
            p.spriteBlend(atlas,frameA,frameB,x+cx*scale,y+cy*scale,width*scale,height*scale,rotation,blend,alpha);
        }
    }
}
