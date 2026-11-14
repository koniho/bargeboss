package com.bargeboss.game;

/** Placement uses the same 64-unit ground grid and real water footprint as play. */
public final class FactoryPlacementTest {
    private static int checks;
    private static void check(boolean ok,String message) {checks++;if(!ok) throw new AssertionError(message);}
    private static boolean grid(float n) {return Math.abs(n/64-Math.round(n/64))<.0001f;}
    public static void run() {
        int seen=0;float minHeight=Float.MAX_VALUE,maxHeight=0;
        boolean low=false,middle=false,high=false;
        for(int slot=0;slot<TrainingChannelView.FACTORY_PLACEMENTS;slot++) {
            int cell=TrainingChannelView.factoryCell(slot);seen|=1<<cell;
            float x=TrainingChannelView.factoryX(slot),y=TrainingChannelView.factoryY(slot);
            float hx=FactoryArt.footprintX(cell),hy=FactoryArt.footprintY(cell),height=FactoryArt.height(cell);
            float[] bounds={x-hx,y-hy,x+hx,y+hy};
            for(float edge:bounds) check(grid(edge),"factory footprint edges register to the 64-unit world tile grid");
            float setback=TrainingChannelView.factorySetback(slot);
            for(float[] water:TrainingChannel.WATER_RECTANGLES) {
                float dx=Math.max(0,Math.max(water[0]-bounds[2],bounds[0]-water[2]));
                float dy=Math.max(0,Math.max(water[1]-bounds[3],bounds[1]-water[3]));
                check(Physics.length(dx,dy)+.001f>=setback,
                        "factory "+cell+" slot "+slot+" respects its water setback even at canal corners");
            }
            for(int other=0;other<slot;other++) {
                int c=TrainingChannelView.factoryCell(other);
                float ox=TrainingChannelView.factoryX(other),oy=TrainingChannelView.factoryY(other);
                check(Math.abs(x-ox)>=hx+FactoryArt.footprintX(c) || Math.abs(y-oy)>=hy+FactoryArt.footprintY(c),
                        "factory foundations never occupy overlapping ground tiles");
            }
            minHeight=Math.min(minHeight,height);maxHeight=Math.max(maxHeight,height);
            low|=height<=75;middle|=height>75 && height<150;high|=height>=150;
            if(height>=150) check(setback>=128,"towering factory structures have the larger shoreline setback");
        }
        check(seen==(1<<FactoryArt.COUNT)-1,"the connected scene places all sixteen different factory modules");
        check(low && middle && high && maxHeight>=2*minHeight,"authored factory heights span low, medium and tall silhouettes");
        System.out.println(checks+" factory ground-grid, water setback, spacing and height assertions passed");
    }
    public static void main(String[] args) {run();}
}
