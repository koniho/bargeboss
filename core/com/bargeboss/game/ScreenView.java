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
        int seconds=Math.max(0,(Game.SHIFT_TICKS-g.ticks+59)/60);
        String time=g.tutorial==null?(seconds/60)+":"+(seconds%60<10?"0":"")+(seconds%60):"FREE";
        if(layout.rails) {
            p.rect(0,0,162,layout.height,NAVY); p.rect(layout.width-162,0,162,layout.height,NAVY);
            p.rect(158,12,3,layout.height-24,GOLD); p.rect(layout.width-161,12,3,layout.height-24,GOLD);
            center(p,"BARGE",81,22,25,PAPER); center(p,"BOSS",81,55,27,GOLD);
            center(p,"CALUMET REACH",81,96,10,MUTED);
            railMetric(p,"SCORE",""+g.score,137,PAPER);
            railMetric(p,"LOADS",g.delivered+" / "+Game.QUOTA,225,GOLD);
            railMetric(p,"HULL",g.hull+"%",313,g.hull<30?VisualStyle.RED:PAPER);
            railMetric(p,g.tutorial==null?"SHIFT":"PRACTICE",time,401,seconds<30?GOLD:PAPER);
        } else {
            p.rect(0,0,layout.width,Layout.HEADER_HEIGHT,NAVY); p.rect(0,0,layout.width,3,GOLD);
            Painter ui=offset(p,layout.offsetX,0);
            VisualStyle.panel(ui,16,10,258,60,0xff203047);
            text(ui,"BARGE BOSS",28,20,24,PAPER); text(ui,"01 / CALUMET INDUSTRIAL REACH",29,53,9,GOLD);
            metric(ui,"SCORE",""+g.score,294,PAPER);
            metric(ui,"LOADS",g.delivered+" / "+Game.QUOTA,430,GOLD);
            metric(ui,"HULL",g.hull+"%",555,g.hull<30?VisualStyle.RED:PAPER);
            metric(ui,g.tutorial==null?"SHIFT":"PRACTICE",time,674,seconds<30?GOLD:PAPER);
            p.rect(16,77,layout.width-32,3,0xff53617a);
        }
        haptics(g,p);
        Layout.Box pause=layout.box(Layout.PAUSE);
        VisualStyle.panel(p,pause.x,pause.y,pause.w,pause.h,0xff293a4a);
        p.rect(pause.centerX()-11,pause.y+17,6,26,PAPER); p.rect(pause.centerX()+4,pause.y+17,6,26,PAPER);
    }
    private static void railMetric(Painter p,String label,String value,float y,int color) {
        center(p,label,81,y,11,MUTED); center(p,value,81,y+23,25,color);
    }
    private static void metric(Painter p,String label,String value,float x,int color) {
        text(p,label,x,17,10,MUTED); text(p,value,x,37,23,color);
    }
    private static void haptics(Game g,Painter p) {
        Layout.Box b=g.layout.box(Layout.HAPTICS);
        VisualStyle.panel(p,b.x,b.y,b.w,b.h,0xff203047);
        center(p,"HAPTICS",b.centerX(),b.y+13,10,MUTED);
        center(p,g.hapticsEnabled?"ON":"OFF",b.centerX(),b.y+34,13,g.hapticsEnabled?GOLD:PAPER);
    }
    public static void title(Game g,Painter p) {
        harbor(g,p); haptics(g,p);
        p=centered(g,p);
        text(p,"A CALUMET RIVER ARCADE",25,23,11,PAPER);
        p.poly(new float[]{44,132,125,110,143,66,533,66,492,238,133,270,151,240,32,246,78,169},INK);
        p.poly(new float[]{55,132,133,117,150,74,521,74,484,231,143,258,163,231,47,237,91,169},0xffa92d37);
        p.poly(new float[]{70,134,143,125,158,82,509,82,476,223,156,246,173,223,62,227,103,169},NAVY);
        logo(p,"BARGE",76,93,84,PAPER); logo(p,"BOSS",155,181,93,GOLD);
        float lamp=1+(float)StrictMath.sin(g.visualTicks*.065)*.25f;
        p.rect(99,263,15*lamp,4,GOLD); p.rect(496,91,5,19*lamp,PAPER);
        center(p,"HEAVY CARGO. BIG PAYDAY.",302,292,15,PAPER);
        Body cargo=new Body(1,2,0,0,-.12f); cargo.vx=28;
        VesselView.draw(g,new Transform(p,455,431,3.2f,0),cargo,false);
        Body tug=new Body(0,-1,0,0,-.12f); tug.vx=32;
        VesselView.draw(g,new Transform(p,219,460,3.7f,0),tug,true);
        p.line(306,417,351,421,4,0xffd9bf7d);
        VisualStyle.panel(p,608,96,324,414,0xf5101c30);
        center(p,"SHIFT 01",770,118,11,GOLD);
        center(p,"CALUMET REACH",770,147,22,PAPER);
        center(p,"6 LOADS. 3 MINUTES.",770,190,13,GOLD);
        center(p,"DRAG TUG. HOLD CARGO.",770,234,12,PAPER);
        center(p,"AIM LINE. RELEASE TO THROW.",770,260,11,PAPER);
        center(p,"CREWS UNLOAD IN 3 SECONDS.",770,286,11,MUTED);
        button(g,p,Layout.LEARN,"LEARN TO DRIVE",false);
        button(g,p,Layout.START,"START SHIFT",true);
        center(p,"ONE BUSY RIVER. ONE GOOD CAPTAIN.",475,548,15,PAPER);
        text(p,"CALUMET HARBOR AUTHORITY",24,580,9,MUTED);
        text(p,"DRAG / PUSH / THROW / UNLOAD",709,580,9,GOLD);
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
        instruction(p,1,199,"DRAG THE TUG","Pull farther for thrust. Release to coast.");
        instruction(p,2,275,"HOLD THE CARGO","Keep still to push. Drag to steer.");
        instruction(p,3,351,"AIM, THEN RELEASE","Drag cargo toward a bollard. Release.");
        text(p,"MISSES RETRACT. AIM AGAIN.",159,409,11,GOLD);
        text(p,"CREWS UNLOAD IN 3 SECONDS, THEN SCORE.",159,445,10,MUTED);
        button(g,p,Layout.LEARN,"PRACTICE CONTROLS",false);
        button(g,p,Layout.START,"RESUME SHIFT",true);
        center(p,"YOUR SHIFT WAITS HERE.",770,231,12,MUTED);
        center(p,"PRACTICE HAS NO TIME LIMIT.",770,257,11,MUTED);
    }
    private static void instruction(Painter p,int index,float y,String title,String detail) {
        VisualStyle.panel(p,106,y-3,39,39,0xff344453); center(p,""+index,126,y+6,20,GOLD);
        text(p,title,159,y,16,PAPER); text(p,detail,159,y+28,10,MUTED);
    }
    public static void pause(Game g,Painter p) {
        p=menu(g,p);
        overlay(p,113,370);
        p.rect(451,142,18,37,GOLD); p.rect(492,142,18,37,GOLD);
        center(p,"SHIFT PAUSED",480,203,28,PAPER);
        center(p,"LINES HELD. ENGINES QUIET.",480,253,12,MUTED);
        button(g,p,Layout.RESUME,"RESUME",true); button(g,p,Layout.RESTART,"RESTART SHIFT",false);
    }
    public static void finish(Game g,Painter p) {
        p=menu(g,p);
        overlay(p,102,434);
        boolean complete=g.delivered>=Game.QUOTA;
        center(p,g.hull<=0?"TUG OUT OF SERVICE":complete?"SHIFT COMPLETE":"SHIFT ENDED",480,133,26,PAPER);
        int earned=complete?1+(g.lost==0?1:0)+(g.hull>=80?1:0):0;
        for(int i=0;i<3;i++) star(p,400+i*80,206+(earned>i?2*(float)StrictMath.sin(g.visualTicks*.05+i):0),25,earned>i?GOLD:0xff465366);
        center(p,""+g.score,480,252,43,GOLD); center(p,"POINTS EARNED",480,309,10,MUTED);
        center(p,g.delivered+" DELIVERED / "+g.lost+" EXPIRED",480,350,13,PAPER);
        center(p,"HULL REMAINING "+g.hull+"%",480,377,12,MUTED);
        center(p,complete?"GOOD WORK, CAPTAIN.":"THE NEXT SHIFT IS YOURS.",480,416,13,PAPER);
        button(g,p,Layout.AGAIN,"ANOTHER SHIFT",true);
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
        public void rect(float xx,float yy,float w,float h,int c) {
            if(shear!=0) { xx-=.3f; yy-=.3f; w+=.6f; h+=.6f; }
            poly(new float[]{xx,yy,xx+w,yy,xx+w,yy+h,xx,yy+h},c);
        }
        public void circle(float xx,float yy,float r,int c) { p.circle(tx(xx,yy),ty(yy),r*scale,c); }
        public void poly(float[] a,int c) { float[] b=new float[a.length]; for(int i=0;i<a.length;i+=2) { b[i]=tx(a[i],a[i+1]); b[i+1]=ty(a[i+1]); } p.poly(b,c); }
        public void line(float xx,float yy,float bx,float by,float w,int c) { p.line(tx(xx,yy),ty(yy),tx(bx,by),ty(by),w*scale,c); }
        public void sprite(int atlas,int frame,float cx,float cy,float width,float height,float rotation,float alpha) {
            p.sprite(atlas,frame,x+cx*scale,y+cy*scale,width*scale,height*scale,rotation,alpha);
        }
        public void spriteBlend(int atlas,int frameA,int frameB,float cx,float cy,float width,float height,float rotation,float blend,float alpha) {
            p.spriteBlend(atlas,frameA,frameB,x+cx*scale,y+cy*scale,width*scale,height*scale,rotation,blend,alpha);
        }
    }
}
