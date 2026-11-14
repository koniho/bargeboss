package com.bargeboss.game;

/** The moving bridge is one rigid hierarchy, including its lights and fade bounds. */
public final class BridgeModelTest {
    private static int checks;
    private static void check(boolean ok,String message) { checks++;if(!ok) throw new AssertionError(message); }
    private static void close(float a,float b,String message) {check(Math.abs(a-b)<.003f,message+": "+a+" / "+b);}
    private static float length(float[] v) {return (float)StrictMath.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);}
    private static float distance(float[] a,float[] b) {return length(new float[]{a[0]-b[0],a[1]-b[1],a[2]-b[2]});}
    private static Game scene() {
        Game g=new Game();g.challengeId=1;g.reset();
        g.layout=Layout.focused(960,600,300,Bridge.Y,1.5f,480,360);
        g.tug.x=300;g.tug.y=Bridge.Y+500;g.trafficBoat.x=8000;g.trafficBoat.y=8000;
        for(Body b:g.cargo) if(b!=null)b.active=false;
        for(Feeder f:g.feeders) {f.phase=Feeder.IDLE;f.load=null;}
        return g;
    }
    public static void main(String[] args) {run();}
    public static void run() {
        ArticulatedModel model=BridgeModel.MODEL;
        for(int node=0;node<model.nodeCount();node++) {
            check(model.nodeId(model.nodeName(node))==node,"rig part names resolve to stable unique node IDs");
            check(model.parent(node)<node,"rig hierarchy is acyclic and parents precede children");
        }
        int[] leaves={BridgeModel.LEFT_LEAF,BridgeModel.RIGHT_LEAF};
        float[][] points={{0,0,0},{130,0,0},{0,18,18},{65,-18,9},{130,18,18}};
        for(int leaf:leaves) {
            float[] hinge=model.pose(0).point(leaf,0,0,0);
            close(hinge[0],leaf==BridgeModel.LEFT_LEAF?Physics.LEFT:Physics.RIGHT,"hinge remains on its bank");
            close(hinge[1],Bridge.Y,"hinge remains at physical bridge gate");
            close(hinge[2],BridgeModel.HINGE_HEIGHT,"hinge uses authored true Z");
            for(float lift:new float[]{0,.25f,.5f,.75f,1}) {
                ArticulatedModel.Pose pose=model.pose(lift);
                close(distance(hinge,pose.point(leaf,0,0,0)),0,"hinge does not orbit while leaf rotates");
                for(int a=0;a<points.length;a++) for(int b=0;b<a;b++) {
                    float[] aa=points[a],bb=points[b];
                    close(distance(pose.point(leaf,aa[0],aa[1],aa[2]),pose.point(leaf,bb[0],bb[1],bb[2])),
                            distance(aa,bb),"deck, rail, diagonal and attached fixture remain rigid");
                }
                float[] tip=pose.point(leaf,BridgeModel.LENGTH,0,0);
                close(distance(hinge,tip),BridgeModel.LENGTH,"span length stays constant");
                close(tip[2]-hinge[2],BridgeModel.LENGTH*(float)StrictMath.sin(lift*Math.PI/2),"tip rises along hinge rotation");
                close(Math.abs(tip[0]-hinge[0]),BridgeModel.LENGTH*(float)StrictMath.cos(lift*Math.PI/2),"horizontal reach contracts without shrinking leaf");
            }
        }
        for(int bank=0;bank<2;bank++) {
            int tower=model.nodeId((bank==0?"west":"east")+"-mechanism-tower");
            check(tower>=0,"fixed mechanism towers have their own semantic rig nodes");
            for(int tick=0;tick<=120;tick++) {
                ArticulatedModel.Pose pose=model.pose(tick/120f);
                float[] fixed=pose.nodeWorldBounds(tower),moving=pose.nodeWorldBounds(leaves[bank]);
                check(fixed!=null && moving!=null,"tower and complete leaf have measurable physical geometry");
                float clearance=bank==0?moving[0]-fixed[3]:fixed[0]-moving[3];
                check(clearance>1,"leaf, truss and attached lamp geometry clear the fixed tower throughout the sweep: "+clearance);
            }
        }
        for(int k=0;k<BridgeModel.LIGHTS.length;k++) {
            for(int other=0;other<k;other++) check(!BridgeModel.LIGHTS[k].id.equals(BridgeModel.LIGHTS[other].id),"each bridge light has a stable unique ID");
            check(BridgeModel.LIGHTS[k].radius>0,"light spill radius authored");
            float x=BridgeModel.LIGHTS[k].x,y=BridgeModel.LIGHTS[k].y,z=BridgeModel.LIGHTS[k].z;
            float nx=BridgeModel.LIGHTS[k].nx,ny=BridgeModel.LIGHTS[k].ny,nz=BridgeModel.LIGHTS[k].nz;
            int node=BridgeModel.LIGHTS[k].node;
            close(length(new float[]{nx,ny,nz}),1,"authored outward normal is unit length");
            for(float lift:new float[]{0,.5f,1}) {
                ArticulatedModel.Pose pose=model.pose(lift);
                float[] p=pose.point(node,x,y,z),q=pose.point(node,x+nx,y+ny,z+nz),normal=pose.normal(node,nx,ny,nz);
                close(length(normal),1,"part rotation preserves unit light normal");
                for(int axis=0;axis<3;axis++) close(q[axis]-p[axis],normal[axis],"light normal follows same rigid transform as fixture");
            }
        }
        Game g=scene();int maxFragments=0;
        for(float lift:new float[]{0,.125f,.25f,.375f,.5f,.625f,.75f,.875f,1}) {
            ArticulatedModel.Pose pose=model.pose(lift);Capture a=new Capture(),b=new Capture();
            ArticulatedModel.Stats sa=model.draw(g,a,pose),sb=model.draw(g,b,pose);
            check(sa.sourceFaces>0 && sa.visibleFaces>0 && sa.drawnFaces>0,"bridge produces visible depth-sorted geometry at every phase");
            check(sa.drawnFaces==a.polygons,"each sorted visible fragment is drawn exactly once");
            check(sa.fragments<3000,"BSP fragmentation remains bounded across the opening cycle: lift="+lift+" fragments="+sa.fragments);
            maxFragments=Math.max(maxFragments,sa.fragments);
            check(a.hash==b.hash && a.calls==b.calls && sa.fragments==sb.fragments,"same pose yields deterministic shared painter primitives");
            for(int leaf:leaves) {
                float[] bounds=pose.nodeBounds(g.layout.camera,leaf);
                check(bounds!=null && bounds[2]>bounds[0] && bounds[3]>bounds[1],"projected fade bounds include the raised leaf");
                float sx=(bounds[0]+bounds[2])*.5f,sy=(bounds[1]+bounds[3])*.5f;
                g.tug.x=g.layout.camera.worldX(sx,sy);g.tug.y=g.layout.camera.worldY(sx,sy);
                SceneryOcclusion foreground=new SceneryOcclusion(new Capture());
                close(pose.opacity(g,foreground,leaf),1,"bridge behind the vessel cannot obscure it");
                VesselView.draw(g,new RiverPainter(foreground,g.layout.camera),g.tug,true,false);
                check(pose.opacity(g,foreground,leaf)<1,"visible leaf faces become translucent over painted vessel pixels");
                g.tug.x=300;g.tug.y=Bridge.Y+500;
                SceneryOcclusion clear=new SceneryOcclusion(new Capture());
                VesselView.draw(g,new RiverPainter(clear,g.layout.camera),g.tug,true,false);
                close(pose.opacity(g,clear,leaf),1,"clear leaf remains fully visible");
            }
        }
        g=scene();g.layout=Layout.focused(960,600,8000,8000,1,480,300);
        Capture off=new Capture();ArticulatedModel.Stats outside=model.draw(g,off,model.pose(.5f));
        check(outside.drawnFaces==0 && off.polygons==0 && outside.culledNodes>0,"offscreen bridge nodes are culled before painting");
        g=scene();g.bridge.phase=Bridge.OPENING;g.bridge.ticks=60;g.visualTicks=90;
        int ticks=g.ticks,phase=g.bridge.phase,bridgeTicks=g.bridge.ticks,visualTicks=g.visualTicks,hull=g.hull;
        float tx=g.tug.x,ty=g.tug.y;Capture before=new Capture(),after=new Capture();
        BridgeView.draw(g,before);BridgeView.draw(g,after);
        check(before.hash==after.hash && before.calls==after.calls,"bridge geometry and organic lighting repeat at the same visual tick");
        check(ticks==g.ticks && phase==g.bridge.phase && bridgeTicks==g.bridge.ticks && visualTicks==g.visualTicks
                && tx==g.tug.x && ty==g.tug.y && hull==g.hull,"rendering cannot advance bridge or gameplay state");
        System.out.println(checks+" rigid bridge, attached light, BSP, fade and determinism assertions passed; max fragments "+maxFragments);
    }
    private static final class Capture implements Painter {
        int calls,polygons;long hash=17;
        private void add(float x) {check(Float.isFinite(x),"bridge output remains finite");hash=hash*31+Float.floatToIntBits(x);}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha) {calls++;add(x);add(y);add(alpha);}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float blend,float alpha) {sprite(a,f,x,y,w,h,r,alpha);}
        public void rect(float x,float y,float w,float h,int color) {calls++;add(x);add(y);add(w);add(h);hash=hash*31+color;}
        public void circle(float x,float y,float r,int color) {calls++;add(x);add(y);add(r);hash=hash*31+color;}
        public void poly(float[] xy,int color) {calls++;polygons++;for(float v:xy)add(v);hash=hash*31+color;}
        public void line(float x,float y,float xx,float yy,float w,int color) {calls++;add(x);add(y);add(xx);add(yy);add(w);hash=hash*31+color;}
    }
}
