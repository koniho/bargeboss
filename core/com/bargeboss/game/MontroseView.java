package com.bargeboss.game;

import java.util.ArrayList;

/** Survey-traced shoreline and dock skeletons; cached parkland excludes the exact water polygon. */
public final class MontroseView {
    private static final float[] TRAIL={670,1010,790,1050,975,1070,1160,1055,1350,980,1520,860,1645,705,1750,525,1790,340};
    private static final float[] ROAD={-200,270,200,205,540,155,730,90,810,10,910,-140};
    private static final float[][] PROPS=props();
    private static final Ground[] GROUND=ground();
    private static final StaticLayer[][] GROUND_LEVELS=groundLevels();
    private static final float[] GROUND_DENSITY={1,.5f,.25f,.125f};
    private static RiverCamera raised(RiverCamera c,float rise){return new RiverCamera(c.left,c.top-rise*c.scale,c.scale,c.worldTop,c.worldRight,c.zoom);}
    private MontroseView(){}
    private static float[][] props(){ArrayList<float[]> out=new ArrayList<float[]>();
        // Dense woods occupy the western park; the peninsula keeps broad open lawns.
        for(int row=-20;row<15;row++)for(int col=-2;col<24;col++){
            float mx=col*100+37,my=row*100+47,x=MontroseHarbor.mapX(mx),y=MontroseHarbor.mapY(my);
            if(MontroseHarbor.openWater(x,y) || MontroseBeach.sandy(mx,my) || MontroseBeach.buildingArea(mx,my))continue;int seed=Math.floorMod(row*37+col*19,13);
            boolean safe=true;for(int k=0;k<8;k++){double a=k*Math.PI/4;if(MontroseHarbor.openWater(x+80*(float)StrictMath.cos(a),y+80*(float)StrictMath.sin(a)))safe=false;}
            if(!safe || (mx>865 && mx<1320 && my<180) || seed>(mx<500?7:2))continue;out.add(new float[]{4+seed%2,x,y,125+seed*7});
        }
        // Small riprap groups follow the outside lake shore, not a rectilinear tile grid.
        float[] c=MontroseHarbor.COAST;
        for(int i=0;i<c.length-2;i+=2){float mx=MontroseHarbor.planX(c[i]),my=MontroseHarbor.planY(c[i+1]);if(my<1100 || (mx<480 && mx>270))continue;
            float dx=c[i+2]-c[i],dy=c[i+3]-c[i+1],len=Physics.length(dx,dy),nx=-dy/len,ny=dx/len;
            if(MontroseHarbor.openWater(c[i]+nx*5,c[i+1]+ny*5)){nx=-nx;ny=-ny;}
            for(float d=12;d<len;d+=55)if(!MontroseShoreArt.terraced(c[i]+dx*d/len+nx*12,c[i+1]+dy*d/len+ny*12))out.add(new float[]{6+(i/2)%2,c[i]+dx*d/len+nx*12,c[i+1]+dy*d/len+ny*12,44});
        }
        // Keep the public promenade and its lettered entrances clear of scenery.
        for(int i=out.size()-1;i>=0;i--){float[] prop=out.get(i);boolean blocked=false;
            for(float[] access:MontroseShoreArt.ACCESS)if(Physics.length(prop[1]-access[0],prop[2]-access[1])<(prop[0]<6?145:46)){blocked=true;break;}
            if(prop[0]>=6)for(float[] walk:MontroseShoreArt.PROMENADE)for(int sample=0;sample<8;sample++){double angle=sample*Math.PI/4;if(PolygonWater.contains(walk,prop[1]+22*(float)StrictMath.cos(angle),prop[2]+22*(float)StrictMath.sin(angle)))blocked=true;}
            if(prop[0]>=6 && MontroseEntranceShore.walkwayInset(MontroseHarbor.planX(prop[1]),MontroseHarbor.planY(prop[2]))>0)blocked=true;
            if(prop[0]<6 && Physics.length(prop[1]-MontroseShoreArt.ACCESS[MontroseHarbor.GREEN_INLET_ACCESS][0],prop[2]-MontroseShoreArt.ACCESS[MontroseHarbor.GREEN_INLET_ACCESS][1])<245)blocked=true;
            if(blocked)out.remove(i);
        }
        java.util.Collections.sort(out,new java.util.Comparator<float[]>(){public int compare(float[] a,float[] b){return Float.compare(a[2]-a[1],b[2]-b[1]);}});
        return out.toArray(new float[out.size()][]);
    }
    private static final class Ground extends StaticLayer {
        final int col,row;final RiverCamera camera;final float[] waterClip;
        final float left,top,w,h;
        Ground(int col,int row){this.col=col;this.row=row;camera=new RiverCamera(20,35,1,row*256,col*256+320,1);
            float[] water=MontroseHarbor.projected(MontroseHarbor.WATER_TRIANGLES,raised(camera,MontroseHarbor.LAND_RISE),col*256-55,row*256-55,(col+1)*256+55,(row+1)*256+55,Float.NEGATIVE_INFINITY);
            float[] steps=MontroseHarbor.projected(MontroseShoreArt.topFootprints(),raised(camera,MontroseHarbor.LAND_RISE),col*256-55,row*256-55,(col+1)*256+55,(row+1)*256+55,Float.NEGATIVE_INFINITY);
            float[] rocks=MontroseEntranceShore.terrainCut(raised(camera,MontroseHarbor.LAND_RISE),col*256-55,row*256-55,(col+1)*256+55,(row+1)*256+55);
            waterClip=new float[water.length+steps.length+rocks.length];System.arraycopy(water,0,waterClip,0,water.length);System.arraycopy(steps,0,waterClip,water.length,steps.length);System.arraycopy(rocks,0,waterClip,water.length+steps.length,rocks.length);
            float[] bounds=groundBounds(camera,col,row);left=bounds[0];top=bounds[1];w=bounds[2];h=bounds[3];}
        public int key(){return 830000+(col+8)*64+row+24;}
        public float width(){return w;}
        public float height(){return h;}
        public boolean backgroundSafe(){return true;}
        public void paint(Painter p){
            if(w<=0 || h<=0)return;
            p=ScreenView.offset(p,-left,-top);
            RiverCamera land=raised(camera,MontroseHarbor.LAND_RISE);Painter world=new RiverPainter(p,land);

            p.pushClipOutside(waterClip);
            try{
                for(int y=0;y<4;y++)for(int x=0;x<4;x++){
                    float wx=col*256+x*64,wy=row*256+y*64;
                    world.rect(wx,wy,64,64,SceneLighting.sceneryColor(0xff273b2b));
                    float[] lawn={wx,wy,wx+64,wy,wx+64,wy+64,wx,wy+64};
                    surface(p,land,lawn,2);
                }
                serviceGround(world,col*256,row*256,(col+1)*256,(row+1)*256);
                MontroseBeach.ground(world,col*256,row*256,(col+1)*256,(row+1)*256);
            }finally{p.popClip();}
        }
    }
    /** Construction-only bounds of visible rigid surfaces. Empty water never needs a raster.
     * Original camera, material clips and painter order stay in their 400x310 coordinates. */
    private static float[] groundBounds(RiverCamera c,int col,int row){
        float l=col*256,t=row*256,r=l+256,b=t+256;
        float[] bounds={Float.MAX_VALUE,Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE};
        for(float[] tri:MontroseHarbor.LAND_TRIANGLES){
            float[] q=PolygonWater.rectangle(tri,l,t,r,b);
            if(q.length<6 || Math.abs(PolygonWater.area(q))<.0001f)continue;
            for(int i=0;i<q.length;i+=2)groundPoint(bounds,c,q[i],q[i+1],MontroseHarbor.LAND_RISE);
        }
        // Include the same clipped front-facing seawall ranges as walls(), plus cap strokes.
        float[] coast=MontroseHarbor.COAST;
        for(int i=2;i<coast.length-4;i+=2){
            float ax=coast[i],ay=coast[i+1],bx=coast[i+2],by=coast[i+3],dx=bx-ax,dy=by-ay,len=Physics.length(dx,dy);
            float nx=-dy/len,ny=dx/len;if(!MontroseHarbor.openWater((ax+bx)/2+nx*8,(ay+by)/2+ny*8)){nx=-nx;ny=-ny;}
            if(ny-nx<=0)continue;float start=0,end=1;
            for(int axis=0;axis<2;axis++){float v=axis==0?dx:dy,o=axis==0?ax:ay,min=axis==0?l:t,max=axis==0?r:b;
                if(Math.abs(v)<.001f){if(o<min || o>=max){end=-1;break;}}else{float aa=(min-o)/v,bb=(max-o)/v;start=Math.max(start,Math.min(aa,bb));end=Math.min(end,Math.max(aa,bb));}}
            if(end<=start)continue;
            groundPoint(bounds,c,ax+dx*start,ay+dy*start,0);groundPoint(bounds,c,ax+dx*end,ay+dy*end,0);
            groundPoint(bounds,c,ax+dx*start,ay+dy*start,MontroseHarbor.LAND_RISE);groundPoint(bounds,c,ax+dx*end,ay+dy*end,MontroseHarbor.LAND_RISE);
        }
        if(bounds[0]==Float.MAX_VALUE)return new float[4];
        // Four source pixels protect filtered material edges, AA and rounded cap strokes. Clamp to the
        // original layer viewport so trimming cannot expose previously clipped geometry.
        float left=Math.max(0,(float)StrictMath.floor(bounds[0]-4)),top=Math.max(0,(float)StrictMath.floor(bounds[1]-4));
        float right=Math.min(400,(float)StrictMath.ceil(bounds[2]+4)),bottom=Math.min(310,(float)StrictMath.ceil(bounds[3]+4));
        return new float[]{left,top,Math.max(0,right-left),Math.max(0,bottom-top)};
    }
    private static void groundPoint(float[] bounds,RiverCamera c,float x,float y,float rise){
        float xx=c.screenX(x,y),yy=c.screenY(x,y)-rise*c.scale;
        bounds[0]=Math.min(bounds[0],xx);bounds[1]=Math.min(bounds[1],yy);bounds[2]=Math.max(bounds[2],xx);bounds[3]=Math.max(bounds[3],yy);
    }
    private static void groundPoly(Painter p,float[] q,int color,float l,float t,float r,float b){
        q=PolygonWater.rectangle(q,l,t,r,b);if(q.length>=6)p.poly(q,SceneLighting.sceneryColor(color));
    }
    private static void strip(Painter p,float mx,float my,float xx,float yy,float width,int color,float l,float t,float r,float b){
        float ax=MontroseHarbor.mapX(mx),ay=MontroseHarbor.mapY(my),bx=MontroseHarbor.mapX(xx),by=MontroseHarbor.mapY(yy),dx=bx-ax,dy=by-ay,len=Physics.length(dx,dy),nx=-dy/len*width/2,ny=dx/len*width/2;
        groundPoly(p,new float[]{ax+nx,ay+ny,bx+nx,by+ny,bx-nx,by-ny,ax-nx,ay-ny},color,l,t,r,b);
    }
    private static void serviceGround(Painter p,float l,float t,float r,float b){
        for(int i=0;i<TRAIL.length-2;i+=2)strip(p,TRAIL[i],TRAIL[i+1],TRAIL[i+2],TRAIL[i+3],20,0xff555e5d,l,t,r,b);
        for(int i=0;i<ROAD.length-2;i+=2)strip(p,ROAD[i],ROAD[i+1],ROAD[i+2],ROAD[i+3],45,0xff343f45,l,t,r,b);
        for(int i=0;i<24;i++){double a=i*Math.PI/12,bb=(i+1)*Math.PI/12;strip(p,680+43*(float)StrictMath.cos(a),1000+43*(float)StrictMath.sin(a),680+43*(float)StrictMath.cos(bb),1000+43*(float)StrictMath.sin(bb),18,0xff555e5d,l,t,r,b);}
        strip(p,890,1045,900,977,20,0xff555e5d,l,t,r,b);
        float x=MontroseHarbor.mapX(925),y=MontroseHarbor.mapY(40),xx=MontroseHarbor.mapX(1290),yy=MontroseHarbor.mapY(145);
        groundPoly(p,new float[]{x,y,xx,y,xx,yy,x,yy},0xff414c53,l,t,r,b);
        for(int i=0;i<10;i++)strip(p,1100+i*18,58,1100+i*18,86,1.2f,0xff777b73,l,t,r,b);
    }
    private static void serviceBuilding(Game g,Painter canvas,RiverCamera c,boolean shadow){
        float wx=MontroseHarbor.mapX(1030),wy=MontroseHarbor.mapY(88),x=c.screenX(wx,wy),y=c.screenY(wx,wy),size=210*c.scale;
        if(x+size<0 || x-size>g.layout.width || y+size<0 || y-size>g.layout.height)return;
        if(shadow){ShadowView.scenery(g,canvas,SpriteAtlas.STATIC_HARBOR,StaticHarborArt.WAREHOUSE,wx,wy,MontroseHarbor.LAND_RISE,210);return;}
        canvas.sprite(SpriteAtlas.STATIC_HARBOR,StaticHarborArt.WAREHOUSE,x,y,size,size,0,1);
        StaticHarborArt.lights(canvas,x,y,size,1,g.visualTicks);
    }
    /** Side walls are rigid vertical geometry, with all corners using the shared projection. */
    private static void walls(Painter p,RiverCamera c,float l,float t,float r,float b){
        float[] coast=MontroseHarbor.COAST;
        for(int i=2;i<coast.length-4;i+=2){
            float ax=coast[i],ay=coast[i+1],bx=coast[i+2],by=coast[i+3],dx=bx-ax,dy=by-ay,len=Physics.length(dx,dy);
            float nx=-dy/len,ny=dx/len;if(!MontroseHarbor.openWater((ax+bx)/2+nx*8,(ay+by)/2+ny*8)){nx=-nx;ny=-ny;}
            if(ny-nx<=0)continue;
            float start=0,end=1;
            for(int axis=0;axis<2;axis++){float v=axis==0?dx:dy,o=axis==0?ax:ay,min=axis==0?l:t,max=axis==0?r:b;
                if(Math.abs(v)<.001f){if(o<min || o>=max){end=-1;break;}}else{float aa=(min-o)/v,bb=(max-o)/v;start=Math.max(start,Math.min(aa,bb));end=Math.min(end,Math.max(aa,bb));}}
            if(end<=start)continue;
            if(MontroseShoreArt.terraced((ax+bx)*.5f-nx*2,(ay+by)*.5f-ny*2))continue;
            MontroseShoreArt.wall(p,c,ax+dx*start,ay+dy*start,ax+dx*end,ay+dy*end,0,MontroseHarbor.LAND_RISE,SurfaceMaterial.CORRUGATED,0xff59605e,.35f);

        }
    }
    /** Affine-map flat source material to exact rigid ground corners. Never rotate an upright sprite. */
    private static void surface(Painter p,RiverCamera c,float[] q,int frame){
        float[] screen=new float[q.length];for(int i=0;i<q.length;i+=2){screen[i]=c.screenX(q[i],q[i+1]);screen[i+1]=c.screenY(q[i],q[i+1]);}
        p.poly(screen,SceneLighting.sceneryColor(frame<2?0xff4e5051:0xff26392e));
        ArrayList<float[]> outside=new ArrayList<float[]>();PolygonWater.subtract(new float[]{0,0,400,0,400,310,0,310},screen,outside);
        ArrayList<Float> values=new ArrayList<Float>();for(float[] a:outside)for(int i=2;i<a.length-2;i+=2)for(float v:new float[]{a[0],a[1],a[i],a[i+1],a[i+2],a[i+3]})values.add(v);
        float[] clip=new float[values.size()];for(int i=0;i<clip.length;i++)clip[i]=values.get(i);p.pushClipOutside(clip);
        try{
            float lx=frame==0?10/627f:frame==1?38/627f:40/444f;
            float ly=frame<2?334/627f:285/444f;
            float ux=frame==0?287/627f:frame==1?291/627f:182/444f,uy=frame<2?-185/627f:-102/444f;
            float vx=frame==0?290/627f:frame==1?287/627f:182/444f,vy=frame<2?184/627f:103/444f;
            // Sample inside the lawn diamond to omit painted tile rims.
            if(frame>=2){lx+=(ux+vx)*.07f;ly+=(uy+vy)*.07f;ux*=.86f;uy*=.86f;vx*=.86f;vy*=.86f;}
            float det=ux*vy-uy*vx;
            float ax=screen[2]-screen[0],ay=screen[3]-screen[1],bx=screen[6]-screen[0],by=screen[7]-screen[1];
            float ex=(ax*vy-bx*uy)/det,ey=(ay*vy-by*uy)/det,fx=(-ax*vx+bx*ux)/det,fy=(-ay*vx+by*ux)/det;
            float px=MarinaArt.pivotX(frame)-lx,py=MarinaArt.pivotY(frame)-ly;
            p.spriteAffine(SpriteAtlas.MARINA,frame,screen[0]+ex*px+fx*py,screen[1]+ey*px+fy*py,ex,ey,fx,fy,frame>=2?.62f:1);
        }finally{p.popClip();}
    }
    private static Ground[] ground(){ArrayList<Ground> out=new ArrayList<Ground>();for(int row=-19;row<8;row++)for(int col=-7;col<15;col++)out.add(new Ground(col,row));return out.toArray(new Ground[out.size()]);}
    /** Geometry is shared; distant views rasterize only the pixels they can display. */
    private static StaticLayer[][] groundLevels(){
        StaticLayer[][] out=new StaticLayer[4][GROUND.length];
        for(int level=0;level<4;level++)for(int i=0;i<GROUND.length;i++){
            final Ground source=GROUND[i];final int tier=level;final float density=1f/(1<<level);
            out[level][i]=level==0?source:new StaticLayer(){
                public int key(){return source.key()+tier*2000;}
                public float width(){return source.width()*density;}
                public float height(){return source.height()*density;}
                public boolean backgroundSafe(){return true;}
                public void paint(Painter p){source.paint(ScreenView.scaled(p,0,0,density));}
            };
        }return out;
    }
    public static void draw(Game g,Painter canvas){RiverCamera c=g.layout.camera;WaveView.draw(g,canvas);Painter world=new RiverPainter(canvas,c);ArcadeView.water(g,world);
        RiverCamera land=raised(c,MontroseHarbor.LAND_RISE);
        MontroseShoreArt.docks(g,canvas);MontroseEntranceShore.pier(g,canvas);
        float[] terraceCut=MontroseShoreArt.terraceCut(land),rockCut=MontroseEntranceShore.wholeTerrainCut(land);
        float[] stepCut=new float[terraceCut.length+rockCut.length];System.arraycopy(terraceCut,0,stepCut,0,terraceCut.length);System.arraycopy(rockCut,0,stepCut,terraceCut.length,rockCut.length);
        canvas.pushClipOutside(stepCut);try{new RiverPainter(canvas,land).poly(MontroseHarbor.LAND_POLYGON,SceneLighting.sceneryColor(0xff273b2b));}finally{canvas.popClip();}
        float minX=Float.MAX_VALUE,minY=minX,maxX=-minX,maxY=-minX;
        for(int corner=0;corner<4;corner++){float x=c.worldX(corner%2*g.layout.width,corner/2*g.layout.height),y=c.worldY(corner%2*g.layout.width,corner/2*g.layout.height);minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);}
        walls(canvas,c,minX-60,minY-60,maxX+60,maxY+60);
        int tier=c.scale<.45f?3:c.scale<.9f?2:c.scale<1.6f?1:0;
        for(int tile=0;tile<GROUND.length;tile++){Ground layer=GROUND[tile];if(layer.w<=0 || layer.h<=0)continue;
            float wx=layer.col*256,wy=layer.row*256,x=c.screenX(wx,wy)+(layer.left-layer.camera.screenX(wx,wy))*c.scale,y=c.screenY(wx,wy)+(layer.top-layer.camera.screenY(wx,wy))*c.scale;
            if(x+layer.width()*c.scale<0 || x>g.layout.width || y+layer.height()*c.scale<0 || y>g.layout.height)continue;
            StaticLayer raster=GROUND_LEVELS[tier][tile];float display=c.scale/GROUND_DENSITY[tier];
            if(canvas.preparesLayers())canvas.prepareLayer(raster,display);canvas.layer(raster,x,y,display,1);
        }
        MontroseShoreArt.terraces(g,canvas);MontroseEntranceShore.substrate(g,canvas);MontroseEntranceShore.rocks(g,canvas);MontroseShoreArt.promenade(g,canvas);MontroseShoreArt.access(g,canvas);MontroseEntranceShore.steps(g,canvas);MontroseShoreArt.gates(g,canvas);
        VesselShoreLight.draw(g,canvas);
        // Every ground receiver finishes before any upright park sprite; a later
        // tree's cast mask must never darken an earlier tree's canopy.
        serviceBuilding(g,canvas,land,true);
        for(float[] a:PROPS){float sx=c.screenX(a[1],a[2]),sy=c.screenY(a[1],a[2])-MontroseHarbor.LAND_RISE*c.scale,pad=a[3]*c.scale;
            if(sx+pad<0 || sx-pad>g.layout.width || sy+pad<0 || sy-pad>g.layout.height)continue;
            ShadowView.scenery(g,canvas,SpriteAtlas.MARINA,(int)a[0],a[1],a[2],MontroseHarbor.LAND_RISE,a[3]);}
        serviceBuilding(g,canvas,land,false);
        for(float[] a:PROPS){float sx=c.screenX(a[1],a[2]),sy=c.screenY(a[1],a[2])-MontroseHarbor.LAND_RISE*c.scale,pad=a[3]*c.scale;
            if(sx+pad<0 || sx-pad>g.layout.width || sy+pad<0 || sy-pad>g.layout.height)continue;
            MarinaArt.drawAnimated(g,canvas,land,(int)a[0],a[1],a[2],a[3],(int)(a[1]*19+a[2]*37));}
        NavigationLights.landShadows(g,canvas);
        MarinaPedestals.drawPools(g,canvas);
        Renderer.river(g,world);
    }
    public static void slip(Game g,Painter p){if(MontroseRescue.active(g)){MontroseRescueView.water(g,p);return;}int color=SoftCue.animated(g,g.mission.settledTicks>0?0xdd4fffc0:0xddffe69c);float c=(float)StrictMath.cos(MontroseHarbor.SLIP_ANGLE),s=(float)StrictMath.sin(MontroseHarbor.SLIP_ANGLE);
        for(int side=-1;side<=1;side+=2)for(int end=-1;end<=1;end+=2){float x=MontroseHarbor.SLIP_X+c*end*96-s*side*38,y=MontroseHarbor.SLIP_Y+s*end*96+c*side*38;
            SoftCue.line(p,x,y,x-c*end*28,y-s*end*28,2,color);SoftCue.line(p,x,y,x+s*side*20,y-c*side*20,2,color);}
    }
}
