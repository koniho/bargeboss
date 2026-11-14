package com.bargeboss.game;

/** Workshop cycles stay on reviewed fixtures, cheap, deterministic and independent. */
public final class FactoryActivityTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){run();}
    public static void run() {
        int hotKinds=0,steamKinds=0,windowKinds=0,maximum=0;
        for(int asset=0;asset<FactoryArt.COUNT;asset++) {
            boolean hadHot=false,hadSteam=false,hadWindow=false;float smallest=2,biggest=0;
            for(int light=0;light<FactoryArt.lightCount(asset);light++) {
                hadHot|=FactoryActivity.hotOpening(asset,light);hadWindow|=FactoryActivity.window(asset,light);
            }
            for(int outlet=0;outlet<FactoryArt.smokeCount(asset);outlet++)hadSteam|=FactoryActivity.steamOutlet(asset,outlet);
            if(hadHot)hotKinds++;if(hadSteam)steamKinds++;if(hadWindow)windowKinds++;
            check(!FactoryActivity.steamOutlet(asset,FactoryArt.smokeCount(asset)),"steam cannot use an absent exhaust slot");
            for(int tick=0;tick<FactoryActivity.period(asset)*2;tick+=3) {
                Capture p=new Capture();int lit=0;
                for(int light=0;light<FactoryArt.lightCount(asset);light++) {
                    float activity=FactoryActivity.activity(asset,light,tick,.73f),gain=FactoryActivity.radiusGain(asset,light,tick,.73f);
                    check(activity>=0 && activity<=1 && gain>=1 && gain<=1.5f,"activity stays bounded and preserves baseline light strength");
                    if(FactoryActivity.hotOpening(asset,light) && activity>0)lit++;
                    if(FactoryActivity.hotOpening(asset,light)||FactoryActivity.window(asset,light)){smallest=Math.min(smallest,activity);biggest=Math.max(biggest,activity);}
                    FactoryActivity.light(p,asset,light,100,120,4,1,tick,.73f);
                }
                check(lit<=1,"at most one furnace portal throws a charge flare per building");
                for(int outlet=0;outlet<FactoryArt.smokeCount(asset);outlet++)FactoryActivity.steam(p,asset,outlet,100,120,4,1,1,tick,.73f);
                check(p.calls<=7,"extra activity budget is bounded at seven submissions per building");maximum=Math.max(maximum,p.calls);
            }
            if(hadHot||hadWindow)check(smallest==0 && biggest>.95f,"work cycles include both rest and visible peak activity");
            Capture hidden=new Capture();
            for(int light=0;light<FactoryArt.lightCount(asset);light++)FactoryActivity.light(hidden,asset,light,100,120,4,0,90,.73f);
            for(int outlet=0;outlet<FactoryArt.smokeCount(asset);outlet++)FactoryActivity.steam(hidden,asset,outlet,100,120,4,1,0,90,.73f);
            check(hidden.calls==0,"hidden buildings retain no sparks or steam");
        }
        check(hotKinds==4 && steamKinds==6 && windowKinds==11,"activity inventory follows reviewed furnace, window and exhaust metadata");
        Capture a=new Capture(),b=new Capture();
        for(int tick=0;tick<160;tick++) {
            FactoryActivity.light(a,0,FactoryActivity.activeOpening(0,tick,0),100,120,4,.28f,tick,0);
            FactoryActivity.light(b,0,FactoryActivity.activeOpening(0,tick,0),100,120,4,.28f,tick,0);
        }
        check(a.hash==b.hash && a.calls==b.calls,"same visual ticks reproduce every spark and cached flare");
        boolean differs=false;for(int tick=0;tick<240;tick++)if(FactoryActivity.activity(0,1,tick,.3f)!=FactoryActivity.activity(0,1,tick,4.7f))differs=true;
        check(differs,"neighbor buildings use different cycle phases");
        System.out.println(checks+" factory activity anchor, cycle and cost assertions passed; maximum extra calls "+maximum);
    }
    private static final class Capture implements Painter {
        int calls;long hash=17;
        private void value(float v){check(Float.isFinite(v),"activity coordinates remain finite");hash=31*hash+Float.floatToIntBits(v);}
        public void layer(StaticLayer layer,float x,float y,float scale,float alpha) {
            calls++;check(layer.key()==0x42000010 || layer.key()==0x42000011,"only two shared activity kernels are used");
            check(alpha>0 && alpha<=1 && scale>0,"layer opacity and scale remain valid");
            if(layer.key()==0x42000011)check(Math.abs(x+14*scale-100)<.001f && Math.abs(y+14*scale-120)<.001f,"charge flare stays exactly on its reviewed source opening");
            value(x);value(y);value(scale);value(alpha);hash=31*hash+layer.key();
        }
        public void line(float x,float y,float xx,float yy,float w,int color){calls++;value(x);value(y);value(xx);value(yy);hash=31*hash+color;}
        public void rect(float x,float y,float w,float h,int color){throw new AssertionError("unexpected activity primitive");}
        public void circle(float x,float y,float radius,int color){throw new AssertionError("uncached activity shape");}
        public void poly(float[] points,int color){throw new AssertionError("unexpected activity geometry");}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){throw new AssertionError("activity must not substitute artwork");}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){throw new AssertionError("activity must not substitute artwork");}
    }
}
