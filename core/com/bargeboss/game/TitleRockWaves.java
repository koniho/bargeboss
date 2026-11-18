package com.bargeboss.game;

/** Side-elevation surf: the same traveling crest clips rock toes and launches painted foam. */
final class TitleRockWaves {
    static final float WIDTH=960;
    static final int SEGMENTS=24;
    // Registered to the genuine transparent entrance in title-rocks-lighthouse-v1.png.
    static final float GAP_LEFT=WIDTH*.682f,GAP_RIGHT=WIDTH*.836f;
    private TitleRockWaves(){}
    static float phase(int tick,float localX){return tick/240f-localX/WIDTH*3;}
    static float waterY(int tick,float localX,float bottom){
        double a=phase(tick,localX)*StrictMath.PI*2;
        return bottom-5-3.5f*(float)StrictMath.sin(a)-.65f*(float)StrictMath.sin(a*2+.8);
    }
    static boolean solid(float localX,float radius){return localX+radius<GAP_LEFT || localX-radius>GAP_RIGHT;}
    static float[] mask(int tick,float x,float bottom){
        float[] triangles=new float[SEGMENTS*12];float step=WIDTH/SEGMENTS;
        for(int i=0;i<SEGMENTS;i++){
            float a=i*step,b=(i+1)*step,ay=waterY(tick,a,bottom),by=waterY(tick,b,bottom);
            int k=i*12;
            triangles[k]=x+a;triangles[k+1]=ay;triangles[k+2]=x+b;triangles[k+3]=by;triangles[k+4]=x+a;triangles[k+5]=bottom+1;
            triangles[k+6]=x+b;triangles[k+7]=by;triangles[k+8]=x+b;triangles[k+9]=bottom+1;triangles[k+10]=x+a;triangles[k+11]=bottom+1;
        }
        return triangles;
    }
    static void draw(Game g,Painter p,float x,float bottom){
        p.pushClipOutside(mask(g.visualTicks,x,bottom));
        try{p.sprite(SpriteAtlas.TITLE_ROCKS,0,x+WIDTH*.5f,bottom-WIDTH/6,WIDTH,WIDTH/3,0,1);}
        finally{p.popClip();}
        float step=WIDTH/SEGMENTS;
        for(int i=0;i<SEGMENTS;i++){
            float a=i*step,b=(i+1)*step,mid=(a+b)*.5f;
            if(x+b<0 || x+a>g.layout.width || !solid(mid,step*.5f))continue;
            float ay=waterY(g.visualTicks,a,bottom),by=waterY(g.visualTicks,b,bottom);
            // A restrained wet lip, with porous paint rather than round particles.
            p.line(x+a,ay,x+b,by,.9f,0x506e9094);
            float rise=Physics.clamp((bottom-2-(ay+by)*.5f)/7,0,1);
            p.spriteAffine(SpriteAtlas.SPLASH,i%4,x+mid,(ay+by)*.5f+.6f,
                    step*1.12f,by-ay,0,5+rise*5,.18f+rise*.32f);
        }
        for(int i=0;i<12;i++){
            float local=(i+.5f)*WIDTH/12;
            if(x+local< -24 || x+local>g.layout.width+24 || !solid(local,30))continue;
            float phase=phase(g.visualTicks,local);phase-=(float)StrictMath.floor(phase);
            float age=(phase-.04f)/.30f;
            if(age<=0 || age>=1)continue;
            float lift=19*4*age*(1-age),width=25+age*20,height=17+age*11;
            float alpha=.66f*(float)StrictMath.sin(age*StrictMath.PI);
            float drift=(i%2==0?1:-1)*age*5;
            p.spriteAffine(SpriteAtlas.SPLASH,i%4,x+local+drift,waterY(g.visualTicks,local,bottom)-lift,
                    width,(i%3-1)*2,0,height,alpha);
        }
    }
}
