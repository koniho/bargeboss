package com.bargeboss.game;

/** Bounded water-contact events emitted only by actual hoist motion. */
final class TravelLiftEffects {
    static final int CAPACITY=32,LIFE=90;
    private final float[] x=new float[CAPACITY],y=new float[CAPACITY];
    private final int[] age=new int[CAPACITY];
    private int cursor;
    int emitted,exhaustPuffs;
    TravelLiftEffects(){java.util.Arrays.fill(age,LIFE);}
    void step(){for(int i=0;i<CAPACITY;i++)if(age[i]<LIFE)age[i]++;}
    private void ripple(float xx,float yy){x[cursor]=xx;y[cursor]=yy;age[cursor]=0;cursor=(cursor+1)%CAPACITY;emitted++;}
    void lowering(Game g,float before,float after){
        if(after<=before)return;
        if(g.haulout.liftTicks%10==0){MontroseTravelLift.exhaust(g);exhaustPuffs++;}
        float now=g.haulout.rig.anchor(0,0,0)[2],prior=now+64*(after-before);
        for(int end=-1;end<=1;end+=2){float yy=g.haulout.rig.anchor(0,end*24,0)[1];
            float surface=g.water.height(g,MontroseHaulout.X,yy,g.visualTicks,null)/ArticulatedModel.Z_SCALE;
            if(prior-12>surface && now-12<=surface)for(int i=-1;i<=1;i++)ripple(MontroseHaulout.X+i*11,yy);
            if(now>surface && now-12<surface && g.haulout.liftTicks%12==0){
                float at=14+12*(surface-(now-12))/12;
                ripple(MontroseHaulout.X-at,yy);ripple(MontroseHaulout.X+at,yy);
            }
        }
    }
    void draw(Game g,Painter p){
        if(g.settings.waterDetail<1 || !(p instanceof RiverPainter))return;
        boolean live=false;for(int a:age)if(a<LIFE){live=true;break;}if(!live)return;
        RiverPainter river=(RiverPainter)p;float pad=70;
        float[] mask=ShoreOcclusion.region(g,river.camera,MontroseHaulout.X-pad,MontroseHaulout.Y-pad,MontroseHaulout.X+pad,MontroseHaulout.Y+pad,Float.NEGATIVE_INFINITY);
        river.canvas().pushClipOutside(mask);
        try{for(int i=0;i<CAPACITY;i++)if(age[i]<LIFE){float t=age[i]/(float)LIFE,r=9+26*t,alpha=.3f*(1-t)*(1-t)*Math.min(1,age[i]/5f);
            WavePatch.sprite(g,p,SpriteAtlas.RIPPLES,i%4,x[i],y[i],r,0,0,r,alpha);
        }}finally{river.canvas().popClip();}
    }
}
