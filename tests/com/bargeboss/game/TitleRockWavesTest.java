package com.bargeboss.game;

/** Crest geometry, entrance registration and clip lifetime for the title breakwater. */
final class TitleRockWavesTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static final class Capture implements Painter {
        int clips,rocks,foam;float origin;
        public void pushClipOutside(float[] xy){check(clips++==0,"rock clips do not nest");check(xy.length==TitleRockWaves.SEGMENTS*12,"bounded crest mask");
            for(int i=0;i<xy.length;i+=6){float cross=(xy[i+2]-xy[i])*(xy[i+5]-xy[i+1])-(xy[i+3]-xy[i+1])*(xy[i+4]-xy[i]);check(cross>0,"water cutout triangles have consistent winding");}}
        public void popClip(){check(--clips==0,"rock clip restores");}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){check(a==SpriteAtlas.TITLE_ROCKS && clips==1,"only rock artwork is clipped to the water crest");rocks++;}
        public void spriteAffine(int a,int f,float x,float y,float ax,float ay,float bx,float by,float alpha){
            check(a==SpriteAtlas.SPLASH && clips==0,"painted spray is above water clip");check(alpha>0 && alpha<=.67f,"foam stays bounded and translucent");
            check(TitleRockWaves.solid(x-origin,Math.abs(ax)*.5f),"foam does not bridge the harbor entrance");foam++;
        }
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float m,float alpha){throw new AssertionError("no blended art needed for shore waves");}
        public void rect(float x,float y,float w,float h,int c){}
        public void circle(float x,float y,float r,int c){throw new AssertionError("shore foam uses painted stamps rather than round particles");}
        public void line(float x,float y,float xx,float yy,float w,int c){check(clips==0,"wet edge follows rock drawing");}
        public void poly(float[] xy,int c){}
    }
    static void run(){
        Game g=new Game();g.configureCanvas(1243,600);int ticks=g.ticks;float x=g.tug.x,y=g.tug.y;
        for(int tick=0;tick<480;tick+=3){
            g.visualTicks=tick;Capture p=new Capture();p.origin=-137;TitleRockWaves.draw(g,p,p.origin,314);
            check(g.visualTicks==tick,"surf rendering never advances the animation clock");
            check(p.rocks==1 && p.clips==0 && p.foam<=36,"one clipped rock sprite and bounded foam per repeat");
            for(int xx=0;xx<=960;xx+=40){float water=TitleRockWaves.waterY(tick,xx,314);
                check(water>303 && water<314,"water intersects only the rock toes");
                check(Math.abs(water-TitleRockWaves.waterY(tick+240,xx,314))<.001f,"surf loops continuously");}
            check(Math.abs(TitleRockWaves.waterY(tick,0,314)-TitleRockWaves.waterY(tick,960,314))<.001f,"scrolling repeat shares one water edge");
        }
        check(g.ticks==ticks && g.tug.x==x && g.tug.y==y,"surf rendering does not advance gameplay");
        System.out.println(checks+" title rock crest, entrance, painted foam and clip lifetime assertions passed");
    }
    public static void main(String[] args){run();}
}
