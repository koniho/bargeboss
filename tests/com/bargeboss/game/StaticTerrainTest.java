package com.bargeboss.game;

import java.util.HashMap;
import java.util.Map;

/** Terrain retention keeps world identity while separating foreground caps from the ground. */
public final class StaticTerrainTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static boolean near(float a,float b){return Math.abs(a-b)<.002f;}
    public static void run() {
        Game g=new Game();g.startTutorial();g=g.activeGame();
        g.layout=Layout.focused(960,600,300,500,1.4f,480,300);
        RiverCamera firstCamera=g.layout.camera;Capture before=new Capture();StaticTerrain.draw(g,before,false);
        Capture retained=new Capture(){
            public boolean retainedSurfaces(StaticLayer layer,float x,float y,float scale){
                check(layer.width()==StaticTerrain.CHUNK && layer.height()==StaticTerrain.CHUNK,"surface command chunk bounds");
                layer.paint(ScreenView.scaled(this,x,y,scale));return true;
            }
        };
        StaticTerrain.draw(g,new SceneryOcclusion(new ViewportPainter(retained,960,600)),false);
        check(retained.surfaceHash==before.surfaceHash && retained.surfaces==before.surfaces,"retained surface commands preserve geometry, UVs, color and order through wrappers");
        Capture foreground=new Capture();StaticTerrain.draw(g,foreground,true);
        check(!before.calls.isEmpty() && !foreground.calls.isEmpty(),"working harbor has cached ground and separate foreground cap layers");
        int backgroundSprites=0;
        check(before.surfaces>0,"background material faces bypass raster layers");
        for(Call call:before.calls.values()) {
            check(call.layer.width()>0 && call.layer.width()<=StaticTerrain.CHUNK
                    && call.layer.height()>0 && call.layer.height()<=StaticTerrain.CHUNK,"trimmed raster bounds stay within their terrain chunk");
            Trace trace=new Trace();call.layer.paint(trace);backgroundSprites+=trace.sprites;
            check(trace.commands>0 && trace.surfaces==0,"cached terrain contains actual markings, never blank material-only rasters");
        }
        check(backgroundSprites==0,"industrial floor materials replace flattened ground texture sprites");
        for(Call call:foreground.calls.values()) {
            check(!before.calls.containsKey(call.layer.key()),"foreground caps never alias a background raster key");
            Trace trace=new Trace();call.layer.paint(trace);
            check(trace.commands>0 && trace.sprites==0,"foreground retention contains only quay-cap primitives, not the land textures");
        }
        g.layout=Layout.focused(1333,600,330,535,1.75f,666.5f,300);
        Capture after=new Capture();StaticTerrain.draw(g,after,false);int shared=0;
        for(Call next:after.calls.values()) {
            Call old=before.calls.get(next.layer.key());if(old==null)continue;shared++;
            check(old.layer==next.layer,"panning zooming and resizing reuse the same immutable terrain content");
            check(old.hash==next.hash,"terrain commands do not depend on camera scale or translation");
            float oldX=(old.x-firstCamera.screenX(0,0))/old.scale,oldY=(old.y-firstCamera.screenY(0,0))/old.scale;
            float nextX=(next.x-g.layout.camera.screenX(0,0))/next.scale,nextY=(next.y-g.layout.camera.screenY(0,0))/next.scale;
            check(near(oldX,nextX) && near(oldY,nextY),"reused chunk anchors remain fixed in canonical projection space");
        }
        check(shared>=2,"camera regression compares multiple overlapping world chunks");
        int ticks=g.ticks,visual=g.visualTicks,gate=g.tutorial.channelGate;float x=g.tug.x,y=g.tug.y;
        Layout layout=g.layout;String progress=g.saveProgress();float[] boundary=TrainingChannel.WATER_BOUNDARY.clone();
        Capture repeat=new Capture();StaticTerrain.draw(g,repeat,false);StaticTerrain.draw(g,new Capture(),true);
        check(repeat.surfaceHash==after.surfaceHash && repeat.surfaces==after.surfaces,"live material geometry, UVs and wetness repeat at a fixed camera");
        check(repeat.calls.size()==after.calls.size(),"repeated terrain drawing submits the same visible chunks");
        for(Call call:repeat.calls.values()) {
            Call old=after.calls.get(call.layer.key());
            check(old!=null && old.hash==call.hash && old.x==call.x && old.y==call.y && old.scale==call.scale,
                    "cached geometry and draw placement are repeatable within a frame");
        }
        check(g.ticks==ticks && g.visualTicks==visual && g.tutorial.channelGate==gate && g.tug.x==x && g.tug.y==y
                && g.layout==layout && progress.equals(g.saveProgress()),"terrain rendering cannot mutate gameplay camera clocks or progress");
        for(int i=0;i<boundary.length;i++)check(boundary[i]==TrainingChannel.WATER_BOUNDARY[i],"retained terrain leaves collision geometry unchanged");
        Capture hinted=new Capture(true);
        Painter wrapped=new SceneryOcclusion(new ViewportPainter(hinted,g.layout.width,g.layout.height));
        StaticTerrain.draw(g,wrapped,false);
        check(hinted.hints.size()>0 && hinted.hints.size()<=4,"native preparation receives a bounded nearby set through wrappers");
        for(StaticLayer hint:hinted.hints.values()) {
            Trace content=new Trace();hint.paint(content);check(content.commands>0 && content.surfaces==0,"preparation never builds blank material-only rasters");
            check(hint.backgroundSafe(),"terrain explicitly permits immutable background painting");
            check(!repeat.calls.containsKey(hint.key()),"prefetch hints stay outside the visible chunk set");
        }
        Capture visibleOnly=new Capture();StaticTerrain.draw(g,new SceneryOcclusion(new ViewportPainter(visibleOnly,g.layout.width,g.layout.height)),false);
        check(hinted.calls.keySet().equals(visibleOnly.calls.keySet()),"preparation never adds visible layer draws");
        for(Call call:hinted.calls.values()){Call old=visibleOnly.calls.get(call.layer.key());check(old.hash==call.hash && old.x==call.x && old.y==call.y,"preparation preserves visible commands and anchors");}
        check(g.ticks==ticks && g.visualTicks==visual && g.layout==layout && g.tug.x==x && g.tug.y==y,"preparation leaves live game and camera untouched");
        // Quiet dry ground inside the finite coverage contains shader faces only, so neither visible nor nearby
        // chunks should consume raster-cache entries or background preparation slots.
        Trace removed=new Trace();StaticTerrain.tile(20,20,false).surfaces(removed);
        check(removed.surfaces==0,"removed outer filler has no ground surfaces");
        Game sparse=new Game();int col=-5,row=0;
        sparse.layout=Layout.renderTile(64,64,new RiverCamera(RiverCamera.Q*288-col*StaticTerrain.CHUNK-64,
                -RiverCamera.Q*RiverCamera.DEPTH*312-row*StaticTerrain.CHUNK-64,1));
        StaticTerrain.Tile surfaceOnly=StaticTerrain.tile(col,row,false);Trace faces=new Trace();surfaceOnly.surfaces(faces);
        check(faces.surfaces>0 && !surfaceOnly.hasRaster,"fixture exercises a material-only terrain chunk");
        Capture sparseCapture=new Capture(true);StaticTerrain.draw(sparse,sparseCapture,false);
        check(sparseCapture.surfaces>0 && sparseCapture.calls.isEmpty(),"material-only ground draws directly without a blank raster layer");
        check(sparseCapture.hints.isEmpty(),"material-only nearby chunks do not queue empty background raster builds");
        System.out.println(checks+" retained terrain key, foreground separation and render-purity assertions passed");
    }
    private static final class Call {
        final StaticLayer layer;final float x,y,scale;final int hash;
        Call(StaticLayer layer,float x,float y,float scale){this.layer=layer;this.x=x;this.y=y;this.scale=scale;Trace trace=new Trace();layer.paint(trace);hash=trace.hash;}
    }
    private static class Trace implements Painter {
        int hash=1,commands,sprites,surfaces,surfaceHash=1;
        public void surfacePolygon(float[] xy,float[] uv,int material,int color,float wetness){
            check(xy.length==uv.length && xy.length>=6,"retained material vertices match UVs");surfaces++;
            for(int i=0;i<xy.length;i++){check(Float.isFinite(xy[i]) && Float.isFinite(uv[i]),"retained material geometry remains finite");surfaceHash=31*surfaceHash+Float.floatToIntBits(xy[i]);surfaceHash=31*surfaceHash+Float.floatToIntBits(uv[i]);}
            surfaceHash=31*surfaceHash+material;surfaceHash=31*surfaceHash+color;surfaceHash=31*surfaceHash+Float.floatToIntBits(wetness);
        }
        void mix(int v){hash=31*hash+v;}void value(float v){mix(Float.floatToIntBits(v));}
        public void rect(float x,float y,float w,float h,int c){commands++;mix(0);value(x);value(y);value(w);value(h);mix(c);}
        public void circle(float x,float y,float r,int c){commands++;mix(1);value(x);value(y);value(r);mix(c);}
        public void poly(float[] xy,int c){commands++;mix(2);for(float v:xy)value(v);mix(c);}
        public void line(float x,float y,float xx,float yy,float w,int c){commands++;mix(3);value(x);value(y);value(xx);value(yy);value(w);mix(c);}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){commands++;sprites++;mix(4);mix(a);mix(f);value(x);value(y);value(w);value(h);value(r);value(alpha);}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float blend,float alpha){throw new AssertionError("static terrain must not retain animated vessel blends");}
    }
    private static class Capture extends Trace {
        private final boolean prepare;
        Capture(){this(false);}Capture(boolean prepare){this.prepare=prepare;}
        final Map<Integer,StaticLayer> hints=new HashMap<Integer,StaticLayer>();
        public boolean preparesLayers(){return prepare;}
        public void prepareLayer(StaticLayer layer,float scale){check(scale>0,"preparation uses a valid projected scale");hints.put(layer.key(),layer);}
        final Map<Integer,Call> calls=new HashMap<Integer,Call>();
        public void layer(StaticLayer layer,float x,float y,float scale,float alpha){
            check(alpha==1,"terrain layer opacity stays solid independently of vessel occlusion");
            check(calls.put(layer.key(),new Call(layer,x,y,scale))==null,"each terrain chunk is submitted only once per pass");
        }
    }
    public static void main(String[] args){run();}
}
