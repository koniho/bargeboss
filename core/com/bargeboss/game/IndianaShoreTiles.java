package com.bargeboss.game;

/** Indiana land-use palette and streets for the shared survey ground cache. */
final class IndianaShoreTiles {
    private static final int[] COLOR={0xff50604c,0xff928d7d,0xff88796a,0xffa29272,0xff625d53,0xff596574,0xff444849};
    private IndianaShoreTiles(){}
    static int kind(float x,float y){int col=(int)StrictMath.floor((x-IndianaSurvey.MIN_X)/IndianaSurvey.CELL),row=(int)StrictMath.floor((y-IndianaSurvey.MIN_Y)/IndianaSurvey.CELL);
        return row<0||col<0||row>=IndianaSurvey.ROWS||col>=IndianaSurvey.COLS?0:IndianaSurvey.COVER[row].charAt(col)-'0';}
    static SurveyGround ground(){
        java.util.ArrayList<float[]> quads=new java.util.ArrayList<float[]>();java.util.ArrayList<Integer> kinds=new java.util.ArrayList<Integer>();
        for(int n=0;n<IndianaSurvey.ROADS.length;n++){float[] road=IndianaSurvey.ROADS[n];int kind=IndianaSurvey.ROADS_KIND[n];float width=kind==4?7:15;
            for(int i=2;i<road.length;i+=2){float x=road[i-2],y=road[i-1],xx=road[i],yy=road[i+1];
                if(IndianaHarbor.contains((x+xx)*.5f,(y+yy)*.5f))continue;
                float dx=xx-x,dy=yy-y,len=Physics.length(dx,dy);if(len<.1f)continue;float nx=-dy/len*width*.5f,ny=dx/len*width*.5f;
                quads.add(new float[]{x+nx,y+ny,xx+nx,yy+ny,xx-nx,yy-ny,x-nx,y-ny});kinds.add(kind);}}
        int[] roadKinds=new int[kinds.size()];for(int i=0;i<roadKinds.length;i++)roadKinds[i]=kinds.get(i);
        return new SurveyGround(IndianaHarbor.geometry(),IndianaSurvey.CELL,new SurveyGround.Style(){
            public int cover(float x,float y){return kind(x,y);}
            public int color(int kind){return COLOR[kind];}
            public int material(int kind){return kind==0?-1:kind==4?SurfaceMaterial.RUST:SurfaceMaterial.CONCRETE;}
            public float wetness(int kind){return .14f;}
        },quads.toArray(new float[quads.size()][]),roadKinds,new float[0][][],new int[0]);
    }
}
