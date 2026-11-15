package com.bargeboss.game;

/** Registration, world-space light continuity and strict opt-in receiver contract. */
public final class SurfaceLightingTest {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    public static void run(){
        for(int source:SurfaceLightArt.sources()){
            int atlas=SurfaceLightArt.forSource(source),count=SpriteAtlas.count(source);
            check(SpriteAtlas.count(atlas)==count*4,"all receiver frames have all four lighting directions");
            for(int f=0;f<count;f++)for(int d=0;d<4;d++){
                int frame=SurfaceLightArt.frame(source,d,f);
                check(SpriteAtlas.pivotX(atlas,frame)==SpriteAtlas.pivotX(source,f) && SpriteAtlas.pivotY(atlas,frame)==SpriteAtlas.pivotY(source,f),"lighting never changes the source pivot");
                check(SpriteAtlas.widthScale(atlas,frame)==SpriteAtlas.widthScale(source,f) && SpriteAtlas.heightScale(atlas,frame)==SpriteAtlas.heightScale(source,f),"lighting matches odd/half-frame scale registration");
                check(!SpriteGrade.applies(atlas),"response layer does not receive scenery grading twice");
            }
        }
        check(SurfaceLightArt.forSource(SpriteAtlas.FACTORY)<0 && SurfaceLightArt.forSource(SpriteAtlas.LAND_MARKER)<0,"scenery and shore beacons require explicit opt-in");
        float[] left=new float[4],right=new float[4];
        SurfaceLighting.add(left,300,400,220,320,0,200,1);SurfaceLighting.add(right,300,400,380,480,0,200,1);
        check(left[2]>0 && left[0]==0 && right[0]>0 && right[2]==0,"passing a lamp changes the illuminated face");
        float[] outside=new float[4];SurfaceLighting.add(outside,300,400,900,900,0,155,1);
        check(sum(outside)==0,"distant lamps cannot light the receiver");
        float[] dark={0,0,0,0},lamp={1,0,0,0};
        check(SurfaceLighting.shade(dark,0)==SurfaceLighting.MAX_SHADE,"unlit surfaces receive the bounded navy shading layer");
        check(SurfaceLighting.shade(lamp,0)==0 && SurfaceLighting.shade(lamp,2)>.5f,"near light reveals its facing surface while the reverse remains shaded");
        for(int step=0;step<100;step++){
            float[] before={step/100f,0,0,0},after={(step+1)/100f,0,0,0};
            check(SurfaceLighting.shade(after,0)<=SurfaceLighting.shade(before,0),"approaching light smoothly removes shading");
        }
        Game g=new Game();g.reset();g.visualTicks=71;
        float[] a=SurfaceLighting.sample(g,220,440,g.tug);g.configureCanvas(1600,600);float[] b=SurfaceLighting.sample(g,220,440,g.tug);
        for(int i=0;i<4;i++)check(a[i]==b[i],"camera size cannot change surface energy");
        for(float x=180;x<=420;x+=3){
            a=SurfaceLighting.sample(g,x,440,g.tug);b=SurfaceLighting.sample(g,x+.01f,440,g.tug);
            check(sum(a)<=1.00001f,"overlapping lamps stay within the energy budget");
            for(int i=0;i<4;i++)check(Math.abs(a[i]-b[i])<.002f,"light transitions are continuous without nearest-source snapping");
        }
        int ticks=g.ticks,visual=g.visualTicks;float tx=g.tug.x,ty=g.tug.y;
        check(Parity.frame(g)==Parity.frame(g),"same light state renders identically");
        check(ticks==g.ticks && visual==g.visualTicks && tx==g.tug.x && ty==g.tug.y,"surface rendering never advances gameplay");
        for(int source:new int[]{SpriteAtlas.TUG,SpriteAtlas.BARGE,SpriteAtlas.STEEL,SpriteAtlas.CONTAINER,SpriteAtlas.BULK})for(int n=0;n<320;n++){
            Body body=new Body(10,source==SpriteAtlas.TUG?-1:0,220,440,n*(float)Math.PI/160);
            Capture capture=new Capture();RiverPainter river=new RiverPainter(capture,g.layout.camera);
            SpriteAtlas.draw(river,source,body.angle,body.x,body.y,80,80,1);
            SurfaceLighting.directional(g,river,source,body.angle,body.x,body.y,80,1,body);
            check(capture.layers>0 && capture.layers<=4,"bounded response draws per receiver");
        }
        System.out.println(checks+" surface lighting registration, smooth falloff, orientation and purity assertions passed");
    }
    private static float sum(float[] a){float sum=0;for(float v:a)sum+=v;return sum;}
    private static final class Capture implements Painter {
        int a,b,layers;float x,y,w,h,mix;
        public void spriteBlend(int atlas,int first,int second,float xx,float yy,float width,float height,float r,float blend,float alpha){
            if(!SurfaceLightArt.atlas(atlas)){a=first;b=second;x=xx;y=yy;w=width;h=height;mix=blend;return;}
            check(SurfaceLightArt.sourceFrame(atlas,first)==a && SurfaceLightArt.sourceFrame(atlas,second)==b && x==xx && y==yy && w==width && h==height && mix==blend,"light follows exact turn, blend and heave anchor");layers++;
        }
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void rect(float x,float y,float w,float h,int c){}public void line(float x,float y,float xx,float yy,float w,int c){}
        public void circle(float x,float y,float r,int c){}public void poly(float[] xy,int c){}
    }
    public static void main(String[] args){run();}
}
