package com.bargeboss.game;

/** Geometry, material grading, transformed normals and shared receiver coverage. */
final class SceneryReceiverTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static void run(){
        Game g=new Game();g.resetPracticeYard();g.changeLighting(3);
        for(Body b:g.cargo)if(b!=null)b.active=false;g.tug.active=false;
        SceneryLighting.begin(g);SceneryLighting light=SceneryLighting.get(g);
        int ambient=0x80403020,albedo=0xff808080;
        light.add(-10000,-10000,25,50,1,0xffff0000);
        int lit=light.shade(ambient,albedo,-10000,-10000,0,0,0,1);
        check((lit>>16&255)>(ambient>>16&255) && (lit&0xff00ffff)==(ambient&0xff00ffff),"colored source preserves alpha and other channels");
        check(light.shade(ambient,albedo,-10000,-10000,0,0,0,-1)==ambient,"back face rejects light");
        check(light.shade(ambient,albedo,-10000,-10000,-50,0,0,1)==ambient,"physical height controls finite reach");
        for(int preset=0;preset<4;preset++)for(int material=0;material<=SurfaceMaterial.CORRUGATED;material++){
            check(LightingProfile.preset(preset).surface(lit,material|SurfaceMaterial.LIT)==lit,"native material must not darken a lit face twice");
            check(SurfaceMaterial.base(material|SurfaceMaterial.LIT)==material,"native material texture preserved");
        }
        float x=-10000,y=-10000;g.layout=Layout.focused(960,600,x,y,2,480,300);
        ArticulatedModel.Builder b=new ArticulatedModel.Builder();int root=b.node("receiver",-1,x,y,18,0,.8f);
        b.box(root,0,0,10,30,24,20,0xffbbaa99,SurfaceMaterial.CONCRETE);
        b.box(root,0,0,24,12,10,4,0xffbbaa99,-1);
        b.box(root,0,0,30,4,4,4,0xffee9933,SurfaceMaterial.EMISSIVE);
        ArticulatedModel model=b.build();ArticulatedModel.Pose pose=model.pose(.4f);model.prepare(pose);
        Capture p=new Capture();p.profile=g.lighting();
        for(int receiver=0;receiver<4;receiver++){
            g.tug.active=false;SceneryLighting.begin(g);p.reset();draw(g,p,receiver,model,pose,x,y);
            long geometry=p.geometry,colors=p.colors;int calls=p.calls;
            g.tug.active=true;g.tug.x=x-20;g.tug.y=y+20;SceneryLighting.begin(g);p.reset();draw(g,p,receiver,model,pose,x,y);
            check(p.geometry==geometry && p.calls==calls,"lighting preserves receiver geometry and submission count");
            check(p.colors!=colors,"rigid flat/textured model, animated worker, vehicle and bench receive nearby light: "+receiver);
            long hash=p.colors;p.reset();draw(g,p,receiver,model,pose,x,y);check(p.colors==hash,"rendering does not advance light sources");
        }
        ArticulatedModel.Builder e=new ArticulatedModel.Builder();int n=e.node("lens",-1,x,y,20,0,0);e.box(n,0,0,0,4,4,4,0xffee9933,SurfaceMaterial.EMISSIVE);
        ArticulatedModel lens=e.build();ArticulatedModel.Pose lp=lens.pose(0);g.tug.active=false;SceneryLighting.begin(g);p.reset();lens.draw(g,p,lp);long emission=p.colors;
        g.tug.active=true;SceneryLighting.begin(g);p.reset();lens.draw(g,p,lp);check(emission==p.colors,"emissive lenses do not receive another light grade");
        System.out.println("Scenery receiver normals, physical height, material grading, alpha, geometry and repeatability checks passed");
    }
    private static void draw(Game g,Capture p,int which,ArticulatedModel m,ArticulatedModel.Pose pose,float x,float y){
        if(which==0)m.draw(g,p,pose);
        else if(which==1)DockWorkers.drawAt(g,p,x,y,1.2f);
        else if(which==2)YardActivity.box(g,p,g.layout.camera,x,y,20,12,0,15,0xffbb9966,0xff998866);
        else m.drawRange(g,p,pose,-2,2);
    }
    private static final class Capture implements Painter {
        LightingProfile profile;int calls;long geometry,colors;
        void reset(){calls=0;geometry=colors=17;}
        public LightingProfile lighting(){return profile;}
        public void poly(float[] xy,int color){calls++;for(float v:xy){check(Float.isFinite(v),"finite transformed geometry");geometry=geometry*31+Float.floatToIntBits(v);}colors=colors*31+color;}
        public void surfacePolygon(float[] xy,float[] uv,int material,int color,float wet){check((material&SurfaceMaterial.LIT)!=0,"texture uses explicit prelit color");poly(xy,profile.surface(color,material));for(float v:uv)geometry=geometry*31+Float.floatToIntBits(v);geometry=geometry*31+SurfaceMaterial.base(material);}
        public void circle(float x,float y,float r,int c){}
        public void line(float x,float y,float xx,float yy,float w,int c){}
        public void rect(float x,float y,float w,float h,int c){}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
    }
    public static void main(String[] args){run();}
}
