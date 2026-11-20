package com.bargeboss.game;

/** Northern park extension, authored from Maps satellite/place-photo reference.
 * Coordinates are a playable interpretation in the existing harbor survey frame. */
final class MontroseBeach {
    static final int COUNT=4;
    static final float BEACON_X=2264,BEACON_Y=-1410;
    private static final float[] SAND={-200,-1900,100,-1590,370,-1350,620,-1160,860,-1000,1100,-860,1320,-750,1520,-660,1710,-595,1870,-560,1990,-552,2070,-570,2107,-610,2070,-310,1910,-300,1700,-345,1490,-440,1260,-550,1030,-690,800,-845,570,-1020,320,-1220,60,-1470,-200,-1710};
    private static final float[] WALK={-200,-1640,120,-1380,440,-1110,770,-860,1080,-650,1390,-470,1680,-330,1900,-250,2100,-230,2150,0};
    private static final float[] SAND_WORLD=world(SAND);
    private static final float[][] PLACES={{1080,-545},{620,-965},{1280,-465},{BEACON_X,BEACON_Y}};
    private static final ArticulatedModel[] BUILDINGS={building(0,140,58,29),building(1,66,45,24),building(2,74,35,18)};
    private static final ArticulatedModel.Pose[] POSES={BUILDINGS[0].pose(0),BUILDINGS[1].pose(0),BUILDINGS[2].pose(0)};
    private MontroseBeach(){}
    private static float[] world(float[] p){float[] q=p.clone();for(int i=0;i<q.length;i+=2){q[i]=MontroseHarbor.mapX(q[i]);q[i+1]=MontroseHarbor.mapY(q[i+1]);}return q;}
    static boolean sandy(float x,float y){return PolygonWater.contains(SAND,x,y);}
    static boolean buildingArea(float x,float y){for(float[] a:PLACES)if(Math.abs(x-a[0])<150 && Math.abs(y-a[1])<110)return true;return false;}
    private static void patch(Painter p,float[] q,int color,float l,float t,float r,float b){q=PolygonWater.rectangle(q,l,t,r,b);if(q.length>=6)p.poly(q,SceneLighting.sceneryColor(color));}
    private static void strip(Painter p,float ax,float ay,float bx,float by,float width,int color,float l,float t,float r,float b){
        float dx=bx-ax,dy=by-ay,len=Physics.length(dx,dy),nx=-dy/len*width,ny=dx/len*width;
        patch(p,world(new float[]{ax+nx,ay+ny,bx+nx,by+ny,bx-nx,by-ny,ax-nx,ay-ny}),color,l,t,r,b);
    }
    /** Called only while preparing bounded ground tiles, never per active frame. */
    static void ground(Painter p,float l,float t,float r,float b){
        if(t>MontroseHarbor.mapY(100))return;
        patch(p,SAND_WORLD,0xff8b8270,l-.6f,t-.6f,r+.6f,b+.6f);
        // Fine wind-shaped sand streaks are baked into each bounded tile.
        int firstX=(int)StrictMath.floor(MontroseHarbor.planX(l)/18),lastX=(int)StrictMath.ceil(MontroseHarbor.planX(r)/18);
        int firstY=(int)StrictMath.floor(MontroseHarbor.planY(t)/18),lastY=(int)StrictMath.ceil(MontroseHarbor.planY(b)/18);
        for(int row=firstY;row<=lastY;row++)for(int col=firstX;col<=lastX;col++){
            int seed=Math.floorMod(row*73+col*137,31);float x=col*18+seed*.37f,y=row*18+seed*.23f;
            if(!sandy(x,y))continue;
            strip(p,x,y,x+2+seed*.13f,y+1,.24f,seed%2==0?0xff9a907c:0xff797566,l,t,r,b);
        }
        // Stable, broad dune hummocks and sparse grass tufts; no raster or random state.
        for(int row=-14;row<-2;row++)for(int col=0;col<21;col++){
            float x=col*100+Math.floorMod(row*13+col*29,47),y=row*100+Math.floorMod(row*19+col*7,51);
            if(!sandy(x,y) || !sandy(x,y-85))continue;
            int seed=Math.floorMod(row*23+col*31,11);if(seed>4)continue;
            patch(p,world(new float[]{x-28,y,x-12,y-12,x+22,y-9,x+39,y+5,x+12,y+17,x-20,y+12}),seed<2?0xff656b55:0xff85816b,l,t,r,b);
            for(int k=0;k<4;k++)strip(p,x-15+k*9,y+3,x-13+k*9,y-1,.28f,0xff555e49,l,t,r,b);
        }
        for(int i=0;i<WALK.length-2;i+=2)strip(p,WALK[i],WALK[i+1],WALK[i+2],WALK[i+3],8,0xff525d60,l,t,r,b);
        strip(p,800,0,1080,-650,10,0xff596163,l,t,r,b);
        strip(p,400,-600,620,-1000,8,0xff596163,l,t,r,b);
        // Restaurant patio, detached restroom and kayak concession apron.
        for(int i=0;i<3;i++){float x=PLACES[i][0],y=PLACES[i][1];patch(p,world(new float[]{x-85,y-48,x+85,y-48,x+85,y+48,x-85,y+48}),0xff606663,l,t,r,b);}
        // Breakwater has a plain weathered concrete crown, including its hooked head.
        float[] coast=MontroseHarbor.COAST;
        for(int i=0;i<coast.length-2;i+=2){float ax=MontroseHarbor.planX(coast[i]),ay=MontroseHarbor.planY(coast[i+1]),bx=MontroseHarbor.planX(coast[i+2]),by=MontroseHarbor.planY(coast[i+3]);
            if(ax>2090 && bx>2090 && ay<-600 && by<-600)strip(p,ax,ay,bx,by,17,0xff6c7470,l,t,r,b);}
    }
    private static ArticulatedModel building(int id,float width,float depth,float height){
        ArticulatedModel.Builder b=new ArticulatedModel.Builder();float[] at=PLACES[id];
        int root=b.node("montrose-beach-building-"+id,-1,MontroseHarbor.mapX(at[0]),MontroseHarbor.mapY(at[1]),MontroseHarbor.LAND_RISE/ArticulatedModel.Z_SCALE,-.35f,0);
        b.box(root,0,0,height*.5f,width,depth,height,0xff757d78,SurfaceMaterial.CONCRETE);
        b.box(root,0,0,height+2,width+10,depth+10,4,0xff35474f);
        b.box(root,0,-depth*.5f-1,height*.5f,width-12,2,height*.44f,0xff273f49);
        if(id==0){
            // Low navy canopy on posts and a warm, restrained band of windows.
            b.box(root,0,-depth*.5f-18,height-2,width+20,34,3,0xff344953);
            for(int i=-2;i<=2;i++){b.box(root,i*29,-depth*.5f-30,(height-3)*.5f,2.5f,2.5f,height-3,0xff526267);b.box(root,i*25,depth*.5f+.8f,height*.55f,15,1.5f,9,0xff776346,SurfaceMaterial.EMISSIVE);}
        }
        return b.build();
    }
    static float depth(Game g,int id){return g.layout.camera.screenY(MontroseHarbor.mapX(PLACES[id][0]),MontroseHarbor.mapY(PLACES[id][1]));}
    static void landmark(Game g,Painter p,int id){
        RiverCamera c=g.layout.camera;float wx=MontroseHarbor.mapX(PLACES[id][0]),wy=MontroseHarbor.mapY(PLACES[id][1]),x=c.screenX(wx,wy),y=c.screenY(wx,wy)-MontroseHarbor.LAND_RISE*c.scale,pad=220*c.scale;
        if(x+pad<0 || x-pad>g.layout.width || y+pad<0 || y-pad>g.layout.height)return;
        if(id<3){BUILDINGS[id].draw(g,p,POSES[id]);return;}
        float size=78*c.scale;
        p.sprite(SpriteAtlas.LAND_MARKER,0,x,y,size,size,0,1);
        float lx=x+(LandMarkerArt.LAMP_X-LandMarkerArt.PIVOT_X)*size,ly=y+(LandMarkerArt.LAMP_Y-LandMarkerArt.PIVOT_Y)*size;
        float power=NavigationLights.flash(g.visualTicks,0);
        SceneryEffects.glow(p,lx,ly,10*c.scale,NavigationLights.RED,power,g.visualTicks,0);
        if(power>0)p.circle(lx,ly,1.5f*c.scale,SoftCue.alpha(0xffffb9a2,power));
    }
}
