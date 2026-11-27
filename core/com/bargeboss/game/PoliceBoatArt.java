package com.bargeboss.game;
/** Roof fixtures use model-space anchors and the same heave as the hull. */
final class PoliceBoatArt {
    static float beamAngle(Body b,int ticks,int index){return b.angle+(ticks%240)*(float)(StrictMath.PI*2/240)+index*(float)StrictMath.PI;}
    static void water(Game g,Painter p,Body b){
        if(b==null || !b.active)return;
        RiverCamera c=g.layout.camera;float cs=(float)StrictMath.cos(b.angle),sn=(float)StrictMath.sin(b.angle),heave=WaterSurface.lift(g,b);
        for(int i=0;i<2;i++){float[] a=PoliceBoatModel.STROBES[i];
            float x=b.x+cs*a[0]-sn*a[1],y=b.y+sn*a[0]+cs*a[1];
            SpotlightBeam.draw(g,p,x,y,beamAngle(b,g.visualTicks,i),220,.23f,0,
                    -(a[2]*ArticulatedModel.Z_SCALE+heave)*c.scale,i==0?0xffff3040:0xff3a7bff);
        }
    }
    static void strobes(Game g,Painter p,Body b,RiverCamera c,float heave){
        float cs=(float)StrictMath.cos(b.angle),sn=(float)StrictMath.sin(b.angle);
        for(int i=0;i<2;i++){float[] a=PoliceBoatModel.STROBES[i];
            float wx=b.x+cs*a[0]-sn*a[1],wy=b.y+sn*a[0]+cs*a[1];
            float x=c.screenX(wx,wy),y=c.screenY(wx,wy)-(heave+a[2]*ArticulatedModel.Z_SCALE)*c.scale;
            float energy=(g.visualTicks/8)%2==i?.85f:.12f;int color=i==0?0xffff3040:0xff3a7bff;
            p.material(x,y,12*c.scale,0,0,9*c.scale,EffectMaterial.GLOW,0,1,SoftCue.alpha(color,energy));
        }
    }
    private PoliceBoatArt(){}
}
