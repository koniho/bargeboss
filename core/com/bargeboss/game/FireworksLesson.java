package com.bargeboss.game;

/** Deterministic tutorial mishap. Only simulated nozzle droplets can extinguish it. */
final class FireworksLesson {
    static final float X=448,Y=-1160,RADIUS=29;
    static final float TOW_X=330,TOW_Y=-1230;
    static final float BERTH_X=-350,BERTH_Y=-1050;
    // Centerline runs south from the celebration basin through two generous bends.
    private static final float[] ROUTE_X={330,300,-50,-50,-240};
    private static final float[] ROUTE_Y={-1120,-1350,-1350,-1050,-1050};
    float target(Game g,boolean x){
        boolean returning=Tutorial.direct(g,g.cargo[3]);
        float best=Float.MAX_VALUE,along=0,total=0;
        for(int i=0;i<ROUTE_X.length-1;i++){
            float dx=ROUTE_X[i+1]-ROUTE_X[i],dy=ROUTE_Y[i+1]-ROUTE_Y[i],len=Physics.length(dx,dy);
            float t=Physics.clamp(((g.tug.x-ROUTE_X[i])*dx+(g.tug.y-ROUTE_Y[i])*dy)/(len*len),0,1);
            float ex=g.tug.x-ROUTE_X[i]-t*dx,ey=g.tug.y-ROUTE_Y[i]-t*dy,d=ex*ex+ey*ey;
            if(d<best){best=d;along=total+t*len;} total+=len;
        }
        float distance=Physics.clamp(along+(returning?-45:45),0,total);
        for(int i=0;i<ROUTE_X.length-1;i++){
            float dx=ROUTE_X[i+1]-ROUTE_X[i],dy=ROUTE_Y[i+1]-ROUTE_Y[i],len=Physics.length(dx,dy);
            if(distance<=len || i==ROUTE_X.length-2){float t=distance/len;return x?ROUTE_X[i]+t*dx:ROUTE_Y[i]+t*dy;}
            distance-=len;
        }
        return x?ROUTE_X[0]:ROUTE_Y[0];
    }
    static final int HEAT=240,IGNITION=140;
    int showTicks,heat,steam;
    boolean launched,ignited,done;
    float launchX,launchY;
    void reset(){showTicks=heat=steam=0;launched=ignited=done=false;}
    void launched(Game g,Body b){
        if(g.tutorial.lesson!=Tutorial.SHOW || (b!=g.cargo[0] && b!=g.cargo[2]) || !g.passages.passable(TutorialTowBridges.ENTRY) || launched)return;
        launched=true;launchX=b.x;launchY=b.y;
    }
    void step(Game g){
        if(steam>0)steam--;
        if(launched && !ignited && ++showTicks>=IGNITION){ignited=true;heat=HEAT;g.radio.transmit(RadioAudio.TEACHER);}
        if(ignited && heat>0 && g.visualTicks%8==0)g.smoke.shore(X,Y,GroundArt.LAND_RISE+18,1);
    }
    boolean waterHit(float x,float y){
        if(!ignited || done || heat==0 || Physics.length(x-X,y-Y)>RADIUS)return false;
        if(--heat==0){done=true;steam=180;}return true;
    }
    boolean visible(){return launched && !ignited || heat>0 || steam>0;}
    float depth(Game g){return g.layout.camera.screenY(X,Y);}
    InstructionPanel.Content content(Game g){
        boolean fire=g.tutorial.lesson>=Tutorial.FIRE_TOW;
        boolean collect=g.tutorial.lesson==Tutorial.FIRE_TOW;
        String title=fire?"THAT WAS NOT THE TINY BUTTON.":launched?"PERHAPS A LITTLE TOO FESTIVE...":"YOU PASSED! CUE THE FIREWORKS.";
        String first=fire?"SMALL UPDATE: THE DOCK IS ON FIRE.":launched?"I SAID CONFETTI. THAT IS A SMALL SUN.":"TAP THE FIREWORKS BARGE, THEN ITS ROCKET.";
        String second=collect?(Tutorial.direct(g,g.cargo[3])?"TOW THE FIRE BARGE INTO THE MARKED WATER.":"FETCH THE FIRE BARGE THROUGH THE SOUTH CANAL."):fire?"HOLD THE NOZZLE. DRAG WATER ONTO THE FLAMES.":launched?"LET'S HOPE NOTHING FLAMMABLE NOTICES.":"A TINY VICTORY DISPLAY. YOU HAVE EARNED IT.";
        return new InstructionPanel.Content("fireworks-lesson-"+g.tutorial.lesson+":"+launched,title,first,second,-1,fire?1-heat/(float)HEAT:0,"INSTRUCTOR / CHANNEL 08",fire?"FIRE RESPONSE":"EQUIPMENT PRACTICE");
    }
    private static float curve(float a,float b,float c,float d,float t){float u=1-t;return u*u*u*a+3*u*u*t*b+3*u*t*t*c+t*t*t*d;}
    private static void flame(Painter p,float x,float y,float w,float h,float lean,int color){
        // Curved asymmetric tongues: a rounded foot and hooked tip, never a row of triangles.
        float[] shape=new float[42];int n=0;
        for(int i=0;i<=8;i++){float t=i/8f;shape[n++]=curve(x-w,x-w*1.4f,x+lean-w*.7f,x+lean,t);shape[n++]=curve(y,y-h*.28f,y-h*.60f,y-h,t);}
        for(int i=1;i<=8;i++){float t=i/8f;shape[n++]=curve(x+lean,x+lean+w*.15f,x+w*1.3f,x+w,t);shape[n++]=curve(y-h,y-h*.72f,y-h*.26f,y,t);}
        for(int i=1;i<=4;i++){float t=i/4f;shape[n++]=x+w-2*w*t;shape[n++]=y+w*.22f*(float)StrictMath.sin(t*Math.PI);}
        p.poly(shape,color);
    }
    void draw(Game g,Painter p){
        RiverCamera c=g.layout.camera;float scale=c.scale;
        if(launched && !ignited){
            float t=Physics.clamp((showTicks-65)/75f,0,1);
            if(t>0){float x=launchX+(X-launchX)*t,y=launchY+(Y-launchY)*t;
                float h=GroundArt.LAND_RISE+150*(1-t)+90*4*t*(1-t);
                float sx=c.screenX(x,y),sy=c.screenY(x,y)-h*scale;
                p.material(sx,sy,30*scale,0,0,30*scale,EffectMaterial.GLOW,0,1,0xffff7f2d);
                p.material(sx,sy,10*scale,0,0,10*scale,EffectMaterial.DOT,0,1,0xffffed9e);
            }return;
        }
        float sx=c.screenX(X,Y),sy=c.screenY(X,Y)-GroundArt.LAND_RISE*scale;
        if(sx< -180 || sx>g.layout.width+180 || sy< -180 || sy>g.layout.height+180)return;
        if(heat>0){
            float strength=.35f+.65f*heat/HEAT;
            p.material(sx,sy,160*scale,0,0,85*scale,EffectMaterial.GLOW,0,strength,0xdfff651d);
            for(int i=0;i<9;i++){
                float pulse=.5f+.5f*(float)StrictMath.sin(g.visualTicks*.17+i*2.1);
                float x=sx+(i-4)*6*scale,h=(48+34*pulse)*strength*scale;
                float lean=(float)StrictMath.sin(g.visualTicks*.11+i*1.7)*13*scale;
                p.material(x,sy-h*.4f,44*scale,0,lean,h*1.8f,EffectMaterial.GLOW,0,1,0xdfff4612);
                flame(p,x,sy,16*scale,h,lean,0xefff4a13);
                flame(p,x,sy,10*scale,h*.72f,lean*.7f,0xffffbd24);
                flame(p,x,sy,5*scale,h*.40f,lean*.3f,0xffffefb0);
            }
            for(int i=0;i<20;i++){
                float age=(g.visualTicks*1.4f+i*17)%120,t=age/120;
                float x=sx+((i%7-3)*8+age*.16f*(float)StrictMath.sin(i))*scale,y=sy-age*.8f*scale;
                p.line(x,y,x-2*scale,y+5*scale,1.8f*scale,SoftCue.alpha(i%2==0?0xffffd329:0xffff692b,(1-t)*strength));
            }
        }else if(steam>0){
            float t=1-steam/180f;
            for(int i=0;i<4;i++)p.smokePuff(sx+(i-1.5f)*13*scale-t*22*scale,sy-(8+t*45)*scale,(12+t*15)*scale,i*.8f+t,SoftCue.alpha(0xcce3f5ee,1-t));
        }
    }
}
