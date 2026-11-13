package com.bargeboss.game;

/** The concept harbor, built from shaded pixel-sized surfaces in the shared river coordinates. */
public final class HarborView {
    private static final int DARK=VisualStyle.OUTLINE, GOLD=VisualStyle.GOLD;
    private HarborView() {}
    public static void draw(Game g,Painter p) {
        int tick=g.visualTicks;
        // Extend the industrial shore to the viewport edges without distorting the river.
        RiverCamera c=g.layout.camera;
        float minX=c.worldX(0,g.layout.height),maxX=c.worldX(0,0);
        float minY=c.worldY(0,0),maxY=c.worldY(g.layout.width,0);
        p.rect(minX,minY,maxX-minX,maxY-minY,0xff536374);
        for(int row=(int)(minX/30)-1;row<maxX/30+1;row++)
            for(int col=(int)(minY/40)-1;col<maxY/40+1;col++) {
                p.rect(row*30+1,col*40+1,28,38,(row+col)%3==0?0xff697382:0xff606c7a);
                p.rect(row*30+3,col*40+3,24,1,0xff7b8490);
            }
        p.rect(170,minY,260,maxY-minY,VisualStyle.TEAL);
        p.rect(140,148,320,688,0xff343f52);
        p.rect(167,148,266,688,0xff082e40);
        p.rect(170,148,260,688,VisualStyle.TEAL);
        ArcadeView.water(g,p);
        quay(p,140,148,30,688,false,tick);
        quay(p,430,148,30,688,true,tick);
        // A narrow shore keeps the whole navigable channel large in the landscape view.
        container(p,144,424,20,14,0xffb94535); container(p,145,442,19,14,0xff365d85);
        container(p,143,458,22,14,0xffcd7e3d);
        container(p,435,254,20,14,0xffb54b35); container(p,436,274,19,14,0xff335674);
        container(p,434,294,21,14,0xff527e91);
        warehouse(p,435,354,20,38,0xff6a7485);
        silo(p,148,577); silo(p,160,620);
        for(int i=0;i<3;i++) {
            float x=445,y=641+i*23;
            p.poly(new float[]{x-11,y+10,x-9,y-2,x-3,y-10,x+5,y-6,x+11,y+7,x+7,y+12},DARK);
            p.poly(new float[]{x-7,y+5,x-5,y-1,x-1,y-6,x+5,y-4,x+8,y+6},0xff5e5b60);
            p.rect(x-3,y-2,4,2,0xff918475);
        }
        warehouse(p,435,762,20,38,0xff735d64);
        for(int i=0;i<3;i++) berth(g,p,i);
        bridge(g,p);
        for(int side=0;side<2;side++) {
            float x=side==0?165:435;
            for(int y=235;y<810;y+=57) pylon(p,x,y,side==0?1:-1,tick);
        }
        for(int i=0;i<3;i++) lamp(p,i==1?428:172,Game.DOCK_Y[i]-42,tick+i*31);
        // The channel stays open at the edge for the two feeder lanes.
    }
    private static void quay(Painter p,float x,float y,float w,float h,boolean right,int tick) {
        p.rect(x,y,w,h,0xff737582);
        for(int row=0;row<23;row++) {
            float yy=y+row*30;
            int tone=(row*7)%5;
            p.rect(x+1,yy+1,w-2,Math.min(28,y+h-yy-1),tone<2?0xff898493:tone==2?0xff94909b:0xff7d7d89);
            p.rect(x+3,yy+3,w-6,1,0xffa4a0a8);
            if(row%3==0) {
                p.rect(x+8,yy+16,4,2,0xff626879); p.rect(x+17,yy+22,6,2,0xffa7a0a3);
                p.line(x+24,yy+7,x+20,yy+12,1,0xff626879);
            }
        }
        float edge=right?x:x+w;
        p.rect(right?edge-3:edge-5,y,8,h,DARK);
        p.rect(right?edge:edge-5,y,5,h,0xff4e596b);
        p.rect(right?edge+5:edge-9,y,4,h,0xffc0b6ab);
        for(int yy=(int)y+8;yy<y+h;yy+=19) {
            p.rect(right?edge:edge-4,yy,4,12,0xff2f4055);
            p.rect(right?edge-6:edge+1,yy+5,5,4,0x554e98a4);
        }
        // A single shoreline rail avoids shrinking the open water to make room for roads.
        float lane=right?x+w-3:x+2;
        p.line(lane,y,lane,y+h,2,0xff424b60);
        for(int yy=(int)y+9;yy<y+h-10;yy+=27) p.rect(lane-1,yy,2,10,0xffd2bf8e);
        if(tick%180<90) p.rect(lane-1,802,2,3,0xffe9b448);
    }
    private static void container(Painter p,float x,float y,float w,float h,int color) {
        p.rect(x+3,y+5,w,h,DARK);
        p.poly(new float[]{x,y,x+w,y,x+w+4,y-5,x+4,y-5},0xff9b867c);
        p.rect(x,y,w,h,color); p.rect(x,y+h-3,w,3,0xff4c3944);
        p.rect(x+2,y+1,w-4,2,0xffe5b179);
        for(int i=5;i<w-2;i+=6) p.rect(x+i,y+3,2,h-6,0x66273544);
        p.line(x+w,y,x+w,y+h,2,DARK);
    }
    private static void warehouse(Painter p,float x,float y,float w,float h,int color) {
        p.rect(x+4,y+7,w,h,DARK);
        p.rect(x,y,w,h,color); p.rect(x+1,y+h-12,w-2,12,0xff45475b);
        p.poly(new float[]{x-3,y,x+w/2,y-12,x+w+3,y,x+w+3,y+7,x-3,y+7},0xff293e52);
        p.line(x-2,y+1,x+w/2,y-10,2,0xff7f8c9c);
        for(int i=7;i<w-6;i+=12) { p.rect(x+i,y+12,7,7,DARK); p.rect(x+i+1,y+13,5,4,0xffc48b53); }
        p.rect(x+w/2-8,y+h-12,16,12,DARK);
        for(int i=0;i<4;i++) p.line(x+w/2-6,y+h-10+i*3,x+w/2+6,y+h-10+i*3,1,0xff77848a);
    }
    private static void silo(Painter p,float x,float y) {
        p.rect(x-8,y,17,37,DARK); p.rect(x-7,y-1,14,35,0xff999b9c);
        p.rect(x-5,y,4,31,0xffcdc5b7); p.rect(x+4,y+1,3,33,0xff697585);
        p.poly(new float[]{x-8,y,x-5,y-6,x+4,y-6,x+8,y},0xffc8c0b4);
        p.rect(x-8,y+28,17,3,0xff4b5b6e); p.rect(x-6,y+36,3,5,DARK); p.rect(x+4,y+36,3,5,DARK);
    }
    private static void pylon(Painter p,float x,float y,int sign,int tick) {
        p.rect(x-7+sign*3,y+4,14,18,0x55224752);
        p.rect(x-7,y-8,14,25,DARK); p.rect(x-5,y-7,10,22,0xff5b5863);
        p.rect(x-4,y-6,3,20,0xff8f827a); p.rect(x-6,y-10,12,5,0xffb0a18d);
        p.rect(x-4,y-9,8,3,0xffe2c386);
        p.rect(x+sign*5-3,y+2,6,10,DARK); p.rect(x+sign*5-2,y+3,4,6,0xff303f50);
        if((tick/45+(int)y)%3==0) p.rect(x-2,y-8,3,2,0xffffd36a);
    }
    static void lamp(Painter p,float x,float y,int tick) {
        int glow=120+(int)(30*StrictMath.sin(tick*.06));
        p.rect(x-4,y-3,8,8,DARK); p.rect(x-2,y-3,4,5,GOLD); p.rect(x-1,y-4,2,2,VisualStyle.CREAM);
        p.rect(x-7,y-6,14,12,(glow/4<<24)|0xffc840);
        int side=x<300?1:-1;
        for(int i=0;i<4;i++) {
            float xx=x+side*(10+i*4),yy=y+5+i*5+(tick/8+i)%3;
            p.rect(xx-2,yy,4+(i%2)*3,2,((80-i*14)<<24)|0xffc840);
        }
    }
    private static void berth(Game g,Painter p,int index) {
        float x=Game.DOCK_X[index]-55,y=Game.DOCK_Y[index]-53;
        boolean ready=false,occupied=false,working=false,selected=false;
        for(Body b:g.cargo) if(b!=null && b.active && b.berth==index) {
            ready|=g.canTie(b); occupied|=b.lineAttached; working|=b.unloadingTicks>0;
            selected|=g.workingCargo()==b;
        }
        int color=occupied?0xff8ce7c2:ready?GOLD:Game.COLORS[index];
        p.rect(x,y,110,106,(selected?0x27000000:0x14000000)|(color&0xffffff));
        int run=(g.visualTicks/6)%16;
        for(int k=0;k<110;k+=16) {
            float xx=x+(k+run)%110;
            p.rect(xx,y,Math.min(8,x+110-xx),2,color); p.rect(xx,y+104,Math.min(8,x+110-xx),2,color);
        }
        for(int k=0;k<106;k+=16) {
            float yy=y+(k+run)%106;
            p.rect(x,yy,2,Math.min(8,y+106-yy),color); p.rect(x+108,yy,2,Math.min(8,y+106-yy),color);
        }
        for(int side=0;side<2;side++) for(int end=0;end<2;end++) {
            float xx=x+side*110, yy=y+end*106, sx=side==0?1:-1,sy=end==0?1:-1;
            p.line(xx,yy,xx+sx*15,yy,4,color); p.line(xx,yy,xx,yy+sy*15,4,color);
        }
        RiverPainter.center(p,""+(char)('A'+index),x+75,y+53,22,0x6656bac1);
        RiverPainter.center(p,Game.CARGO_NAMES[index],x+34,y+53,8,0x99b8d9cf);
        String badge=(working?"UNLOAD ":occupied?"TYING ":"BERTH ")+(char)('A'+index);
        badge(p,badge,x+95,y+53,8,color);
        if(selected && !occupied && !ready) {
            float bob=3*(float)StrictMath.sin(g.visualTicks*.1),cx=x+55,cy=y+12+bob;
            p.poly(new float[]{cx-6,cy-11,cx+6,cy-11,cx+6,cy,cx+12,cy,cx,cy+12,cx-12,cy,cx-6,cy},DARK);
            p.poly(new float[]{cx-4,cy-9,cx+4,cy-9,cx+4,cy+2,cx+8,cy+2,cx,cy+9,cx-8,cy+2,cx-4,cy+2},GOLD);
        }
        float bank=index==1?433:143;
        p.rect(bank,Game.DOCK_Y[index]-52,24,3,DARK);
        for(int i=0;i<3;i++) p.rect(bank+i*8,Game.DOCK_Y[index]-52,4,3,GOLD);
        if(working) lamp(p,index==1?429:171,Game.DOCK_Y[index]+40,g.visualTicks*3);
    }
    private static void bridge(Game g,Painter p) {
        p.rect(140,148,320,61,0xff39465b);
        p.rect(140,185,320,23,DARK); p.rect(140,179,320,19,0xff716d79);
        p.rect(140,179,320,3,0xffb7a293); p.rect(140,195,320,3,0xff485368);
        for(int x=144;x<450;x+=22) p.rect(x,187,11,2,0xffd0af71);
        for(int side=0;side<2;side++) {
            float tower=side==0?164:432,sign=side==0?1:-1;
            p.rect(tower-8,153,20,60,DARK); p.rect(tower-5,154,13,53,0xff76757e);
            p.rect(tower-5,154,4,53,0xffc4b3a0); p.rect(tower-7,150,18,6,0xffb4a493);
            float end=tower+sign*56,raise=20+2*(float)StrictMath.sin(g.visualTicks*.013);
            p.poly(new float[]{tower,180,tower+sign*11,180,end+sign*11,180-raise,end,180-raise},DARK);
            p.line(tower+sign*5,179,end+sign*5,178-raise,7,0xff8c8995);
            for(int i=0;i<5;i++) {
                float t=i/5f,xx=tower+sign*(4+t*55),yy=178-t*raise;
                p.line(xx,yy,xx+sign*5,yy-2,3,GOLD);
            }
            lamp(p,tower,155,g.visualTicks+side*30);
        }
        badge(p,"CALUMET",300,180,7,VisualStyle.CREAM);
    }
    static void badge(Painter p,String text,float x,float y,float size,int color) {
        float width=Lettering.width(text,size)+10;
        if(p instanceof RiverPainter) VisualStyle.panel(p,x-size-3,y-width/2,size+6,width,VisualStyle.NAVY);
        else VisualStyle.panel(p,x-width/2,y-3,width,size+6,VisualStyle.NAVY);
        RiverPainter.center(p,text,x,y,size,color);
    }

}
