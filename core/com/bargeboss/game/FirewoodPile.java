package com.bargeboss.game;

/** Permanent dry-quay timber stack; shared shaded timber faces, fixed isometric geometry. */
final class FirewoodPile {
    static final float X=FireworksLesson.X+15,Y=FireworksLesson.Y;
    private static final ArticulatedModel[] MODELS={build(false),build(true)};
    private static final ArticulatedModel.Pose[] POSES={MODELS[0].pose(0),MODELS[1].pose(0)};
    static void prepare(){for(int i=0;i<2;i++)MODELS[i].prepare(POSES[i]);}
    private FirewoodPile(){}
    private static ArticulatedModel build(boolean charred){
        ArticulatedModel.Builder b=new ArticulatedModel.Builder();
        int root=b.node("firewood.stack",-1,X,Y,GroundArt.LAND_RISE/ArticulatedModel.Z_SCALE,0,0);
        // Pallet sleepers keep the tall stack visually grounded on the dock apron.
        for(int side=-1;side<=1;side+=2)b.box(root,0,side*25,2,52,7,4,0xff4d3529,-1);
        int[] colors=charred?new int[]{0xff393b3c,0xff50504b,0xff292d32,0xff454643}:new int[]{0xffaa7950,0xff95643f,0xffb88a5b,0xff805536};
        for(int layer=0;layer<6;layer++){
            int count=6-layer/2;
            for(int row=0;row<count;row++){
                float x=(row-(count-1)*.5f)*8.2f;
                float length=70-((row+layer)%3)*4,offset=((row*3+layer)%3-1)*2;
                b.box(root,x,offset,7+layer*6,7.3f,length,5.2f,colors[(row+layer)%colors.length],-1);
                if(charred){
                    // Ash remains on exposed ends and split grain, over a blackened core.
                    b.box(root,x,offset+length*.5f+.08f,7+layer*6,6.8f,.16f,4.5f,0xff9a9586,-1);
                    b.box(root,x+1.4f,offset-6,9.68f+layer*6,.65f,26, .15f,0xff77766d,-1);
                }
            }
        }
        return b.build();
    }
    static void draw(Game g,Painter p){
        RiverCamera c=g.layout.camera;
        Painter top=new RiverPainter(GroundArt.raisedCanvas(g,p),c);
        top.poly(new float[]{X-27,Y-39,X+29,Y-39,X+33,Y+42,X-25,Y+42},0x59030910);
        int state=g.tutorial.fireworks.ignited?1:0;
        MODELS[state].drawLit(g,p,POSES[state],new FireLight(g));
    }
}
