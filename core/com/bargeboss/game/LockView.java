package com.bargeboss.game;

/** Fixed-view XYZ lock machinery. Rigid leaves rotate about bank hinges; lamps follow parts. */
public final class LockView {
    private static final ArticulatedModel[] MODELS=new ArticulatedModel[2];
    private static final float[] OPENINGS={Float.NaN,Float.NaN};
    private static final int STEEL=0xff4c5757,RUST=0xffa75f3e,CONCRETE=0xff7b8076;
    private LockView(){}
    public static float gateY(int index){return LockDoors.hingeY(index);}
    public static float gateDepth(Game g,int index){return g.layout.camera.screenY((Physics.LEFT+Physics.RIGHT)*.5f,gateY(index));}
    private static float opening(Lock lock,int index){return index==0?lock.exitOpening():lock.entryOpening();}
    private static boolean passable(Lock lock,int index){return index==0?lock.exitPassable():lock.entryPassable();}
    /** All moving parts have explicit pivots, stable names and local geometry. */
    static ArticulatedModel model(int index,float opening){
        if(MODELS[index]!=null && Float.floatToIntBits(OPENINGS[index])==Float.floatToIntBits(opening))return MODELS[index];
        ArticulatedModel.Builder b=new ArticulatedModel.Builder();
        float y=gateY(index),half=LockDoors.LENGTH;
        int fixed=b.node("bank-machinery",-1,0,0,0,0,0);
        for(int side=0;side<2;side++){
            float x=side==0?Physics.LEFT-10:Physics.RIGHT+10;
            b.box(fixed,x,y,17,20,34,34,CONCRETE,SurfaceMaterial.CONCRETE);
            b.box(fixed,x,y,35,24,38,4,STEEL,SurfaceMaterial.STEEL);
            b.box(fixed,x,y+12,43,10,10,12,RUST,SurfaceMaterial.RUST);
            b.box(fixed,x,y+12,51,7,7,4,0xff202f38);
            // Thick exposed hinge shafts and driven crank housings remain on land.
            b.box(fixed,side==0?Physics.LEFT-1:Physics.RIGHT+1,y,18,6,12,36,RUST,SurfaceMaterial.RUST);
        }
        for(int side=0;side<2;side++){
            float x=LockDoors.hingeX(side);
            int leaf=b.node(side==0?"left-leaf":"right-leaf",-1,x,y,0,LockDoors.angle(index,side,opening),0);
            b.box(leaf,half*.5f,0,12,half,8,24,STEEL,SurfaceMaterial.STEEL);
            b.box(leaf,half*.5f,0,25,half+2,10,3,RUST,SurfaceMaterial.RUST);
            b.box(leaf,half*.5f,0,2,half+2,10,3,0xff253741);
            for(float at=12;at<half;at+=24)b.box(leaf,at,-4.5f,13,4,3,24,RUST,SurfaceMaterial.RUST);
            b.beam(leaf,5,-5,5,half-5,-5,22,3,0xff73868b);
            b.box(leaf,half-6,0,29,5,5,5,0xff24323b);
        }
        OPENINGS[index]=opening;MODELS[index]=b.build();return MODELS[index];
    }
    private static boolean visible(Game g,int index){
        RiverCamera c=g.layout.camera;float x=c.screenX(300,gateY(index)),y=c.screenY(300,gateY(index)),s=c.scale;
        return x+250*s>=0 && x-250*s<=g.layout.width && y+180*s>=0 && y-100*s<=g.layout.height;
    }
    /** Below-hull water treatment and moving wet marks communicate the enclosed level cycle. */
    public static void water(Game g,Lock lock,Painter canvas){
        RiverCamera c=g.layout.camera;Painter world=new RiverPainter(canvas,c);
        float mid=(Lock.EXIT_Y+Lock.ENTRY_Y)*.5f,px=c.screenX(300,mid),py=c.screenY(300,mid),s=c.scale;
        if(px+500*s<0 || px-500*s>g.layout.width || py+300*s<0 || py-300*s>g.layout.height)return;
        world.rect(Physics.LEFT+2,Lock.EXIT_Y+2,Physics.RIGHT-Physics.LEFT-4,Lock.ENTRY_Y-Lock.EXIT_Y-4,0x182f6570);
        flow(g,lock,new WaterEffectsPainter(g,world));
        float wet=-8+lock.waterLevel()*32;
        for(int side=0;side<2;side++){
            float x=side==0?Physics.LEFT+1:Physics.RIGHT-1;
            line3(g,canvas,x,Lock.EXIT_Y+10,wet,x,Lock.ENTRY_Y-10,wet,2.4f,0xcc8de2ce);
            for(int mark=0;mark<=4;mark++){
                float z=-8+mark*8;
                line3(g,canvas,x,Lock.ENTRY_Y-48,z,x,Lock.ENTRY_Y-36,z,1,0x88909a87);
            }
            // A high/low staff on each wall makes the rising wet line readable in-world.
            line3(g,canvas,x,Lock.ENTRY_Y-42,-8,x,Lock.ENTRY_Y-42,24,2,0xaa738d94);
            line3(g,canvas,x,Lock.ENTRY_Y-42,-8,x,Lock.ENTRY_Y-42,wet,3,0xdd8de2ce);
            // Sluice turbulence is local to the chamber and fades outside the filling phase.
            if(lock.phase==Lock.FILLING)for(int ripple=0;ripple<4;ripple++){
                float age=(g.visualTicks*.014f+ripple*.25f);age-=(float)StrictMath.floor(age);
                float radius=5+age*19,alpha=.3f*(float)StrictMath.sin(age*Math.PI);
                float centerX=x+(side==0?22:-22),centerY=Lock.EXIT_Y+65+side*135;
                for(int segment=0;segment<12;segment++){
                    double a=segment*Math.PI/6,b=(segment+.7)*Math.PI/6;
                    SoftCue.line(world,centerX+radius*(float)StrictMath.cos(a),centerY+radius*(float)StrictMath.sin(a),
                            centerX+radius*(float)StrictMath.cos(b),centerY+radius*(float)StrictMath.sin(b),1,SoftCue.alpha(0xffa6d9c8,alpha));
                }
            }
        }
        if(lock.phase==Lock.FILLING || lock.phase==Lock.CLOSING || lock.phase==Lock.OPENING)
            SoftCue.caption(world,"WATER LEVEL "+Math.round(lock.waterLevel()*100)+"%",300,mid,11,VisualStyle.CREAM);
    }
    /** Sparse drifting streaks sample the same velocity field that acts on hulls. */
    private static void flow(Game g,Lock lock,Painter p){
        for(int row=0;row<12;row++)for(int col=0;col<3;col++){
            float x=220+col*80,y=-130+row*50;
            float age=(g.ticks*.009f+row*.173f+col*.317f)%1;
            float vx=lock.currentX(x,y),vy=lock.currentY(x,y),speed=Physics.length(vx,vy);
            if(speed<2)continue;
            float xx=x+vx*(age-.5f)*1.7f,yy=y+vy*(age-.5f)*1.7f;
            float alpha=.55f*(float)StrictMath.sin(age*Math.PI)*Math.min(1,speed/22);
            int color=SoftCue.alpha(0xffa4e2d3,alpha);
            SoftCue.line(p,xx-vx*.6f,yy-vy*.6f,xx,yy,1.2f,color);
            SoftCue.dot(p,xx,yy,1.7f,color);
        }
    }
    private static void line3(Game g,Painter p,float x,float y,float z,float xx,float yy,float zz,float width,int color){
        RiverCamera c=g.layout.camera;
        p.line(c.screenX(x,y),c.screenY(x,y)-z*ArticulatedModel.Z_SCALE*c.scale,c.screenX(xx,yy),c.screenY(xx,yy)-zz*ArticulatedModel.Z_SCALE*c.scale,width*c.scale,color);
    }
    static void lights(Game g,SceneryLighting out){
        for(int i=0;i<2;i++){ArticulatedModel m=model(i,opening(g.lock,i));ArticulatedModel.Pose pose=m.pose(0);float y=gateY(i);
            int color=passable(g.lock,i)?0xff8fe4a2:0xffff5945;
            for(int side=0;side<2;side++){
                out.add(side==0?Physics.LEFT-10:Physics.RIGHT+10,y+12,54,125,.5f,color);
                float[] a=pose.point(m.nodeId(side==0?"left-leaf":"right-leaf"),LockDoors.LENGTH-6,0,32);out.add(a[0],a[1],a[2],100,.5f,0xffffc779);
            }
        }
    }
    private static void lamp(Game g,Painter p,ArticulatedModel.Pose pose,int node,float x,float y,float z,int color,float strength,int seed){
        float[] xyz=pose.point(node,x,y,z);if(pose.occluded(xyz))return;
        RiverCamera c=g.layout.camera;float sx=c.screenX(xyz[0],xyz[1]),sy=c.screenY(xyz[0],xyz[1])-xyz[2]*ArticulatedModel.Z_SCALE*c.scale;
        SceneryEffects.glow(p,sx,sy,4*c.scale,color,strength,g.visualTicks,seed*.73f);
        p.circle(sx,sy,1.6f*c.scale,SoftCue.alpha(color,strength));
    }
    /** Insert each gate at its unraised world depth among vessels and rope spans. */
    public static void drawGate(Game g,Lock lock,Painter canvas,int index){
        if(!visible(g,index))return;
        ArticulatedModel m=model(index,opening(lock,index));ArticulatedModel.Pose pose=m.pose(0);m.draw(g,canvas,pose);
        boolean green=passable(lock,index);int signal=green?0xff8ff2a1:0xffff6750;
        float flash=green?.85f:.55f+.45f*(.5f+.5f*(float)StrictMath.sin(g.visualTicks*.08));
        int fixed=m.nodeId("bank-machinery");float y=gateY(index);
        lamp(g,canvas,pose,fixed,Physics.LEFT-10,y+12,54,signal,flash,index*4);
        lamp(g,canvas,pose,fixed,Physics.RIGHT+10,y+12,54,signal,flash,index*4+1);
        float half=(Physics.RIGHT-Physics.LEFT)*.5f;
        lamp(g,canvas,pose,m.nodeId("left-leaf"),half-6,0,32,0xffffc779,.8f,index*4+2);
        lamp(g,canvas,pose,m.nodeId("right-leaf"),half-6,0,32,0xffffc779,.8f,index*4+3);
        Painter world=new RiverPainter(canvas,g.layout.camera);
        String label=index==1?(lock.phase<=Lock.CLOSING?lock.signal():"ENTRY SEALED"):(lock.phase>=Lock.OPENING?lock.signal():"EXIT SEALED");
        SoftCue.caption(world,label,300,y+(index==1?42:-42),10,green?0xffa5edb6:VisualStyle.GOLD);
        if(!green && lock.phase!=Lock.COMPLETE)for(int x=230;x<370;x+=24)SoftCue.line(world,x,y+(index==1?70:-70),x+11,y+(index==1?70:-70),1,0x77ffd086);
    }
    public static void draw(Game g,Lock lock,Painter canvas){drawGate(g,lock,canvas,0);drawGate(g,lock,canvas,1);}
}
