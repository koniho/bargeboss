package com.bargeboss.game;

/** Overlapping native masks retain the legacy cutout's coverage and depth eligibility. */
public final class UnionClipTest {
    public static void run(){int checks=0;
        for(float scale:new float[]{.6f,2.3f})for(int index=0;index<MontroseHarbor.DOCKS.length;index+=11){
            float[] dock=MontroseHarbor.DOCKS[index];float x=dock[0],y=dock[1];
            RiverCamera c=new RiverCamera(71,93,scale);
            for(float depth:new float[]{Float.NEGATIVE_INFINITY,c.screenY(x,y)}){
                float[] old=MontroseHarbor.shore(c,x-90,y-90,x+90,y+90,depth);
                float[] union=MontroseHarbor.shore(c,x-90,y-90,x+90,y+90,depth,true);
                if(old==union)throw new AssertionError("Clip strategies must have distinct cache keys");
                for(int i=0;i<union.length;i+=6){checks++;
                    if(PolygonWater.cross(union[i],union[i+1],union[i+2],union[i+3],union[i+4],union[i+5])<0)
                        throw new AssertionError("Union winding can cancel overlapping shore");
                }
                float sx=c.screenX(x,y),sy=c.screenY(x,y);
                for(int yy=-100;yy<100;yy+=5)for(int xx=-100;xx<100;xx+=5){checks++;
                    float px=sx+(xx+.371f)*scale,py=sy+(yy+.529f)*scale;
                    if(ShoreOcclusion.contains(old,px,py)!=ShoreOcclusion.contains(union,px,py))
                        throw new AssertionError("Union clip changed shoreline coverage: dock "+index);
                }
            }
        }
        Painter capable=new Painter(){public boolean supportsUnionClips(){return true;}
            public void rect(float x,float y,float w,float h,int c){}public void circle(float x,float y,float r,int c){}
            public void line(float x,float y,float xx,float yy,float w,int c){}public void poly(float[] q,int c){}
            public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
            public void spriteBlend(int a,int f,int b,float x,float y,float w,float h,float r,float mix,float alpha){}
        };
        if(!new RiverPainter(capable,new RiverCamera(0,0,1)).supportsUnionClips())throw new AssertionError("River wrapper lost clip strategy");
        System.out.println(checks+" union shoreline winding/coverage/depth comparisons passed");
    }
}
