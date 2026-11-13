package com.bargeboss.game;

/** The concept harbor, built from shaded pixel-sized surfaces in the shared river coordinates. */
public final class HarborView {
    private static final int DARK=VisualStyle.OUTLINE, GOLD=VisualStyle.GOLD;
    private HarborView() {}
    public static void draw(Game g,Painter p) {
        int tick=g.visualTicks;
        p.rect(0,148,600,688,0xff343f52);
        p.rect(103,148,394,688,0xff082e40);
        p.rect(110,148,380,688,VisualStyle.TEAL);
        ArcadeView.water(g,p);
        quay(p,0,148,108,688,false,tick);
        quay(p,492,148,108,688,true,tick);
        // Small shore districts leave a clear apron around each working berth.
        warehouse(p,9,356,64,39,0xff7e6764);
        container(p,13,416,27,13,0xffb94535); container(p,42,416,31,13,0xff365d85);
        container(p,13,400,27,13,0xffcd7e3d); container(p,42,400,31,13,0xff527e97);
        container(p,17,774,55,15,0xff95423a); container(p,20,756,43,14,0xff426b8b);
        container(p,520,254,30,15,0xffb54b35); container(p,552,254,33,15,0xff335674);
        container(p,521,235,29,15,0xff527e91); container(p,553,235,32,15,0xffc78734);
        warehouse(p,524,354,67,38,0xff6a7485);
        for(int i=0;i<3;i++) silo(p,20+i*24,579+(i%2)*4);
        for(int i=0;i<5;i++) {
            float x=532+(i%2)*26,y=641+(i/2)*20;
            p.poly(new float[]{x-15,y+12,x-13,y-2,x-4,y-12,x+8,y-8,x+17,y+7,x+10,y+15},DARK);
            p.poly(new float[]{x-10,y+7,x-8,y-1,x-2,y-8,x+7,y-5,x+12,y+7},0xff5e5b60);
            p.rect(x-4,y-3,5,3,0xff918475);
        }
        warehouse(p,513,762,75,38,0xff735d64);
        for(int i=0;i<3;i++) berth(g,p,i);
        bridge(g,p);
        for(int side=0;side<2;side++) {
            float x=side==0?105:495;
            for(int y=235;y<810;y+=57) pylon(p,x,y,side==0?1:-1,tick);
        }
        // Amber work lamps cast flickering pixel reflections onto the water.
        for(int i=0;i<3;i++) {
            float x=i==1?488:112,y=Game.DOCK_Y[i]-42;
            lamp(p,x,y,tick+i*31);
        }
        p.rect(110,817,380,19,0xff123b48); p.rect(110,817,380,2,0xff4e9a96);
        Lettering.center(p,"SOUTH REACH / CALUMET",300,824,8,0xff88b3b5);
    }
    private static void quay(Painter p,float x,float y,float w,float h,boolean right,int tick) {
        p.rect(x,y,w,h,0xff737582);
        for(int row=0;row<23;row++) {
            float yy=y+row*30;
            for(int col=0;col<3;col++) {
                float xx=x+col*34;
                int tone=(row*7+col*13)%5;
                p.rect(xx+1,yy+1,32,28,tone<2?0xff898493:tone==2?0xff94909b:0xff7d7d89);
                p.rect(xx+3,yy+3,23,1,0xffa4a0a8);
                if((row+col)%3==0) {
                    p.rect(xx+8,yy+16,4,2,0xff626879); p.rect(xx+21,yy+22,6,2,0xffa7a0a3);
                    p.line(xx+26,yy+7,xx+22,yy+12,1,0xff626879);
                }
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
        // Service lane and rail ties are confined to the far bank.
        float lane=right?x+w-18:x+2;
        p.rect(lane,y,15,h,0xff424b60);
        for(int yy=(int)y+9;yy<y+h;yy+=27) p.rect(lane+6,yy,2,12,0xffd2bf8e);
        p.line(lane+16,y,lane+16,y+h,1,0xff303c52);
        if(tick%180<90) p.rect(lane+5,802,4,3,0xffe9b448);
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
        Lettering.center(p,""+(char)('A'+index),x+55,y+35,29,0x6656bac1);
        Lettering.center(p,Game.CARGO_NAMES[index],x+55,y+69,10,0x99b8d9cf);
        VisualStyle.panel(p,x+9,y-21,92,17,VisualStyle.NAVY);
        String badge=(working?"UNLOAD ":occupied?"TYING ":"BERTH ")+(char)('A'+index);
        Lettering.center(p,badge,x+55,y-16,9,color);
        if(selected && !occupied && !ready) {
            float bob=3*(float)StrictMath.sin(g.visualTicks*.1),cx=x+55,cy=y+12+bob;
            p.poly(new float[]{cx-6,cy-11,cx+6,cy-11,cx+6,cy,cx+12,cy,cx,cy+12,cx-12,cy,cx-6,cy},DARK);
            p.poly(new float[]{cx-4,cy-9,cx+4,cy-9,cx+4,cy+2,cx+8,cy+2,cx,cy+9,cx-8,cy+2,cx-4,cy+2},GOLD);
        }
        float bank=index==1?501:15;
        p.rect(bank,Game.DOCK_Y[index]-52,84,3,DARK);
        for(int i=0;i<8;i++) p.rect(bank+i*10,Game.DOCK_Y[index]-52,5,3,GOLD);
        if(working) lamp(p,index==1?493:107,Game.DOCK_Y[index]+40,g.visualTicks*3);
    }
    private static void bridge(Game g,Painter p) {
        p.rect(0,148,600,61,0xff39465b);
        p.rect(0,185,600,23,DARK); p.rect(0,179,600,19,0xff716d79);
        p.rect(0,179,600,3,0xffb7a293); p.rect(0,195,600,3,0xff485368);
        for(int x=4;x<600;x+=22) p.rect(x,187,11,2,0xffd0af71);
        for(int side=0;side<2;side++) {
            float tower=side==0?102:486,sign=side==0?1:-1;
            p.rect(tower-8,153,20,60,DARK); p.rect(tower-5,154,13,53,0xff76757e);
            p.rect(tower-5,154,4,53,0xffc4b3a0); p.rect(tower-7,150,18,6,0xffb4a493);
            float end=tower+sign*56,raise=20+2*(float)StrictMath.sin(g.visualTicks*.013);
            p.poly(new float[]{tower,180,tower+sign*11,180,end+sign*11,180-raise,end,180-raise},DARK);
            p.line(tower+sign*5,179,end+sign*5,178-raise,7,0xff8c8995);
            for(int i=0;i<5;i++) {
                float t=i/5f,xx=tower+sign*(4+t*55),yy=178-t*raise;
                p.line(xx,yy,xx+sign*5,yy-2,3,GOLD);
            }
            lamp(p,tower,151,g.visualTicks+side*30);
        }
        VisualStyle.panel(p,205,151,190,20,VisualStyle.NAVY);
        Lettering.center(p,"CALUMET LIFT BRIDGE",300,157,9,VisualStyle.CREAM);
    }
}
