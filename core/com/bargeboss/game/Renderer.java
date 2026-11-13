package com.bargeboss.game;

/** Every stage feature, HUD, menu and glyph is shared. Native hosts draw primitives only. */
public final class Renderer {
    private static final int INK=0xff101e25, PAPER=0xffeee7d4, MUTED=0xff8eaaa9, WATER=0xff244c54;
    private Renderer() {}
    private static void text(Painter p,String s,float x,float y,float size,int color) { Lettering.draw(p,s,x,y,size,color); }
    private static void center(Painter p,String s,float x,float y,float size,int color) { Lettering.center(p,s,x,y,size,color); }
    public static void draw(Game g,Painter p) {
        p.rect(0,0,600,960,INK);
        p.rect(0,0,600,5,Game.COLORS[0]);
        text(p,"BARGE BOSS",24,23,23,PAPER); text(p,"CALUMET / SHIFT 01",25,60,10,MUTED);
        p.rect(508,18,68,67,0xff2c3c41); p.rect(531,39,6,24,PAPER); p.rect(547,39,6,24,PAPER);
        text(p,"SCORE",25,99,9,MUTED); text(p,""+g.score,25,116,16,PAPER);
        text(p,"DELIVERIES",192,99,9,MUTED); text(p,g.delivered+" / "+Game.QUOTA,192,116,16,PAPER);
        text(p,"HULL",354,99,9,MUTED); text(p,g.hull+"%",354,116,16,g.hull<30?Game.COLORS[2]:PAPER);
        int seconds=Math.max(0,(Game.SHIFT_TICKS-g.ticks+59)/60);
        text(p,"SHIFT",488,99,9,MUTED); text(p,(seconds/60)+":"+(seconds%60<10?"0":"")+(seconds%60),488,116,16,PAPER);
        river(g,p);
        p.rect(0,836,600,124,INK); p.rect(24,836,552,1,0xff476064);
        String status=g.pushing?"PUSH ASSIST / DRAG TO STEER":g.noticeTicks>0?g.notice:"TAP WATER TO MOVE / HOLD CARGO TO PUSH";
        center(p,status,300,850,10,g.pushing?Game.COLORS[0]:PAPER);
        button(p,30,876,168,51,"BRAKE",0xff30464b,PAPER);
        button(p,216,876,168,51,"PAUSE",0xff30464b,PAPER);
        button(p,402,876,168,51,"HELP",0xff30464b,PAPER);
        center(p,"INDUSTRIAL CORRIDOR / PROTOTYPE 0.1",300,943,8,MUTED);
        if(g.mode==Game.TITLE || g.mode==Game.HELP) title(g,p);
        if(g.mode==Game.PAUSE) pause(p);
        if(g.mode==Game.FINISH) finish(g,p);
    }
    private static void river(Game g,Painter p) {
        p.rect(0,148,600,688,0xff424b49);
        // Bank service roads and parallel freight rail.
        for(int side=0;side<2;side++) {
            float ox=side==0?0:492;
            p.rect(ox+8,148,27,688,0xff313d3e);
            for(int y=158;y<836;y+=22) p.rect(ox+20,y,2,9,0xff788077);
            p.line(ox+48,148,ox+48,836,2,0xffa4a08c); p.line(ox+58,148,ox+58,836,2,0xffa4a08c);
            for(int y=150;y<836;y+=12) p.line(ox+43,y,ox+63,y,3,0xff292f30);
            for(int y=246;y<820;y+=185) {
                p.rect(ox+69,y,33,67,0xff283639); p.rect(ox+66,y-4,32,65,0xff798078);
                for(int j=0;j<6;j++) p.line(ox+69,y+j*9,ox+95,y+j*9,2,0xff565e59);
            }
        }
        p.rect(103,148,394,688,0xff202f33); p.rect(110,148,380,688,WATER);
        for(int y=151;y<830;y+=24) for(int x=117;x<484;x+=37) {
            float wave=(float)StrictMath.sin((g.ticks/60f)+x*.04+y*.07)*4;
            p.line(x,y+wave,x+12,y+wave,1,0xff38606a);
        }
        // Main current arrows, kept faint beneath the working berths.
        for(int y=252;y<810;y+=105) {
            p.line(302,y,302,y+18,1.5f,0xff47767a); p.line(297,y+13,302,y+18,1.5f,0xff47767a);
            p.line(307,y+13,302,y+18,1.5f,0xff47767a);
        }
        // Lift bridge: upper playable boundary is the fender line below it.
        p.rect(0,165,600,41,0xff263335); p.rect(0,171,600,25,0xff838777);
        for(int x=4;x<600;x+=40) {
            p.line(x,170,x+35,197,3,0xff394447); p.line(x+35,170,x,197,3,0xff394447);
        }
        p.rect(102,159,11,51,0xffc4b994); p.rect(487,159,11,51,0xffc4b994);
        text(p,"CALUMET LIFT BRIDGE",186,151,8,PAPER);
        for(int i=0;i<3;i++) dock(p,i,g.ticks);
        // Coal piles and silos, original procedural artwork.
        for(int i=0;i<3;i++) { p.circle(83,380+i*20,13,0xff263538); p.circle(80,377+i*20,11,0xff858d80); p.circle(80,377+i*20,7,0xffa9aa96); }
        for(int i=0;i<5;i++) p.circle(545+i%2*17,701+i*12,16,0xff202c2e);
        crane(p,83,583,1,Game.COLORS[0]); crane(p,518,336,-1,Game.COLORS[2]);
        text(p,"STEEL",4,550,8,0xffb9b49d); text(p,"YARD",4,563,8,0xffb9b49d);
        text(p,"BULK",539,653,8,0xffb9b49d); text(p,"WORKS",535,666,8,0xffb9b49d);
        // Passing workboat lane with an explicit warning before entering.
        if(g.trafficWarning() || g.trafficActive()) {
            for(int x=113;x<486;x+=18) p.line(x,395,x+8,395,2,0x88efa94a);
            if(g.trafficWarning()) center(p,"CROSS TRAFFIC INBOUND",300,372,10,Game.COLORS[0]);
        }
        if(g.trafficActive()) {
            Body ship=new Body(-1,-1,g.trafficX(),395,0); hull(p,ship,0xff9bac9f,false,g.ticks);
        }
        if(g.navigating && !g.pushing) {
            for(int i=1;i<12;i++) {
                float t=i/12f; p.circle(g.tug.x+(g.targetX-g.tug.x)*t,g.tug.y+(g.targetY-g.tug.y)*t,1.3f,0x99eee7d4);
            }
            p.circle(g.targetX,g.targetY,8,0x99eee7d4); p.circle(g.targetX,g.targetY,5,WATER);
            p.line(g.targetX-13,g.targetY,g.targetX+13,g.targetY,1,PAPER);
            p.line(g.targetX,g.targetY-13,g.targetX,g.targetY+13,1,PAPER);
        }
        for(int i=0;i<g.cargo.length;i++) {
            Body b=g.cargo[i]; if(b==null || !b.active) continue;
            int color=Game.COLORS[b.berth];
            if(g.selected==i) {
                p.circle(b.x,b.y,45,0x22eee7d4);
                for(int j=1;j<10;j++) {
                    float t=j/10f; p.circle(b.x+(Game.DOCK_X[b.berth]-b.x)*t,b.y+(Game.DOCK_Y[b.berth]-b.y)*t,2,0x99ffffff & color);
                }
            }
            hull(p,b,color,false,g.ticks);
            center(p,""+(char)('A'+b.berth)+"-"+(b.id<10?"0":"")+b.id,b.x,b.y-49,9,PAPER);
            p.rect(b.x-25,b.y+39,50,3,0xff18343c);
            p.rect(b.x-25,b.y+39,50*Math.max(0,1-b.age/68),3,b.age>48?Game.COLORS[2]:color);
            if(b.dockTime>0) { p.rect(b.x-25,b.y+45,50*b.dockTime/1.2f,4,PAPER); }
        }
        hull(p,g.tug,Game.COLORS[0],true,g.ticks);
        if(g.ticks<600) { center(p,"YOU",g.tug.x,g.tug.y+35,9,PAPER); }
        p.rect(110,817,380,19,0xff1b3b43); center(p,"SOUTH REACH / CURRENT  v",300,823,8,MUTED);
    }
    private static void dock(Painter p,int index,int ticks) {
        float x=Game.DOCK_X[index]-55,y=Game.DOCK_Y[index]-53; int color=Game.COLORS[index];
        p.rect(x,y,110,106,(color&0xffffff)|0x22000000);
        for(int k=0;k<110;k+=16) {
            p.rect(x+k,y,9,2,color); p.rect(x+k,y+104,9,2,color);
        }
        for(int k=0;k<106;k+=16) { p.rect(x,y+k,2,9,color); p.rect(x+108,y+k,2,9,color); }
        center(p,""+(char)('A'+index),x+55,y+20,27,(color&0xffffff)|0x88000000);
        center(p,Game.CARGO_NAMES[index],x+55,y+60,10,color);
        center(p,"BERTH "+(char)('A'+index),x+55,y-17,9,color);
        float edge=index==1?488:106;
        for(int k=0;k<5;k++) { p.rect(edge,y+k*23,6,12,0xff131f23); p.rect(edge,y+k*23,6,4,color); }
    }
    private static void crane(Painter p,float x,float y,int direction,int color) {
        p.rect(x-10,y-10,20,20,0xff1b2d32); p.rect(x-7,y-7,14,14,color);
        p.line(x,y,x+direction*60,y-28,4,color); p.line(x,y+7,x+direction*60,y-28,2,0xff8b815b);
        p.line(x+direction*60,y-28,x+direction*60,y-9,1,PAPER); p.circle(x+direction*60,y-7,3,PAPER);
    }
    private static float[] shape(Body b,float[] local,float ox,float oy) {
        float[] result=new float[local.length]; float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        for(int i=0;i<local.length;i+=2) { result[i]=b.x+ox+local[i]*c-local[i+1]*s; result[i+1]=b.y+oy+local[i]*s+local[i+1]*c; }
        return result;
    }
    private static void block(Painter p,Body b,float x,float y,float w,float h,int color) {
        p.poly(shape(b,new float[]{x,y,x+w,y,x+w,y+h,x,y+h},0,0),color);
    }
    private static void hull(Painter p,Body b,int color,boolean tug,int tick) {
        float len=b.half+b.radius;
        float[] silhouette={-len,-11,len-7,-14,len,0,len-7,14,-len,11};
        p.poly(shape(b,silhouette,4,5),0x55091219);
        if(b.speed()>12) for(int i=0;i<3;i++) {
            float behind=-len-7-i*9-(tick%18)*.3f;
            float[] wake=shape(b,new float[]{behind,-8-i*3,behind-6,-5-i*3,behind-6,5+i*3,behind,8+i*3},0,0);
            p.line(wake[0],wake[1],wake[2],wake[3],1.4f,0x7782b2b4);
            p.line(wake[4],wake[5],wake[6],wake[7],1.4f,0x7782b2b4);
        }
        p.poly(shape(b,silhouette,0,0),0xff13272c);
        block(p,b,-len+4,-10,len*2-12,20,0xffa4a68c);
        if(tug) {
            block(p,b,-16,-10,24,20,color); block(p,b,-10,-8,14,16,PAPER);
            block(p,b,-2,-7,4,14,0xff3b6268); block(p,b,10,-9,4,18,0xff121e23);
            block(p,b,-17,-4,4,8,0xff4b5756);
            float[] light=shape(b,new float[]{14,-10,14,10},0,0);
            p.circle(light[0],light[1],2,0xff80d9a2); p.circle(light[2],light[3],2,0xffee796a);
        } else {
            block(p,b,-24,-10,46,20,color);
            for(int i=-20;i<22;i+=6) block(p,b,i,-8,1.3f,16,0x440c1b20);
            block(p,b,-2,-10,2,20,0xff293d40);
        }
    }
    private static void button(Painter p,float x,float y,float w,float h,String label,int bg,int fg) {
        p.rect(x,y,w,h,bg); center(p,label,x+w/2,y+(h-13)/2,13,fg);
    }
    private static void panel(Painter p,float top,float height) {
        p.rect(0,148,600,812,0xbb0b1820); p.rect(40,top,520,height,INK);
        p.rect(40,top,520,4,Game.COLORS[0]);
    }
    private static void title(Game g,Painter p) {
        panel(p,258,500);
        text(p,"01 / CALUMET REACH",75,290,12,Game.COLORS[0]);
        text(p,"HEAVY CARGO.",75,329,29,PAPER); text(p,"TIGHT WATER.",75,370,29,PAPER);
        text(p,"A THREE-MINUTE INDUSTRIAL RIVER SHIFT",75,423,10,MUTED);
        text(p,"01",75,466,15,Game.COLORS[0]); text(p,"TAP WATER TO MOVE YOUR TUG",119,467,11,PAPER);
        text(p,"02",75,506,15,Game.COLORS[0]); text(p,"HOLD CARGO TO LINE UP AND PUSH",119,507,11,PAPER);
        text(p,"03",75,546,15,Game.COLORS[0]); text(p,"DRAG WHILE HOLDING TO STEER",119,547,11,PAPER);
        text(p,"MATCH BERTH LETTERS. SLOW DOWN TO DOCK.",75,595,10,MUTED);
        text(p,"DELIVER 6 LOADS. WATCH THE CROSS TRAFFIC.",75,615,10,MUTED);
        button(p,100,650,400,76,g.mode==Game.HELP?"RESUME SHIFT":"START SHIFT",Game.COLORS[0],INK);
    }
    private static void pause(Painter p) {
        panel(p,358,378); center(p,"SHIFT PAUSED",300,399,26,PAPER);
        center(p,"THE RIVER CAN WAIT.",300,453,12,MUTED);
        button(p,100,540,400,72,"RESUME",Game.COLORS[0],INK);
        button(p,100,632,400,72,"RESTART SHIFT",0xff30464b,PAPER);
    }
    private static void finish(Game g,Painter p) {
        panel(p,308,450);
        center(p,g.hull<=0?"TUG OUT OF SERVICE":g.delivered>=Game.QUOTA?"SHIFT COMPLETE":"SHIFT ENDED",300,350,23,PAPER);
        center(p,g.delivered>=Game.QUOTA?"QUOTA MET. GOOD WORK, CAPTAIN.":"THE NEXT SHIFT IS YOURS.",300,400,11,MUTED);
        center(p,""+g.score,300,455,43,Game.COLORS[0]);
        center(p,"POINTS EARNED",300,516,10,MUTED);
        center(p,g.delivered+" DELIVERED / "+g.lost+" EXPIRED",300,561,13,PAPER);
        center(p,"HULL REMAINING "+g.hull+"%",300,593,11,MUTED);
        button(p,100,650,400,76,"ANOTHER SHIFT",Game.COLORS[0],INK);
    }
}
