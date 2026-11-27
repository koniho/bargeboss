package com.bargeboss.game;

/** Water-colored route chevrons. Cyan emission belongs only to the pickup event. */
final class BoostPickupArt {
    private BoostPickupArt(){}
    /** Caller owns the shore cutout; every chevron is on the water underneath vessels. */
    static void water(Game g,Painter p,int i){
        boolean ready=g.transit.spent[i]==0;
        boolean triggered=g.transit.flashBuoy==i && g.transit.flash>0;
        if(!ready && !triggered)return;
        float x=MontroseTransit.BUOYS[i*2],y=MontroseTransit.BUOYS[i*2+1];
        int before=Math.max(0,i-1),after=Math.min(MontroseTransit.count()-1,i+1);
        float dx=MontroseTransit.BUOYS[after*2]-MontroseTransit.BUOYS[before*2];
        float dy=MontroseTransit.BUOYS[after*2+1]-MontroseTransit.BUOYS[before*2+1];
        float length=Math.max(1,Physics.length(dx,dy));dx/=length;dy/=length;
        float fade=triggered?g.transit.flash/40f:0;fade*=fade;
        Painter water=new RiverPainter(p,g.layout.camera);
        for(int k=0;k<3;k++){
            float cx=x+dx*(k-1)*13,cy=y+dy*(k-1)*13;
            if(triggered){
                // The glow traces the same chevron, never a surrounding ring or filled disc.
                stroke(water,cx,cy,dx,dy,7,SoftCue.alpha(0xff19dfff,.12f*fade));
                stroke(water,cx,cy,dx,dy,4.5f,SoftCue.alpha(0xff22e7ff,.26f*fade));
            }
            int color=triggered?SoftCue.alpha(0xff5aeeff,fade):0xa057858b;
            float[] local={-5,-7,-1,-7,6,0,-1,7,-5,7,2,0},q=new float[local.length];
            for(int n=0;n<local.length;n+=2){q[n]=cx+dx*local[n]-dy*local[n+1];q[n+1]=cy+dy*local[n]+dx*local[n+1];}
            water.poly(q,color);
            if(triggered)stroke(water,cx,cy,dx,dy,.8f,SoftCue.alpha(0xffc7fbff,.85f*fade));
        }
    }
    private static void stroke(Painter p,float x,float y,float dx,float dy,float width,int color){
        p.line(x-dx*3+dy*7,y-dy*3-dx*7,x+dx*4,y+dy*4,width,color);
        p.line(x+dx*4,y+dy*4,x-dx*3-dy*7,y-dy*3+dx*7,width,color);
    }
}
