package com.bargeboss.game;
/** Compare this exact painter trace against the previous ArticulatedModel implementation. */
public final class BridgeDepthReview implements Painter {
    private long hash=1,calls;
    private void add(int v){hash=31*hash+v;}
    private void value(float v){add(Float.floatToIntBits(v));}
    public void poly(float[] xy,int color){calls++;add(color);add(xy.length);for(float v:xy)value(v);}
    public void surfacePolygon(float[] xy,float[] uv,int material,int color,float strength){poly(xy,color);add(material);value(strength);for(float v:uv)value(v);}
    public void rect(float x,float y,float w,float h,int c){poly(new float[]{x,y,w,h},c);}
    public void circle(float x,float y,float r,int c){poly(new float[]{x,y,r},c);}
    public void line(float x,float y,float xx,float yy,float w,int c){poly(new float[]{x,y,xx,yy,w},c);}
    public void sprite(int atlas,int frame,float x,float y,float w,float h,float r,float alpha){add(atlas);add(frame);poly(new float[]{x,y,w,h,r,alpha},0);}
    public void spriteBlend(int atlas,int first,int second,float x,float y,float w,float h,float r,float blend,float alpha){add(second);value(blend);sprite(atlas,first,x,y,w,h,r,alpha);}
    public static void main(String[] args){
        BridgeDepthReview p=new BridgeDepthReview();int intervals=0;
        for(int location=0;location<3;location++){
            Game root=new Game();if(location==0)root.startTutorial();else{root.challengeId=location==1?0:Challenges.INDIANA;root.reset();}
            Game g=root.activeGame();
            for(LandmarkBridge bridge:g.passages.bridges){
                g.layout=Layout.focused(960,600,bridge.x,bridge.y,.8f,480,380);g.visualTicks=180;
                for(float phase:new float[]{0,.125f,.37f,.75f,1}){
                    float previous=Float.NEGATIVE_INFINITY;
                    for(int step=0;step<=40;step++){
                        float next=step==40?Float.POSITIVE_INFINITY:bridge.y-bridge.x-800+step*40;
                        bridge.drawRange(g,p,phase,phase==1,previous,next);previous=next;intervals++;
                    }
                }
            }
        }
        System.out.println("intervals="+intervals+" calls="+p.calls+" exact_trace="+Long.toHexString(p.hash));
    }
}
