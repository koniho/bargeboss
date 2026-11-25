package com.bargeboss.game;

/** Slowly evolving body texture for the base water fill: a sparse world-anchored field of broad,
 * soft, low-contrast patches (deeper troughs and faint sheen) that drift with a gentle current,
 * wander about their cells and breathe in and out over tens of seconds. Each patch is one GLOW
 * material quad (shader on Metal/Vulkan), drawn right after the flat fill and before waves, so land,
 * masks and wave detail cover it as before. Visual clock only; gameplay never reads it. */
final class WaterTexture {
    private WaterTexture(){}
    /** Finest world cell; doubled (a world-anchored tier, like the ground cache) until a cell spans
     * at least SCREEN_CELL pixels, so the texture keeps the same on-screen grain at every zoom. */
    static final float CELL=24,SCREEN_CELL=170;
    /** Field drift in world units per second (a slow current, river-down-screen). */
    static final float CURRENT_X=2.4f,CURRENT_Y=1.1f;

    static void draw(Game g,Painter p,int water){
        if(g.settings.waterDetail<1 || (water>>>24)!=255)return;
        RiverCamera c=g.layout.camera;float[] b=SurveyGround.visible(g);
        float step=CELL;while(step*c.scale<SCREEN_CELL && step<CELL*64)step*=2;
        float seconds=g.visualTicks/60f,driftX=seconds*CURRENT_X,driftY=seconds*CURRENT_Y;
        int dark=mix(water,0xff000000,.6f),light=mix(water,0xffcfeefa,.5f);
        // Patches live in drifting field space so their identity and seed never jump while moving.
        float pad=step*1.2f;
        int c0=(int)StrictMath.floor((b[0]-driftX-pad)/step),c1=(int)StrictMath.floor((b[2]-driftX+pad)/step);
        int r0=(int)StrictMath.floor((b[1]-driftY-pad)/step),r1=(int)StrictMath.floor((b[3]-driftY+pad)/step);
        float qx=RiverCamera.Q*c.scale,qy=RiverCamera.Q*RiverCamera.DEPTH*c.scale;
        for(int col=c0;col<=c1;col++)for(int row=r0;row<=r1;row++){
            int seed=hash(col*7919+row*104729+(int)step);
            float phase=(seed&1023)/1023f*6.2831855f,size=.95f+((seed>>>10)&255)/255f*.6f;
            boolean sheen=((seed>>>18)&3)<2;
            // Breathing over ~40-70 s; a patch fades fully out before it reappears elsewhere in its cell.
            float rate=.09f+((seed>>>20)&15)/15f*.06f,life=(float)StrictMath.sin(seconds*rate+phase);
            if(life<=0)continue;
            float wander=step*.28f;
            float x=(col+.5f)*step+driftX+wander*(float)StrictMath.sin(seconds*.031f+phase*1.7f);
            float y=(row+.5f)*step+driftY+wander*(float)StrictMath.cos(seconds*.027f+phase*.9f);
            float length=step*1.55f*size,width=step*(.85f+.25f*life)*size;
            if(x+length<b[0] || x-length>b[2] || y+length<b[1] || y-length>b[3])continue;
            float alpha=life*(sheen?.2f:.4f);
            // Exact projection of a world ellipse stretched along the current (world x, then y).
            p.material(c.screenX(x,y),c.screenY(x,y),qx*length,-qy*length,qx*width,qy*width,
                EffectMaterial.GLOW,0,1,SoftCue.alpha(sheen?light:dark,alpha));
        }
    }
    private static int mix(int a,int b,float t){
        int r=Math.round(((a>>16)&255)*(1-t)+((b>>16)&255)*t),gg=Math.round(((a>>8)&255)*(1-t)+((b>>8)&255)*t),bb=Math.round((a&255)*(1-t)+(b&255)*t);
        return 0xff000000|(r<<16)|(gg<<8)|bb;
    }
    private static int hash(int seed){int h=seed*0x45d9f3b;h=(h^(h>>>16))*0x45d9f3b;return h^(h>>>16);}
}
