package com.bargeboss.game;

/** Ground/water receiving shadows, separate from sprite shading and gameplay occlusion. */
public final class ShadowView {
    private ShadowView() {}
    /** Smoothly combine nearby lamps; no nearest-lamp switch can snap a shadow. */
    static void light(Game g,float wx,float wy,float[] out) {
        float dx=.48f,dy=.72f,energy=0;
        if(g.lighting()!=LightingProfile.DUSK){dx=-g.lighting().sunX;dy=-g.lighting().sunY;}
        for(int side=0;!g.montroseStage() && side<2;side++)for(int lamp=HarborAtmosphere.firstLamp(g);lamp<=5;lamp++) {
            float lx=HarborAtmosphere.lampBank(side),ly=HarborAtmosphere.lampY(lamp);
            if(g.tutorial!=null && TrainingChannel.contains(lx,ly))continue;
            float x=wx-lx,y=wy-ly,d2=x*x+y*y;
            if(d2>=160*160)continue;
            float weight=1-d2/(160*160f);weight*=weight;
            float sx=RiverCamera.Q*(x+y),sy=RiverCamera.Q*RiverCamera.DEPTH*(y-x),length=Math.max(16,Physics.length(sx,sy));
            dx+=sx/length*weight*2.2f;dy+=sy/length*weight*2.2f;
            energy+=weight*SceneryEffects.intensity(g.visualTicks,lamp*1.31f+side*.77f);
        }
        // Bounded vector blend through opposed light sources, rather than normalizing a near-zero vector.
        float length=Math.max(1,Physics.length(dx,dy));out[0]=dx/length;out[1]=dy/length;out[2]=Math.min(1,energy);
    }
    private static void mask(Painter p,int source,int frame,float x,float y,float size,float dx,float dy,float flatten,float alpha,boolean soft) {
        int atlas=ShadowArt.forSource(source),f=frame+(soft?SpriteAtlas.count(source):0);
        float w=size*SpriteAtlas.widthScale(atlas,f),h=size*SpriteAtlas.heightScale(atlas,f);
        p.spriteAffine(atlas,f,x,y,w,0,-dx*h,(flatten-dy)*h,alpha*p.lighting().shadowStrength);
    }
    static void scenery(Game g,Painter p,int source,int frame,float wx,float wy,float rise,float size) {
        RiverCamera c=g.layout.camera;float x=c.screenX(wx,wy),y=c.screenY(wx,wy)-rise*c.scale,s=size*c.scale;
        if(x+s<0 || x-s>g.layout.width || y+s<0 || y-s>g.layout.height)return;
        float[] light=new float[3];light(g,wx,wy,light);
        // A close contact layer anchors the base. The softer cast layer responds to lamps.
        mask(p,source,frame,x,y,s,0,0,.13f,.22f,false);
        mask(p,source,frame,x,y,s,light[0]*.65f,light[1]*.65f,.24f,.17f+.17f*light[2],true);
    }
    public static void factories(Game g,Painter p) {
        for(int i=0;i<TrainingChannelView.FACTORY_PLACEMENTS;i++) {
            int f=TrainingChannelView.factoryCell(i);
            scenery(g,p,SpriteAtlas.FACTORY,f,TrainingChannelView.factoryX(i),TrainingChannelView.factoryY(i),GroundArt.LAND_RISE,FactoryArt.size(f));
        }
    }
    public static void harbor(Game g,Painter p) {
        for(int side=0;side<2;side++) {
            int[] cells=side==0?new int[]{0,4,2,1}:new int[]{5,3,7,6};float[] along={70,250,465,625};
            float bank=side==0?Physics.RIGHT+48:Physics.LEFT-82;
            for(int segment=g.lockStage()?-2:0;segment<=0;segment++)for(int i=0;i<4;i++) {
                float y=148+along[i]+segment*688;
                int f=cells[i],src=f==0 || f==1?SpriteAtlas.STATIC_HARBOR:SpriteAtlas.TUTORIAL_SCENERY;
                f=src==SpriteAtlas.STATIC_HARBOR?StaticHarborArt.WAREHOUSE:TutorialSceneryArt.fromLegacy(f);
                scenery(g,p,src,f,bank,y,0,src==SpriteAtlas.STATIC_HARBOR?StaticHarborArt.WORLD_SIZE:TutorialSceneryArt.size(f));
            }
        }
        for(int segment=g.lockStage()?-1:0;segment<=0;segment++) {
            scenery(g,p,SpriteAtlas.CHIMNEYS,1,Physics.RIGHT+78,520+segment*688,0,ChimneyArt.WORLD_SIZE);
            scenery(g,p,SpriteAtlas.CHIMNEYS,0,Physics.RIGHT+78,740+segment*688,0,ChimneyArt.WORLD_SIZE);
        }
        if(g.orderStage())for(int frame=0;frame<2;frame++)scenery(g,p,SpriteAtlas.BRIDGE,frame,frame==0?Physics.RIGHT+35:Physics.LEFT-35,178,0,108);
    }
    public static void vessel(Game g,Painter p,Body b,boolean tug,float size) {
        if(!(p instanceof RiverPainter))return;
        RiverPainter river=(RiverPainter)p;RiverCamera c=river.camera;Painter canvas=river.canvas();
        int src=b.yacht?SpriteAtlas.YACHT:tug?SpriteAtlas.TUG:SpriteAtlas.BARGE;
        float heading=b.angle/(float)(StrictMath.PI/16),whole=(float)StrictMath.floor(heading);
        int f=((int)whole+RiverCamera.DIRECTION_OFFSET)%32;if(f<0)f+=32;
        float blend=Physics.clamp((heading-whole-.4f)*5,0,1);blend=blend*blend*(3-2*blend);
        float[] light=new float[3];light(g,b.x,b.y,light);
        float lift=Math.max(0,WaterSurface.lift(g,b)),alpha=(.22f+.10f*light[2])/(1+lift*.08f);
        float x=c.screenX(b.x,b.y)+light[0]*3*c.scale,y=c.screenY(b.x,b.y)+light[1]*3*c.scale;
        // Water-plane contact stays put as the vessel bobs above it; both turning frames remain registered.
        if(blend<1)mask(canvas,src,f,x,y,size*c.scale,light[0]*.08f,0,.55f,alpha*(1-blend),true);
        if(blend>0)mask(canvas,src,(f+1)%32,x,y,size*c.scale,light[0]*.08f,0,.55f,alpha*blend,true);
        if(b.ability!=BargeAbilities.NONE){int equipment=b.ability==BargeAbilities.FIRE?SpriteAtlas.FIRE_BARGE:SpriteAtlas.FIREWORKS;
            float h=b.angle*64/(2*(float)StrictMath.PI),w=(float)StrictMath.floor(h),mix=Physics.clamp((h-w-.4f)*5,0,1);mix=mix*mix*(3-2*mix);
            int a=(((int)w+RiverCamera.DIRECTION_OFFSET*2)%64+64)%64;
            if(mix<1)mask(canvas,equipment,a,x,y,size*c.scale,light[0]*.1f,0,.45f,alpha*.35f*(1-mix),true);
            if(mix>0)mask(canvas,equipment,(a+1)%64,x,y,size*c.scale,light[0]*.1f,0,.45f,alpha*.35f*mix,true);
        }
    }
}
