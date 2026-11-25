package com.bargeboss.game;

/** Layered, drifting surface facets and broken wave crests; no tiled water strips. */
public final class WaveView {
    private WaveView() {}
    // Ambient crests are small and translucent; three segments retain their motion
    // while avoiding hundreds of native path preparations per frame. Wakes are separate.
    private static final int CREST_SEGMENTS=3;
    private static final float[] BEND=new float[CREST_SEGMENTS+1],EDGE=new float[CREST_SEGMENTS+1];
    static {for(int i=0;i<=CREST_SEGMENTS;i++){float t=i/(float)CREST_SEGMENTS;BEND[i]=(float)StrictMath.sin(t*StrictMath.PI);EDGE[i]=(float)StrictMath.sin((t-.1f)*StrictMath.PI);}}
    private static float noise(int seed) {
        int hash=seed*0x45d9f3b;hash=(hash^(hash>>>16))*0x45d9f3b;hash^=hash>>>16;
        return (hash&65535)/65535f;
    }
    /** World-anchored detail moves with the river during camera follow and zoom. */
    public static void draw(Game g,Painter canvas) {
        canvas.rect(0,0,g.layout.width,g.layout.height,canvas.lighting().water);
        WaterTexture.draw(g,canvas,canvas.lighting().water);
        surface(g,canvas);
    }
    /** Wave detail without clearing a scene that has already drawn its land/water mask. */
    public static void surface(Game g,Painter canvas) {
        // Every location uses this pass, including training and low water detail.
        g.spotlight.water(g,canvas);
        if(g.settings.waterDetail<2)return;
        RiverCamera c=g.layout.camera;boolean montrose=g.montroseStage();
        // Hosts that expand batches should not pay to collect and reorder them first.
        WaterBatchPainter batch=canvas.prefersPolygonBatches()?new WaterBatchPainter(canvas,g.layout.width,g.layout.height):null;
        WaterEffectsPainter p=new WaterEffectsPainter(g,new RiverPainter(batch==null?canvas:batch,c));
        float minX=Float.MAX_VALUE,minY=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
        for(int corner=0;corner<4;corner++) {
            float sx=corner%2*g.layout.width,sy=corner/2*g.layout.height;
            float x=c.worldX(sx,sy),y=c.worldY(sx,sy);
            minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
        }
        // Survey harbors reuse the camera-affine shore mask, so each crest is one quad instead of
        // a clip against every local water triangle (as Montrose does with its ambient clip).
        boolean survey=g.surveyStage();
        float[] mask=montrose?g.water.ambientClip.get(g,c,minX-64,minY-64,maxX+64,maxY+64,canvas.supportsUnionClips())
                :survey?ShoreOcclusion.view(g,c,64,canvas.supportsUnionClips()):ShoreOcclusion.EMPTY;
        if(montrose || survey)p.preclipped();
        if(mask.length>0)canvas.pushClipOutside(mask);
        try {
        int tick=g.visualTicks;
        float[] displacement=montrose?new float[2]:null;
        SeaWaves.Surface surface=montrose?new SeaWaves.Surface(g,tick):null;
        float distantDetail=SceneDetail.fade(c.scale,1.25f,2.25f);
        // Cell identities stay in world space. Slow oscillation gives currents
        // motion without a periodic tile jump when the camera crosses a cell.
        int firstX=(int)StrictMath.floor((minX-50)/38),lastColumn=(int)StrictMath.ceil((maxX+50)/38);
        int firstY=(int)StrictMath.floor((minY-50)/30),lastRow=(int)StrictMath.ceil((maxY+50)/30);
        for(int row=firstY;row<=lastRow;row++) for(int col=firstX;col<=lastColumn;col++) {
            int seed=col*1973+row*9277+19037;
            float detail=(seed&3)==0?1:distantDetail;
            if(detail<.025f)continue;
            // Conservative cell bounds include drift, maximum orbital displacement,
            // heave and the longest crest. Reject invisible cells before wave math.
            float cellX=(col+.5f)*38,cellY=(row+.5f)*30;
            float cellScreenX=c.screenX(cellX,cellY),cellScreenY=c.screenY(cellX,cellY),pad=90*c.scale;
            if(cellScreenX<-pad || cellScreenX>g.layout.width+pad || cellScreenY<-pad || cellScreenY>g.layout.height+pad)continue;
            // Stable spatial thinning keeps ambient water lively without competing
            // with the vessel wakes. Never base this on time or camera position.
            float rough=montrose?g.montroseWeather.waves(g)*SeaWaves.exposure((col+.5f)*38,(row+.5f)*30):0;
            if(noise(seed+41)<(montrose?Math.max(.1f,.45f-rough*.12f):.45f))continue;
            float x=0,y=0;
            if(!montrose){
                x=(col+.15f+noise(seed+2)*.7f)*38+(float)StrictMath.sin(tick*.008+seed)*4;
                y=(row+.15f+noise(seed+5)*.7f)*30+(float)StrictMath.sin(tick*.006+seed*.7)*3;
            }
            if(montrose){
                float calm=1-Physics.clamp(rough,0,1);
                x=(col+.15f+noise(seed+2)*.7f)*38+(float)StrictMath.sin(tick*.008+seed)*4*calm;
                y=(row+.15f+noise(seed+5)*.7f)*30+(float)StrictMath.sin(tick*.006+seed*.7)*3*calm;
                surface.displacement(x,y,displacement);x+=displacement[0];y+=displacement[1];
            }
            float sx=c.screenX(x,y),sy=c.screenY(x,y);
            if(sx<-45*c.scale || sx>g.layout.width+45*c.scale || sy<-25*c.scale || sy>g.layout.height+25*c.scale) continue;
            if(!WaterEffectsPainter.contains(g,x,y)) continue;
            float phase=tick*(.016f+noise(seed+11)*.009f)+noise(seed)*6.283f;
            float life=.5f+.5f*(float)StrictMath.sin(phase),fade=life*life;
            if(montrose){
                rough=g.montroseWeather.waves(g)*SeaWaves.exposure(x,y);
                float waveLife=.5f+.5f*Physics.clamp(surface.height(x,y)/Math.max(.1f,3.2f*rough),-1,1);
                life+=(waveLife-life)*Physics.clamp(rough,0,1);
                fade=life*life;detail*=1+rough*.25f;
            }
            float span=10+noise(seed+7)*23,rise=.8f+noise(seed+13)*2.3f;
            // Broad dark trough and a discontinuous highlight, each with a
            // slightly different phase. No evenly spaced full-width bands.
            float stroke=montrose?Math.max(1,.65f+.35f*rough):1;
            float lastX=0,lastY=0;
            for(int part=0;part<=CREST_SEGMENTS;part++) {
                float t=part/(float)CREST_SEGMENTS,along=(t-.5f)*span;
                float bend=BEND[part]*(rise+.3f*(float)StrictMath.sin(t*9+phase));
                float xx=x+(along-bend)*RiverCamera.Q,yy=y+(along+bend)*RiverCamera.Q;
                if(montrose){float h=surface.height(xx,yy);xx+=RiverCamera.riseX(h);yy+=RiverCamera.riseY(h);}
                if(part>0) {
                    p.line(lastX+1,lastY-1,xx+1,yy-1,2.4f,SoftCue.alpha(0x16102028,detail));
                    int alpha=(int)((12+fade*68)*EDGE[part]*detail);
                    if(part!=2 || noise(seed+29)>.35f) p.line(lastX,lastY,xx,yy,(.9f+fade*.5f)*stroke,(alpha<<24)|(g.lighting().glint&0xffffff));
                }
                lastX=xx;lastY=yy;
            }
            if(life>.58f && noise(seed+31)>.35f) {
                float arm=2+noise(seed+37)*6;
                float xx=x+4,yy=y-4;
                if(montrose){float h=surface.height(xx,yy);xx+=RiverCamera.riseX(h);yy+=RiverCamera.riseY(h);}
                p.line(xx-arm*RiverCamera.Q,yy-arm*RiverCamera.Q,xx+arm*RiverCamera.Q,yy+arm*RiverCamera.Q,
                        .8f,((int)((life-.58f)*175*detail)<<24)|(g.lighting()==LightingProfile.DAYLIGHT?g.lighting().glint&0xffffff:0xb3d5cb));
            }
        }
        if(batch!=null)batch.flush();
        } finally {if(mask.length>0)canvas.popClip();}
    }

    public static void draw(Painter p,int tick,float left,float top,float width,float height,float scale,float origin) {
        p.rect(left,top,width,height,p.lighting().water);
        if(height<=0 || width<=0) return;
        // Broad low-contrast facets give the surface depth without covering vessels in fog.
        float broad=160*scale;
        int broadColumns=(int)(width/broad)+3,broadRows=(int)(height/(broad*.55f))+3;
        for(int row=-1;row<broadRows;row++) for(int col=-1;col<broadColumns;col++) {
            int seed=col*917+row*353+9281;
            float x=left+(col+noise(seed))*broad+(origin-tick*.09f*scale)%broad;
            float y=top+(row+noise(seed+17))*broad*.55f;
            float sway=(float)StrictMath.sin(tick*.009+seed)*7*scale;
            float[] facet={x-broad*.6f,y+sway,x-broad*.15f,y-broad*.16f+sway,
                    x+broad*.65f,y-broad*.08f+sway,x+broad*.45f,y+broad*.15f+sway,x-broad*.3f,y+broad*.19f+sway};
            for(int i=1;i<facet.length;i+=2) facet[i]=Physics.clamp(facet[i],top,top+height);
            p.poly(facet,(noise(seed+3)>.5f?0x0810202b:0x05598086));
        }
        float cellX=58*scale,cellY=27*scale;
        int columns=(int)(width/cellX)+3,rows=(int)(height/cellY)+3;
        float drift=(origin-tick*.17f*scale)%cellX;
        for(int row=-1;row<rows;row++) for(int col=-1;col<columns;col++) {
            int seed=col*1973+row*9277+19037;
            // Stable spatial thinning keeps ambient water lively without competing
            // with the vessel wakes. Never base this on time or camera position.
            if(noise(seed+41)<.45f)continue;
            float phase=tick*(.024f+noise(seed+11)*.014f)+noise(seed)*6.283f;
            float life=.5f+.5f*(float)StrictMath.sin(phase);
            float x=left+(col+noise(seed+2)*.95f)*cellX+drift;
            float y=top+(row+noise(seed+5)*.95f)*cellY+(float)StrictMath.sin(phase*.7f)*2*scale;
            float span=(10+noise(seed+7)*32)*scale;
            float rise=(1.2f+noise(seed+13)*2.7f)*scale;
            if(y<top+rise+3*scale || y>top+height-4*scale) continue;
            float slope=(noise(seed+19)-.5f)*.12f;
            float lastX=x-span*.5f,lastY=y-span*.5f*slope;
            for(int part=1;part<=5;part++) {
                float t=part/5f,xx=x+(t-.5f)*span;
                float yy=y-(float)StrictMath.sin(t*StrictMath.PI)*(rise+.45f*scale*(float)StrictMath.sin(t*7+seed))+(t-.5f)*span*slope;
                float edge=(float)StrictMath.sin((t-.1f)*StrictMath.PI);
                int alpha=(int)((13+life*51)*edge);
                p.line(lastX,lastY+2*scale,xx,yy+2*scale,2.0f*scale,0x2010202c);
                if(part!=2 || noise(seed+29)>.33f)
                    p.line(lastX,lastY,xx,yy,(.65f+life*.5f)*scale,(alpha<<24)|0x7faaa9);
                lastX=xx;lastY=yy;
            }
            if(life>.55f && noise(seed+31)>.48f) {
                float glint=span*(.12f+noise(seed+37)*.17f),gy=y+5*scale;
                int alpha=(int)((life-.55f)*120);
                p.poly(new float[]{x-glint,gy,x+glint*.4f,gy-.6f*scale,
                        x+glint,gy+.2f*scale,x-glint*.2f,gy+1.1f*scale},(alpha<<24)|0xa6c4bb);
            }
            // Occasional short silver flecks prevent every crest becoming a complete crescent.
            if(life>.75f && noise(seed+23)>.62f) p.line(x-span*.13f,y-rise-.3f*scale,
                    x+span*.05f,y-rise-.1f*scale,.75f*scale,0x426d999d);
        }
    }
}
