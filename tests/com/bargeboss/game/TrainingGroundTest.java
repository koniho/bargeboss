package com.bargeboss.game;

/** Raised quay drawing must retain the open water/collision boundary below it. */
public final class TrainingGroundTest {
    private static int checks;
    private static void check(boolean ok,String why) {checks++;if(!ok) throw new AssertionError(why);}
    public static void run() {
        for(float width:new float[]{960,1333}) for(float[] focus:new float[][]{{-1180,500},{-640,390},{170,500},{300,650}}) {
            Game root=new Game();root.configureCanvas(width,600);root.startTutorial();Game g=root.activeGame();
            g.tug.x=focus[0];g.tug.y=focus[1];g.fitTutorialCamera();
            float x=g.tug.x,y=g.tug.y;int tick=g.ticks;float[] boundary=TrainingChannel.WATER_BOUNDARY.clone();
            Capture capture=new Capture(g);TrainingGroundView.walls(g,capture);TrainingGroundView.ground(g,capture);TrainingGroundView.caps(g,capture);TrainingGroundView.foreground(g,capture);
            check(capture.land>0 && capture.tiles>0 && capture.walls>0,"raised industrial scene includes dry ground, registered tiles and visible walls");
            check(g.tug.x==x && g.tug.y==y && g.ticks==tick,"raised shore rendering cannot move boats or advance the simulation");
            for(int i=0;i<boundary.length;i++) check(boundary[i]==TrainingChannel.WATER_BOUNDARY[i],"shore rendering leaves the collision boundary immutable");
        }
        System.out.println(checks+" raised ground, dry texture, open mouth and wall registration assertions passed");
    }
    private static final class Capture implements Painter {
        final Game game;final RiverCamera camera;final float rise;int land,tiles,walls;
        Capture(Game g) {game=g;camera=g.layout.camera;rise=GroundArt.LAND_RISE*camera.scale;}
        public void poly(float[] xy,int color) {
            if(color==GroundArt.BASE) {
                land++;float left=Float.MAX_VALUE,top=Float.MAX_VALUE,right=-Float.MAX_VALUE,bottom=-Float.MAX_VALUE;
                for(int i=0;i<xy.length;i+=2) {
                    float x=camera.worldX(xy[i],xy[i+1]+rise),y=camera.worldY(xy[i],xy[i+1]+rise);
                    left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
                }
                for(float[] water:TrainingChannel.WATER_RECTANGLES) {
                    float w=Math.min(right,water[2])-Math.max(left,water[0]),h=Math.min(bottom,water[3])-Math.max(top,water[1]);
                    check(w<.002f || h<.002f,"raised ground polygons subtract the entire connected waterway");
                }
            }
            if(xy.length==8 && near(xy[0],xy[6]) && near(xy[2],xy[4]) && near(xy[7]-xy[1],rise) && near(xy[5]-xy[3],rise)) {
                walls++;
                float sx=(xy[4]+xy[6])*.5f,sy=(xy[5]+xy[7])*.5f;
                float x=camera.worldX(sx,sy),y=camera.worldY(sx,sy);
                check(boundaryDistance(x,y)<1.4f,"wall feet follow the true water boundary including shallow corrugation");
                check(Math.abs(x-TrainingChannel.MAX)>3 || y<=445 || y>=555,"there is no retaining wall across the canal mouth");
            }
        }
        public void sprite(int atlas,int frame,float x,float y,float w,float h,float rotation,float alpha) {
            if(atlas!=SpriteAtlas.GROUND) return;
            tiles++;float wx=camera.worldX(x,y+rise),wy=camera.worldY(x,y+rise);
            check(Math.abs((wx-32)/64-Math.round((wx-32)/64))<.001f && Math.abs((wy-32)/64-Math.round((wy-32)/64))<.001f,
                    "raised source tile pivot lands on the shared 64-unit grid center");
            check(!TrainingGroundView.intersectsWater(Math.round((wx-32)/64)*64,Math.round((wy-32)/64)*64,64,64),
                    "painted ground textures appear only on completely dry cells");
            check(rotation==0 && w==h,"ground art keeps its authored camera without bitmap rotation or stretching");
        }
        private static float boundaryDistance(float x,float y) {
            float best=Float.MAX_VALUE;float[] p=TrainingChannel.WATER_BOUNDARY;
            for(int i=0;i<p.length;i+=2) {
                int n=(i+2)%p.length;float dx=p[n]-p[i],dy=p[n+1]-p[i+1],len=dx*dx+dy*dy;
                float t=Physics.clamp(((x-p[i])*dx+(y-p[i+1])*dy)/len,0,1);
                best=Math.min(best,Physics.length(x-p[i]-t*dx,y-p[i+1]-t*dy));
            }
            return best;
        }
        private static boolean near(float a,float b) {return Math.abs(a-b)<.002f;}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float blend,float alpha) {sprite(a,f,x,y,w,h,r,alpha);}
        public void rect(float x,float y,float w,float h,int color) {}
        public void circle(float x,float y,float r,int color) {}
        public void line(float x,float y,float xx,float yy,float w,int color) {}
    }
    public static void main(String[] args) {run();}
}
