package com.bargeboss.game;
/** Live rigid rescue deck on the accepted painted hull; all headings share one geometry. */
final class RescueDeck {
    private static final ArticulatedModel.Rigid GEAR=build().rigid();
    private static ArticulatedModel build(){ArticulatedModel.Builder b=new ArticulatedModel.Builder();int root=b.node("rescue-deck",-1,0,0,0,0,0);
        b.box(root,0,0,0,34,21,1,0xffb5b4a0,SurfaceMaterial.STEEL);
        for(int side=-1;side<=1;side+=2){b.box(root,0,side*8,2,28,3,3,0xffdb762b,SurfaceMaterial.SAFETY);
            for(int end=-1;end<=1;end+=2)b.beam(root,end*16,side*10,0,end*16,side*10,7,1,0xffb7b9a8,SurfaceMaterial.STEEL);
            b.beam(root,-16,side*10,7,16,side*10,7,1,0xffd8d8be,SurfaceMaterial.STEEL);}
        // Stable starboard aft ladder; local (-12, +beam) matches boarding logic.
        for(int side=-1;side<=1;side+=2)b.beam(root,-12+side*3,11,2,-12+side*3,22,-7,1,0xffc2c7ae,SurfaceMaterial.STEEL);
        for(int i=0;i<4;i++)b.beam(root,-15,12+i*3,1-i*2,-9,12+i*3,1-i*2,1,0xffd4d8be,SurfaceMaterial.STEEL);
        return b.build();}
    static void draw(Game g,Painter p,Body b){if(!(p instanceof RiverPainter))return;Painter canvas=((RiverPainter)p).canvas();float rise=WaterSurface.lift(g,b)+8;
        GEAR.drawSolid(g,canvas,b.x,b.y,rise,b.angle,0,0);
        if(MontroseRescue.swimmers(g) && g.rescue.barge.boat==b)for(int i=0;i<MontroseRescue.PEOPLE;i++)if(g.rescue.state[i]==4)g.rescue.swimmerModel.draw(g,canvas,i);
        for(int side=-1;side<=1;side+=2)RescueRingModel.draw(g,canvas,RescueBarge.localX(b,side*13,0),RescueBarge.localY(b,side*13,0),rise+2,b.angle,0,0);
    }
    static void prepare(){RescueRingModel.prepare();}
    private RescueDeck(){}
}
