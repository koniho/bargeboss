package com.bargeboss.game;

/** Restrained in-world rescue cues and bounded, depth-sorted swimmer sprites. */
public final class MontroseRescueView {
    private MontroseRescueView(){}
    public static void water(Game g,Painter p){
        if(MontroseRescue.cruisers(g)){
            if(g.rescue.rafted){Body a=g.cargo[0],b=g.cargo[1];for(int end=-1;end<=1;end+=2){float dy=end*8;
                SoftCue.line(p,a.x,a.y+dy,b.x,b.y+dy,1.5f,0xcdbda67b);}
                SoftCue.caption(p,"RELEASE RAFT LINE",(a.x+b.x)*.5f,(a.y+b.y)*.5f+32,8,VisualStyle.GOLD);
            }
            for(int i=0;i<2;i++){MooredBoats.Berth b=MontroseRescue.berth(i);Body boat=g.cargo[i];
                float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);int color=SoftCue.animated(g,boat.deliveryRecorded?0x9965d5b8:0xbbe8c567);
                for(int side=-1;side<=1;side+=2)for(int end=-1;end<=1;end+=2){float x=b.x+c*end*24-s*side*12,y=b.y+s*end*24+c*side*12;
                    SoftCue.line(p,x,y,x-c*end*10,y-s*end*10,1,color);SoftCue.line(p,x,y,x+s*side*6,y-c*side*6,1,color);}
                SoftCue.caption(p,MontroseRescue.slip(i),b.x,b.y+35,8,color);
            }
        }
        if(MontroseRescue.swimmers(g)){
            MontroseRescue r=g.rescue;
            for(int i=0;i<MontroseRescue.PEOPLE;i++){
                float radius=r.state[i]==0?3.5f:4.5f;int color=r.flash[i]>0?0xb866d9b1:r.state[i]==0?0x447bc2c8:0x446bafad;
                SoftCue.ring(p,r.x[i],r.y[i],radius,1,color,.6f);
            }
            if(r.aiming>=0){int i=r.aiming;SoftCue.ring(p,r.x[i],r.y[i],11,1,0xbbdfc274,1.1f);}
        }
    }
    public static int[] ordered(Game g){if(!MontroseRescue.swimmers(g))return new int[0];int[] ids=new int[MontroseRescue.PEOPLE+1];int count=MontroseRescue.PEOPLE+(g.rescue.flying>=0?1:0);
        for(int i=0;i<count;i++){ids[i]=i;for(int j=i;j>0 && depth(g,ids[j])<depth(g,ids[j-1]);j--){int n=ids[j];ids[j]=ids[j-1];ids[j-1]=n;}}
        return java.util.Arrays.copyOf(ids,count);
    }
    private static float flightX(MontroseRescue r){float t=r.flightTicks/(float)MontroseRescue.THROW_TICKS;return r.fromX+(r.toX-r.fromX)*t;}
    private static float flightY(MontroseRescue r){float t=r.flightTicks/(float)MontroseRescue.THROW_TICKS;return r.fromY+(r.toY-r.fromY)*t;}
    public static float depth(Game g,int i){MontroseRescue r=g.rescue;return g.layout.camera.screenY(i==20?flightX(r):r.x[i],i==20?flightY(r):r.y[i]);}
    public static void draw(Game g,Painter p,int i){MontroseRescue r=g.rescue;RiverCamera c=g.layout.camera;
        float wx=i==20?flightX(r):r.x[i],wy=i==20?flightY(r):r.y[i],x=c.screenX(wx,wy),y=c.screenY(wx,wy);
        if(x< -40 || x>g.layout.width+40 || y< -40 || y>g.layout.height+40)return;
        float[] clip=ShoreOcclusion.region(g,c,wx-22,wy-22,wx+22,wy+22,c.screenY(wx,wy));p.pushClipOutside(clip);
        try{
            if(i==20){float t=r.flightTicks/(float)MontroseRescue.THROW_TICKS,up=5+60*t*(1-t);Painter plane=new RiverPainter(ScreenView.offset(p,0,-up*c.scale),c);
                plane.circle(wx,wy,4,0xffd68d43);plane.circle(wx,wy,2.2f,0xff18313b);
            }else{
                // Sprite registration is shared; the parent agent supplies this authored sheet.
                int frame=i%4+(r.state[i]==1?8:((g.visualTicks/36+i)%2)*4);
                p.sprite(SpriteAtlas.SWIMMERS,frame,x,y-.4f*c.scale*(float)StrictMath.sin(g.visualTicks*.05+i),11*c.scale,11*c.scale,0,1);
            }
        }finally{p.popClip();}
    }
}
