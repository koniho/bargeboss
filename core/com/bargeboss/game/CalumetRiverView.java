package com.bargeboss.game;

/** First survey pass: geographically placed surface tiles and rigid bridge silhouettes. */
public final class CalumetRiverView {
    private static final BridgeLandmark[] BRIDGES=bridges();
    private CalumetRiverView(){}
    private static final class BridgeLandmark {
        final float x,y;final ArticulatedModel model;final ArticulatedModel.Pose pose;
        BridgeLandmark(float[] entry,int id){
            x=entry[0];y=entry[1];int kind=(int)entry[2];
            // Span east/west between the surveyed banks. 92nd and CN cross the oblique inlet.
            float yaw=id<2?.82f:(float)Math.PI/2,dx=(float)StrictMath.cos(yaw),dy=(float)StrictMath.sin(yaw);
            float left=0,right=0;while(left<700 && CalumetRiver.contains(x-dx*left,y-dy*left))left+=3;while(right<700 && CalumetRiver.contains(x+dx*right,y+dy*right))right+=3;
            float span=left+right,width=kind==3?58:kind==2?36:36;
            ArticulatedModel.Builder b=new ArticulatedModel.Builder();int root=b.node("calumet-bridge-"+id,-1,x,y,GroundArt.LAND_RISE/ArticulatedModel.Z_SCALE,yaw,0);
            int steel=0xff686c67,concrete=0xff928778,rust=0xff8d674b;
            for(int side=-1;side<=1;side+=2){float at=side<0?-left:right;
                b.box(root,at+side*12,0,9,24,width+20,18,concrete,SurfaceMaterial.CONCRETE);
                if(kind==2){b.box(root,at+side*10,0,55,15,width+15,110,steel,SurfaceMaterial.STEEL);}
                if(kind==1){
                    int leaf=b.node("bridge-leaf-"+id+"-"+side,root,at,0,12,side<0?0:(float)Math.PI,1.15f);
                    float half=span*.47f;b.box(leaf,half*.5f,0,0,half,width,5,steel,SurfaceMaterial.STEEL);
                    for(int edge=-1;edge<=1;edge+=2){
                        b.beam(leaf,0,edge*width*.5f,5,half,edge*width*.5f,5,2,rust,SurfaceMaterial.RUST);
                        b.beam(leaf,0,edge*width*.5f,23,half,edge*width*.5f,23,2,steel,SurfaceMaterial.STEEL);
                        for(int k=0;k<4;k++)b.beam(leaf,half*k/4,edge*width*.5f,5,half*(k+1)/4,edge*width*.5f,23,2,steel,SurfaceMaterial.STEEL);
                    }
                }
                b.box(root,at+side*24,-width*.5f-10,18,20,18,30,concrete,SurfaceMaterial.CONCRETE);
                b.box(root,at+side*24,-width*.5f-19.5f,22,10,1,6,0xffbc853a,SurfaceMaterial.EMISSIVE);
            }
            if(kind==2 || kind==3){float height=kind==3?160:105;
                b.box(root,(right-left)*.5f,0,height,span+60,width,9,steel,SurfaceMaterial.STEEL);
                for(int side=-1;side<=1;side+=2){b.beam(root,-left,side*width*.5f,height+5,right,side*width*.5f,height+5,3,rust,SurfaceMaterial.RUST);
                    for(int k=0;k<6;k++)b.beam(root,-left+span*k/6,side*width*.5f,height+6,-left+span*(k+1)/6,side*width*.5f,height+30,2,steel,SurfaceMaterial.STEEL);}
            }
            model=b.build();pose=model.pose(1);
        }
    }
    private static BridgeLandmark[] bridges(){BridgeLandmark[] out=new BridgeLandmark[CalumetSurveyData.BRIDGES.length];for(int i=0;i<out.length;i++)out[i]=new BridgeLandmark(CalumetSurveyData.BRIDGES[i],i);
        java.util.Arrays.sort(out,new java.util.Comparator<BridgeLandmark>(){public int compare(BridgeLandmark a,BridgeLandmark b){return Float.compare(a.y-a.x,b.y-b.x);}});return out;}
    static void prepare(){CalumetWaterGeometry.prepare();for(BridgeLandmark b:BRIDGES)b.model.prepare(b.pose);}
    static int count(Game g){return g.calumetStage()?BRIDGES.length:0;}
    static float depth(Game g,int i){return g.layout.camera.screenY(BRIDGES[i].x,BRIDGES[i].y);}
    static void landmark(Game g,Painter p,int i){BridgeLandmark b=BRIDGES[i];RiverCamera c=g.layout.camera;float x=c.screenX(b.x,b.y),y=c.screenY(b.x,b.y),pad=600*c.scale;
        if(x+pad<0 || x-pad>g.layout.width || y+pad<0 || y-pad>g.layout.height)return;b.model.draw(g,p,b.pose);}
    public static void draw(Game g,Painter p){
        p.rect(0,0,g.layout.width,g.layout.height,0xff151b25);
        WaveView.surface(g,p);ArcadeView.water(g,new RiverPainter(p,g.layout.camera));
        ShorelineView.water(g,p);CalumetRiverTiles.walls(g,p);CalumetRiverTiles.ground(g,p);ShorelineView.walls(g,p);
        Renderer.river(g,new RiverPainter(p,g.layout.camera));
    }
    static void berth(Game g,Painter p,int kind){int d=g.voyage.destination(kind);float x=g.voyage.dockX(d),y=g.voyage.dockY(d),nx=g.voyage.normalX(d),ny=g.voyage.normalY(d),tx=-ny,ty=nx;
        float[] q=new float[8];for(int i=0;i<4;i++){float a=i<2?-34:34,b=i==0 || i==3?-57:57;q[i*2]=x+nx*a+tx*b;q[i*2+1]=y+ny*a+ty*b;}
        int color=SoftCue.animated(g,0x779ec8ae);for(int i=0;i<4;i++){int j=(i+1)%4;SoftCue.line(p,q[i*2],q[i*2+1],q[j*2],q[j*2+1],1,color);}
    }
}
