package com.bargeboss.game;

/** Shared, simulation-timed boost spray. Six ballistic packets and two foamy prop lanes;
 * no emitters, accumulated particle storage or additional atlases. */
final class TransitEffects {
    private TransitEffects(){}
    static void bow(Game g,Painter p,boolean boosted){
        Body t=g.tug;float speed=t.speed();if(speed<=55)return;
        RiverCamera c=g.layout.camera;float s=c.scale,k=Math.min(1,(speed-55)/50f),strength=k*(boosted?1.25f:1);
        float ca=(float)StrictMath.cos(t.angle),sa=(float)StrictMath.sin(t.angle);
        p.pushClipOutside(ShoreOcclusion.region(g,c,t.x-150,t.y-150,t.x+150,t.y+150,Float.NEGATIVE_INFINITY,p.supportsUnionClips()));
        try{
            if(boosted){Painter water=new RiverPainter(p,c);
                float fade=Math.min(1,g.transit.boostTicks/18f)*k;
                for(int side=-1;side<=1;side+=2){
                    float x=t.x-ca*(t.half+24)-sa*side*7,y=t.y-sa*(t.half+24)+ca*side*7;
                    WavePatch.sprite(g,water,SpriteAtlas.FOAM,side<0?2:3,x,y,ca*75,sa*75,-sa*19,ca*19,.32f*fade);
                    // Thin reflected cyan accents follow the prop wash, leaving the hull open.
                    float endX=x-ca*28,endY=y-sa*28;
                    water.line(x,y,endX,endY,1.1f,SoftCue.alpha(SoftCue.CYAN,.35f*fade));
                }
            }
            for(int j=0;j<6;j++){
                float u=Math.floorMod(g.visualTicks+j*7,32)/32f,side=j%2==0?1:-1;
                float along=t.half+3-u*(28+speed*.12f),across=side*(7+u*(18+strength*9));
                float x=t.x+ca*along-sa*across,y=t.y+sa*along+ca*across;
                float height=4+32*u*(1-u)*strength,size=(5+u*9)*s*strength;
                float dx=RiverCamera.Q*(-ca-sa)+side*RiverCamera.Q*(ca-sa)*.6f;
                float dy=RiverCamera.Q*RiverCamera.DEPTH*(ca-sa)+side*RiverCamera.Q*RiverCamera.DEPTH*(ca+sa)*.6f-(1-2*u)*.65f;
                float length=Physics.length(dx,dy);dx/=length;dy/=length;
                float alpha=(float)StrictMath.sin(u*StrictMath.PI)*(1-u)*.85f*Math.min(1,strength);
                p.spriteAffine(SpriteAtlas.SPLASH,j%4,c.screenX(x,y),c.screenY(x,y)-height*s,dx*size*1.7f,dy*size*1.7f,-dy*size,dx*size,alpha);
            }
        }finally{p.popClip();}
    }
}
