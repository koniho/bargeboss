package com.bargeboss.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reusable projection-space terrain chunks. Geometry and raster keys ignore the live camera. */
public final class StaticTerrain {
    public static final int CHUNK=256;
    private static final LinkedHashMap<Integer,Tile> TILES=new LinkedHashMap<Integer,Tile>(32,.75f,true) {
        protected boolean removeEldestEntry(Map.Entry<Integer,Tile> entry){return size()>256;}
    };
    private StaticTerrain() {}
    public static void draw(Game g,Painter p,boolean foreground) {
        RiverCamera c=g.layout.camera;
        float ox=c.screenX(0,0),oy=c.screenY(0,0),unit=CHUNK*c.scale;
        int firstX=(int)StrictMath.floor(-ox/unit),lastX=(int)StrictMath.floor((g.layout.width-ox)/unit);
        int firstY=(int)StrictMath.floor(-oy/unit),lastY=(int)StrictMath.floor((g.layout.height-oy)/unit);
        // Materials stay live on native shaders; cached markings/caps remain above them.
        for(int row=firstY;row<=lastY;row++)for(int col=firstX;col<=lastX;col++) {
            Tile tile=tile(col,row,foreground);
            if(!p.retainedSurfaces(tile.surfaceLayer,ox+col*CHUNK*c.scale,oy+row*CHUNK*c.scale,c.scale))
                tile.surfaces(ScreenView.scaled(p,ox+col*CHUNK*c.scale,oy+row*CHUNK*c.scale,c.scale));
        }
        for(int row=firstY;row<=lastY;row++)for(int col=firstX;col<=lastX;col++) {
            Tile tile=tile(col,row,foreground);
            if(tile.hasRaster)p.layer(tile,ox+(col*CHUNK+tile.left)*c.scale,oy+(row*CHUNK+tile.top)*c.scale,c.scale,1);
        }
        if(p.preparesLayers())prepareNearby(g,p,foreground,ox,oy,unit,firstX,lastX,firstY,lastY);
    }
    /** Nearest four cells outside the viewport. Hints do not alter visible ordering. */
    private static void prepareNearby(Game g,Painter p,boolean foreground,float ox,float oy,float unit,int firstX,int lastX,int firstY,int lastY) {
        int[] cols=new int[4],rows=new int[4];float[] scores={Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE};
        for(int row=firstY-1;row<=lastY+1;row++)for(int col=firstX-1;col<=lastX+1;col++) {
            if(col>=firstX && col<=lastX && row>=firstY && row<=lastY)continue;
            float x=ox+col*unit,y=oy+row*unit;
            float dx=Math.max(0,Math.max(-x-unit,x-g.layout.width)),dy=Math.max(0,Math.max(-y-unit,y-g.layout.height));
            float cx=x+unit*.5f-g.layout.width*.5f,cy=y+unit*.5f-g.layout.height*.5f;
            float score=dx*dx+dy*dy+.01f*(cx*cx+cy*cy);
            for(int i=0;i<4;i++)if(score<scores[i]) {
                for(int j=3;j>i;j--){scores[j]=scores[j-1];cols[j]=cols[j-1];rows[j]=rows[j-1];}
                scores[i]=score;cols[i]=col;rows[i]=row;break;
            }
        }
        for(int i=0;i<4;i++)if(scores[i]!=Float.MAX_VALUE){Tile t=tile(cols[i],rows[i],foreground);if(t.hasRaster)p.prepareLayer(t,g.layout.camera.scale);}
    }
    static synchronized Tile tile(int col,int row,boolean foreground) {
        if(col< -2048 || col>=2048 || row< -2048 || row>=2048)throw new IllegalArgumentException("Terrain chunk outside key range");
        int key=0x22000000|(foreground?0x01000000:0)|((col+2048)<<12)|(row+2048);
        Tile tile=TILES.get(key);if(tile!=null)return tile;
        Recorder record=new Recorder();Painter p=new ViewportPainter(record,CHUNK,CHUNK);
        Game scene=new Game();
        // These chunks belong only to the training yard. A fresh game's default
        // is the geographic Calumet voyage, whose dry mask would erase our walls.
        // Set render context without starting/resetting a playable tutorial.
        scene.tutorial=new Tutorial();
        // Canonical projection is screenX=Q*(x+y), screenY=Q*DEPTH*(y-x).
        RiverCamera c=new RiverCamera(RiverCamera.Q*288-col*CHUNK,-RiverCamera.Q*RiverCamera.DEPTH*312-row*CHUNK,1);
        scene.layout=Layout.renderTile(CHUNK,CHUNK,c);
        if(foreground)TrainingGroundView.foreground(scene,p);
        else {
            TrainingGroundView.walls(scene,p);TrainingGroundView.ground(scene,p);
            IndustrialRoadView.draw(scene,p);TrainingGroundView.caps(scene,p);
        }
        tile=new Tile(key,record);TILES.put(key,tile);return tile;
    }
    static final class Tile extends StaticLayer {
        final int key;final ArrayList<Command> commands;final float left,top,w,h;final boolean hasRaster;
        final StaticLayer surfaceLayer=new StaticLayer(){
            public int key(){return Tile.this.key;}
            public float width(){return CHUNK;}public float height(){return CHUNK;}
            public void paint(Painter p){surfaces(p);}
        };
        Tile(int key,Recorder r){this.key=key;commands=r.commands;
            boolean raster=false;for(Command command:commands)if(command.type<5){raster=true;break;}hasRaster=raster;
            left=Math.max(0,(float)StrictMath.floor(r.left));top=Math.max(0,(float)StrictMath.floor(r.top));
            w=Math.max(1,Math.min(CHUNK,(float)StrictMath.ceil(r.right))-left);
            h=Math.max(1,Math.min(CHUNK,(float)StrictMath.ceil(r.bottom))-top);
        }
        public boolean backgroundSafe(){return true;}
        public int key(){return key;}public float width(){return w;}public float height(){return h;}
        void surfaces(Painter p){for(Command command:commands)if(command.type>=5)command.paint(p);}
        public void paint(Painter p){p=ScreenView.offset(p,-left,-top);for(Command command:commands)if(command.type!=5)command.paint(p);}
    }
    private static final class Command {
        final int type,a,b,color;final float[] v;float[] uv;float wetness;
        Command(int type,int a,int b,int color,float[] v){this.type=type;this.a=a;this.b=b;this.color=color;this.v=v;}
        void paint(Painter p){switch(type){
            case 0:p.rect(v[0],v[1],v[2],v[3],color);break;
            case 1:p.line(v[0],v[1],v[2],v[3],v[4],color);break;
            case 2:p.poly(v,color);break;
            case 3:p.circle(v[0],v[1],v[2],color);break;
            case 5:p.surfacePolygon(v,uv,a,color,wetness);break;
            case 6:p.pushClipOutside(v);break;
            case 7:p.popClip();break;
            default:p.sprite(a,b,v[0],v[1],v[2],v[3],v[4],v[5]);
        }}
    }
    private static final class Recorder implements Painter {
        final ArrayList<Command> commands=new ArrayList<Command>();
        float left=CHUNK,top=CHUNK,right=0,bottom=0;
        private void bounds(float l,float t,float r,float b){left=Math.min(left,l-1);top=Math.min(top,t-1);right=Math.max(right,r+1);bottom=Math.max(bottom,b+1);}
        public void surfacePolygon(float[] xy,float[] uv,int material,int color,float wetness){
            float[][] points=new float[xy.length/2][4];
            for(int i=0;i<points.length;i++)points[i]=new float[]{xy[i*2],xy[i*2+1],uv[i*2],uv[i*2+1]};
            for(int edge=0;edge<4;edge++){
                ArrayList<float[]> next=new ArrayList<float[]>();if(points.length==0)return;
                int axis=edge/2;float bound=edge%2==0?0:CHUNK;
                float[] previous=points[points.length-1];boolean pin=edge%2==0?previous[axis]>=bound:previous[axis]<=bound;
                for(float[] point:points){boolean inside=edge%2==0?point[axis]>=bound:point[axis]<=bound;
                    if(inside!=pin){float t=(bound-previous[axis])/(point[axis]-previous[axis]);float[] cut=new float[4];for(int j=0;j<4;j++)cut[j]=previous[j]+t*(point[j]-previous[j]);next.add(cut);}
                    if(inside)next.add(point);previous=point;pin=inside;}
                points=next.toArray(new float[next.size()][]);
            }
            if(points.length<3)return;float[] out=new float[points.length*2],texture=out.clone();
            for(int i=0;i<points.length;i++){out[i*2]=points[i][0];out[i*2+1]=points[i][1];texture[i*2]=points[i][2];texture[i*2+1]=points[i][3];}
            Command command=new Command(5,material,0,color,out);command.uv=texture;command.wetness=wetness;commands.add(command);
        }
        public void pushClipOutside(float[] triangles){commands.add(new Command(6,0,0,0,triangles.clone()));}
        public void popClip(){commands.add(new Command(7,0,0,0,new float[0]));}
        public void rect(float x,float y,float w,float h,int c){commands.add(new Command(0,0,0,c,new float[]{x,y,w,h}));bounds(x,y,x+w,y+h);}
        public void line(float x,float y,float xx,float yy,float w,int c){commands.add(new Command(1,0,0,c,new float[]{x,y,xx,yy,w}));bounds(Math.min(x,xx)-w/2,Math.min(y,yy)-w/2,Math.max(x,xx)+w/2,Math.max(y,yy)+w/2);}
        public void poly(float[] xy,int c){commands.add(new Command(2,0,0,c,xy.clone()));for(int i=0;i<xy.length;i+=2)bounds(xy[i],xy[i+1],xy[i],xy[i+1]);}
        public void circle(float x,float y,float r,int c){commands.add(new Command(3,0,0,c,new float[]{x,y,r}));bounds(x-r,y-r,x+r,y+r);}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){commands.add(new Command(4,a,f,0,new float[]{x,y,w,h,r,alpha}));
            float ww=w*SpriteAtlas.widthScale(a,f),hh=h*SpriteAtlas.heightScale(a,f),l=x-SpriteAtlas.pivotX(a,f)*ww,t=y-SpriteAtlas.pivotY(a,f)*hh;bounds(l,t,l+ww,t+hh);}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){throw new IllegalStateException("Static terrain cannot contain turning sprites");}
    }
}
