package com.bargeboss.game;

/** Extruded OSM building footprints for the Chicago River stage. Camera yaw is fixed,
 * so painter order, visible walls and wall shading are precomputed once. Draw work is
 * bounded to buildings whose projected box meets the screen. */
final class ChicagoCity {
    static final int COUNT=ChicagoSurvey.HEIGHTS.length;
    private static final float FLOOR=7.2f;
    private static final int[] PALETTE={0xff56606e,0xff4a5463,0xff5f6670,0xff6b6560,0xff747a80,0xff3f4c5c,0xff655b55,0xff7f7b74};
    /** Per building: world bounds, depth key, base color and visible wall list. */
    private static final float[][] BOX=new float[COUNT][];
    private static final int[] COLOR=new int[COUNT];
    private static final int[][] WALLS=new int[COUNT][];
    private static final float[][] WALL_SHADE=new float[COUNT][];
    /** Outward wall normals (x,y pairs) in WALLS order, for scenery point lights. */
    private static final float[][] WALL_NORMAL=new float[COUNT][];
    private static final boolean[] CONVEX=new boolean[COUNT];
    static {
        for(int i=0;i<COUNT;i++){float[] o=ChicagoSurvey.OUTLINES[i];BOX[i]=PolygonWater.bounds(o);
            boolean positive=PolygonWater.area(o)>0;int n=o.length/2,count=0;int[] walls=new int[n];float[] shade=new float[n],normal=new float[n*2];
            for(int e=0;e<n;e++){int a=e*2,b=(e+1)%n*2;float dx=o[b]-o[a],dy=o[b+1]-o[a+1],len=Physics.length(dx,dy);if(len<.01f)continue;
                // Outward normal: right of the edge for counter-clockwise (positive) rings.
                float nx=(positive?dy:-dy)/len,ny=(positive?-dx:dx)/len;
                if(-nx+ny<=.001f)continue;
                shade[count]=.55f+.45f*Physics.clamp((ny*.8f-nx*.35f+.35f),0,1);normal[count*2]=nx;normal[count*2+1]=ny;walls[count++]=e;
            }
            // Far walls first so concave footprints overlap correctly.
            int[] ordered=java.util.Arrays.copyOf(walls,count);float[] shades=java.util.Arrays.copyOf(shade,count),normals=java.util.Arrays.copyOf(normal,count*2);
            for(int k=1;k<count;k++){int w=ordered[k];float s=shades[k],nx=normals[k*2],ny=normals[k*2+1],d=mid(o,w);int j=k-1;
                while(j>=0 && mid(o,ordered[j])>d){ordered[j+1]=ordered[j];shades[j+1]=shades[j];normals[j*2+2]=normals[j*2];normals[j*2+3]=normals[j*2+1];j--;}
                ordered[j+1]=w;shades[j+1]=s;normals[j*2+2]=nx;normals[j*2+3]=ny;}
            WALLS[i]=ordered;WALL_SHADE[i]=shades;WALL_NORMAL[i]=normals;CONVEX[i]=convex(o);COLOR[i]=color(i);
        }
    }
    private ChicagoCity(){}
    private static float mid(float[] o,int e){int n=o.length/2,a=e*2,b=(e+1)%n*2;return (o[a+1]+o[b+1]-o[a]-o[b])*.5f;}
    private static boolean convex(float[] o){int n=o.length/2;float sign=0;
        for(int i=0;i<n;i++){int a=i*2,b=(i+1)%n*2,c=(i+2)%n*2;float cross=PolygonWater.cross(o[a],o[a+1],o[b],o[b+1],o[c],o[c+1]);
            if(Math.abs(cross)<.001f)continue;if(sign==0)sign=Math.signum(cross);else if(Math.signum(cross)!=sign)return false;}
        return true;
    }
    private static int hash(int i){int h=i*0x45d9f3b;h^=h>>>16;h*=0x45d9f3b;h^=h>>>16;return h&0x7fffffff;}
    private static int color(int i){String name=name(i);
        if(name.contains("WRIGLEY"))return 0xffd8d2c2;
        if(name.contains("TRUMP"))return 0xff8ea4b8;
        if(name.contains("MERCHANDISE MART"))return 0xff7d6050;
        if(name.contains("333 WACKER"))return 0xff4f7c70;
        if(name.contains("WOLF POINT") || name.contains("SALESFORCE") || name.contains("RIVER POINT"))return 0xff5d7890;
        if(name.contains("MARINA CITY"))return 0xffb4ada1;
        if(name.contains("TRIBUNE"))return 0xffbdb4a2;
        return PALETTE[hash(i)%PALETTE.length];
    }
    static String name(int i){for(int n=0;n<ChicagoSurvey.NAMED.length;n++)if(ChicagoSurvey.NAMED[n]==i)return ChicagoSurvey.NAMES[n];return "";}
    static float height(int i){return ChicagoSurvey.HEIGHTS[i];}
    /** Ground depth of the nearest footprint corner; buildings sort as solid blocks. */
    static float depth(Game g,int i){float[] b=BOX[i];return g.layout.camera.screenY(b[0],b[3]);}
    static float[] bounds(int i){return BOX[i];}
    /** Screen-space rectangle of the full extrusion: left, top, right, bottom. */
    static void screenBox(Game g,int i,float[] out){RiverCamera c=g.layout.camera;float[] b=BOX[i];float l=Float.MAX_VALUE,t=l,r=-l,d=-l;
        for(int k=0;k<4;k++){float x=(k&1)==0?b[0]:b[2],y=(k&2)==0?b[1]:b[3],sx=c.screenX(x,y),sy=c.screenY(x,y);l=Math.min(l,sx);r=Math.max(r,sx);t=Math.min(t,sy);d=Math.max(d,sy);}
        float rise=GroundArt.LAND_RISE*c.scale;out[0]=l;out[1]=t-rise-height(i)*ArticulatedModel.Z_SCALE*c.scale;out[2]=r;out[3]=d-rise;
    }
    private static final ThreadLocal<float[]> BOX_SCRATCH=new ThreadLocal<float[]>(){protected float[] initialValue(){return new float[4];}};
    static boolean visible(Game g,int i){float[] s=BOX_SCRATCH.get();screenBox(g,i,s);return s[2]>=-8 && s[0]<=g.layout.width+8 && s[3]>=-8 && s[1]<=g.layout.height+8;}
    /** True when screen point (sx,sy) lies inside the building's projected solid: some base
     * footprint point directly below it on screen, within the extrusion height. */
    static boolean covers(Game g,int i,float sx,float sy){RiverCamera c=g.layout.camera;float[] o=ChicagoSurvey.OUTLINES[i];int n=o.length/2;
        float rise=GroundArt.LAND_RISE*c.scale,lo=sy,hi=sy+height(i)*ArticulatedModel.Z_SCALE*c.scale;
        // Vertical screen segment x=sx, y in [lo,hi] against the projected base polygon.
        int crossings=0;float px=c.screenX(o[(n-1)*2],o[(n-1)*2+1]),py=c.screenY(o[(n-1)*2],o[(n-1)*2+1])-rise;
        for(int k=0;k<n;k++){float qx=c.screenX(o[k*2],o[k*2+1]),qy=c.screenY(o[k*2],o[k*2+1])-rise;
            if((px<=sx)!=(qx<=sx) && qx!=px){float y=py+(sx-px)/(qx-px)*(qy-py);if(y>=lo && y<=hi)return true;if(y<lo)crossings++;}
            px=qx;py=qy;}
        // No edge inside the segment: covered only when the segment lies wholly inside the polygon.
        return (crossings&1)==1;
    }
    private static final ThreadLocal<float[][]> PROJECTED=new ThreadLocal<float[][]>(){protected float[][] initialValue(){return new float[2][64];}};
    /** Base of every wall sits on the raised land plane, in model z units. */
    private static final float GROUND_Z=GroundArt.LAND_RISE/ArticulatedModel.Z_SCALE;
    static void draw(Game g,Painter p,int i,float alpha){
        if(alpha<=.01f || !visible(g,i))return;
        RiverCamera c=g.layout.camera;float[] o=ChicagoSurvey.OUTLINES[i];int n=o.length/2;
        float rise=GroundArt.LAND_RISE*c.scale,h=height(i),top=h*ArticulatedModel.Z_SCALE*c.scale;
        float[][] scratch=PROJECTED.get();if(scratch[0].length<n){scratch[0]=new float[n];scratch[1]=new float[n];}
        float[] sx=scratch[0],sy=scratch[1];
        for(int k=0;k<n;k++){sx[k]=c.screenX(o[k*2],o[k*2+1]);sy[k]=c.screenY(o[k*2],o[k*2+1])-rise;}
        int base=COLOR[i];LightingProfile light=p.lighting();SceneryLighting scene=SceneryLighting.get(g);
        int[] walls=WALLS[i];float[] shades=WALL_SHADE[i],normals=WALL_NORMAL[i];
        boolean windows=c.scale>=.5f && alpha>.6f && light!=LightingProfile.DAYLIGHT;
        // Point lights reach street level; sample each face low enough for vessel lamps,
        // the searchlight and fires to register on the podium rather than the crown.
        float lightZ=GROUND_Z+Math.min(h*.5f,30);
        for(int w=0;w<walls.length;w++){int a=walls[w],b=(a+1)%n;
            float[] quad={sx[a],sy[a],sx[b],sy[b],sx[b],sy[b]-top,sx[a],sy[a]-top};
            float len=Physics.length(o[b*2]-o[a*2],o[b*2+1]-o[a*2+1]);
            int shaded=shade(base,shades[w]);
            int lit=scene.shade(light.surface(shaded,SurfaceMaterial.CONCRETE),shaded,(o[a*2]+o[b*2])*.5f,(o[a*2+1]+o[b*2+1])*.5f,lightZ,normals[w*2],normals[w*2+1],0);
            if(alpha>=.99f)p.surfacePolygon(quad,new float[]{0,0,len,0,len,h,0,h},SurfaceMaterial.CONCRETE|SurfaceMaterial.LIT,lit,0);
            else p.poly(quad,SoftCue.alpha(lit,alpha));
            if(windows)windows(g,p,i,w,sx[a],sy[a],sx[b],sy[b],len,alpha);
        }
        int roof=shade(base,1.12f);float[] box=BOX[i];
        int roofLit=scene.shade(light.surface(roof,SurfaceMaterial.CONCRETE),roof,(box[0]+box[2])*.5f,(box[1]+box[3])*.5f,GROUND_Z+h,0,0,1);
        if(CONVEX[i]){float[] q=new float[n*2];for(int k=0;k<n;k++){q[k*2]=sx[k];q[k*2+1]=sy[k]-top;}
            if(alpha>=.99f)p.surfacePolygon(q,roofUV(o),SurfaceMaterial.CONCRETE|SurfaceMaterial.LIT,roofLit,0);else p.poly(q,SoftCue.alpha(roofLit,alpha));}
        else{float[] t=ChicagoSurvey.ROOFS[i];int color=SoftCue.alpha(roofLit,alpha);
            for(int k=0;k<t.length;k+=6){float[] q=new float[6];for(int v=0;v<3;v++){float x=t[k+v*2],y=t[k+v*2+1];q[v*2]=c.screenX(x,y);q[v*2+1]=c.screenY(x,y)-rise-top;}p.poly(q,color);}}
        // Tall towers carry a red aviation lamp; deterministic slow blink from shared ticks.
        if(h>300 && (g.visualTicks/40+i)%3!=0){float cx=0,cy=0;for(int k=0;k<n;k++){cx+=sx[k];cy+=sy[k];}cx/=n;cy=cy/n-top;
            p.material(cx,cy-3*c.scale,9*c.scale,0,0,9*c.scale,EffectMaterial.GLOW,0,alpha,0xffff3b2e);}
    }
    private static float[] roofUV(float[] o){float[] uv=new float[o.length];for(int k=0;k<o.length;k++)uv[k]=o[k];return uv;}
    /** Sparse warm/cool lit floor bands: one segment per alternate floor, hashed span. */
    private static void windows(Game g,Painter p,int i,int wall,float ax,float ay,float bx,float by,float len,float alpha){
        if(len<10)return;RiverCamera c=g.layout.camera;int floors=(int)(height(i)/FLOOR);
        for(int f=2;f<floors;f+=3){int h=hash(i*131+wall*17+f);if(h%5<3)continue;
            float s=(h>>>3)%100/100f*.5f,e=s+.25f+(h>>>10)%100/100f*.45f;if(e>.95f)e=.95f;
            float z=f*FLOOR*ArticulatedModel.Z_SCALE*c.scale;
            int tint=(h>>>17)%4==0?0xffbcd7ff:0xffffc777;
            p.line(ax+(bx-ax)*s,ay+(by-ay)*s-z,ax+(bx-ax)*e,ay+(by-ay)*e-z,1.3f*c.scale,SoftCue.alpha(tint,.42f*alpha));
        }
    }
    private static int shade(int color,float f){
        int r=Math.min(255,Math.round((color>>16&255)*f)),gr=Math.min(255,Math.round((color>>8&255)*f)),b=Math.min(255,Math.round((color&255)*f));
        return color&0xff000000|r<<16|gr<<8|b;
    }
}
