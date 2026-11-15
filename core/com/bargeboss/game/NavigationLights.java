package com.bargeboss.game;

/** US lateral marks: fixed returning directions, independent of camera and vessel heading. */
public final class NavigationLights {
    public static final int RED=0xffff4c48,GREEN=0xff43f5a1,YELLOW=0xffffcd4b;
    public static final class Entrance {
        public final String name;
        public final float x,y,dx,dy,halfWidth;
        Entrance(String name,float x,float y,float dx,float dy,float halfWidth){this.name=name;this.x=x;this.y=y;this.dx=dx;this.dy=dy;this.halfWidth=halfWidth;}
        // World +Y points down. A right turn from the returning vector is (-dy,dx).
        public float markerX(boolean red){return x+(red?1:-1)*-dy*halfWidth;}
        public float markerY(boolean red){return y+(red?1:-1)*dx*halfWidth;}
    }
    private static final Entrance[] PRACTICE={
        new Entrance("OPEN BASIN / CANAL",TrainingChannel.MIN+10,500,1,0,50),
        new Entrance("CANAL / HARBOR",TrainingChannel.MAX-10,500,1,0,50),
        new Entrance("BRIDGE APPROACH",300,Bridge.Y+80,0,-1,120),
        new Entrance("TRAIN CHANNEL ENTRANCE",300,TrainingChannel.TOW_BASIN_TOP+10,0,-1,24),
        new Entrance("TRAIN CHANNEL EXIT",300,TrainingChannel.NARROW_EXIT-10,0,-1,24)
    };
    private static final Entrance[] LOCAL={new Entrance("HARBOR ENTRANCE",300,Physics.BOTTOM-45,0,-1,120)};
    private static final Entrance[] TRANSIT={LOCAL[0],new Entrance("BRIDGE APPROACH",300,Bridge.Y+80,0,-1,120),new Entrance("UPSTREAM HARBOR",300,-280,0,-1,120)};
    private static final Entrance[] INDUSTRIAL={LOCAL[0],
        new Entrance("SOUTH PIER REACH",321,400,0,-1,99),
        new Entrance("NORTH PIER REACH",275.5f,332,0,-1,95.5f)};
    private NavigationLights(){}
    public static Entrance[] entrances(Game g){return g.tutorial!=null?PRACTICE:g.bridgeStage()?TRANSIT:g.challengeId==2?INDUSTRIAL:LOCAL;}
    /** One single flash every two seconds; long dark interval preserves the marine rhythm. */
    public static float flash(int ticks,int phase){
        int t=Math.floorMod(ticks+phase,120);
        if(t<7)return t/7f;
        if(t<25)return 1;
        if(t<37)return (37-t)/12f;
        return 0;
    }
    public static int count(Game g){return entrances(g).length*2+(g.tutorial!=null?2:0);}
    private static boolean practice(Game g,int i){return i>=entrances(g).length*2;}
    private static int practiceIndex(Game g,int i){return i-entrances(g).length*2;}
    public static float x(Game g,int i){return practice(g,i)?Tutorial.DRIVE_X[practiceIndex(g,i)]:entrances(g)[i/2].markerX(i%2==1);}
    public static float y(Game g,int i){return practice(g,i)?Tutorial.DRIVE_Y[practiceIndex(g,i)]:entrances(g)[i/2].markerY(i%2==1);}
    public static int color(Game g,int i){return practice(g,i)?YELLOW:i%2==1?RED:GREEN;}
    public static float depth(Game g,int i){return g.layout.camera.screenY(x(g,i),y(g,i));}
    public static boolean visible(Game g,int i){
        RiverCamera c=g.layout.camera;float px=c.screenX(x(g,i),y(g,i)),py=c.screenY(x(g,i),y(g,i)),pad=95*c.scale;
        return px+pad>=0 && px-pad<=g.layout.width && py+pad>=0 && py-pad<=g.layout.height;
    }
    public static int[] ordered(Game g){
        int[] pending=new int[count(g)];int n=0;
        for(int i=0;i<pending.length;i++)if(visible(g,i)){int at=n++;while(at>0 && depth(g,pending[at-1])>depth(g,i)){pending[at]=pending[at-1];at--;}pending[at]=i;}
        int[] out=new int[n];System.arraycopy(pending,0,out,0,n);return out;
    }
    private static float power(Game g,int i){
        // Yellow exercise buoys retain a steady base glow between brighter pulses.
        return practice(g,i)?.5f+.5f*flash(g.visualTicks,practiceIndex(g,i)*33):flash(g.visualTicks,(i/2)*19);
    }
    /** Water effects stay below all hulls and are clipped against actual waterways. */
    public static void reflections(Game g,Painter canvas){
        Painter water=new WaterEffectsPainter(g,new RiverPainter(canvas,g.layout.camera));
        for(int i=0;i<count(g);i++)if(visible(g,i)){
            float wx=x(g,i),wy=y(g,i),power=power(g,i);if(power<=0)continue;
            int rgb=color(g,i)&0xffffff;float radius=practice(g,i)?29:17;
            for(int shell=4;shell>=1;shell--)water.circle(wx,wy,radius*shell/4,((int)(power*(7+(4-shell)*3))<<24)|rgb);
            for(int strip=0;strip<7;strip++){
                float shift=2*(float)StrictMath.sin(g.visualTicks*.055+strip*1.8+i);
                float along=8+strip*5,wide=(practice(g,i)?14:8)*(1-strip/9f);
                int alpha=(int)(power*(64-strip*8));
                // Equal world X/Y offsets make horizontal isometric ripples;
                // opposite offsets carry their reflected light down the screen.
                water.line(wx-along-wide+shift,wy+along-wide+shift,wx-along+wide+shift,wy+along+wide+shift,
                        3,((alpha/5)<<24)|rgb);
                water.line(wx-along-wide+shift,wy+along-wide+shift,wx-along+wide+shift,wy+along+wide+shift,
                        1, (alpha<<24)|rgb);
            }
        }
    }
    /** A small isometric steel float with an upright lantern; no per-frame state changes. */
    public static void draw(Game g,Painter canvas,int i){
        if(!visible(g,i))return;
        boolean exercise=practice(g,i),red=color(g,i)==RED;float wx=x(g,i),wy=y(g,i);
        RiverCamera c=g.layout.camera;float size=exercise?1:.55f;
        // The narrow-channel markers are compact so their floats leave room for one hull.
        if(!exercise && entrances(g)[i/2].halfWidth<30)size=.38f;
        float scale=c.scale*size,height=g.water.height(wx,wy,g.visualTicks,null);
        float sx=c.screenX(wx,wy),sy=c.screenY(wx,wy)-height*c.scale;
        Painter p=ScreenView.scaled(canvas,sx,sy,scale);int col=color(g,i);
        float power=power(g,i),sway=(float)StrictMath.sin(g.visualTicks*.048+wx*.017+wy*.009)*1.2f;
        // Float footprint projects onto the water plane; upper structure remains upright.
        p.poly(new float[]{-13,0,0,-7,13,0,0,7},0xff101b23);
        p.poly(new float[]{-11,-2,0,-8,11,-2,0,4},0xff34444a);
        p.poly(new float[]{-11,-2,0,4,0,8,-11,2},0xff18282d);
        p.poly(new float[]{0,4,11,-2,11,2,0,8},0xff587378);
        p.line(-9,-2,0,3,1.4f,col);p.line(0,3,9,-2,1.4f,col);
        p.poly(new float[]{-6,-4,6,-4,5+sway,-23,-5+sway,-23},0xff202e34);
        p.poly(new float[]{-5,-4,-1,-2,-1+sway,-23,-5+sway,-23},0xff8c6d3f);
        p.line(-5,-9,5,-9,3,col);p.line(-5+sway*.65f,-18,5+sway*.65f,-18,2,col);
        if(red)p.poly(new float[]{sway,-36,-7+sway,-25,7+sway,-25},col);
        else p.rect(-6+sway,-34,12,9,col);
        p.rect(-2+sway,-37,4,4,0xff212c31);
        SceneryEffects.glow(p,sway,-40,exercise?7:8,col,power,g.visualTicks,i*.8f);
        p.rect(-4+sway,-43,8,7,0xff17242b);
        p.rect(-3+sway,-42,6,5,SoftCue.alpha(col,.18f+.82f*power));
        if(power>0)p.rect(-1.5f+sway,-41.5f,3,3,SoftCue.alpha(0xfffff6db,power));
        p.line(-5+sway,-44,5+sway,-44,1.8f,0xff718184);
        if(exercise){
            int number=practiceIndex(g,i)+1;
            Lettering.center(p,""+number,sway,-31,6,0xff18242a);
            p.line(-8,0,-6,-3,2,0xffffe9a8);p.line(5,2,8,0,2,0xffffe9a8);
        }
    }
}
