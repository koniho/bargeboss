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
    // Bridge approaches use the bridge's own signals; separate towers intersect the spans.
    private static final Entrance[] PRACTICE={
        new Entrance("TRAIN CHANNEL ENTRANCE",300,TrainingChannel.TOW_BASIN_TOP-26,0,-1,48),
        new Entrance("TRAIN CHANNEL EXIT",300,TrainingChannel.NARROW_EXIT+26,0,-1,48),
        new Entrance("PRACTICE HARBOR ENTRANCE",TrainingChannel.BASIN_LEFT-12,650,1,0,94)
    };
    // Fixed east/red and west/green towers at the two official map breakwater tips.
    // Returning vector is perpendicular to their slightly diagonal entrance line.
    private static final float MARINA_HALF=Physics.length(89.5f,16);
    private static final Entrance[] MARINA={new Entrance("MONTROSE INLET",MontroseHarbor.mapX(458.5f),MontroseHarbor.mapY(1274),16/MARINA_HALF,-89.5f/MARINA_HALF,MARINA_HALF*MontroseHarbor.MAP_SCALE)};
    private static final Entrance[] INDIANA={new Entrance("INDIANA OUTER HARBOR",600,-890,0,1,165),new Entrance("SHIP CANAL",375,210,0,1,88)};
    private static final Entrance[] LOCAL={new Entrance("HARBOR ENTRANCE",300,Physics.BOTTOM-45,0,-1,120)};
    private static final Entrance[] TRANSIT={LOCAL[0],new Entrance("UPSTREAM HARBOR",300,-280,0,-1,20)};
    private static final class RiverMarks {static final Entrance[] ENTRANCES=fromMarkers(CalumetRiver.MARKERS);}
    private static final class IndianaMarks {static final Entrance[] ENTRANCES=fromMarkers(IndianaHarbor.MARKERS);}
    private static Entrance[] fromMarkers(float[][] marks){Entrance[] out=new Entrance[marks.length/2];
        for(int i=0;i<out.length;i++){
            float[] a=marks[i*2],b=marks[i*2+1],red=a[3]>0?a:b,green=a[3]>0?b:a;
            float length=Physics.length(red[0]-green[0],red[1]-green[1]);
            out[i]=new Entrance("HARBOR REACH",(a[0]+b[0])*.5f,(a[1]+b[1])*.5f,
                    (red[1]-green[1])/length,-(red[0]-green[0])/length,length*.5f);
        }return out;
    }
    private static float[][] markers(Game g){return g.calumetStage()?CalumetRiver.MARKERS:g.indianaStage()?IndianaHarbor.MARKERS:null;}
    private NavigationLights(){}
    public static Entrance[] entrances(Game g){return g.calumetStage()?RiverMarks.ENTRANCES:g.indianaStage()?IndianaMarks.ENTRANCES:g.montroseStage()?MARINA:g.tutorial!=null?PRACTICE:g.lockStage()?TRANSIT:LOCAL;}
    /** One single flash every two seconds; long dark interval preserves the marine rhythm. */
    public static float flash(int ticks,int phase){return flash(ticks,phase,120);}
    static float flash(int ticks,int phase,int period){
        int t=Math.floorMod(ticks+phase,period);
        if(t<7)return t/7f;
        if(t<25)return 1;
        if(t<37)return (37-t)/12f;
        return 0;
    }
    public static int count(Game g){if(g.calumetStage() || g.indianaStage())return markers(g).length;return entrances(g).length*2+(g.montroseStage()?MontroseHarbor.MOORINGS.length:g.tutorial!=null?2:0);}
    private static boolean mooring(Game g,int i){return g.montroseStage() && i>=2;}
    private static boolean practice(Game g,int i){return g.tutorial!=null && i>=entrances(g).length*2;}
    public static boolean land(Game g,int i){return g.montroseStage()?i<2:g.tutorial!=null && !practice(g,i);}
    private static int practiceIndex(Game g,int i){return i-entrances(g).length*2;}
    public static float anchorX(Game g,int i){if(g.calumetStage() || g.indianaStage())return markers(g)[i][0];return mooring(g,i)?MontroseHarbor.MOORINGS[i-2][0]:practice(g,i)?Tutorial.DRIVE_X[practiceIndex(g,i)]:entrances(g)[i/2].markerX(i%2==1);}
    public static float anchorY(Game g,int i){if(g.calumetStage() || g.indianaStage())return markers(g)[i][1];return mooring(g,i)?MontroseHarbor.MOORINGS[i-2][1]:practice(g,i)?Tutorial.DRIVE_Y[practiceIndex(g,i)]:entrances(g)[i/2].markerY(i%2==1);}
    public static float x(Game g,int i){Buoys.Buoy b=g.buoys.get(g,i);return b==null?anchorX(g,i):b.x;}
    public static float y(Game g,int i){Buoys.Buoy b=g.buoys.get(g,i);return b==null?anchorY(g,i):b.y;}
    private static float roll(Game g,int i){Buoys.Buoy b=g.buoys.get(g,i);return b==null?0:b.roll;}
    public static int color(Game g,int i){if(g.calumetStage() || g.indianaStage())return markers(g)[i][3]>0?RED:GREEN;return mooring(g,i)?0xffdedcc3:practice(g,i)?YELLOW:i%2==1?RED:GREEN;}
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
        return mooring(g,i)?0:practice(g,i)?.5f+.5f*flash(g.visualTicks,practiceIndex(g,i)*33):flash(g.visualTicks,(i/2)*19,g.montroseStage()?180:120);
    }
    /** Water effects stay below all hulls and are clipped against actual waterways. */
    public static void reflections(Game g,Painter canvas){
        if(g.montroseStage())MontroseBeaconArt.reflection(g,canvas);
        Painter world=new RiverPainter(canvas,g.layout.camera);
        Painter water=new WaterEffectsPainter(g,world);
        for(int i=0;i<count(g);i++)if(visible(g,i) && (!land(g,i) || g.montroseStage())){
            float wx=x(g,i),wy=y(g,i),power=power(g,i);
            if(!land(g,i))BuoyArt.shadows(g,canvas,wx,wy,size(g,i),frame(g,i),roll(g,i));
            if(power<=0)continue;
            int rgb=color(g,i)&0xffffff;float radius=practice(g,i)?29:17;
            for(int shell=4;shell>=1;shell--)water.circle(wx,wy,radius*shell/4,((int)(power*(7+(4-shell)*3))<<24)|rgb);
            WaterReflection.draw(g,world,wx,wy,practice(g,i)?30:20,55,
                    ((int)(power*170)<<24)|rgb,i);
        }
    }
    /** Raised-deck shadows and light spill, after cached ground and before depth-sorted objects. */
    public static void landShadows(Game g,Painter canvas){
        if(g.montroseStage() && MontroseBeaconArt.visible(g,MontroseBeaconArt.HORN))MontroseBeaconArt.ground(g,canvas,MontroseBeaconArt.HORN);
        for(int i=0;i<count(g);i++)if(land(g,i) && visible(g,i))
            LandMarkerArt.ground(g,canvas,x(g,i),y(g,i),frame(g,i),color(g,i),power(g,i));
    }
    private static int frame(Game g,int i){return mooring(g,i)?BuoyArt.YELLOW:practice(g,i)?BuoyArt.YELLOW:color(g,i)==RED?BuoyArt.RED:BuoyArt.GREEN;}
    public static float size(Game g,int i){if(g.calumetStage() || g.indianaStage())return BuoyArt.MARKER_SIZE;return mooring(g,i)?10:practice(g,i)?BuoyArt.PRACTICE_SIZE:entrances(g)[i/2].halfWidth<30?BuoyArt.CHANNEL_SIZE:BuoyArt.MARKER_SIZE;}
    /** Tutorial and Montrose entrances use fixed beacons; mooring/practice marks float. */
    public static void draw(Game g,Painter canvas,int i){
        if(!visible(g,i))return;
        if(land(g,i)){LandMarkerArt.draw(g,canvas,x(g,i),y(g,i),frame(g,i),color(g,i),power(g,i));return;}
        BuoyArt.draw(g,canvas,x(g,i),y(g,i),size(g,i),frame(g,i),color(g,i),power(g,i),practice(g,i)?practiceIndex(g,i)+1:0,roll(g,i));
    }
}
