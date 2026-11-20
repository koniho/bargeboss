package com.bargeboss.game;

/** Sparse compact workshops preserve open tutorial sightlines. */
public final class FactoryPlacementTest {
    private static int checks;
    private static void check(boolean ok,String message) {checks++;if(!ok) throw new AssertionError(message);}
    private static boolean grid(float n) {return Math.abs(n/64-Math.round(n/64))<.0001f;}
    private static float waterDistance(float x,float y,float width,float height) {
        float nearest=Float.MAX_VALUE;
        for(float[] water:TrainingChannel.WATER_RECTANGLES) {
            float dx=Math.max(0,Math.max(water[0]-(x+width),x-water[2]));
            float dy=Math.max(0,Math.max(water[1]-(y+height),y-water[3]));
            nearest=Math.min(nearest,Physics.length(dx,dy));
        }
        return nearest;
    }
    public static void run() {
        int minCol=IndustrialRoadView.MIN_COL,maxCol=IndustrialRoadView.MAX_COL;
        int minRow=IndustrialRoadView.MIN_ROW,maxRow=IndustrialRoadView.MAX_ROW;
        int columns=maxCol-minCol,rows=maxRow-minRow;
        int[][] owner=new int[rows][columns];
        for(int row=0;row<rows;row++) for(int col=0;col<columns;col++)owner[row][col]=-1;
        check(TrainingChannelView.FACTORY_PLACEMENTS>10 && TrainingChannelView.FACTORY_PLACEMENTS<100,"sparse compact workshops leave tutorial sightlines open");
        check(TrainingChannelView.FACTORY_PLACEMENTS<=rows*columns,"placement count is bounded by ground-cell capacity");
        int seen=0;float minHeight=Float.MAX_VALUE,maxHeight=0;
        boolean low=false,middle=false,high=false;float previousDepth=-Float.MAX_VALUE;
        for(int slot=0;slot<TrainingChannelView.FACTORY_PLACEMENTS;slot++) {
            int cell=TrainingChannelView.factoryCell(slot);seen|=1<<cell;
            float x=TrainingChannelView.factoryX(slot),y=TrainingChannelView.factoryY(slot);
            float hx=FactoryArt.footprintX(cell),hy=FactoryArt.footprintY(cell),height=FactoryArt.height(cell);
            float[] bounds={x-hx,y-hy,x+hx,y+hy};
            for(float edge:bounds) check(grid(edge),"factory footprint edges register to the 64-unit world tile grid");
            check(y-x>=previousDepth-.001f,"permanent factories are precomputed in stable isometric ground-depth order");
            previousDepth=y-x;
            float setback=TrainingChannelView.factorySetback(slot);
            check(waterDistance(bounds[0],bounds[1],hx*2,hy*2)+.001f>=setback,
                    "whole factory footprint preserves its water setback, including concave canal corners");
            int fromCol=Math.round(bounds[0]/64),toCol=Math.round(bounds[2]/64);
            int fromRow=Math.round(bounds[1]/64),toRow=Math.round(bounds[3]/64);
            check(Math.floorMod(fromCol,2)==0 && Math.floorMod(fromRow,2)==0,"workshops occupy regular plots with intervening service yards");
            check(IndustrialRoadView.roadClear(fromCol-1,fromRow) || IndustrialRoadView.roadClear(toCol,fromRow)
                    || IndustrialRoadView.roadClear(fromCol,fromRow-1) || IndustrialRoadView.roadClear(fromCol,toRow),"each workshop fronts a connected road");
            check(fromCol>=minCol && toCol<=maxCol && fromRow>=minRow && toRow<=maxRow,"factory remains within the bounded industrial district");
            for(int row=fromRow;row<toRow;row++) for(int col=fromCol;col<toCol;col++) {
                check(!IndustrialRoadView.isRoadCell(col,row),"multi-cell factories never straddle a road lane");
                check(owner[row-minRow][col-minCol]<0,"each ground cell belongs to at most one factory");
                owner[row-minRow][col-minCol]=slot;
            }
            minHeight=Math.min(minHeight,height);maxHeight=Math.max(maxHeight,height);
            low|=height<=75;middle|=height>75 && height<150;high|=height>=150;
            if(height>=150) check(setback>=128,"towering factory structures have the larger shoreline setback");
        }
        check(seen==(15<<12),"the tutorial uses four compact workshop modules");
        check(low && middle && !high && maxHeight<100,"tutorial keeps low and medium silhouettes without tall factories");
        int occupied=0,roads=0,apron=0;
        for(int row=minRow;row<maxRow;row++) for(int col=minCol;col<maxCol;col++) {
            boolean road=IndustrialRoadView.isRoadCell(col,row);

            float distance=waterDistance(col*64,row*64,64,64);
            boolean clearRoad=road && distance>=48 && distance<TrainingScenery.MARGIN;
            check(!road || clearRoad,"connected roads preserve the full raised shoreline apron and finite margin");
            int slot=owner[row-minRow][col-minCol];
            if(slot>=0)occupied++;
            if(clearRoad)roads++;
            boolean navigation=TrainingChannelView.navigationApron(col,row);
            if(navigation)check(slot<0,"train-exit beacon retains its reserved foreground sightline");
            if(!road && slot<0){apron++;}
        }
        check(occupied<roads,"workshops leave most dry land open");
        check(roads>0 && apron>0,"continuous roads and navigable shoreline aprons remain represented");
        System.out.println(checks+" sparse workshop-grid assertions passed; "+TrainingChannelView.FACTORY_PLACEMENTS+" modules, "+occupied+" occupied cells, "+roads+" road tiles, "+apron+" shore/water cells");
    }
    public static void main(String[] args) {run();}
}
