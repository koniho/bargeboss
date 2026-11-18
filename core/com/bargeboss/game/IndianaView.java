package com.bargeboss.game;

/** Indiana-only rigid scenery and restrained red dusk materials; no platform logic. */
public final class IndianaView {
    private static final int CONCRETE=0xff8b7870,STEEL=0xff79615c,RUST=0xffa56848;
    private static final Landmark[] LANDMARKS=landmarks();
    private IndianaView(){}
    static void prepare(){for(Landmark a:LANDMARKS)a.model.prepare(a.pose);}
    private static final class Landmark {
        final float x,y;final int type;final float[] smoke;final ArticulatedModel model;final ArticulatedModel.Pose pose;
        Landmark(float x,float y,int type){this.x=x;this.y=y;this.type=type;model=model(x,y,type);pose=model.pose(type==3?1:0);smoke=pose.point(0,35,0,144);}
        Landmark(int bridgeIndex){float[] path=IndianaSurvey.BRIDGES[bridgeIndex];x=(path[0]+path[path.length-2])*.5f;y=(path[1]+path[path.length-1])*.5f;type=3;model=bridge(bridgeIndex);pose=model.pose(1);smoke=new float[3];}
    }
    private static Landmark[] landmarks(){java.util.ArrayList<Landmark> out=new java.util.ArrayList<Landmark>();
        out.add(new Landmark(IndianaHarbor.north(41.68093),IndianaHarbor.east(-87.44040),0));
        out.add(new Landmark(IndianaHarbor.north(41.659805),IndianaHarbor.east(-87.4516675),2));
        for(int i=0;i<IndianaSurvey.BRIDGES.length;i++)out.add(new Landmark(i));
        Landmark[] a=out.toArray(new Landmark[out.size()]);
        java.util.Arrays.sort(a,new java.util.Comparator<Landmark>(){public int compare(Landmark x,Landmark y){return Float.compare(x.y-x.x,y.y-y.x);}});return a;
    }
    private static ArticulatedModel bridge(int index){float[] path=IndianaSurvey.BRIDGES[index];
        float x=(path[0]+path[path.length-2])*.5f,y=(path[1]+path[path.length-1])*.5f;
        float dx=path[path.length-2]-path[0],dy=path[path.length-1]-path[1],len=Physics.length(dx,dy);dx/=len;dy/=len;
        float left=0,right=0;while(left<1200 && IndianaHarbor.contains(x-dx*left,y-dy*left))left+=6;while(right<1200 && IndianaHarbor.contains(x+dx*right,y+dy*right))right+=6;
        float span=left+right;if(span<20)span=len;float width=IndianaSurvey.BRIDGES_KIND[index]==4?22:42;
        ArticulatedModel.Builder b=new ArticulatedModel.Builder();int root=b.node("survey-bridge-"+index,-1,x,y,GroundArt.LAND_RISE/ArticulatedModel.Z_SCALE,(float)StrictMath.atan2(dy,dx),0);
        for(int side=-1;side<=1;side+=2){float at=side<0?-left:right;
            b.box(root,at+side*10,0,5,20,width+16,10,CONCRETE,SurfaceMaterial.CONCRETE);
            int leaf=b.node("survey-bridge-leaf-"+index+"-"+side,root,at,0,12,side<0?0:(float)StrictMath.PI,1.18f);
            float half=span*.46f;b.box(leaf,half*.5f,0,0,half,width,5,STEEL,SurfaceMaterial.STEEL);
            for(int edge=-1;edge<=1;edge+=2){b.beam(leaf,0,edge*width*.5f,3,half,edge*width*.5f,3,2,RUST,SurfaceMaterial.RUST);
                b.beam(leaf,0,edge*width*.5f,21,half,edge*width*.5f,21,2,RUST,SurfaceMaterial.RUST);
                for(int n=0;n<4;n++)b.beam(leaf,half*n/4,edge*width*.5f,3,half*(n+1)/4,edge*width*.5f,21,1.7f,STEEL,SurfaceMaterial.STEEL);}
            b.box(root,at+side*16,-width*.5f-12,16,18,18,28,CONCRETE,SurfaceMaterial.CONCRETE);
            b.box(root,at+side*16,-width*.5f-22,21,10,1,5,0xffb27634,SurfaceMaterial.EMISSIVE);
        }return b.build();
    }
    private static ArticulatedModel model(float x,float y,int type){ArticulatedModel.Builder b=new ArticulatedModel.Builder();
        int root=b.node(type==0?"indiana-east-breakwater-light":type==2?"hick-tower":"indiana-industrial-"+x+"-"+y,-1,x,y,GroundArt.LAND_RISE/ArticulatedModel.Z_SCALE,0,0);
        if(type==0){
            b.box(root,0,0,6,65,65,12,CONCRETE,SurfaceMaterial.CONCRETE);
            b.box(root,0,0,54,27,27,96,0xffb8afa0,SurfaceMaterial.CONCRETE);
            b.box(root,0,0,104,38,38,7,STEEL,SurfaceMaterial.STEEL);
            b.box(root,0,0,114,25,25,15,0xff688c78,SurfaceMaterial.STEEL);
            b.box(root,0,0,125,34,34,5,RUST,SurfaceMaterial.RUST);
            b.box(root,0,-13,115,13,1,7,0xff59c58a,SurfaceMaterial.EMISSIVE);
        }else if(type==4){
            b.box(root,0,0,1,14,14,2,CONCRETE,SurfaceMaterial.CONCRETE);
            b.box(root,0,0,5,6,6,8,RUST,SurfaceMaterial.RUST);
            b.box(root,0,0,10,14,8,3,STEEL,SurfaceMaterial.STEEL);
        }else if(type==2){
            // Hick's two-storey pale control building, upper window band and external stairs.
            b.box(root,0,0,22,58,42,44,0xffb3a796,SurfaceMaterial.CONCRETE);
            b.box(root,0,0,49,62,46,15,0xffb9afa0,SurfaceMaterial.CORRUGATED);
            b.box(root,0,0,59,72,55,5,STEEL,SurfaceMaterial.RUST);
            for(int i=-2;i<=2;i++){b.box(root,i*10,-23.4f,49,7,1,9,0xff9a6535,SurfaceMaterial.EMISSIVE);b.box(root,i*10,23.4f,49,7,1,9,0xff9a6535,SurfaceMaterial.EMISSIVE);}
            for(int i=0;i<10;i++)b.box(root,39,30-i*5,2+i*4,13,6,4,STEEL,SurfaceMaterial.STEEL);
            b.beam(root,45,33,12,45,-20,52,1.5f,RUST,SurfaceMaterial.RUST);
            for(int i=0;i<2;i++){float xx=80+i*18;b.beam(root,xx,-100,0,xx,100,0,1.5f,STEEL,SurfaceMaterial.STEEL);}
        }else if(type==3){
            b.box(root,0,0,10,60,70,20,CONCRETE,SurfaceMaterial.CONCRETE);
            // Raised rail bascule: genuine rigid leaf about its bank-side mechanical pivot.
            int leaf=b.node("indiana-raised-rail-leaf",root,20,0,26,0,1.18f);
            b.box(leaf,100,0,0,200,34,6,STEEL,SurfaceMaterial.STEEL);
            for(int side=-1;side<=1;side+=2){b.beam(leaf,0,side*18,5,200,side*18,5,4,RUST,SurfaceMaterial.RUST);b.beam(leaf,0,side*18,34,200,side*18,34,4,RUST,SurfaceMaterial.RUST);
                for(int i=0;i<5;i++)b.beam(leaf,i*40,side*18,5,(i+1)*40,side*18,34,3,RUST,SurfaceMaterial.RUST);}
            b.box(root,-25,0,60,34,58,32,CONCRETE,SurfaceMaterial.CONCRETE);
        }else{
            b.box(root,0,0,30,110,160,60,0xff956d62,SurfaceMaterial.CORRUGATED);
            b.box(root,0,0,65,118,168,10,STEEL,SurfaceMaterial.RUST);
            for(int i=-1;i<=1;i++){b.box(root,35,i*48,102,18,18,75,STEEL,SurfaceMaterial.RUST);b.box(root,35,i*48,141,24,24,5,RUST,SurfaceMaterial.RUST);
                b.box(root,-56,i*48,22,1,27,16,0xffb75e2e,SurfaceMaterial.EMISSIVE);}
            for(int i=0;i<3;i++)b.box(root,-35+i*32,90,25,22,24,50,RUST,SurfaceMaterial.STEEL);
            b.beam(root,-45,85,43,45,85,43,7,STEEL,SurfaceMaterial.RUST);
        }
        if(type==0)IndianaDetails.light(b,root);
        if(type==2)IndianaDetails.tower(b,root);
        if(type==1)IndianaDetails.mill(b,root,x>1000 || y>1300);
        return b.build();
    }
    static int count(Game g){return g.indianaStage()?LANDMARKS.length:0;}
    static float depth(Game g,int i){return g.layout.camera.screenY(LANDMARKS[i].x,LANDMARKS[i].y);}
    static void landmark(Game g,Painter p,int i){Landmark a=LANDMARKS[i];a.model.draw(g,p,a.pose);
        if(a.type==1 && HarborAtmosphere.visible(g,a.x,a.y,180*g.layout.camera.scale)){
            RiverCamera c=g.layout.camera;SceneryEffects.smoke(p,c.screenX(a.smoke[0],a.smoke[1]),c.screenY(a.smoke[0],a.smoke[1])-a.smoke[2]*ArticulatedModel.Z_SCALE*c.scale,4*c.scale,c.scale,.45f,g.visualTicks,a.x*.013f,true);
        }
    }
    public static void draw(Game g,Painter p){
        p.rect(0,0,g.layout.width,g.layout.height,0xff151b25);
        WaveView.surface(g,p);ArcadeView.water(g,new RiverPainter(p,g.layout.camera));
        ShorelineView.water(g,p);
        IndianaShoreTiles.walls(g,p);IndianaShoreTiles.ground(g,p);
        ShorelineView.walls(g,p);
        Renderer.river(g,new RiverPainter(p,g.layout.camera));
    }
    static void berth(Game g,Painter p){float x=IndianaHarbor.DOCK_X,y=IndianaHarbor.DOCK_Y;
        int color=SoftCue.animated(g,0x889ec8ae);
        SoftCue.line(p,x-53,y-22,x+53,y-22,1,color);SoftCue.line(p,x+53,y-22,x+53,y+22,1,color);
        SoftCue.line(p,x+53,y+22,x-53,y+22,1,color);SoftCue.line(p,x-53,y+22,x-53,y-22,1,color);
    }
}
