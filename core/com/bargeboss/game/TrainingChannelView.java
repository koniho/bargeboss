package com.bargeboss.game;

/** One connected industrial practice yard, from the canal entrance to the working berth. */
public final class TrainingChannelView {
    private static final int NAVIGATION_APRON_COL=2,NAVIGATION_APRON_ROW=-7;
    private static final int[][] FACTORIES=makeFactories();
    public static final int FACTORY_PLACEMENTS=FACTORIES.length;
    private TrainingChannelView() {}
    public static void draw(Game g,Painter canvas) {
        RiverCamera c=g.layout.camera;Painter p=new RiverPainter(canvas,c);
        canvas.rect(0,0,g.layout.width,g.layout.height,HarborAtmosphere.WATER_BASE);
        if((g.renderExclusions&RenderExclusions.AMBIENT_WATER)==0)WaveView.surface(g,canvas);
        ArcadeView.water(g,p);
        NavigationLights.reflections(g,canvas);
        LockView.water(g,g.lock,canvas);
        FeederView.draw(g,p);
        // Ground is cached; solid lower-bank caps also join the world depth
        // order so they cover hulls without cutting through nearer buildings.
        if((g.renderExclusions&RenderExclusions.SHORE_GEOMETRY)==0)StaticTerrain.draw(g,canvas,false);
        VesselShoreLight.draw(g,canvas);
        NavigationLights.landShadows(g,canvas);
        ShadowView.factories(g,canvas);
        int gate=g.tutorial==null?0:g.tutorial.channelGate;
        // One transient lesson cue; physical markers belong only at region mouths.
        if(g.trainingChannel() && gate<TrainingChannel.X.length) {
            float x=TrainingChannel.X[gate],y=TrainingChannel.Y[gate];
            for(int j=-4;j<=4;j++)SoftCue.line(p,x,y+j*10-3,x,y+j*10+3,1,SoftCue.animated(g,0x88ffc52f));
            SoftCue.caption(p,gate==TrainingChannel.X.length-1?"HARBOR":"NEXT TURN",x+20,y,9,VisualStyle.GOLD);
        }
        HarborView.berths(g,p);
        gateBurst(g,p);
        HarborAtmosphere.bankHaze(g,canvas);
        LightingView.quays(g,GroundArt.raisedCanvas(g,canvas));
        factoriesAndVessels(g,canvas);
        BridgeView.draw(g,canvas);
        gateIndicator(g,canvas);
    }
    static void gateIndicator(Game g,Painter canvas){RiverCamera c=g.layout.camera;
        if(g.trainingChannel()) {
            int next=Math.min(g.tutorial==null?0:g.tutorial.channelGate,TrainingChannel.X.length-1);
            float gx=TrainingChannel.X[next],gy=TrainingChannel.Y[next];
            float sy=c.screenY(gx,gy);
            if(sy<82) {
                float sx=Physics.clamp(c.screenX(gx,gy),80,g.layout.width-80);
                ControlView.arrow(g,new RiverPainter(canvas,c),c.worldX(sx,122),c.worldY(sx,122),c.worldX(sx,88),c.worldY(sx,88),VisualStyle.GOLD);
                Lettering.center(canvas,"NEXT GATE",sx,131,10,VisualStyle.CREAM);
            }
        }
    }
    private static void gateBurst(Game g,Painter world) {
        Tutorial t=g.tutorial;
        if(t==null || t.gateClearTicks<=0 || t.clearedGate<1) return;
        Painter water=new WaterEffectsPainter(g,world);
        float x=TrainingChannel.X[t.clearedGate],y=TrainingChannel.Y[t.clearedGate];
        float age=1-t.gateClearTicks/90f,fade=(1-age)*(1-age);
        int mint=((int)(220*fade)<<24)|0x99ffcb;
        int white=((int)(245*fade)<<24)|0xfff0c5;
        ControlView.ring(g,water,x,y,12+100*age,1,mint,4*(1-age)+1);
        ControlView.ring(g,water,x,y,6+65*age,1,white,2);
        water.line(x,y-TrainingChannel.HALF_WIDTH,x,y+TrainingChannel.HALF_WIDTH,2+7*fade,mint);
        for(int side=-1;side<=1;side+=2) for(int i=0;i<5;i++) {
            double angle=(i+1)*Math.PI/6;
            float distance=8+55*age;
            float sx=x+(float)StrictMath.cos(angle)*distance;
            float sy=y+side*(43-(float)StrictMath.sin(angle)*distance);
            water.circle(sx,sy,1+2*fade,i%2==0?white:mint);
        }
    }
    /** Packed factory blocks separated by whole-cell roads, built once in stable depth order. */
    private static int[][] makeFactories() {
        int width=IndustrialRoadView.MAX_COL-IndustrialRoadView.MIN_COL;
        int height=IndustrialRoadView.MAX_ROW-IndustrialRoadView.MIN_ROW;
        boolean[][] used=new boolean[height][width];
        // Keep the green train-exit beacon's foreground sightline open. The pump
        // house formerly in this one cell covered its lantern and almost all of
        // its tower; ground remains a normal raised quay apron, not a HUD layer.
        used[NAVIGATION_APRON_ROW-IndustrialRoadView.MIN_ROW][NAVIGATION_APRON_COL-IndustrialRoadView.MIN_COL]=true;
        int[][] pending=new int[width*height][];int count=0;
        for(int row=IndustrialRoadView.MIN_ROW;row<IndustrialRoadView.MAX_ROW;row++)
            for(int col=IndustrialRoadView.MIN_COL;col<IndustrialRoadView.MAX_COL;col++) {
                if(used[row-IndustrialRoadView.MIN_ROW][col-IndustrialRoadView.MIN_COL] || IndustrialRoadView.isRoadCell(col,row)) continue;
                if(Math.floorMod(col*7+row*11,13)!=0)continue;
                int seed=12+Math.floorMod(col+row,4);
                for(int attempt=0;attempt<4;attempt++) {
                    int cell=12+(seed-12+attempt)%4;
                    int wide=(int)FactoryArt.footprintX(cell)/32,tall=(int)FactoryArt.footprintY(cell)/32;
                    if(col+wide>IndustrialRoadView.MAX_COL || row+tall>IndustrialRoadView.MAX_ROW) continue;
                    if(!CalumetArt.clearFactory(col*64,row*64,wide*64,tall*64) || !IndustrialRoadView.clearance(col*64,row*64,wide*64,tall*64,setback(cell))) continue;
                    boolean fits=true;
                    for(int y=row;y<row+tall;y++) for(int x=col;x<col+wide;x++)
                        if(used[y-IndustrialRoadView.MIN_ROW][x-IndustrialRoadView.MIN_COL] || IndustrialRoadView.isRoadCell(x,y)) fits=false;
                    if(!fits) continue;
                    pending[count++]=new int[]{cell,col,row};
                    for(int y=row;y<row+tall;y++) for(int x=col;x<col+wide;x++)
                        used[y-IndustrialRoadView.MIN_ROW][x-IndustrialRoadView.MIN_COL]=true;
                    break;
                }
            }
        int[][] result=new int[count][];System.arraycopy(pending,0,result,0,count);
        // Camera translation and zoom never change isometric ground depth order.
        for(int i=1;i<count;i++) {
            int[] entry=result[i];float depth=factoryDepth(entry);int j=i-1;
            while(j>=0 && factoryDepth(result[j])>depth) {result[j+1]=result[j];j--;}
            result[j+1]=entry;
        }
        return result;
    }
    private static float factoryDepth(int[] entry) {
        return (entry[2]-entry[1])*64+FactoryArt.footprintY(entry[0])-FactoryArt.footprintX(entry[0]);
    }
    static boolean navigationApron(int col,int row){return col==NAVIGATION_APRON_COL && row==NAVIGATION_APRON_ROW;}
    private static float setback(int cell) {
        return FactoryArt.height(cell)>=150?128:
                Math.max(FactoryArt.footprintX(cell),FactoryArt.footprintY(cell))>32?96:72;
    }
    static int factoryCell(int slot) {return FACTORIES[slot][0];}
    static float factorySetback(int slot) {return setback(factoryCell(slot));}
    static float factoryX(int slot) {return FACTORIES[slot][1]*64+FactoryArt.footprintX(factoryCell(slot));}
    static float factoryY(int slot) {return FACTORIES[slot][2]*64+FactoryArt.footprintY(factoryCell(slot));}
    private static void factoriesAndVessels(Game g,Painter canvas) {
        RiverCamera c=g.layout.camera;
        Body[] bodies=new Body[g.cargo.length+6];int count=0;
        bodies[count++]=g.tug;
        if(g.teacherBoat()!=null)bodies[count++]=g.teacherBoat();
        for(Body b:g.cargo) if(b!=null && b.active) bodies[count++]=b;
        if(g.tutorial==null)for(Feeder f:g.feeders)if(f.phase!=Feeder.IDLE){bodies[count++]=f.boat;if(f.load!=null)bodies[count++]=f.load;}
        if(g.trafficActive())bodies[count++]=g.trafficBoat;
        for(int i=1;i<count;i++) {
            Body b=bodies[i];int j=i-1;
            while(j>=0 && c.screenY(bodies[j].x,bodies[j].y)>c.screenY(b.x,b.y)) {bodies[j+1]=bodies[j];j--;}
            bodies[j+1]=b;
        }
        DockView.shadows(g,canvas);BridgeLayers machinery=new BridgeLayers(g,canvas);
        Painter world=new RiverPainter(canvas,c);int boat=0,vehicle=0,shore=0,span=0,mark=0,spray=0,gateIndex=0,prop=0,crew=0;int crane=0;int[] cranes=DockView.ordered(g);int[] workers=DockWorkers.ordered(g);int[] splashes=SprayView.ordered(g);int[] lights=NavigationLights.ordered(g);TowView.Segments rope=new TowView.Segments(g);int[] vehicles=YardActivity.visible(g);
        for(int i=0;i<=FACTORY_PLACEMENTS;i++) {
            float next=i<FACTORY_PLACEMENTS?c.screenY(factoryX(i),factoryY(i)):Float.MAX_VALUE;
            while(crane<cranes.length || boat<count || vehicle<vehicles.length || shore<TrainingGroundView.frontCount() || span<rope.count || mark<lights.length || spray<splashes.length || gateIndex<2 || prop<CalumetArt.count() || crew<workers.length || WorldArrows.pending()) {
                float bd=boat<count?c.screenY(bodies[boat].x,bodies[boat].y):Float.MAX_VALUE;
                float vd=vehicle<vehicles.length?YardActivity.depth(g,vehicles[vehicle]):Float.MAX_VALUE;
                float sd=shore<TrainingGroundView.frontCount()?TrainingGroundView.frontDepth(g,shore):Float.MAX_VALUE;
                float rd=span<rope.count?rope.depth(c,span):Float.MAX_VALUE;
                float ld=mark<lights.length?NavigationLights.depth(g,lights[mark]):Float.MAX_VALUE;
                float ed=spray<splashes.length?SprayView.depth(g,splashes[spray]):Float.MAX_VALUE;
                float gd=gateIndex<2?LockView.gateDepth(g,gateIndex):Float.MAX_VALUE;
                float pd=prop<CalumetArt.count()?CalumetArt.depth(g,prop):Float.MAX_VALUE;
                float wd=crew<workers.length?DockWorkers.depth(g,workers[crew]):Float.MAX_VALUE;
                float ad=WorldArrows.depth();
                float cd=crane<cranes.length?DockView.depth(g,cranes[crane]):Float.MAX_VALUE;
                machinery.to(Math.min(next,Math.min(cd,Math.min(ad,Math.min(wd,Math.min(pd,Math.min(gd,Math.min(ed,Math.min(ld,Math.min(rd,Math.min(sd,Math.min(bd,vd))))))))))));
                if(crane<cranes.length && cd<=Math.min(next,Math.min(ad,Math.min(wd,Math.min(pd,Math.min(gd,Math.min(ed,Math.min(ld,Math.min(rd,Math.min(sd,Math.min(bd,vd))))))))))){DockView.draw(g,canvas,cranes[crane++]);continue;}
                if(WorldArrows.pending()&&ad<=Math.min(next,Math.min(wd,Math.min(pd,Math.min(gd,Math.min(ed,Math.min(ld,Math.min(rd,Math.min(sd,Math.min(bd,vd)))))))))){WorldArrows.draw(world);continue;}
                float other=Math.min(ed,Math.min(ld,Math.min(rd,Math.min(sd,Math.min(bd,vd)))));
                if(gd<=other && gd<=pd && gd<=wd && gd<=next){LockView.drawGate(g,g.lock,canvas,gateIndex++);continue;}
                if(pd<=other && pd<=wd && pd<=next){CalumetArt.draw(g,canvas,prop++);continue;}
                if(wd<=other && wd<=next && crew<workers.length){DockWorkers.draw(g,canvas,workers[crew++]);continue;}
                if(Math.min(ed,Math.min(ld,Math.min(rd,Math.min(sd,Math.min(bd,vd)))))>next)break;
                if(ed<=ld && ed<=rd && ed<=sd && ed<=bd && ed<=vd && spray<splashes.length){SprayView.draw(g,canvas,splashes[spray++]);continue;}
                if(ld<=rd && ld<=sd && ld<=bd && ld<=vd && mark<lights.length){NavigationLights.draw(g,canvas,lights[mark++]);continue;}
                if(rd<=sd && rd<=bd && rd<=vd && span<rope.count){rope.draw(world,span++);continue;}
                if(sd<=bd && sd<=vd && shore<TrainingGroundView.frontCount()){if((g.renderExclusions&RenderExclusions.SHORE_GEOMETRY)==0)TrainingGroundView.frontSection(g,canvas,shore);shore++;continue;}
                if(bd<=vd && boat<count)Renderer.body(g,world,bodies[boat++]);
                else YardActivity.drawVehicle(g,canvas,vehicles[vehicle++]);
            }
            machinery.to(next);
            if(i<FACTORY_PLACEMENTS)FactoryArt.draw(g,canvas,factoryCell(i),factoryX(i),factoryY(i));
        }
        machinery.to(Float.POSITIVE_INFINITY);
    }
}
