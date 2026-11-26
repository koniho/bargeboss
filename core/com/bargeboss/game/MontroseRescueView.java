package com.bargeboss.game;

/** Restrained in-world rescue cues and bounded, depth-sorted continuous 3-D swimmers. */
public final class MontroseRescueView {
    private MontroseRescueView(){}
    public static void water(Game g,Painter p){
        if(MontroseRescue.cruisers(g)){
            if(g.rescue.rafted){Body a=g.cargo[0],b=g.cargo[1];for(int end=-1;end<=1;end+=2){float dy=end*8;
                SoftCue.line(p,a.x,a.y+dy,b.x,b.y+dy,1.5f,0xcdbda67b);}
                SoftCue.caption(p,"RELEASE RAFT LINE",(a.x+b.x)*.5f,(a.y+b.y)*.5f+32,8,VisualStyle.GOLD);
            }
            if(g.rescue.slipsRevealed())for(int i=0;i<2;i++){MooredBoats.Berth b=MontroseRescue.berth(i);Body boat=g.cargo[i];
                float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);int color=SoftCue.animated(g,boat.deliveryRecorded?0x9965d5b8:0xbbe8c567);
                for(int side=-1;side<=1;side+=2)for(int end=-1;end<=1;end+=2){float x=b.x+c*end*24-s*side*12,y=b.y+s*end*24+c*side*12;
                    SoftCue.line(p,x,y,x-c*end*10,y-s*end*10,1,color);SoftCue.line(p,x,y,x+s*side*6,y-c*side*6,1,color);}
                SoftCue.caption(p,MontroseRescue.slip(i),b.x,b.y+35,8,color);
            }
        }
        if(MontroseRescue.swimmers(g)){
            MontroseRescue r=g.rescue;
            for(int i=0;i<MontroseRescue.PEOPLE;i++){
                if(!r.found[i] || r.state[i]>=3)continue;
                SwimmerModel.water(g,p,i);
                float radius=r.state[i]==0?3.5f:4.5f;int color=r.flash[i]>0?0xb866d9b1:r.state[i]==0?0x447bc2c8:0x446bafad;
                SoftCue.ring(p,r.x[i],r.y[i],radius,1,color,.6f);
            }
            if(r.aiming>=0){int i=r.aiming;SoftCue.ring(p,r.x[i],r.y[i],11,1,0xbbdfc274,1.1f);}
        }
    }
    public static int[] ordered(Game g){
        if(!MontroseRescue.swimmers(g))return new int[0];MontroseRescue r=g.rescue;
        int[] ids=new int[MontroseRescue.PEOPLE+RescueBarge.RINGS];int count=0;
        for(int i=0;i<MontroseRescue.PEOPLE;i++)if(r.found[i] && r.state[i]!=4 && (r.state[i]!=6 || r.barge.timer[i]<RescueBarge.HURT+RescueBarge.VANISH))ids[count++]=i;
        for(int i=0;i<RescueBarge.RINGS;i++)if(r.barge.phase[i]>0)ids[count++]=MontroseRescue.PEOPLE+i;
        for(int i=1;i<count;i++)for(int j=i;j>0 && depth(g,ids[j])<depth(g,ids[j-1]);j--){int n=ids[j];ids[j]=ids[j-1];ids[j-1]=n;}
        return java.util.Arrays.copyOf(ids,count);
    }
    public static float depth(Game g,int i){MontroseRescue r=g.rescue;return g.layout.camera.screenY(i>=20?r.barge.x[i-20]:r.x[i],i>=20?r.barge.y[i-20]:r.y[i]);}
    public static void draw(Game g,Painter p,int i){MontroseRescue r=g.rescue;RiverCamera c=g.layout.camera;
        float wx=i>=20?r.barge.x[i-20]:r.x[i],wy=i>=20?r.barge.y[i-20]:r.y[i],x=c.screenX(wx,wy),y=c.screenY(wx,wy);
        if(x< -60 || x>g.layout.width+60 || y< -100 || y>g.layout.height+100)return;
        if(i>=20){r.barge.drawRing(g,p,i-20);return;}
        int t=r.barge.timer[i];float s=c.scale;
        if(r.state[i]==6){
            // Vanish: a white pop and a spreading ring where the swimmer was.
            float u=(t-RescueBarge.HURT)/(float)RescueBarge.VANISH,ring=(5+u*18)*s;
            p.material(x,y,ring*2,0,0,ring*2*RiverCamera.DEPTH,EffectMaterial.RING,.06f,1,SoftCue.alpha(0xffffffff,(1-u)*.8f));
            if(u<.3f)p.material(x,y-4*s,14*s,0,0,12*s,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xffffffff,1-u/.3f));
            return;
        }
        if(r.state[i]==5){
            // Impact: a hot flash and splash ring, then thrashing with a flicker that speeds up to the vanish.
            if(t<14){float f=1-t/14f;p.material(x,y-4*s,30*s,0,0,24*s,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xffff5a3c,f*.85f));
                float ring=(5+t*2.2f)*s;p.material(x,y,ring*2,0,0,ring*2*RiverCamera.DEPTH,EffectMaterial.RING,.07f,1,SoftCue.alpha(0xffeaf7ff,f));}
            int period=t<45?10:t<70?6:3;
            if(t>=30 && t%period<period/2)p=new OpacityPainter(p,.25f);
        }
        r.swimmerModel.draw(g,p,i);
    }
}
