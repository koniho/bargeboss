package com.bargeboss.game;

/** One connected industrial practice yard, from the canal entrance to the working berth. */
public final class TrainingChannelView {
    private static final int[][] FACTORIES=makeFactories();
    public static final int FACTORY_PLACEMENTS=FACTORIES.length;
    private TrainingChannelView() {}
    public static void draw(Game g,Painter canvas) {
        RiverCamera c=g.layout.camera;Painter p=new RiverPainter(canvas,c);
        canvas.rect(0,0,g.layout.width,g.layout.height,HarborAtmosphere.WATER_BASE);
        WaveView.surface(g,canvas);
        ArcadeView.water(g,p);
        StaticTerrain.draw(g,canvas,false);
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
        StaticTerrain.draw(g,canvas,true);
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
    /** Packed factory blocks separated by whole-cell roads, built once in stable depth order. */
    private static int[][] makeFactories() {
        int width=IndustrialRoadView.MAX_COL-IndustrialRoadView.MIN_COL;
        int height=IndustrialRoadView.MAX_ROW-IndustrialRoadView.MIN_ROW;
        boolean[][] used=new boolean[height][width];
        int[][] pending=new int[width*height][];int count=0;
        for(int row=IndustrialRoadView.MIN_ROW;row<IndustrialRoadView.MAX_ROW;row++)
            for(int col=IndustrialRoadView.MIN_COL;col<IndustrialRoadView.MAX_COL;col++) {
                if(used[row-IndustrialRoadView.MIN_ROW][col-IndustrialRoadView.MIN_COL] || IndustrialRoadView.isRoadCell(col,row)) continue;
                int seed=Math.floorMod(col*7+row*11,FactoryArt.COUNT);
                for(int attempt=0;attempt<FactoryArt.COUNT;attempt++) {
                    int cell=(seed+attempt)%FactoryArt.COUNT;
                    int wide=(int)FactoryArt.footprintX(cell)/32,tall=(int)FactoryArt.footprintY(cell)/32;
                    if(col+wide>IndustrialRoadView.MAX_COL || row+tall>IndustrialRoadView.MAX_ROW) continue;
                    if(!IndustrialRoadView.clearance(col*64,row*64,wide*64,tall*64,setback(cell))) continue;
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
            while(boat<count && c.screenY(bodies[boat].x,bodies[boat].y)<=c.screenY(factoryX(i),factoryY(i))) Renderer.body(g,world,bodies[boat++]);
            int slot=i;
            FactoryArt.draw(g,canvas,factoryCell(slot),factoryX(slot),factoryY(slot));
        }
        while(boat<count) Renderer.body(g,world,bodies[boat++]);
    }
}
