package com.bargeboss.game;

/** Lights ease between registered poses; occlusion still follows actual hull blending. */
public final class VesselLightingTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static boolean near(float a,float b){return Math.abs(a-b)<.0001f;}
    public static void main(String[] args){run();}
    public static void run() {
        for(boolean river:new boolean[]{false,true})for(int n=-320;n<=640;n++) {
            float angle=n*(float)(Math.PI/160);Capture sprite=new Capture();
            SpriteAtlas.draw(river?new RiverPainter(sprite):sprite,SpriteAtlas.TUG,angle,300,500,80,80,1);
            for(int light=0;light<TugLightArt.COUNT;light++) {
                float wa=TugLightArt.visible(light,sprite.a)?1-sprite.blend:0,wb=TugLightArt.visible(light,sprite.b)?sprite.blend:0,total=wa+wb;
                check(near(total,TugLightArt.opacity(light,angle,river)),"fixture visibility uses actual source frame weights");
                if(total<=0)continue;
                float positionBlend=SpriteAtlas.attachmentBlend(angle);
                wa=TugLightArt.visible(light,sprite.a)?1-positionBlend:0;
                wb=TugLightArt.visible(light,sprite.b)?positionBlend:0;total=wa+wb;
                for(boolean vertical:new boolean[]{false,true}) {
                    float a=vertical?TugLightArt.v(light,sprite.a)-TugArt.pivotY(sprite.a):TugLightArt.u(light,sprite.a)-TugArt.pivotX(sprite.a);
                    float b=vertical?TugLightArt.v(light,sprite.b)-TugArt.pivotY(sprite.b):TugLightArt.u(light,sprite.b)-TugArt.pivotX(sprite.b);
                    check(near((a*wa+b*wb)/total*TugArt.SCALE,TugLightArt.offset(light,angle,river,vertical)),"light stays attached through camera yaw, crossfade, hidden frames and negative wrap");
                }
            }
        }
        Game g=new Game();g.tug.x=300;g.tug.y=500;g.visualTicks=154;
        for(int f=0;f<32;f++) {
            g.tug.angle=f*(float)(Math.PI/16);Capture a=new Capture(),b=new Capture(),repeat=new Capture();
            RiverCamera camera=new RiverCamera(0,0,2);
            VesselLighting.tug(g,new RiverPainter(a,camera),g.tug,0);
            VesselLighting.tug(g,new RiverPainter(b,camera),g.tug,3);
            VesselLighting.tug(g,new RiverPainter(repeat,camera),g.tug,0);
            check(a.calls>=10 && a.calls<=15,"bounded fixture budget at every heading");
            check(a.hash==repeat.hash,"paused/repeated render is deterministic");
            check(a.calls==b.calls,"wave bob does not flicker lamp visibility");
            for(int i=0;i<a.calls;i++)check(near(a.x[i],b.x[i]) && near(a.y[i]-6,b.y[i]),"every glow/lens shares hull bob with no lateral drift");
        }
        Capture reflections=new Capture();VesselLighting.reflection(g,new RiverPainter(reflections),g.tug);
        check(reflections.calls<=18,"water partition clipping retains bounded reflection work");
        Body npc=new Body(97,-1,300,500,0);Capture npcLights=new Capture();VesselLighting.tug(g,new RiverPainter(npcLights),npc,0);
        check(npcLights.calls>=10 && npcLights.calls<=15,"NPC tugs use same reviewed fixture system");
        Body cargo=new Body(3,0,300,500,0);Capture barge=new Capture();VesselLighting.barge(g,new RiverPainter(barge),cargo,0);
        check(barge.calls==12,"two authored barge fittings cost twelve primitives");
        check(near(VesselLighting.bargeLocalX(cargo,0),-VesselLighting.bargeLocalX(cargo,1)),"authored bow/stern fittings use symmetric body-local anchors");
        check(Math.abs(VesselLighting.bargeLocalX(cargo,0))<cargo.half+cargo.radius,"fittings remain inside hull length");
        check(g.tug.x==300 && g.tug.y==500 && g.visualTicks==154,"lighting never moves vessels or advances time");
        System.out.println(checks+" vessel lighting anchor, bob, visibility and budget assertions passed");
    }
    private static final class Capture implements Painter {
        int a,b,calls;float blend;float[] x=new float[64],y=new float[64];long hash=17;
        private void add(float xx,float yy,float other,int color){x[calls]=xx;y[calls++]=yy;hash=hash*31+Float.floatToIntBits(xx);hash=hash*31+Float.floatToIntBits(yy);hash=hash*31+Float.floatToIntBits(other);hash=hash*31+color;}
        public void circle(float xx,float yy,float r,int color){add(xx,yy,r,color);}
        public void rect(float xx,float yy,float w,float h,int color){add(xx,yy,w+h,color);}
        public void line(float xx,float yy,float xxx,float yyy,float w,int color){add(xx,yy,w,color);}
        public void poly(float[] points,int color){add(points[0],points[1],points.length,color);}
        public void sprite(int atlas,int frame,float xx,float yy,float w,float h,float rotation,float alpha){a=b=frame;blend=0;}
        public void spriteBlend(int atlas,int first,int second,float xx,float yy,float w,float h,float rotation,float mix,float alpha){a=first;b=second;blend=mix;}
    }
}
