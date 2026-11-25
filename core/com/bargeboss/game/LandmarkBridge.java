package com.bargeboss.game;

import java.util.ArrayList;

/** Photo-led rigid bridge kit. All dimensions are XYZ world units, never rotated artwork. */
final class LandmarkBridge {
    static final int BASCULE=0,LIFT=1,SKYWAY=2,ROLLING=3,FIXED=4;
    final String id,name;
    final float x,y,yaw,dx,dy,left,right,width,height;
    final int type,style;
    final ArticulatedModel fixed,moving;
    final ArticulatedModel.Pose fixedPose;
    private ArticulatedModel.Motion motion;
    final Lamp[] lamps;
    final int[] leaves;
    final float[] radioPoint;
    private static final int STEEL=0xff777d80,RUST=0xff896c59,STONE=0xffa39786;
    static final class Lamp {
        final String id;final boolean moving;final int node;final float x,y,z,nx=0,ny=0,nz=1,radius;final boolean signal;
        Lamp(String id,boolean moving,int node,float x,float y,float z,float radius,boolean signal){this.id=id;this.moving=moving;this.node=node;this.x=x;this.y=y;this.z=z;this.radius=radius;this.signal=signal;}
    }
    static final int CALUMET=0,INDIANA=1,CHICAGO=2;
    LandmarkBridge(String id,String name,float x,float y,float yaw,int type,int style,float width,int location){
        this(id,name,x,y,yaw,type,style,width,bank(location,x,y,yaw,-1),bank(location,x,y,yaw,1));
    }
    LandmarkBridge(String id,String name,float x,float y,float yaw,int type,int style,float width,float l,float r){
        this.id=id;this.name=name;this.x=x;this.y=y;this.yaw=yaw;this.type=type;this.style=style;this.width=width;
        dx=(float)StrictMath.cos(yaw);dy=(float)StrictMath.sin(yaw);
        left=l;right=r;height=type==SKYWAY?230:type==FIXED?180:type==LIFT?170:18;
        if(l+r<25)throw new IllegalArgumentException("Bridge center outside channel: "+id);
        ArticulatedModel.Builder a=new ArticulatedModel.Builder(),b=new ArticulatedModel.Builder();
        int root=a.node(id+"-foundation",-1,x,y,GroundArt.LAND_RISE/ArticulatedModel.Z_SCALE,yaw,0);
        int motion=b.node(id+"-motion-root",-1,x,y,GroundArt.LAND_RISE/ArticulatedModel.Z_SCALE,yaw,0);
        ArrayList<Lamp> lights=new ArrayList<Lamp>();ArrayList<Integer> leafIds=new ArrayList<Integer>();
        for(int side=-1;side<=1;side+=2){float bank=side<0?-left:right;
            a.box(root,bank+side*14,0,5,28,width+24,10,STONE,SurfaceMaterial.CONCRETE);
            if(type==SKYWAY || type==FIXED)continue;
            // Fender piles and abutment sit outside the navigable bank line.
            for(int edge=-1;edge<=1;edge+=2)a.box(root,bank+side*5,edge*(width*.5f+9),4,9,12,20,0xff686658,SurfaceMaterial.WOOD);
            if(type==LIFT){tower(a,root,bank+side*18,width,height,style==0?0xffa66e4e:style==6?0xffb8b7ae:RUST);
                int counter=b.movingNode(id+"-counterweight-"+side,motion,bank+side*18,0,height-28,0,0,0,0,-(height-56));
                b.box(counter,0,0,0,22,width-8,20,STONE,SurfaceMaterial.CONCRETE);
                lamp(a,lights,false,id+"-tower-beacon-"+side,root,bank+side*18,0,height+16,2.3f,true);
            }else{
                float houseHeight=style==2?72:style==3?48:34;
                int house=a.node(id+"-bridgehouse-"+side,root,bank+side*29,-width*.5f-18,0,0,0);
                a.box(house,0,0,houseHeight*.5f,style==2?18:26,24,houseHeight,style==1?0xff705750:STONE,SurfaceMaterial.CONCRETE);
                a.box(house,0,0,houseHeight+2,32,30,5,0xff535b60,SurfaceMaterial.STEEL);
                for(int edge=-1;edge<=1;edge+=2)a.box(house,0,edge*12.5f,houseHeight-7,20,1,9,0xffbd985c,SurfaceMaterial.EMISSIVE);
                lamp(a,lights,false,id+"-house-lamp-"+side,house,0,-14,houseHeight-1,4,false);
            }
            lamp(a,lights,false,id+"-channel-signal-"+side,root,bank+side*4,-width*.5f-10,24,3,true);
        }
        float span=left+right;
        if(type==LIFT){
            int deck=b.movingNode(id+"-lift-span",motion,-left,0,18,0,0,0,0,height-48);leafIds.add(deck);
            truss(b,deck,span,width,style==0?38:46,style==0?0xffa77350:style==6?0xffb8b7ae:RUST,true,false);
            b.box(deck,span*.5f,0,51,27,width*.6f,16,STONE,SurfaceMaterial.CORRUGATED);
            lamp(b,lights,true,id+"-span-cabin",deck,span*.5f,-width*.3f-1,54,3,false);
            for(int side=-1;side<=1;side+=2)lamp(b,lights,true,id+"-span-marker-"+side,deck,span*.5f,side*width*.5f,48,2.3f,true);
        }else if(type==SKYWAY || type==FIXED){
            highSpan(a,root,lights);
        }else{
            int count=type==ROLLING?1:2;
            for(int k=0;k<count;k++){
                int side=k==0?-1:1;float bank=side<0?-left:right,length=type==ROLLING?span:span*.5f-2;
                float angle=side<0?0:(float)StrictMath.PI;
                int leaf=b.movingNode(id+"-leaf-"+k,motion,bank,0,18,angle,1.37f,type==ROLLING?-18:0,0,0);leafIds.add(leaf);
                float trussHeight=type==ROLLING?46:style==2?17:style==4?40:style==3?27:23;
                truss(b,leaf,length,width,trussHeight,style==2?0xff8d9ea2:style==4?0xff557e78:RUST,type==ROLLING,style==2);
                b.box(leaf,-15,0,-8,25,width*.8f,22,STONE,SurfaceMaterial.CONCRETE);
                if(type==ROLLING){
                    // Scherzer rolling heel: rigid rear frame and counterweight translate with its rack.
                    for(int edge=-1;edge<=1;edge+=2){
                        b.beam(leaf,-28,edge*width*.5f,0,-28,edge*width*.5f,66,4,STEEL,SurfaceMaterial.STEEL);
                        b.beam(leaf,-28,edge*width*.5f,66,45,edge*width*.5f,46,4,STEEL,SurfaceMaterial.STEEL);
                        a.beam(root,bank-55,edge*width*.5f,9,bank+7,edge*width*.5f,9,5,STEEL,SurfaceMaterial.STEEL);
                    }
                    b.box(leaf,-30,0,65,25,width+10,20,STONE,SurfaceMaterial.CONCRETE);
                }
                for(int edge=-1;edge<=1;edge+=2)lamp(b,lights,true,id+"-leaf-tip-"+k+"-"+edge,leaf,length-3,edge*width*.5f,style==2?5:trussHeight+3,2.2f,true);
            }
        }
        fixed=a.build();fixedPose=fixed.pose(0);moving=b.build();
        // Same foundation transform and house dimensions used by the visible right-bank house.
        float houseHeight=style==2?72:style==3?48:34;
        radioPoint=fixedPose.point(root,right+29,-width*.5f-18,houseHeight*.65f);
        lamps=lights.toArray(new Lamp[lights.size()]);leaves=new int[leafIds.size()];for(int i=0;i<leaves.length;i++)leaves[i]=leafIds.get(i);
    }
    private static float bank(int location,float x,float y,float yaw,int side){
        float distance=0,dx=(float)StrictMath.cos(yaw)*side,dy=(float)StrictMath.sin(yaw)*side;
        while(distance<1000 && water(location,x+dx*distance,y+dy*distance))distance+=3;
        return distance;
    }
    private static boolean water(int location,float x,float y){return location==INDIANA?IndianaHarbor.contains(x,y):location==CHICAGO?ChicagoRiver.contains(x,y):CalumetRiver.contains(x,y);}
    private static void truss(ArticulatedModel.Builder b,int n,float length,float width,float h,int color,boolean roof,boolean below){
        b.box(n,length*.5f,0,0,length,width,5,STEEL,SurfaceMaterial.STEEL);
        for(int side=-1;side<=1;side+=2){float yy=side*width*.5f;
            b.beam(n,0,yy,0,length,yy,0,3.7f,color,SurfaceMaterial.STEEL);
            for(int i=0;i<6;i++){
                float x=i*length/6,xx=(i+1)*length/6;
                float z=below?-h*(1-i/6f):(roof && i==0?h*.38f:h),zz=below?-h*(1-(i+1)/6f):(roof && i==5?h*.38f:h);
                b.beam(n,x,yy,z,xx,yy,zz,3.1f,color,SurfaceMaterial.RUST);
                b.beam(n,x,yy,0,xx,yy,zz,2.7f,color,SurfaceMaterial.STEEL);
                if(i>0)b.beam(n,x,yy,0,x,yy,z,2.4f,color,SurfaceMaterial.STEEL);
            }
            if(below)b.beam(n,0,yy,7,length,yy,7,1.6f,STONE,SurfaceMaterial.STEEL);
            b.beam(n,0,side*(width*.5f-5),3,length,side*(width*.5f-5),3,1.4f,roof?0xffc2b5a1:0xffbfad78,SurfaceMaterial.STEEL);
        }
        if(roof)for(int i=1;i<6;i+=2)b.beam(n,length*i/6,-width*.5f,h,length*i/6,width*.5f,h,2.5f,color,SurfaceMaterial.STEEL);
    }
    private static void tower(ArticulatedModel.Builder b,int n,float x,float width,float height,int color){
        for(int edge=-1;edge<=1;edge+=2){float y=edge*(width*.5f+8);
            for(int side=-1;side<=1;side+=2)b.beam(n,x+side*16,y,8,x+side*10,y,height,5,color,SurfaceMaterial.STEEL);
            for(int j=0;j<4;j++){float z=10+j*(height-10)/4,zz=10+(j+1)*(height-10)/4;
                b.beam(n,x-12,y,z,x+12,y,zz,2.7f,color,SurfaceMaterial.RUST);
                b.beam(n,x+12,y,z,x-12,y,zz,2.7f,color,SurfaceMaterial.RUST);}
            b.box(n,x,y,height+6,28,22,14,color,SurfaceMaterial.CORRUGATED);
        }
        b.beam(n,x,-width*.5f-8,height,x,width*.5f+8,height,7,color,SurfaceMaterial.STEEL);
    }
    private void highSpan(ArticulatedModel.Builder b,int root,ArrayList<Lamp> lights){
        float start=-left-420,end=right+420,length=end-start;
        b.box(root,(start+end)*.5f,0,height,length,width,9,type==FIXED?STONE:STEEL,SurfaceMaterial.CONCRETE);
        for(int edge=-1;edge<=1;edge+=2){float yy=edge*width*.5f;
            b.beam(root,start,yy,height+5,end,yy,height+5,3,STONE,SurfaceMaterial.STEEL);
            if(type==SKYWAY)for(int i=0;i<16;i++){
                float a=start+length*i/16,bb=start+length*(i+1)/16;
                float z=skyChord(a),zz=skyChord(bb);
                b.beam(root,a,yy,z,bb,yy,zz,4.5f,STEEL,SurfaceMaterial.STEEL);
                b.beam(root,a,yy,height,bb,yy,zz,3.3f,STEEL,SurfaceMaterial.STEEL);
                b.beam(root,a,yy,height,a,yy,z,3.1f,STEEL,SurfaceMaterial.STEEL);
            }
        }
        for(int side=-1;side<=1;side+=2)for(int i=0;i<3;i++){
            float at=(side<0?-left:right)+side*(24+i*176);
            int pier=b.node(id+"-approach-pier-"+side+"-"+i,root,at,0,0,0,0);
            if(type==FIXED){
                b.box(pier,0,0,6,38,42,12,STONE,SurfaceMaterial.CONCRETE);
                b.beam(pier,0,0,12,0,0,height-40,16,0xff7e969b,SurfaceMaterial.CONCRETE);
                for(int edge=-1;edge<=1;edge+=2)b.beam(pier,0,0,height-55,0,edge*width*.36f,height-12,12,0xff7e969b,SurfaceMaterial.CONCRETE);
            }
            if(type!=FIXED)for(int edge=-1;edge<=1;edge+=2){
                float yy=edge*(width*.36f);
                b.box(pier,0,yy,6,34,27,12,STONE,SurfaceMaterial.CONCRETE);
                b.beam(pier,0,yy,12,0,yy,height-12,type==SKYWAY?9:14,type==SKYWAY?STEEL:STONE,SurfaceMaterial.CONCRETE);
            }
            b.beam(pier,0,-width*.45f,height-12,0,width*.45f,height-12,11,STEEL,SurfaceMaterial.STEEL);
            if(type==SKYWAY){b.beam(pier,0,-width*.36f,20,0,width*.36f,height-18,5,STEEL,SurfaceMaterial.STEEL);b.beam(pier,0,width*.36f,20,0,-width*.36f,height-18,5,STEEL,SurfaceMaterial.STEEL);}
            lamp(b,lights,false,id+"-approach-lamp-"+side+"-"+i,root,at,-width*.5f,height+16,4,false);
            b.beam(root,at,-width*.5f,height,at,-width*.5f,height+16,1.8f,STEEL,SurfaceMaterial.STEEL);
        }
    }
    private float skyChord(float x){float bank=x<0?-left:right;return height+20+75*Math.max(0,1-Math.abs(x-bank)/240);}
    private static void lamp(ArticulatedModel.Builder b,ArrayList<Lamp> lights,boolean moving,String id,int node,float x,float y,float z,float radius,boolean signal){
        b.box(node,x,y,z,3,3,3,STEEL,SurfaceMaterial.STEEL);
        lights.add(new Lamp(id,moving,node,x,y,z+1.6f,radius,signal));
    }
    void prepare(){fixed.prepare(fixedPose);if(motion==null)motion=moving.motion(0,1,0,0);motion.pose(0);}
    ArticulatedModel.Pose pose(float amount){if(motion==null)throw new IllegalStateException("Prepare bridge motion before drawing");return motion.pose(Math.max(0,Math.min(1,amount)));}
    int motionFragments(){return motion==null?0:motion.fragmentCount();}
    boolean movable(){return type!=SKYWAY && type!=FIXED;}
    float along(float xx,float yy){return (xx-x)*dx+(yy-y)*dy;}
    float across(float xx,float yy){return -(xx-x)*dy+(yy-y)*dx;}
    boolean visible(Game g){RiverCamera c=g.layout.camera;float px=c.screenX(x,y),py=c.screenY(x,y),pad=(Math.max(left,right)+600)*c.scale;return px+pad>=0 && px-pad<=g.layout.width && py+pad>=0 && py-pad<=g.layout.height;}
    /** Separate model-derived water-plane deck shadows. Land is painted over this pass. */
    void shadow(Game g,Painter p,float amount){
        if(!visible(g))return;ArticulatedModel.Pose pose=pose(amount);
        if(!movable())shadowQuad(p,fixedPose,0,-left-420,right+420,width,height);
        else for(int leaf:leaves)shadowQuad(p,pose,leaf,0,type==LIFT || type==ROLLING?left+right:(left+right)*.5f-2,width,0);
    }
    private static void shadowQuad(Painter p,ArticulatedModel.Pose pose,int node,float start,float end,float width,float z){
        for(int soft=3;soft>=0;soft--){float spread=soft*2.5f;float[] q=new float[8];
            for(int i=0;i<4;i++){float[] v=pose.point(node,i<2?start-spread:end+spread,(i==0 || i==3?-1:1)*(width*.5f+spread),z);
                q[i*2]=v[0]+v[2]*.35f;q[i*2+1]=v[1]+v[2]*.25f;}
            p.poly(q,soft==0?0x16060e17:0x05060e17);
        }
    }
    void lights(Game g,SceneryLighting out,float amount,boolean passable){
        if(!visible(g))return;
        ArticulatedModel.Pose movingPose=pose(amount);
        for(int i=0;i<lamps.length;i++){Lamp lamp=lamps[i];ArticulatedModel.Pose owner=lamp.moving?movingPose:fixedPose;
            float[] at=owner.point(lamp.node,lamp.x,lamp.y,lamp.z);
            out.add(at[0],at[1],at[2],lamp.signal?65:140,(lamp.signal?.28f:.65f)*SceneryEffects.intensity(g.visualTicks,i*.79f),lamp.signal?(passable?0xff7ee2aa:0xffff6850):0xffffbd79);
        }
    }
    void draw(Game g,Painter p,float amount,boolean passable){
        drawRange(g,p,amount,passable,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY);
    }
    /** Lamp world points and halo occlusion depend only on the pose amount, not the camera.
     * Cached per amount so depth slices and still frames skip the exact ray tests. */
    private float lampAmount=Float.NaN;private float[][] lampAt;private boolean[] lampHidden;
    private void lampCache(ArticulatedModel.Pose pose,float amount){
        if(lampAt!=null && amount==lampAmount)return;
        if(lampAt==null){lampAt=new float[lamps.length][];lampHidden=new boolean[lamps.length];}
        for(int i=0;i<lamps.length;i++){Lamp lamp=lamps[i];ArticulatedModel.Pose owner=lamp.moving?pose:fixedPose;
            float[] at=owner.point(lamp.node,lamp.x,lamp.y,lamp.z);lampAt[i]=at;lampHidden[i]=fixedPose.occluded(at) || pose.occluded(at);}
        lampAmount=amount;
    }
    /** Projected rectangle of both poses plus lamp halos; false when it misses the screen. */
    boolean screenBounds(Game g,float amount,float[] out){
        if(!visible(g))return false;ArticulatedModel.Pose pose=pose(amount);RiverCamera c=g.layout.camera;
        out[0]=out[1]=Float.MAX_VALUE;out[2]=out[3]=-Float.MAX_VALUE;
        fixedPose.screenBounds(c,out);pose.screenBounds(c,out);
        float halo=0;for(Lamp lamp:lamps)halo=Math.max(halo,lamp.radius);halo=halo*3*c.scale+8;
        out[0]-=halo;out[1]-=halo;out[2]+=halo;out[3]+=halo;
        return out[2]>=0 && out[0]<=g.layout.width && out[3]>=0 && out[1]<=g.layout.height;
    }
    void drawRange(Game g,Painter p,float amount,boolean passable,float min,float max){
        if(!visible(g))return;ArticulatedModel.Pose pose=pose(amount);
        // Lamps sit on model geometry, so an interval missing both poses has nothing to paint.
        if(!fixedPose.overlapsDepth(min,max) && !pose.overlapsDepth(min,max))return;
        fixed.drawRange(g,p,fixedPose,min,max);moving.drawRange(g,p,pose,min,max);
        RiverCamera c=g.layout.camera;lampCache(pose,amount);
        for(int i=0;i<lamps.length;i++){Lamp lamp=lamps[i];float[] at=lampAt[i];
            if(at[1]-at[0]<=min || at[1]-at[0]>max || lampHidden[i])continue;
            float px=c.screenX(at[0],at[1]),py=c.screenY(at[0],at[1])-at[2]*ArticulatedModel.Z_SCALE*c.scale;
            int color=lamp.signal?(passable?0xff7ee2aa:0xffff6850):0xffffbd79;
            SceneryEffects.glow(p,px,py,lamp.radius*c.scale,color,1,g.visualTicks,i*.79f);
        }
    }
}
