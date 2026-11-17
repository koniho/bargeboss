package com.bargeboss.game;

/** Exact dry-footprint/cost checks for the fixed Indiana shore modules. */
public final class IndianaShoreTilesTest {
    public static void main(String[] args){
        ArticulatedModel.Builder builder=new ArticulatedModel.Builder();IndianaShoreTiles.append(builder);
        ArticulatedModel model=builder.build();ArticulatedModel.Pose pose=model.pose(0);
        int checks=0;
        for(int i=0;i<model.nodeCount();i++){
            float[] box=pose.nodeWorldBounds(i);
            if(box==null)throw new AssertionError("Empty shore module");
            for(float[] water:IndianaHarbor.WATER){
                float w=Math.min(box[3],water[2])-Math.max(box[0],water[0]);
                float h=Math.min(box[4],water[3])-Math.max(box[1],water[1]);
                if(w>.001f && h>.001f)throw new AssertionError("Module crosses water: "+model.nodeName(i));
                checks++;
            }
            if(model.nodeName(i).contains("service-road") || model.nodeName(i).contains("asphalt-repair")){
                float[][] buildings={{554,392.5f,626,447.5f},{541,1266,659,1434},{-239,1096,-121,1264},{-29,-584,89,-416},{1171,-484,1289,-316}};
                for(float[] building:buildings){
                    if(Math.min(box[3],building[2])>Math.max(box[0],building[0]) && Math.min(box[4],building[3])>Math.max(box[1],building[1]))throw new AssertionError("Road crosses building: "+model.nodeName(i));
                    checks++;
                }
            }
            if(box[2]<-.001f || box[5]>GroundArt.LAND_RISE/ArticulatedModel.Z_SCALE+1)
                throw new AssertionError("Shore module changed ground elevation: "+model.nodeName(i));
            checks++;
        }
        if(model.sourceFaceCount()>400)throw new AssertionError("Shore detail budget exceeded: "+model.sourceFaceCount());
        model.prepare(pose);
        Game game=new Game();game.challengeId=Challenges.INDIANA;game.reset();
        game.layout=Layout.focused(1243,700,375,200,.32f,621,350);
        java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(1243,700,java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics=image.createGraphics();
        ArticulatedModel.Stats stats=model.draw(game,new DesktopPainter(graphics),pose);graphics.dispose();
        System.out.println("Shore detail BSP fragments="+stats.fragments+", overview drawn faces="+stats.drawnFaces);
        System.out.println(checks+" Indiana shore footprint/elevation checks; "+model.nodeCount()+" cullable nodes, "+model.sourceFaceCount()+" source faces");
    }
}
