package com.bargeboss.game;

/** Three fixed, horizontal harbor planes. Only their shared parallax offsets move. */
final class TitleShoreView {
    static final int ROCK_SHADOW=0xff101c22;
    private static final int[] FACTORIES={6,4,0,1,3,7};
    private TitleShoreView() {}
    private static float drift(int tick,float speed,float period){return (tick*speed)%period;}
    static void draw(Game g,Painter p){
        float h=TitleView.horizon(g),extent=g.layout.width;
        // Distant authored industrial modules, with atmospheric separation from the bridge.
        float d=drift(g.visualTicks,.055f,900);
        for(float x=-d-900;x<extent;x+=900)for(int i=0;i<FACTORIES.length;i++){
            int frame=FACTORIES[i];float size=i%3==0?190:155;
            float center=x+65+i*150;if(center+size*.5f<0 || center-size*.5f>extent)continue;
            p.sprite(SpriteAtlas.FACTORY,frame,center,h-25,size,size,0,.62f);
        }
        p.rect(0,h-16,extent,34,0xff15252c);
        // A distinct middle plane: static steel truss bridges and elevated pipe runs.
        d=drift(g.visualTicks,.12f,780);
        for(float x=-d-780;x<extent;x+=780){
            if(x+406>=0 && x+42<extent)bridge(p,x+60,h+12);
            if(x+680>=0 && x+450<extent)pipes(p,x+450,h+10);
        }
        // Low riprap breakwater is nearest; the navigable opening remains visibly water.
        d=(drift(g.visualTicks,.21f,960)+Math.max(0,625-(extent-352-225)))%960;
        for(float x=-d-960;x<extent;x+=960){
            if(x+634>=0)wall(p,x,h+22,625);
            if(x+969>=0 && x+796<extent)wall(p,x+805,h+22,155);
            beacon(p,x+617,h+21,0xffef705a);beacon(p,x+813,h+21,0xff7bd8ad);
        }
    }
    private static void bridge(Painter p,float x,float y){
        int steel=0xff293039,rust=0xff55403a;
        p.rect(x-18,y-12,32,27,0xff1b272b);p.rect(x+314,y-12,32,27,0xff1b272b);
        p.poly(new float[]{x,y-17,x+330,y-17,x+340,y-24,x+10,y-24},0xff3e4141);
        p.rect(x,y-17,330,8,steel);
        for(int side=0;side<2;side++){
            float dy=-side*7;
            p.line(x,y-18+dy,x+330,y-18+dy,4,rust);
            p.line(x,y-64+dy,x+330,y-64+dy,4,rust);
            for(int i=0;i<=6;i++)p.line(x+i*55,y-18+dy,x+i*55,y-64+dy,3,steel);
            for(int i=0;i<6;i++)p.line(x+i*55,y-(i%2==0?18:64)+dy,x+(i+1)*55,y-(i%2==0?64:18)+dy,3,rust);
        }
        for(int i=0;i<=3;i++){
            float xx=x+i*110;p.line(xx,y-64,xx,y-71,2,steel);
            p.circle(xx,y-72,1.8f,0xffdba36b);
        }
        for(int end=0;end<2;end++){
            float xx=x+end*330;
            p.poly(new float[]{xx+8,y-92,xx+15,y-97,xx+15,y-14,xx+8,y-9},0xff1c272e);
            p.rect(xx-8,y-92,16,83,steel);
            p.rect(xx-12,y-95,24,6,rust);p.circle(xx,y-99,2,0xffb45b50);
        }
    }
    private static void pipes(Painter p,float x,float y){
        for(int i=0;i<4;i++)p.rect(x+i*75,y-39,5,40,0xff25333a);
        p.line(x,y-39,x+230,y-39,8,0xff493c39);
        p.line(x,y-41,x+230,y-41,1,0xff6b5143);
        for(int i=0;i<8;i++)p.line(x+i*31,y-44,x+i*31,y-34,2,0xff30353b);
    }
    private static void wall(Painter p,float x,float y,float width){
        p.poly(new float[]{x,y,x+width,y,x+width+9,y+32,x-9,y+32},ROCK_SHADOW);
        // Stable small number of broad angular boulders; no random state or per-frame tessellation.
        for(int row=0;row<2;row++)for(int i=0;i<(int)(width/27);i++){
            int seed=(i*73+row*37)%101;
            float xx=x+3+i*27+(row==0?0:5),yy=y+row*14;
            float cut=3+seed%6,rise=seed%5,w=23+seed%5;
            float[] rock={xx,yy+4,xx+cut,yy-rise,xx+w-4,yy+1,xx+w,yy+10,xx+w-5,yy+15,xx+4,yy+14};
            p.poly(rock,(i+row)%3==0?0xff384348:(i+row)%3==1?0xff303b40:0xff29353c);
            p.poly(new float[]{xx,yy+4,xx+cut,yy-rise,xx+w-4,yy+1,xx+w-7,yy+6,xx+5,yy+7},
                    seed%2==0?0xff465055:0xff3d494e);
            p.line(xx+w-7,yy+6,xx+w,yy+10,.8f,0xff202d33);
        }
        p.line(x-4,y+32,x+width+5,y+32,2,0xff203238);
    }
    private static void beacon(Painter p,float x,float y,int color){
        p.rect(x-6,y-2,12,9,0xff3c4547);p.rect(x-2,y-23,4,22,0xff536163);
        p.rect(x-4,y-24,8,3,0xff25343b);p.circle(x,y-27,2.3f,color);
        p.circle(x,y-27,5,(color&0xffffff)|0x22000000);
    }
}
