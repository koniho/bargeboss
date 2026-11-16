package com.bargeboss.game;

/** Exact comparison with the original full-distance wake sum, including crest edges. */
public final class WaterEnergyBoundsTest {
    private static int checks;
    private static void compare(WaterSurface w,Body ignore,Body owner,float x,float y){
        float sum=0;
        for(int i=0;i<WaterSurface.CAPACITY;i++)if(w.age[i]<WaterSurface.LIFE && owner!=ignore){
            float distance=Physics.length(x-w.x[i],y-w.y[i]),radius=w.age[i]*.55f;
            float band=Math.max(0,1-Math.abs(distance-radius)/32);
            sum+=w.strength[i]*band*(1-w.age[i]/(float)WaterSurface.LIFE);
        }
        checks++;
        if(Float.floatToIntBits(w.energy(x,y,ignore))!=Float.floatToIntBits(Math.min(1.8f,sum)))
            throw new AssertionError("Wake energy changed at "+x+","+y);
    }
    public static void run(){
        WaterSurface w=new WaterSurface();Body owner=new Body(1,0,0,0,0,false);
        try{
            java.lang.reflect.Field field=WaterSurface.class.getDeclaredField("source");field.setAccessible(true);
            Body[] source=(Body[])field.get(w);
            for(int i=0;i<WaterSurface.CAPACITY;i++){
                source[i]=owner;w.x[i]=(i%16)*83-650;w.y[i]=(i/16)*101-600;
                w.age[i]=(i*17)%301;w.strength[i]=(i%11+1)*.09f;
            }
        }catch(Exception e){throw new AssertionError(e);}
        for(int y=-1400;y<=1400;y+=19)for(int x=-1400;x<=1400;x+=17)compare(w,null,owner,x,y);
        for(int i=0;i<WaterSurface.CAPACITY;i++)for(float delta:new float[]{-33,-32,-31.999f,0,31.999f,32,33}){
            float r=w.age[i]*.55f+delta;
            compare(w,null,owner,w.x[i]+r,w.y[i]);compare(w,null,owner,w.x[i],w.y[i]+r);
            compare(w,owner,owner,w.x[i]+r,w.y[i]);
        }
        System.out.println(checks+" exact bounded wake-energy comparisons passed");
    }
}
