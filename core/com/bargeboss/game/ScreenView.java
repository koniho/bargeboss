package com.bargeboss.game;

/** Harbor-cabinet presentation, authored entirely with the shared native primitives. */
public final class ScreenView {
    private static final int INK=VisualStyle.OUTLINE, NAVY=VisualStyle.NAVY, PAPER=VisualStyle.CREAM,
            GOLD=VisualStyle.GOLD, MUTED=VisualStyle.MUTED;
    private ScreenView() {}
    private static void text(Painter p,String s,float x,float y,float size,int c) { Lettering.draw(p,s,x,y,size,c); }
    private static void center(Painter p,String s,float x,float y,float size,int c) { Lettering.center(p,s,x,y,size,c); }
    public static void hud(Game g,Painter p) {
        p.rect(0,0,600,148,NAVY); p.rect(0,0,600,4,GOLD);
        VisualStyle.panel(p,15,15,350,68,0xff203047);
        text(p,"BARGE BOSS",29,27,24,PAPER); text(p,"01 / CALUMET INDUSTRIAL REACH",30,61,10,GOLD);
        VisualStyle.bolt(p,354,26); VisualStyle.bolt(p,354,72);
        haptics(g,p);
        VisualStyle.panel(p,508,18,68,67,0xff293a4a);
        p.rect(530,37,7,27,PAPER); p.rect(546,37,7,27,PAPER);
        p.rect(16,91,568,51,INK);
        text(p,"SCORE",26,98,10,MUTED); text(p,""+g.score,26,115,20,PAPER);
        text(p,"LOADS",181,98,10,MUTED); text(p,g.delivered+" / "+Game.QUOTA,181,115,20,GOLD);
        text(p,"HULL",325,98,10,MUTED); text(p,g.hull+"%",325,115,20,g.hull<30?VisualStyle.RED:PAPER);
        int seconds=Math.max(0,(Game.SHIFT_TICKS-g.ticks+59)/60);
        text(p,g.tutorial==null?"SHIFT":"PRACTICE",476,98,10,MUTED);
        text(p,g.tutorial==null?(seconds/60)+":"+(seconds%60<10?"0":"")+(seconds%60):"FREE",476,115,20,seconds<30?GOLD:PAPER);
        p.rect(16,145,568,3,0xff53617a);
    }
    private static void haptics(Game g,Painter p) {
        VisualStyle.panel(p,380,18,116,67,0xff203047);
        center(p,"HAPTICS",438,33,11,MUTED); center(p,g.hapticsEnabled?"ON":"OFF",438,55,13,g.hapticsEnabled?GOLD:PAPER);
    }
    public static void title(Game g,Painter p) {
        harbor(g,p);
        text(p,"A CALUMET RIVER ARCADE",25,28,11,PAPER);
        // A compact switch remains at its normal touch coordinates on the title.
        haptics(g,p);
        p.poly(new float[]{52,157,137,125,154,83,527,83,488,263,135,292,150,260,42,268,86,188},INK);
        p.poly(new float[]{63,155,145,132,161,91,515,91,480,254,143,280,161,251,57,258,96,187},0xffa92d37);
        p.poly(new float[]{81,157,153,140,168,99,503,99,471,246,158,267,170,243,70,247,108,184},NAVY);
        logo(p,"BARGE",78,112,84,PAPER);
        logo(p,"BOSS",155,201,93,GOLD);
        float lamp=1+(float)StrictMath.sin(g.visualTicks*.065)*.25f;
        p.rect(94,283,15*lamp,4,GOLD); p.rect(489,109,5,19*lamp,PAPER);
        center(p,"HEAVY CARGO. BIG PAYDAY.",300,322,15,PAPER);
        // The same vessel art used in play makes the title's hero a living harbor scene.
        Body cargo=new Body(1,2,0,0,-.12f); cargo.vx=28;
        VesselView.draw(g,new Transform(p,451,472,3.25f,0),cargo,false);
        Body tug=new Body(0,-1,0,0,-.12f); tug.vx=32;
        VesselView.draw(g,new Transform(p,209,454,3.8f,0),tug,true);
        p.line(291,453,345,460,4,0xffd9bf7d);
        VisualStyle.panel(p,76,536,448,206,0xee101c30);
        button(g,p,100,576,400,64,"LEARN TO DRIVE",false);
        button(g,p,100,650,400,76,"START SHIFT",true);
        center(p,"01  DRAG THE TUG TO DRIVE",300,784,14,PAPER);
        center(p,"02  HOLD CARGO. DRAG TO STEER.",300,814,13,PAPER);
        center(p,"03  AIM AT A BOLLARD. RELEASE TO THROW.",300,844,13,PAPER);
        center(p,"6 LOADS / 3 MINUTES / ONE BUSY RIVER",300,900,11,GOLD);
        text(p,"CALUMET HARBOR AUTHORITY",24,940,9,MUTED); text(p,"SHIFT 01",489,940,9,MUTED);
    }
    private static void logo(Painter p,String word,float x,float y,float size,int fill) {
        Painter italic=new Transform(p,x,y,1,-.18f);
        Lettering.draw(italic,word,4,6,size,INK);
        Lettering.draw(italic,word,-2,-2,size,INK);
        Lettering.draw(italic,word,0,0,size,fill);
        // Pixel highlights add a bevel without replacing the shared glyph alphabet.
        float width=Lettering.width(word,size);
        p.line(x+1,y-1,x+width-5,y-1,2,fill==GOLD?0xffffef8a:0xffffffff);
    }
    private static void harbor(Game g,Painter p) {
        int[] sky={0xff7467a8,0xff8c6ea7,0xffae7aa5,0xffd98d9c,0xfff5aa8f,0xffffc487};
        for(int i=0;i<sky.length;i++) p.rect(0,i*65,600,66,sky[i]);
        p.circle(412,305,62,0xffffd8a0);
        // Blocks rather than gradients preserve the concept's pixel-painting rhythm.
        for(int i=0;i<11;i++) {
            float x=(i*71+g.visualTicks/35f)%750-100,y=71+(i%4)*52;
            p.rect(x,y,42+i%3*17,9,0x66f4a8a0); p.rect(x+15,y-7,45,8,0x55ffc2a0);
        }
        for(int i=0;i<18;i++) {
            int x=i*36-10,h=35+(i*31)%103;
            p.rect(x,365-h,30,h,0xff555877); p.rect(x+5,356-h,19,9,0xff555877);
            if(i%4==0) p.line(x+15,356-h,x+15,332-h,2,0xff555877);
            for(int row=0;row<h/14;row++) for(int col=0;col<3;col++)
                if((row+col+i)%3==0) p.rect(x+5+col*8,374-h+row*13,3,4,0xffbd9b9c);
        }
        p.rect(0,364,600,596,VisualStyle.TEAL);
        p.rect(0,364,600,8,0xfff1b682);
        // Raised bascule bridge and warm work lights behind the hero tug.
        for(int side=0;side<2;side++) {
            float x=side==0?5:595,dir=side==0?1:-1;
            p.poly(new float[]{x,365,x+dir*146,330,x+dir*151,349,x+dir*4,384},0xff27364e);
            p.line(x,362,x+dir*146,327,6,0xffb08782);
            for(int i=1;i<7;i++) {
                float xx=x+dir*i*21,yy=362-i*5;
                p.line(xx,yy,xx+dir*14,yy+12,3,0xff727281);
                p.rect(xx-2,yy-6,4,6,GOLD);
                p.rect(xx-4,yy+24,8,2,0x88eebc70);
            }
        }
        for(int row=0;row<34;row++) for(int col=0;col<9;col++) {
            float x=col*72+(row%2)*23,y=378+row*18;
            float drift=(float)StrictMath.sin(g.visualTicks*.025+row*.73+col)*8;
            int color=col>4 && row<10?0x99ffc276:row%3==0?0x88578aab:0x6657b8be;
            p.rect(x+drift,y,15+(row+col)%4*7,2+(row%2),color);
        }
        p.poly(new float[]{0,750,82,722,82,960,0,960},0xff1b2b40);
        p.poly(new float[]{600,744,529,718,529,960,600,960},0xff1b2b40);
        for(int i=0;i<6;i++) {
            p.rect(0,767+i*29,48,11,0xff293b50); p.rect(552,756+i*29,48,11,0xff293b50);
        }
    }
    public static void help(Game g,Painter p) {
        overlay(p,219,538);
        center(p,"CAPTAIN'S FIELD GUIDE",300,247,23,GOLD);
        instruction(p,1,303,"DRAG THE TUG","Pull farther for thrust. Release to coast.");
        instruction(p,2,369,"HOLD THE CARGO","Keep still to push. Drag to steer.");
        instruction(p,3,435,"AIM, THEN RELEASE","Drag cargo toward a bollard. Release.");
        text(p,"MISSES RETRACT. AIM AGAIN.",131,490,10,GOLD);
        center(p,"CREWS UNLOAD IN 3 SECONDS, THEN SCORE.",300,520,11,MUTED);
        button(g,p,100,576,400,64,"PRACTICE THE CONTROLS",false);
        button(g,p,100,650,400,76,"RESUME SHIFT",true);
    }
    private static void instruction(Painter p,int index,float y,String title,String detail) {
        VisualStyle.panel(p,74,y-3,39,39,0xff344453); center(p,""+index,94,y+6,20,GOLD);
        text(p,title,131,y,16,PAPER); text(p,detail,131,y+27,10,MUTED);
    }
    public static void pause(Game g,Painter p) {
        overlay(p,321,416);
        p.rect(268,352,20,44,GOLD); p.rect(311,352,20,44,GOLD);
        center(p,"SHIFT PAUSED",300,422,28,PAPER);
        center(p,"LINES HELD. ENGINES QUIET.",300,476,12,MUTED);
        button(g,p,100,540,400,72,"RESUME",true);
        button(g,p,100,632,400,72,"RESTART SHIFT",false);
    }
    public static void finish(Game g,Painter p) {
        overlay(p,264,494);
        boolean complete=g.delivered>=Game.QUOTA;
        center(p,g.hull<=0?"TUG OUT OF SERVICE":complete?"SHIFT COMPLETE":"SHIFT ENDED",300,296,23,PAPER);
        int earned=complete?1+(g.lost==0?1:0)+(g.hull>=80?1:0):0;
        for(int i=0;i<3;i++) {
            float rise=earned>i?2*(float)StrictMath.sin(g.visualTicks*.05+i):0;
            star(p,230+i*70,370+rise,24,earned>i?GOLD:0xff465366);
        }
        center(p,""+g.score,300,425,43,GOLD); center(p,"POINTS EARNED",300,482,10,MUTED);
        center(p,g.delivered+" DELIVERED / "+g.lost+" EXPIRED",300,524,13,PAPER);
        center(p,"HULL REMAINING "+g.hull+"%",300,553,12,MUTED);
        center(p,complete?"GOOD WORK, CAPTAIN.":"THE NEXT SHIFT IS YOURS.",300,598,13,PAPER);
        button(g,p,100,650,400,76,"ANOTHER SHIFT",true);
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
        p.rect(0,148,600,812,0xcc07101f); VisualStyle.panel(p,40,top,520,height,NAVY);
        for(int i=0;i<2;i++) { VisualStyle.bolt(p,52+i*496,top+14); VisualStyle.bolt(p,52+i*496,top+height-14); }
        p.rect(72,top+3,456,3,GOLD);
    }
    public static void button(Game g,Painter p,float x,float y,float w,float h,String label,boolean primary) {
        p.rect(x+2,y+5,w,h,INK);
        VisualStyle.panel(p,x,y,w,h,primary?GOLD:0xff2b4156);
        if(primary) {
            p.rect(x+7,y+5,w-14,3,0xffffe891); p.rect(x+7,y+h-7,w-14,3,0xffbb791d);
            float pulse=2*(float)StrictMath.sin(g.visualTicks*.075);
            p.poly(new float[]{x+20+pulse,y+h/2-7,x+30+pulse,y+h/2,x+20+pulse,y+h/2+7},NAVY);
        }
        float size=label.length()>18?14:18;
        center(p,label,x+w/2,y+(h-size)/2,size,primary?INK:PAPER);
    }
    /** Scale and shear, expressed as identical primitives on all hosts. */
    private static final class Transform implements Painter {
        private final Painter p; private final float x,y,scale,shear;
        Transform(Painter p,float x,float y,float scale,float shear) { this.p=p; this.x=x; this.y=y; this.scale=scale; this.shear=shear; }
        private float tx(float xx,float yy) { return x+(xx+shear*yy)*scale; }
        private float ty(float yy) { return y+yy*scale; }
        public void rect(float xx,float yy,float w,float h,int c) {
            // Adjacent sheared glyph cells overlap slightly so native antialiasing
            // cannot expose a grid of seams inside the large solid logo letters.
            if(shear!=0) { xx-=.3f; yy-=.3f; w+=.6f; h+=.6f; }
            poly(new float[]{xx,yy,xx+w,yy,xx+w,yy+h,xx,yy+h},c);
        }
        public void circle(float xx,float yy,float r,int c) { p.circle(tx(xx,yy),ty(yy),r*scale,c); }
        public void poly(float[] a,int c) { float[] b=new float[a.length]; for(int i=0;i<a.length;i+=2) { b[i]=tx(a[i],a[i+1]); b[i+1]=ty(a[i+1]); } p.poly(b,c); }
        public void line(float xx,float yy,float bx,float by,float w,int c) { p.line(tx(xx,yy),ty(yy),tx(bx,by),ty(by),w*scale,c); }
    }
}
