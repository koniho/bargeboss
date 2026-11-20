package com.bargeboss.game;

/** Analytic water shared by hulls, floats and crests. No host state or runtime textures. */
public final class SeaWaves {
    private SeaWaves(){}
    private static final float[] DX={0,.35f},DY={-1,-.9367497f};
    private static final float[] K={(float)(2*StrictMath.PI/220),(float)(2*StrictMath.PI/95)};
    private static final float[] OMEGA={(float)(2*StrictMath.PI/3.4),(float)(2*StrictMath.PI/2.1)};
    private static final float[] AMPLITUDE={2.4f,.8f};
    static float smooth(float a,float b,float x){float t=(x-a)/(b-a);if(t<=0)return 0;if(t>=1)return 1;return t*t*(3-2*t);}
    /** Authored exposure, not a bathymetric/refraction simulation. Narrow entrance admits chop. */
    public static float exposure(float x,float y){
        float mx=MontroseHarbor.planX(x),my=MontroseHarbor.planY(y);
        float lake=Math.max(smooth(1100,1420,my),smooth(1650,2050,mx));
        float entrance=smooth(1080,1350,my)*(1-smooth(120,300,Math.abs(mx-350)));
        // The beach opens northward; retain calm marina south of the park.
        float north=(1-smooth(-650,-200,my))*(.45f+.55f*smooth(2050,2350,mx));
        return Math.max(north,Math.max(lake,entrance));
    }
    static float directionX(Game g,int wave){return (float)(DX[wave]*g.settings.waveCos-DY[wave]*g.settings.waveSin);}
    static float directionY(Game g,int wave){return (float)(DX[wave]*g.settings.waveSin+DY[wave]*g.settings.waveCos);}
    static float bend(float v,int wave){return 10*(float)StrictMath.sin(v*.012+wave*1.7)+5*(float)StrictMath.sin(v*.027+wave);}
    static float bendSlope(float v,int wave){return .12f*(float)StrictMath.cos(v*.012+wave*1.7)+.135f*(float)StrictMath.cos(v*.027+wave);}
    /** Paired orbital components share phase/cosine work; identical to value components 3 and 4. */
    static void montroseDisplacement(Game g,float x,float y,float tick,float[] result){
        float e=exposure(x,y)*g.settings.waveIntensity(),sx=0,sy=0;
        if(e!=0)for(int i=0;i<2;i++){
            float dx=directionX(g,i),dy=directionY(g,i),v=-dy*x+dx*y;
            float phase=K[i]*(dx*x+dy*y-bend(v,i))-OMEGA[i]*tick*Physics.DT;
            float displacement=AMPLITUDE[i]*e*(float)StrictMath.cos(phase);
            sx+=displacement*dx;sy+=displacement*dy;
        }
        result[0]=sx;result[1]=sy;
    }
    /** Per-draw constants; samples retain scalar physics arithmetic and StrictMath. */
    static final class Surface {
        final float intensity;
        final float[] dx=new float[2],dy=new float[2],time=new float[2];
        Surface(Game g,float tick){
            intensity=g.settings.waveIntensity();
            for(int i=0;i<2;i++){dx[i]=directionX(g,i);dy[i]=directionY(g,i);time[i]=OMEGA[i]*tick*Physics.DT;}
        }
        float height(float x,float y){
            float e=exposure(x,y)*intensity;if(e==0)return 0;
            float sum=0;
            for(int i=0;i<2;i++){
                float v=-dy[i]*x+dx[i]*y;
                float phase=K[i]*(dx[i]*x+dy[i]*y-bend(v,i))-time[i];
                sum+=AMPLITUDE[i]*e*(float)StrictMath.sin(phase);
            }
            return sum;
        }
        void displacement(float x,float y,float[] result){
            float e=exposure(x,y)*intensity,sx=0,sy=0;
            if(e!=0)for(int i=0;i<2;i++){
                float v=-dy[i]*x+dx[i]*y;
                float phase=K[i]*(dx[i]*x+dy[i]*y-bend(v,i))-time[i];
                float d=AMPLITUDE[i]*e*(float)StrictMath.cos(phase);
                sx+=d*dx[i];sy+=d*dy[i];
            }
            result[0]=sx;result[1]=sy;
        }
    }
    static float value(Game g,float x,float y,float tick,int component){
        boolean montrose=g.montroseStage();
        float exposure=montrose?exposure(x,y)*g.settings.waveIntensity():1;
        if(exposure==0)return 0;
        float sum=0;
        for(int i=0;i<2;i++){
            float dx,dy,k,omega,amplitude;
            if(montrose){dx=directionX(g,i);dy=directionY(g,i);k=K[i];omega=OMEGA[i];amplitude=AMPLITUDE[i]*exposure;}
            else {
                float kx=i==0?.017f:-.013f,ky=i==0?.009f:.021f;
                k=Physics.length(kx,ky);dx=-kx/k;dy=-ky/k;omega=(i==0?.048f:.031f)/Physics.DT;amplitude=i==0?.65f:.22f;
            }
            float v=-dy*x+dx*y;
            float phase=k*(dx*x+dy*y-(montrose?bend(v,i):0))-omega*tick*Physics.DT;
            if(component>=3){sum+=amplitude*(float)StrictMath.cos(phase)*(component==3?dx:dy);continue;}
            float h=amplitude*(float)StrictMath.sin(phase);
            if(component!=0 && montrose){
                float slope=bendSlope(v,i),nx=dx+dy*slope,ny=dy-dx*slope,len=Physics.length(nx,ny);
                dx=nx/len;dy=ny/len;
            }
            if(!montrose)h=-h;
            sum+=component==0?h:h*omega*(component==1?dx:dy);
        }
        return sum;
    }
    /** View-bounded crest strips use exactly the phase used by value(). Shared shore clip is active. */
    static void draw(Game g,Painter p){
        if(!g.montroseStage() || !(p instanceof RiverPainter))return;
        RiverCamera c=((RiverPainter)p).camera;
        if(g.settings.waveIntensity()==0)return;
        int budget=512;
        for(int wave=0;wave<2;wave++){
            float dx=directionX(g,wave),dy=directionY(g,wave),lo=Float.MAX_VALUE,hi=-lo,left=lo,right=-lo;
            for(int corner=0;corner<4;corner++){
                float sx=corner%2*g.layout.width,sy=corner/2*g.layout.height;
                float x=c.worldX(sx,sy),y=c.worldY(sx,sy),u=x*dx+y*dy,v=-x*dy+y*dx;
                lo=Math.min(lo,u);hi=Math.max(hi,u);left=Math.min(left,v);right=Math.max(right,v);
            }
            float phase=OMEGA[wave]*g.visualTicks*Physics.DT,period=(float)(2*StrictMath.PI);
            int first=(int)StrictMath.floor((lo*K[wave]-phase-period*.25f)/period)-1;
            int last=(int)StrictMath.ceil((hi*K[wave]-phase-period*.25f)/period)+1;
            float segment=Math.max(24,Math.max(24/c.scale,(right-left)*(last-first+1)/240));
            for(int crest=first;crest<=last;crest++){
                float u=(phase+period*.25f+crest*period)/K[wave];
                if(u<lo-30 || u>hi+30)continue;
                for(float v=(float)StrictMath.floor(left/segment)*segment;v<=right;v+=segment){
                    if(--budget<0)return;
                    float mid=v+segment*.5f,at=u+bend(mid,wave);
                    float x=dx*at-dy*mid,y=dy*at+dx*mid;
                    if(!MontroseHarbor.contains(x,y))continue;
                    float e=exposure(x,y);if(e==0)continue;
                    float broken=smooth(.05f,.8f,(float)(StrictMath.sin(mid*.065+crest*1.7)*.7+StrictMath.sin(mid*.137+crest*.8)*.3));
                    if(broken<.03f)continue;
                    float curve=bend(v+segment,wave)-bend(v,wave);
                    float width=4+3*g.settings.waveIntensity();
                    WavePatch.foam(g,p,x,y,dx*curve-dy*segment,dy*curve+dx*segment,dx*width,dy*width,
                            WaterFoam.phase(g.visualTicks,crest),5,e*broken*(wave==0?.14f:.06f)*Math.min(2,g.settings.waveIntensity()));
                }
            }
        }
    }
}
