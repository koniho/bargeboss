package com.bargeboss.game;

/** Dusk palette and industrial water light. Pure shared rendering; no wave forces. */
public final class HarborAtmosphere {
    public static final int WATER_DEEP=0xff040d16,WATER_BASE=0xff081c27,WATER_GLINT=0xff63999e,
            SHORE_DARK=0xff141c27,SHORE_LIT=0xff242a30,QUAY_EDGE=0xff807354;
    private HarborAtmosphere() {}
    static float lampY(int index) { return 244+index*112; }
    static float lampBank(int side) { return side==0?Physics.RIGHT+9:Physics.LEFT-9; }
    static boolean visible(Game g,float x,float y,float margin) {
        RiverCamera c=g.layout.camera;float sx=c.screenX(x,y),sy=c.screenY(x,y);
        return sx>-margin && sx<g.layout.width+margin && sy>-margin && sy<g.layout.height+margin;
    }
    static int firstLamp(Game g) { return g.bridgeStage()?-7:0; }
    /** Warm, broken reflections follow the same bank lamps, with dark water between glints. */
    public static void reflections(Game g,Painter world) {
        world=new WaterEffectsPainter(g,world);
        for(int side=0;side<2;side++) for(int lamp=firstLamp(g);lamp<=5;lamp++) {
            float bank=side==0?Physics.RIGHT:Physics.LEFT,along=lampY(lamp),sign=side==0?-1:1;
            if(g.tutorial!=null && TrainingChannel.contains(lampBank(side),along)) continue;
            if(!visible(g,bank,along,70*g.layout.camera.scale)) continue;
            // Broad low-energy reflected light sits beneath the broken wave glints.
            // Clip every lobe to actual water so it cannot illuminate piers or land.
            float breath=SceneryEffects.intensity(g.visualTicks,lamp*1.31f+side*.77f);
            for(int ring=8;ring>=1;ring--)
                world.circle(bank+sign*17,along,ring*5.5f,((int)((2+(8-ring)*.5f)*breath)<<24)|0xe5a35e);
            for(int i=0;i<21;i++) {
                float phase=g.visualTicks*.045f+lamp*1.31f+i*1.87f;
                float shimmer=(.55f+.45f*(float)StrictMath.sin(phase))*SceneryEffects.intensity(g.visualTicks,lamp*1.31f+side*.77f);
                float distance=3+i*3.1f,x=bank+sign*distance;
                float y=along-sign*distance+(float)StrictMath.sin(phase*.61f)*2.1f;
                float span=(3.6f+i*.42f)*(0.5f+shimmer),half=span*.5f;
                int alpha=(int)((side==0?215:145)*shimmer*(1-i/24f));
                world.line(x-half,y-half,x,y-.45f,2.4f,(alpha/3<<24)|0xff7e30);
                world.line(x,y-.45f,x+half,y+half,1.8f,(alpha<<24)|(i<5?0xffcf83:0xe6a35e));
                if(i%4==0) world.line(x-half*1.7f,y-half*1.7f+1.5f,x+half*1.4f,y+half*1.4f+1.5f,
                        1.0f,(alpha/3<<24)|0xf8c77a);
            }
        }
    }
    /** A thin warm aerial layer over the far industrial bank, behind all play objects. */
    public static void bankHaze(Game g,Painter canvas) {
        RiverCamera c=g.layout.camera;float s=c.scale;
        for(int lamp=firstLamp(g);lamp<=5;lamp+=2) {
            float wx=Physics.RIGHT+100,wy=lampY(lamp);
            if(!visible(g,wx,wy,180*s)) continue;
            float x=c.screenX(wx,wy),y=c.screenY(wx,wy)-20*s;
            float drift=(float)StrictMath.sin(g.visualTicks*.004+lamp)*9*s;
            for(int ring=5;ring>=1;ring--) {
                float rx=(45+ring*15)*s,ry=(13+ring*5)*s;
                float[] ellipse=new float[32];
                for(int point=0;point<16;point++) {
                    double a=point*StrictMath.PI/8;
                    ellipse[point*2]=x+drift+rx*(float)StrictMath.cos(a);
                    ellipse[point*2+1]=y+ry*(float)StrictMath.sin(a);
                }
                canvas.poly(ellipse,0x04c89972);
            }
        }
    }
}
