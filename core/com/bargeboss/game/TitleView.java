package com.bargeboss.game;

/** Cinematic title voyage, sampled only from the shared presentation clock. */
public final class TitleView {
    private TitleView() {}
    private static float sine(double value) { return (float)StrictMath.sin(value); }
    private static float scroll(int tick,float speed,float width) { return (tick*speed)%width; }
    private static void strip(Painter p,int atlas,float width,float height,float top,float extent,float drift) {
        for(float x=-drift;x<extent;x+=width)
            p.sprite(atlas,0,x+width/2,top+height/2,width,height,0,1);
    }
    public static void background(Game g,Painter p) {
        int tick=g.visualTicks;float horizon=342+g.layout.offsetY;
        p.rect(0,0,g.layout.width,g.layout.height,0xff244c75);
        float skyHeight=horizon/.87f,skyWidth=skyHeight*3;
        strip(p,SpriteAtlas.TITLE_BACKDROP,skyWidth,skyHeight,0,g.layout.width,scroll(tick,.035f,skyWidth));
        // The low harbor passes faster than the distant skyline; identical end tiles
        // enter before old tiles leave, so the voyage has no reset or finite end.
        float harborWidth=1170,harborHeight=harborWidth/3;
        strip(p,SpriteAtlas.TITLE_HARBOR,harborWidth,harborHeight,horizon-harborHeight*.82f,
                g.layout.width,scroll(tick,.19f,harborWidth));
        float waterTop=horizon+5;
        for(int row=0;waterTop+row*42<g.layout.height;row++) {
            float y=waterTop+row*42,width=1050,height=width*.12f/3;
            strip(p,SpriteAtlas.TITLE_WATER,width,height,y,g.layout.width,scroll(tick,.38f+row*.13f,width));
        }
        // Moving broken highlights add scale and depth without moving menu targets.
        for(int row=0;row<14;row++) for(int i=0;i<12;i++) {
            float period=g.layout.width+100;
            float x=(i*113+row*39+period-scroll(tick,.45f+row*.1f,period))%period-50;
            float y=waterTop+row*20+sine(tick*.035+row+i)*2;
            if(y>g.layout.height) continue;
            int alpha=35+(int)(20*(1+sine(tick*.03+i+row)));
            p.line(x,y,x+12+(i+row)%4*7,y,1+row*.08f,(alpha<<24)|0x99cfdf);
        }
    }
    public static void voyage(Game g,Painter canvas) {
        Painter p=ScreenView.centered(g,canvas);int tick=g.visualTicks;
        float tugBob=sine(tick*.05)*3+sine(tick*.021)*1.3f;
        float bargeBob=sine(tick*.05-1.15)*2.3f+sine(tick*.021-.8)*1.1f;
        float tugX=215+sine(tick*.014)*2,tugY=427+tugBob;
        float bargeX=622+sine(tick*.014-.3)*2,bargeY=478+bargeBob;
        foam(p,tick,38,512+tugBob,1.25f,false);
        foam(p,tick+29,431,519+bargeBob,.8f,false);
        // Rope ends share the boats' heave; a little sag changes with their relative motion.
        p.line(tugX+204,tugY+62,bargeX-182,bargeY+27,5,0xff18283d);
        p.line(tugX+204,tugY+61,bargeX-182,bargeY+26,2.6f,0xffe2be79);
        p.sprite(SpriteAtlas.TITLE_VESSELS,1,bargeX,bargeY,440,440*424/1536f,sine(tick*.05-1.15)*.009f,1);
        p.sprite(SpriteAtlas.TITLE_VESSELS,0,tugX,tugY,460,460*540/1100f,sine(tick*.05)*.012f,1);
        smoke(p,tick,tugX-76,tugY-36);
        foam(p,tick,45,533+tugBob,1.05f,true);
        foam(p,tick+29,433,521+bargeBob,.9f,true);
        // A separate little bow splash makes the tug feel powered on every wave cycle.
        for(int i=0;i<8;i++) {
            float age=((tick+i*8)%64)/64f;
            float x=425+age*32,y=498+tugBob-18*sine(age*Math.PI)+age*15;
            int alpha=(int)((1-age)*210);
            p.rect(x,y,3+age*3,2,(alpha<<24)|0xe7fbff);
        }
    }
    private static void smoke(Painter p,int tick,float x,float y) {
        // Eighteen overlapping pixel cloudlets rise from the registered smokestack,
        // grow and drift astern. New puffs appear while old puffs dissolve offscreen.
        for(int i=17;i>=0;i--) {
            float age=((tick+i*9)%162)/162f;
            float cx=x-age*185+sine(age*11+i)*5,cy=y-age*108+sine(age*8)*4;
            float r=7+age*20;
            int alpha=(int)(230*Math.min(1,(1-age)*3));
            puff(p,cx,cy,r,(alpha<<24)|0x27334d);
            puff(p,cx-r*.3f,cy-r*.27f,r*.65f,((int)(alpha*.55f)<<24)|0x718096);
        }
    }
    private static void puff(Painter p,float x,float y,float r,int color) {
        p.poly(new float[]{x-r,y-r*.35f,x-r*.6f,y-r*.8f,x-r*.1f,y-r*.8f,
                x+r*.1f,y-r,x+r*.65f,y-r*.7f,x+r*.65f,y-r*.2f,x+r,y,
                x+r*.8f,y+r*.55f,x+r*.3f,y+r*.55f,x,y+r*.8f,
                x-r*.55f,y+r*.65f,x-r*.55f,y+r*.25f,x-r,y+r*.2f},color);
    }
    private static void foam(Painter p,int tick,float x,float y,float scale,boolean front) {
        for(int i=0;i<24;i++) {
            float age=((tick+i*7)%168)/168f;
            float xx=x+(front?i*15:-age*185)*scale;
            float yy=y+sine(tick*.06+i*.82)*3+(front?0:age*13);
            float crest=Math.max(0,sine(tick*.055+i*.8));
            int alpha=(int)((front?150:100)*(1-age*.65f));
            p.rect(xx,yy,10*scale,2.5f,(alpha<<24)|0x8edbed);
            if(crest>.25f) {
                p.rect(xx+2,yy-crest*5,5*scale,3,(alpha<<24)|0xe8faff);
                p.rect(xx+5,yy-crest*7,2*scale,2,(alpha<<24)|0xe8faff);
            }
        }
    }
}
