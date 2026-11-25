package com.bargeboss.game;

/** Calumet land-use palette and streets for the shared survey ground cache. */
final class CalumetRiverTiles {
    private static final int[] COLOR={0xff4b6048,0xff918d7d,0xff8a765b,0xff5e5851,0xff344b36,0xff425d4a,0xff6c6d6b};
    private CalumetRiverTiles(){}
    static int kind(float x,float y){int col=(int)StrictMath.floor((x-CalumetSurveyData.MIN_X)/CalumetSurveyData.CELL),row=(int)StrictMath.floor((y-CalumetSurveyData.MIN_Y)/CalumetSurveyData.CELL);
        return row<0||col<0||row>=CalumetSurveyData.COVER.length||col>=CalumetSurveyData.COVER[0].length()?0:CalumetSurveyData.COVER[row].charAt(col)-'0';}
    static SurveyGround ground(){
        java.util.ArrayList<float[]> quads=new java.util.ArrayList<float[]>();java.util.ArrayList<Integer> kinds=new java.util.ArrayList<Integer>();
        for(float[] road:CalumetSurveyData.ROADS){int kind=road[4]>0?3:6;float width=kind==3?5:17;float x=road[0],y=road[1],xx=road[2],yy=road[3];
            if(CalumetRiver.contains((x+xx)*.5f,(y+yy)*.5f))continue;
            float dx=xx-x,dy=yy-y,len=Physics.length(dx,dy);if(len<.1f)continue;float nx=-dy/len*width*.5f,ny=dx/len*width*.5f;
            quads.add(new float[]{x+nx,y+ny,xx+nx,yy+ny,xx-nx,yy-ny,x-nx,y-ny});kinds.add(kind);}
        int[] roadKinds=new int[kinds.size()];for(int i=0;i<roadKinds.length;i++)roadKinds[i]=kinds.get(i);
        return new SurveyGround(CalumetWaterGeometry.geometry(),CalumetSurveyData.CELL,new SurveyGround.Style(){
            public int cover(float x,float y){return kind(x,y);}
            public int color(int kind){return COLOR[kind];}
            public int material(int kind){return kind==0 || kind==4 || kind==5?-1:kind==3?SurfaceMaterial.RUST:SurfaceMaterial.CONCRETE;}
            public float wetness(int kind){return .14f;}
        },quads.toArray(new float[quads.size()][]),roadKinds,new float[0][][],new int[0]);
    }
}
