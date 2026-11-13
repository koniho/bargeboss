package com.bargeboss.game;

/** Pixel water, churn and arcade rewards. Deterministic drawing only; no wake forces. */
public final class ArcadeView {
    private ArcadeView() {}
    public static void water(Game g,Painter p) {
        int tick=g.visualTicks;
        // Broken, screen-horizontal crests travel downstream and breathe at different phases.
        // Gentle ambient motion remains visible with every vessel idle; foam stays brighter.
        for(int row=0;row<(g.bridgeStage()?38:17);row++) for(int col=0;col<5;col++) {
            int seed=row*137+col*71+row*col*13;
            float phase=tick*.025f+seed;
            float x=184+col*47+seed%15+(float)StrictMath.sin(phase)*2;
            float y=g.channelTop()+((row*35+seed%23+tick*.025f)%(Physics.BOTTOM-g.channelTop()));
            float length=9+seed%17;
            float edge=Math.min(1,Math.min((y-g.channelTop())/18,(Physics.BOTTOM-y)/30));
            float shimmer=.5f+.5f*(float)StrictMath.sin(phase*.73f);
            int alpha=(int)((25+55*shimmer)*edge);
            float end=Math.min(Physics.BOTTOM,y+length);
            p.line(x-2,y,x-2,end,2.5f,((int)(alpha*.45f)<<24)|0x073846);
            p.line(x,y,x+.6f,end,1.2f,(alpha<<24)|0x89d5cd);
            if(seed%3==0) p.line(x+2,y+3,x+2,Math.min(end,y+length*.6f),.8f,((alpha/2)<<24)|0xc1e6d5);
        }
        // Small pools of moving golden reflections beneath the industrial lamps.
        for(int side=0;side<2;side++) for(int dock=0;dock<3;dock++) if(g.berthEnabled(dock)) for(int i=0;i<7;i++) {
            float x=(side==0?177:419)+(side==0?1:-1)*(4+(i*7)%18);
            float y=g.dockY(dock)-20+i*7;
            float flicker=(float)StrictMath.sin(tick*.08+i+dock);
            int alpha=40+(int)(35*(1+StrictMath.sin(tick*.035+i*1.8+dock)));
            p.rect(x+flicker*2,y,1.2f,3+(i%3)*2+flicker,(alpha<<24)|0xebbe64);
        }
        g.water.draw(g,p);
    }
    public static void wake(Painter p,Body b,int ticks) {
        if(b.lineAttached) return;
        float speed=b.speed();
        if(speed<4) {
            // Resting hulls gently disturb the surface instead of sitting on a flat decal.
            float pulse=(float)StrictMath.sin(ticks*.04+b.id)*1.5f;
            p.rect(b.x-15,b.y+10+pulse,13,1,0x338cceca);
            p.rect(b.x+6,b.y+12-pulse,11,1,0x4478bfbf);
            return;
        }
        float travel=(float)StrictMath.atan2(b.vy,b.vx);
        float power=Math.min(1,speed/65),c=(float)StrictMath.cos(travel),s=(float)StrictMath.sin(travel);
        float kick=b.floatingInitialized?b.wakeKickTicks/18f:(float)StrictMath.pow(Math.max(0,-StrictMath.sin(ticks*.048+b.x*.017+b.y*.009)),10);
        float stern=-(b.radius+b.half*(float)StrictMath.abs(StrictMath.cos(travel-b.angle)));
        if(kick>0) for(int side=-1;side<=1;side+=2) {
            float lastX=b.x+c*stern-s*side*9,lastY=b.y+s*stern+c*side*9;
            for(int i=1;i<=10;i++) {
                float age=(i+(ticks%10)/10f)/10f;
                float back=stern-age*(24+power*52);
                float width=side*(9+age*(8+power*21));
                width+=2*(float)StrictMath.sin(ticks*.17+i*1.4);
                float x=b.x+c*back-s*width,y=b.y+s*back+c*width;
                int alpha=(int)(((1-age)*175+25)*kick);
                int foam=(alpha<<24)|0xc8fff0;
                p.line(lastX,lastY,x,y,2.5f+power*2.5f,((int)(85*kick)<<24)|0x68c5ce);
                // Overlapping little steps read as pixel spray at gameplay size and title scale.
                float size=(2.5f+power*2)*(1-age*.45f);
                p.rect(x-size,y-size*.4f,size*2,size*.8f,foam);
                p.rect(x-size*.45f,y-size,size*.9f,size*2,foam);
                if(i%2==0) p.rect(x+side*3,y-3,2,2,((int)(187*kick)<<24)|0xf4fff5);
                lastX=x; lastY=y;
            }
        }
        // Turbulent propeller trail is centered inside the spreading V.
        if(b.berth<0) for(int i=0;i<7;i++) {
            float t=((ticks+i*7)%49)/49f;
            float back=stern-8-t*(25+power*22);
            float jitter=(float)StrictMath.sin(t*15+i)*4;
            float x=b.x+c*back-s*jitter,y=b.y+s*back+c*jitter;
            p.rect(x,y,2+3*(1-t),1.5f+power,0x88a5e9e2);
        }
        float bow=b.half+b.radius+3;
        if(kick>0) for(int side=-1;side<=1;side+=2) for(int i=0;i<4;i++) {
            float xx=bow-i*1.5f,yy=side*(3+i*3);
            p.rect(b.x+c*xx-s*yy-1,b.y+s*xx+c*yy-1,3,2,((int)(221*kick)<<24)|0xeaffef);
        }
    }
    public static void effects(Game g,Painter p) {
        if(g.impactTicks>0) {
            float t=1-g.impactTicks/45f;
            ring(p,g.impactX,g.impactY,10+40*t,0x99b8f9e8,1.4f);
            for(int i=0;i<10;i++) {
                double a=i*Math.PI/5;
                float r=12+37*t;
                float x=g.impactX+(float)StrictMath.cos(a)*r,y=g.impactY+(float)StrictMath.sin(a)*r-12*(float)StrictMath.sin(t*Math.PI);
                float size=3*(1-t)+1;
                p.rect(x-size,y,size*2,size,0xddfff5d0);
                p.rect(x,y-size,size,size*2,0xaad7fff2);
            }
        }
        if(g.celebrationTicks>0) {
            float t=1-g.celebrationTicks/90f;
            if(t<.6f) ring(p,g.celebrationX,g.celebrationY,15+95*t,0xbb8bffd0,3*(1-t));
            for(int i=0;i<12;i++) {
                double a=i*Math.PI*2/12;
                float r=12+79*t;
                float x=g.celebrationX+(float)StrictMath.cos(a)*r,y=g.celebrationY+(float)StrictMath.sin(a)*r+26*t*t;
                if(i%3==0) star(p,x,y,4.5f*(1-t*.4f),VisualStyle.GOLD);
                else {
                    p.rect(x,y,3+(i%2)*2,2,i%2==0?0xffffc65b:0xffa0ffd2);
                    if(t<.7f) p.rect(x+2,y-3,1,2,0xfffff3c9);
                }
            }
            float y=g.celebrationY-25-25*t;
            String points="+"+g.celebrationPoints;
            RiverPainter.center(p,points,g.celebrationX+2,y+3,25,VisualStyle.OUTLINE);
            RiverPainter.center(p,points,g.celebrationX,y,25,0xffffda64);
            RiverPainter.center(p,"LOAD CLEARED!",g.celebrationX+1,y+34,10,VisualStyle.OUTLINE);
            RiverPainter.center(p,"LOAD CLEARED!",g.celebrationX,y+33,10,VisualStyle.CREAM);
        }
    }
    public static void star(Painter p,float x,float y,float radius,int color) {
        float[] points=new float[20];
        for(int i=0;i<10;i++) {
            double a=-Math.PI/2+i*Math.PI/5;float r=i%2==0?radius:radius*.47f;
            points[i*2]=x+(float)StrictMath.cos(a)*r;points[i*2+1]=y+(float)StrictMath.sin(a)*r;
        }
        p.poly(points,color);
    }
    private static void ring(Painter p,float x,float y,float radius,int color,float size) {
        for(int i=0;i<24;i++) {
            double a=i*Math.PI/12;
            float xx=x+(float)StrictMath.cos(a)*radius,yy=y+(float)StrictMath.sin(a)*radius*.65f;
            p.rect(xx,yy,3+size,size,color);
        }
    }
}
