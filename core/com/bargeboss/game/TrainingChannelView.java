package com.bargeboss.game;

/** One connected industrial practice yard, from the canal entrance to the working berth. */
public final class TrainingChannelView {
    public static final int FACTORY_PLACEMENTS=FactoryArt.COUNT+4;
    private static final int[] HARBOR_FACTORIES={0,4,8,13};
    private static final float[] HARBOR_FACTORY_Y={320,512,704,160};
    private TrainingChannelView() {}
    public static void draw(Game g,Painter canvas) {
        RiverCamera c=g.layout.camera;Painter p=new RiverPainter(canvas,c);
        canvas.rect(0,0,g.layout.width,g.layout.height,HarborAtmosphere.WATER_BASE);
        WaveView.surface(g,canvas);
        ArcadeView.water(g,p);
        TrainingGroundView.walls(g,canvas);
        TrainingGroundView.ground(g,canvas);
        factoryAprons(g,new RiverPainter(GroundArt.raisedCanvas(g,canvas),c));
        TrainingGroundView.caps(g,canvas);
        float low=Float.MAX_VALUE,high=-Float.MAX_VALUE;
        for(int corner=0;corner<4;corner++) {
            float sx=(corner%2)*g.layout.width,sy=(corner/2)*g.layout.height;
            low=Math.min(low,c.worldX(sx,sy)-35);high=Math.max(high,c.worldX(sx,sy)+35);
        }
        int gate=g.tutorial.channelGate;
        for(int i=1;i<TrainingChannel.X.length;i++) {
            float x=TrainingChannel.X[i],y=TrainingChannel.Y[i];
            if(x<low || x>high) continue;
            int color=i<gate?ControlView.GREEN:i==gate?VisualStyle.GOLD:VisualStyle.MUTED;
            for(int side=-1;side<=1;side+=2) {
                float yy=y+side*43;
                p.circle(x,yy,6,VisualStyle.OUTLINE);p.circle(x,yy,4,color);
                p.line(x,yy,x+13,yy,2,VisualStyle.CREAM);
                p.poly(new float[]{x+13,yy,x+7,yy,x+10,yy+side*10},color);
            }
            for(int j=-5;j<=5;j++) p.line(x,y+j*10-3,x,y+j*10+3,2,color);
            RiverPainter.center(p,i==TrainingChannel.X.length-1?"HARBOR":"GATE "+i,x+20,y,9,color);
        }
        HarborView.berths(g,p);
        gateBurst(g,p);
        HarborAtmosphere.bankHaze(g,canvas);
        LightingView.quays(g,GroundArt.raisedCanvas(g,canvas));
        factoriesAndVessels(g,canvas);
        TrainingGroundView.foreground(g,canvas);
        DockView.draw(g,canvas);
        if(g.trainingChannel()) {
            int next=Math.min(gate,TrainingChannel.X.length-1);
            float gx=TrainingChannel.X[next],gy=TrainingChannel.Y[next];
            float sy=c.screenY(gx,gy);
            if(sy<82) {
                float sx=Physics.clamp(c.screenX(gx,gy),80,g.layout.width-80);
                ControlView.arrow(canvas,sx,122,sx,88,VisualStyle.GOLD);
                Lettering.center(canvas,"NEXT GATE",sx,131,10,VisualStyle.CREAM);
            }
        }
    }
    private static void gateBurst(Game g,Painter world) {
        Tutorial t=g.tutorial;
        if(t.gateClearTicks<=0 || t.clearedGate<1) return;
        Painter water=new WaterEffectsPainter(g,world);
        float x=TrainingChannel.X[t.clearedGate],y=TrainingChannel.Y[t.clearedGate];
        float age=1-t.gateClearTicks/90f,fade=(1-age)*(1-age);
        int mint=((int)(220*fade)<<24)|0x99ffcb;
        int white=((int)(245*fade)<<24)|0xfff0c5;
        ControlView.ring(water,x,y,12+100*age,1,mint,4*(1-age)+1);
        ControlView.ring(water,x,y,6+65*age,1,white,2);
        water.line(x,y-TrainingChannel.HALF_WIDTH,x,y+TrainingChannel.HALF_WIDTH,2+7*fade,mint);
        for(int side=-1;side<=1;side+=2) for(int i=0;i<5;i++) {
            double angle=(i+1)*Math.PI/6;
            float distance=8+55*age;
            float sx=x+(float)StrictMath.cos(angle)*distance;
            float sy=y+side*(43-(float)StrictMath.sin(angle)*distance);
            water.circle(sx,sy,1+2*fade,i%2==0?white:mint);
        }
    }
    /** Every footprint occupies whole 64-unit grid cells, including rectangular blocks. */
    static int factoryCell(int slot) {
        return slot<FactoryArt.COUNT?slot:HARBOR_FACTORIES[slot-FactoryArt.COUNT];
    }
    static float factorySetback(int slot) {
        int cell=factoryCell(slot);
        return FactoryArt.height(cell)>=150?128:
                Math.max(FactoryArt.footprintX(cell),FactoryArt.footprintY(cell))>32?96:72;
    }
    static float factoryX(int slot) {
        float half=FactoryArt.footprintX(factoryCell(slot));
        if(slot>=FactoryArt.COUNT)
            return (float)StrictMath.ceil((Physics.RIGHT+factorySetback(slot))/64)*64+half;
        float nominal=TrainingChannel.X[0]+(slot/2)*192;
        return (float)StrictMath.floor((nominal-half)/64)*64+half;
    }
    static float factoryY(int slot) {
        int cell=factoryCell(slot);float hx=FactoryArt.footprintX(cell),hy=FactoryArt.footprintY(cell);
        if(slot>=FactoryArt.COUNT) {
            float nominal=HARBOR_FACTORY_Y[slot-FactoryArt.COUNT];
            return (float)StrictMath.floor((nominal-hy)/64)*64+hy;
        }
        int side=slot%2==0?-1:1;float x=factoryX(slot),setback=factorySetback(slot);
        float center=side>0?-Float.MAX_VALUE:Float.MAX_VALUE;
        for(float[] r:TrainingChannel.WATER_RECTANGLES) {
            float dx=Math.max(0,Math.max(r[0]-(x+hx),(x-hx)-r[2]));
            if(dx>=setback) continue;
            // Nearby canal corners count too, even if they are just outside
            // the footprint's X span. Preserve the full radial apron clearance.
            float distance=(float)StrictMath.sqrt(setback*setback-dx*dx);
            float limit=side<0?r[1]-hy-distance:r[3]+hy+distance;
            center=side<0?Math.min(center,limit):Math.max(center,limit);
        }
        return (float)(side<0?StrictMath.floor((center-hy)/64):StrictMath.ceil((center-hy)/64))*64+hy;
    }
    private static void factoryAprons(Game g,Painter world) {
        for(int slot=0;slot<FACTORY_PLACEMENTS;slot++) {
            int cell=factoryCell(slot);float x=factoryX(slot),y=factoryY(slot);
            float hx=FactoryArt.footprintX(cell)+16,hy=FactoryArt.footprintY(cell)+16;
            if(!HarborAtmosphere.visible(g,x,y,(hx+hy)*g.layout.camera.scale)) continue;
            // A quiet service apron gives the registered slab a shared ground plane.
            world.rect(x-hx,y-hy,hx*2,hy*2,0xff40494b);
            world.line(x-hx,y+hy,x+hx,y+hy,1,0x88626c69);
            world.line(x+hx,y-hy,x+hx,y+hy,1,0x88626c69);
        }
    }
    private static void factoriesAndVessels(Game g,Painter canvas) {
        RiverCamera c=g.layout.camera;
        float[] depth=new float[FACTORY_PLACEMENTS];int[] order=new int[FACTORY_PLACEMENTS];
        for(int slot=0;slot<FACTORY_PLACEMENTS;slot++) {
            order[slot]=slot;depth[slot]=c.screenY(factoryX(slot),factoryY(slot));
        }
        for(int i=1;i<FACTORY_PLACEMENTS;i++) {
            float d=depth[i];int slot=order[i],j=i-1;
            while(j>=0 && depth[j]>d) {depth[j+1]=depth[j];order[j+1]=order[j];j--;}
            depth[j+1]=d;order[j+1]=slot;
        }
        Body[] bodies=new Body[g.cargo.length+1];int count=0;
        bodies[count++]=g.tug;
        for(Body b:g.cargo) if(b!=null && b.active) bodies[count++]=b;
        for(int i=1;i<count;i++) {
            Body b=bodies[i];int j=i-1;
            while(j>=0 && c.screenY(bodies[j].x,bodies[j].y)>c.screenY(b.x,b.y)) {bodies[j+1]=bodies[j];j--;}
            bodies[j+1]=b;
        }
        Painter world=new RiverPainter(canvas,c);int boat=0;
        for(int i=0;i<FACTORY_PLACEMENTS;i++) {
            while(boat<count && c.screenY(bodies[boat].x,bodies[boat].y)<=depth[i]) Renderer.body(g,world,bodies[boat++]);
            int slot=order[i];
            FactoryArt.draw(g,canvas,factoryCell(slot),factoryX(slot),factoryY(slot));
        }
        while(boat<count) Renderer.body(g,world,bodies[boat++]);
    }
}
