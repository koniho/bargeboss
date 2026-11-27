package com.bargeboss.game;

/** Draws the tag circuit in the training yard's industrial style: shared water texture and waves,
 * raised concrete islands and banks with steel retaining walls on the faces the camera sees, sliding
 * lock gates, depth-sorted boats, lift bridges whose decks rise over passing hulls, and the IT marker. */
final class TagArenaView {
    private TagArenaView(){}
    static final float RISE=14,BRIDGE_LOW=12,BRIDGE_HIGH=62;
    static final int TOP=0xff63655e,SEAM=0xff52544e,WALL=0xff434d53,WALL_DARK=0xff333b40,EDGE=0xffa39d88,BOLLARD=0xff2a2f33,GATE=0xff6b4a2c,STEEL=0xff59636a,DECK=0xff9c8c6c;
    /** Land outside the loop, as big rectangles (north, south, west, east). */
    private static final float[][] FRAME={{-3000,-3000,TagArena.W+3000,0},{-3000,TagArena.H,TagArena.W+3000,TagArena.H+3000},{-3000,0,0,TagArena.H},{TagArena.W,0,TagArena.W+3000,TagArena.H}};

    static void draw(Game g,Painter canvas){
        RiverCamera c=g.layout.camera;TagArena a=g.arena;
        canvas.rect(0,0,g.layout.width,g.layout.height,g.lighting().water);
        if((g.renderExclusions&RenderExclusions.AMBIENT_WATER)==0){WaterTexture.draw(g,canvas,g.lighting().water);WaveView.surface(g,canvas);}
        g.water.draw(g,new RiverPainter(canvas,c));
        float[] view=SurveyGround.visible(g);
        boolean islands=a.course instanceof ArenaCourse.Islands;
        if(islands)IslandArt.draw(g,canvas,c,(ArenaCourse.Islands)a.course,view);
        else{
            // Land: outer banks, then the islands; lock chamber gate leaves.
            for(float[] r:FRAME)block(canvas,c,r,false,view);
            for(float[] r:TagArena.ISLANDS)block(canvas,c,r,true,view);
            for(int k=TagArena.LOCK_W;k<=TagArena.LOCK_E;k++)gate(canvas,c,k,a.open[k]);
        }
        corral(g,canvas,c,a);powerUps(g,canvas,c,a);
        WeatherView.scenery(g,canvas);
        // Boats, nearest last.
        Body[] order=new Body[a.npcs.length+1];int n=0;order[n++]=g.tug;for(Body b:a.npcs)if(b.active)order[n++]=b;
        for(int i=1;i<n;i++){Body b=order[i];int j=i-1;while(j>=0 && c.screenY(order[j].x,order[j].y)>c.screenY(b.x,b.y)){order[j+1]=order[j];j--;}order[j+1]=b;}
        RiverPainter river=new RiverPainter(canvas,c);
        float sceneryDepth=Float.NEGATIVE_INFINITY;
        for(int i=0;i<n;i++){float depth=c.screenY(order[i].x,order[i].y);if(islands)IslandArt.props(g,canvas,sceneryDepth,depth);sceneryDepth=depth;if(order[i]==g.tug)VesselView.draw(g,river,g.tug,true);else VesselView.draw(g,river,order[i],false);}
        if(islands)IslandArt.props(g,canvas,sceneryDepth,Float.POSITIVE_INFINITY);
        // Lasso flight and tow lines.
        TowView.Segments rope=new TowView.Segments(g);for(int i=0;i<rope.count;i++)rope.draw(river,i);
        // Lift bridges over the boats.
        for(int k=0;k<a.course.gates();k++)if(a.course.bridge[k]){if(islands)a.footbridges[k].draw(g,canvas,a.open[k]);else bridge(canvas,c,a.course,k,a.open[k],false);}
        markers(g,canvas,c,a);
    }
    private static float sx(RiverCamera c,float x,float y){return c.screenX(x,y);}
    private static float sy(RiverCamera c,float x,float y,float z){return c.screenY(x,y)-z*c.scale;}
    /** A raised concrete block: top face, plus the steel wall on each face that looks toward the camera
     * (outward normal +y or -x). Island walls face the water on all sides; frame blocks only where water is. */
    private static void block(Painter p,RiverCamera c,float[] r,boolean island,float[] view){
        float l=r[0],t=r[1],rr=r[2],b=r[3];
        if(rr<view[0]-40 || l>view[2]+40 || b<view[1]-40 || t>view[3]+40)return;
        // South face (normal +y) and west face (normal -x) are the visible walls in this projection.
        p.convexQuad(sx(c,l,b),sy(c,l,b,0),sx(c,rr,b),sy(c,rr,b,0),sx(c,rr,b),sy(c,rr,b,RISE),sx(c,l,b),sy(c,l,b,RISE),WALL);
        p.convexQuad(sx(c,l,t),sy(c,l,t,0),sx(c,l,b),sy(c,l,b,0),sx(c,l,b),sy(c,l,b,RISE),sx(c,l,t),sy(c,l,t,RISE),WALL_DARK);
        p.convexQuad(sx(c,l,t),sy(c,l,t,RISE),sx(c,rr,t),sy(c,rr,t,RISE),sx(c,rr,b),sy(c,rr,b,RISE),sx(c,l,b),sy(c,l,b,RISE),TOP);
        // Expansion seams on the slab, within view.
        float x0=Math.max(l,view[0]-64),x1=Math.min(rr,view[2]+64),y0=Math.max(t,view[1]-64),y1=Math.min(b,view[3]+64);
        for(float x=(float)StrictMath.ceil(x0/96)*96;x<x1;x+=96)p.line(sx(c,x,y0),sy(c,x,y0,RISE),sx(c,x,y1),sy(c,x,y1,RISE),.8f*c.scale,SEAM);
        for(float y=(float)StrictMath.ceil(y0/96)*96;y<y1;y+=96)p.line(sx(c,x0,y),sy(c,x0,y,RISE),sx(c,x1,y),sy(c,x1,y,RISE),.8f*c.scale,SEAM);
        // Bollards along the water-facing copings.
        if(island){for(float x=l+60;x<rr;x+=120){if(x<view[0] || x>view[2])continue;bollard(p,c,x,t+6);bollard(p,c,x,b-6);}}
        // Coping: a pale edge along the water-facing rims.
        p.line(sx(c,l,b),sy(c,l,b,RISE),sx(c,rr,b),sy(c,rr,b,RISE),1.6f*c.scale,EDGE);
        p.line(sx(c,l,t),sy(c,l,t,RISE),sx(c,l,b),sy(c,l,b,RISE),1.6f*c.scale,EDGE);
        if(island){p.line(sx(c,l,t),sy(c,l,t,RISE),sx(c,rr,t),sy(c,rr,t,RISE),1.6f*c.scale,EDGE);p.line(sx(c,rr,t),sy(c,rr,t,RISE),sx(c,rr,b),sy(c,rr,b,RISE),1.6f*c.scale,EDGE);}
    }
    private static void bollard(Painter p,RiverCamera c,float x,float y){
        float px=sx(c,x,y),py=sy(c,x,y,RISE);p.circle(px,py-2*c.scale,2.4f*c.scale,BOLLARD);p.circle(px,py-3*c.scale,1.6f*c.scale,0xff4a5257);
    }
    /** Sliding lock gate: two leaves retract into the chamber walls as it opens. */
    private static void gate(Painter p,RiverCamera c,int k,float open){
        float x=TagArena.GATE_X[k],y0=TagArena.GATE_Y0[k],y1=TagArena.GATE_Y1[k],mid=(y0+y1)*.5f,reach=(mid-y0)*(1-open);
        for(int side=0;side<2;side++){
            float a=side==0?y0:y1,e=side==0?y0+reach:y1-reach;if(Math.abs(e-a)<1)continue;
            p.convexQuad(sx(c,x-4,a),sy(c,x-4,a,0),sx(c,x-4,e),sy(c,x-4,e,0),sx(c,x-4,e),sy(c,x-4,e,RISE+2),sx(c,x-4,a),sy(c,x-4,a,RISE+2),GATE);
            p.convexQuad(sx(c,x-4,a),sy(c,x-4,a,RISE+2),sx(c,x-4,e),sy(c,x-4,e,RISE+2),sx(c,x+4,e),sy(c,x+4,e,RISE+2),sx(c,x+4,a),sy(c,x+4,a,RISE+2),0xff8a6440);
        }
        // Signal light on the chamber wall: red closed, amber moving, green open.
        int light=open>=.8f?0xff4cff7a:open>0?0xffffc52f:0xffff4040;
        p.material(sx(c,x,y0-8),sy(c,x,y0-8,RISE+10),10*c.scale,0,0,10*c.scale,EffectMaterial.GLOW,0,1,light);
    }
    /** Vertical-lift bridge along a gate segment: towers at both ends and a deck that rises as it opens.
     * Footbridges (islands) are narrower and lower, in timber. */
    private static void bridge(Painter p,RiverCamera c,ArenaCourse course,int k,float open,boolean foot){
        float ax=course.ax[k],ay=course.ay[k],bx=course.bx[k],by=course.by[k],len=Physics.length(bx-ax,by-ay);
        float ux=(bx-ax)/len,uy=(by-ay)/len,nx=-uy,ny=ux,half=foot?9:16,low=foot?8:BRIDGE_LOW,high=foot?46:BRIDGE_HIGH;
        float e=open*open*(3-2*open),h=low+(high-low)*e;
        // Extend past the channel onto the banks.
        ax-=ux*10;ay-=uy*10;bx+=ux*10;by+=uy*10;
        float off=h*.5f;
        p.convexQuad(sx(c,ax+nx*half+off,ay+ny*half),sy(c,ax+nx*half+off,ay+ny*half,0),sx(c,bx+nx*half+off,by+ny*half),sy(c,bx+nx*half+off,by+ny*half,0),
                sx(c,bx-nx*half+off,by-ny*half),sy(c,bx-nx*half+off,by-ny*half,0),sx(c,ax-nx*half+off,ay-ny*half),sy(c,ax-nx*half+off,ay-ny*half,0),0x40000000);
        int post=foot?0xff6a4e33:STEEL;float top=high+(foot?8:14);
        for(int end=0;end<2;end++){
            float ex=end==0?ax:bx,ey=end==0?ay:by;
            for(int side=-1;side<=1;side+=2)p.line(sx(c,ex+nx*half*side,ey+ny*half*side),sy(c,ex+nx*half*side,ey+ny*half*side,0),sx(c,ex+nx*half*side,ey+ny*half*side),sy(c,ex+nx*half*side,ey+ny*half*side,top),(foot?2.2f:3)*c.scale,post);
            p.line(sx(c,ex+nx*half,ey+ny*half),sy(c,ex+nx*half,ey+ny*half,top),sx(c,ex-nx*half,ey-ny*half),sy(c,ex-nx*half,ey-ny*half,top),(foot?2.2f:3)*c.scale,post);
        }
        int deck=foot?0xffa07850:DECK,lip=foot?0xff7a5a3a:0xff6a5d46;
        p.convexQuad(sx(c,ax+nx*half,ay+ny*half),sy(c,ax+nx*half,ay+ny*half,h),sx(c,bx+nx*half,by+ny*half),sy(c,bx+nx*half,by+ny*half,h),
                sx(c,bx-nx*half,by-ny*half),sy(c,bx-nx*half,by-ny*half,h),sx(c,ax-nx*half,ay-ny*half),sy(c,ax-nx*half,ay-ny*half,h),deck);
        // Front lip on whichever long edge faces the camera.
        float lx=ny>0?nx:-nx,ly=ny>0?ny:-ny;
        p.convexQuad(sx(c,ax+lx*half,ay+ly*half),sy(c,ax+lx*half,ay+ly*half,h),sx(c,bx+lx*half,by+ly*half),sy(c,bx+lx*half,by+ly*half,h),
                sx(c,bx+lx*half,by+ly*half),sy(c,bx+lx*half,by+ly*half,h-3),sx(c,ax+lx*half,ay+ly*half),sy(c,ax+lx*half,ay+ly*half,h-3),lip);
        int light=open>=.8f?0xff4cff7a:open>0?0xffffc52f:0xffff4040;
        for(int end=0;end<2;end++){float ex=end==0?ax:bx,ey=end==0?ay:by;p.material(sx(c,ex,ey),sy(c,ex,ey,top+4),10*c.scale,0,0,10*c.scale,EffectMaterial.GLOW,0,1,light);}
    }
    private static void markers(Game g,Painter p,RiverCamera c,TagArena a){
        for(int i=0;i<a.npcs.length;i++){
            Body b=a.npcs[i];if(!b.active)continue;
            if(a.mode==TagArena.TAG && a.it==i)itMarker(g,p,c,b);
            if(a.stunned[i]>0)dizzy(g,p,c,b,a.stunned[i]);
            if(a.mode==TagArena.TAG && a.towed(g,i)){float x=sx(c,b.x,b.y),y=sy(c,b.x,b.y,40);float f=1-a.holdTicks/(float)TagArena.HOLD_SNAP;
                p.rect(x-22,y-4,44,6,0xe6050a10);p.rect(x-21,y-3,42*f,4,f<.3f?0xffff5a4a:0xffffd166);}
            if(a.shoutTicks[i]>0)bubble(g,p,c,b,a.shout[i],110-a.shoutTicks[i],a.shoutTicks[i]);
        }
        if(a.mode==TagArena.TAG && a.it<0)itMarker(g,p,c,g.tug);
    }
    /** Corral: a ring of orange floats with a label (chase only). */
    private static void corral(Game g,Painter p,RiverCamera c,TagArena a){
        if(a.mode!=TagArena.CHASE)return;
        Body towed=null;for(int i=0;i<a.npcs.length;i++)if(a.towed(g,i))towed=a.npcs[i];
        int target=towed==null?-1:a.nearestPen(towed.x,towed.y);
        for(int pen=0;pen<a.penX.length;pen++){
            float px=a.penX[pen],py=a.penY[pen];
            for(int k=0;k<16;k++){double t=k*Math.PI/8;float x=px+(float)Math.cos(t)*TagArena.CORRAL_REACH*.8f,y=py+(float)Math.sin(t)*TagArena.CORRAL_REACH*.8f;
                float bob=(float)StrictMath.sin(g.visualTicks*.08f+k)*1.2f;p.circle(sx(c,x,y),sy(c,x,y,2+bob),3.2f*c.scale,0xffff8a2b);p.circle(sx(c,x,y),sy(c,x,y,3+bob),1.6f*c.scale,0xffffd8a8);}
            if(pen==target)MontroseJob.ring(g,p,px,py,TagArena.CORRAL_REACH*.6f,0xff6dffb0,1);
            Lettering.center(p,"CORRAL",sx(c,px,py),sy(c,px,py,40),Physics.clamp(11*c.scale,11,16),pen==target?0xff6dffb0:0xffffb070);
        }
    }
    /** Power-up buoys: bobbing glowing orbs with their letter. */
    private static void powerUps(Game g,Painter p,RiverCamera c,TagArena a){
        for(int k=0;k<a.powerLive.length;k++){
            if(!a.powerLive[k])continue;
            int col=0xffff6a00;String label="N";
            float bob=4+3*(float)StrictMath.sin(g.visualTicks*.12f+k*2),x=sx(c,a.powerX[k],a.powerY[k]),y=sy(c,a.powerX[k],a.powerY[k],bob+6),r=9*c.scale;
            p.material(x,sy(c,a.powerX[k],a.powerY[k],0),r*4,0,0,r*2,EffectMaterial.GLOW,0,1,SoftCue.alpha(col,.45f));
            p.circle(x,y,r,col);p.circle(x,y,r*.7f,0xff101418);
            Lettering.center(p,label,x,y-r*.55f,Math.max(10,r*1.1f),col);
        }
    }
    /** Stunned boat: stars circling over the hull. */
    private static void dizzy(Game g,Painter p,RiverCamera c,Body b,int left){
        float x=sx(c,b.x,b.y),y=sy(c,b.x,b.y,32),r=10*c.scale;
        for(int k=0;k<3;k++){double t=g.visualTicks*.18+k*2.094;ScreenView.star(p,x+(float)Math.cos(t)*r,y+(float)Math.sin(t)*r*.45f,3.5f*c.scale,SoftCue.alpha(0xffffe27a,Math.min(1,left/20f)));}
    }
    private static void itMarker(Game g,Painter p,RiverCamera c,Body b){
        float x=sx(c,b.x,b.y),y=sy(c,b.x,b.y,0),pulse=.6f+.4f*(float)StrictMath.sin(g.visualTicks*.3f),r=(b.half+b.radius)*1.8f*c.scale;
        p.material(x,y,r*2,0,0,r*2*RiverCamera.DEPTH,EffectMaterial.RING,.06f,1,SoftCue.alpha(0xffff3a3a,pulse));
        float ly=sy(c,b.x,b.y,44)-6;float size=Physics.clamp(18*c.scale,16,30);
        Lettering.center(p,"IT",x+1.5f,ly+1.5f,size,0xcc000000);Lettering.center(p,"IT",x,ly,size,0xffff4a3a);
    }
    private static void bubble(Game g,Painter p,RiverCamera c,Body b,String text,int age,int left){
        float bx=sx(c,b.x,b.y),by=sy(c,b.x,b.y,26)-22;
        if(bx<-150 || bx>g.layout.width+150 || by<-40 || by>g.layout.height+40)return;
        float pop=age<5?.6f+age*.08f:1,size=12*pop,w=Lettering.width(text,size)+16,h=size+12,x=bx-w*.5f,y=by-h,al=Physics.clamp(left/12f,0,1);
        int ink=SoftCue.alpha(0xff101014,al),paper=SoftCue.alpha(0xfffffbf0,al);
        p.poly(new float[]{bx-7,y+h-1,bx+7,y+h-1,bx+2,y+h+10},ink);p.rect(x-2,y-2,w+4,h+4,ink);p.rect(x,y,w,h,paper);
        p.poly(new float[]{bx-5,y+h-1,bx+5,y+h-1,bx+2,y+h+7},paper);
        int col=b.neon;int r=((col>>16)&255)*55/100,gg=((col>>8)&255)*55/100,bb=(col&255)*55/100;
        Lettering.center(p,text,bx,y+5,size,SoftCue.alpha(0xff000000|(r<<16)|(gg<<8)|bb,al));
    }
    /** Clock and, in tag, who is IT. */
    static void hud(Game g,Painter p){
        TagArena a=g.arena;if(a==null || g.mode!=Game.PLAY)return;
        int left=a.secondsLeft();String text=left/60+":"+(left%60<10?"0":"")+left%60;boolean hurry=left<=10;
        float x=g.layout.width-84,y=66,size=hurry?26+4*(.5f+.5f*(float)StrictMath.sin(g.visualTicks*.3f)):24;
        Lettering.center(p,text,x+1.5f,y+1.5f,size,0xcc0a1016);Lettering.center(p,text,x,y,size,hurry?0xffff5a4a:0xffffe27a);
        // Active power-ups.
        float py=104;
        if(a.nitro>0){Lettering.center(p,"NITRO "+((a.nitro+59)/60),x,py,14,0xffff6a00);py+=18;}
        if(a.mode==TagArena.CHASE && a.combo>1 && a.comboTicks>0)Lettering.center(p,"COMBO x"+a.combo,x,py,16,0xffff6fd8);
        if(a.mode==TagArena.TAG && a.it<0){float pulse=.5f+.5f*(float)StrictMath.sin(g.visualTicks*.25f);Lettering.center(p,"YOU'RE IT!",g.layout.width*.5f,90,22,SoftCue.alpha(0xffff4a3a,.6f+.4f*pulse));}
    }
}
