package com.bargeboss.game;

/** Shared tow target, sagging line, tension highlights and release recoil. */
public final class TowView {
    private TowView() {}
    public static void draw(Game g,Painter p) {
        Body held=g.heldCargo();
        if(g.canTow(held))for(int i=-1;i<g.cargo.length;i++) {
            Body target=i<0?g.tug:g.cargo[i];if(!g.canTowTo(held,target))continue;
            float pulse=2*(float)StrictMath.sin(g.visualTicks*.12);
            boolean aimed=g.towAiming && g.towAimBody==target;
            ControlView.ring(g,p,target.x,target.y,target==g.tug?39+pulse:45+pulse,1,aimed?ControlView.GREEN:ControlView.BLUE,2);
            if(aimed)SoftCue.caption(p,"RELEASE TO LINK",target.x+40,target.y,9,VisualStyle.CREAM);
        }
        for(int slot=0;slot<Towline.CAPACITY;slot++) {
        Towline line=g.tow.link(slot);if(line==null)continue;
        if(line.cargo!=null) {
            float x=line.cargoX(),y=line.cargoY(),xx=line.tugX(g.tug),yy=line.tugY(g.tug);
            int color=RopeMaterial.COLOR;
            if(line.catchTicks>0) {
                float t=1-line.catchTicks/24f;
                int glow=((int)(220*(1-t))<<24)|0x92ecc2;
                ControlView.ring(g,p,x,y,8+30*t,1,glow,3);
                ControlView.ring(g,p,xx,yy,8+30*t,1,glow,3);
            }
            if((g.tow.count()==1 || held==line.cargo) && (g.tutorial==null || g.tutorial.lesson!=Tutorial.RELEASE)) SoftCue.caption(p,"TAP TO LET GO",line.cargo.x-47,line.cargo.y,8,ControlView.BLUE);
            if(line.leader==g.tug)SoftCue.caption(p,line.tension>12?"PULLING":"LINE SLACK",(x+xx)/2+15,(y+yy)/2,8,color);
        }
        }
    }
    /** Small 3-D spans join the same ground-depth order as the hulls they attach to. */
    public static final class Segments {
        private final Game game;
        public final int count;
        final float[] x,y,height,strand;
        final int[] order,points,colors;
        final boolean[] mooring;
        int flightTip=-1;
        public Segments(Game g) {
            game=g;int used=0,capacity=Towline.CAPACITY+g.cargo.length+1+(g.montroseStage()?4:0);
            x=new float[capacity*25+33];y=new float[x.length];height=new float[x.length];strand=new float[x.length];
            order=new int[capacity*24+32];points=new int[order.length];colors=new int[order.length];mooring=new boolean[order.length];
            for(int link=0;link<Towline.CAPACITY;link++) {
                Towline line=g.tow.link(link);if(line==null)continue;
                float sx,sy,ex,ey,slack,h0,h1;int color;
                if(line.cargo!=null) {
                    sx=RopeAttachment.x(line.cargo,line.cargoEnd);sy=RopeAttachment.y(line.cargo,line.cargoEnd);ex=RopeAttachment.x(line.leader,line.leaderEnd);ey=RopeAttachment.y(line.leader,line.leaderEnd);
                    slack=Physics.clamp((line.length-Physics.length(ex-sx,ey-sy))*.25f,0,12);
                    h0=RopeAttachment.height(line.cargo)+WaterSurface.lift(g,line.cargo);h1=RopeAttachment.height(line.leader)+WaterSurface.lift(g,line.leader);
                    color=RopeMaterial.COLOR;
                } else if(line.releaseTicks>0) {
                    float t=line.releaseTicks/18f;sx=line.releaseToX;sy=line.releaseToY;
                    ex=sx+(line.releaseX-sx)*t;ey=sy+(line.releaseY-sy)*t;
                    slack=8*t;h0=line.releaseToHeight;h1=h0+(line.releaseHeight-h0)*t;color=((int)(230*t)<<24)|(RopeMaterial.COLOR&0xffffff);
                } else continue;
                int base=link*25;
                for(int i=0;i<=24;i++) {
                    float t=i/24f;x[base+i]=sx+(ex-sx)*t;y[base+i]=sy+(ey-sy)*t;
                    height[base+i]=Math.max(.5f,h0+(h1-h0)*t-4*t*(1-t)*slack);
                    if(i>0)strand[base+i]=strand[base+i-1]+Physics.length(x[base+i]-x[base+i-1],y[base+i]-y[base+i-1])/5f;
                }
                for(int i=0;i<24;i++){points[used]=base+i;colors[used]=color;used++;}
            }
            for(int slot=0;slot<=g.cargo.length;slot++) {
                Body b=slot==g.cargo.length?g.tug:g.cargo[slot];if(b==null || !b.active || !b.lineAttached || b.yacht)continue;
                float tx=g.bollardX(b),ty=g.bollardY(b),rise=g.ballHull(b)?PracticeMooring.TOP:g.tutorial!=null?GroundArt.LAND_RISE:0;
                float[] fit=new float[2];RopeAttachment.mooring(b,tx,ty,fit);
                // Bollard coordinates include raised projection; recover ground for depth sorting.
                float ex=tx-RiverCamera.riseX(rise),ey=ty-RiverCamera.riseY(rise);
                float h0=RopeAttachment.height(b)+WaterSurface.lift(g,b),progress=Math.min(1,b.dockTime/1.2f);
                float slack=b==g.tug?g.slipMooring.slack(g):b.lineSnapTicks>0?0:Math.min(9,23*(1-progress*progress*progress));
                int base=(Towline.CAPACITY+slot)*25;
                for(int i=0;i<=24;i++) {
                    float t=i/24f;x[base+i]=fit[0]+(ex-fit[0])*t;y[base+i]=fit[1]+(ey-fit[1])*t;
                    height[base+i]=Math.max(.5f,h0+(rise-h0)*t-4*t*(1-t)*slack);
                    if(!WaterEffectsPainter.contains(g,x[base+i],y[base+i]))height[base+i]=Math.max(height[base+i],rise+.5f);
                    if(i>0)strand[base+i]=strand[base+i-1]+Physics.length(x[base+i]-x[base+i-1],y[base+i]-y[base+i-1])/5f;
                }
                for(int i=0;i<24;i++){points[used]=base+i;colors[used]=RopeMaterial.COLOR;mooring[used]=true;used++;}
            }
            if(g.ropeBody!=null && g.ropeBody.active && (g.ropePhase==1 || g.ropePhase==2)) {
                int base=capacity*25;float[] point=new float[3];
                for(int i=0;i<=32;i++) {
                    RopeFlight.sample(g,i/32f,point);x[base+i]=point[0];y[base+i]=point[1];height[base+i]=point[2];
                    if(i>0)strand[base+i]=strand[base+i-1]+Physics.length(x[base+i]-x[base+i-1],y[base+i]-y[base+i-1])/5f;
                }
                for(int i=0;i<32;i++){points[used]=base+i;colors[used]=RopeMaterial.COLOR;mooring[used]=!g.ropeToTug;used++;}
                flightTip=base+31;
            }
            if(g.montroseStage()){
                int crewBase=(Towline.CAPACITY+g.cargo.length+1)*25;
                used=g.haulout.lineCrew.append(g,this,used,crewBase);
                used=g.mission.lineCrew.append(g,this,used,crewBase+50);
            }
            count=used;
            for(int i=0;i<count;i++) {
                int j=i-1;while(j>=0 && groundDepth(order[j])>groundDepth(i)){order[j+1]=order[j];j--;}
                order[j+1]=i;
            }
        }
        private float groundDepth(int span){int i=points[span];return (y[i]+y[i+1]-x[i]-x[i+1])*.5f;}
        public float depth(RiverCamera c,int slot){int i=points[order[slot]];return c.screenY((x[i]+x[i+1])*.5f,(y[i]+y[i+1])*.5f);}
        public void draw(Painter p,int slot) {
            int i=points[order[slot]],color=colors[order[slot]];boolean river=p instanceof RiverPainter;
            float sx=x[i]+LassoPath.liftX(river,height[i]),sy=y[i]+LassoPath.liftY(river,height[i]);
            float ex=x[i+1]+LassoPath.liftX(river,height[i+1]),ey=y[i+1]+LassoPath.liftY(river,height[i+1]);
            Painter canvas=river?((RiverPainter)p).canvas():p;
            float[] clip=river && !(mooring[order[slot]] && !WaterEffectsPainter.contains(game,(x[i]+x[i+1])*.5f,(y[i]+y[i+1])*.5f))?ShoreOcclusion.region(game,((RiverPainter)p).camera,
                    Math.min(x[i],x[i+1])-36,Math.min(y[i],y[i+1])-36,Math.max(x[i],x[i+1])+36,Math.max(y[i],y[i+1])+36,depth(((RiverPainter)p).camera,slot)):ShoreOcclusion.EMPTY;
            if(clip.length>0)canvas.pushClipOutside(clip);
            try {
            // Four bounded sight tests resolve crossings through cabin/cargo, beyond center-depth sorting.
            int visibleStart=-1;
            for(int part=0;part<=4;part++) {
                float t=(part+.5f)*.25f;
                boolean visible=part<4 && (!river || !RopeOcclusion.hidden(game,x[i]+(x[i+1]-x[i])*t,y[i]+(y[i+1]-y[i])*t,height[i]+(height[i+1]-height[i])*t));
                if(visible && visibleStart<0)visibleStart=part;
                if(!visible && visibleStart>=0) {
                    float a=visibleStart*.25f,b=part*.25f;
                    p.ropeSegment(sx+(ex-sx)*a,sy+(ey-sy)*a,sx+(ex-sx)*b,sy+(ey-sy)*b,2.8f,
                            strand[i]+(strand[i+1]-strand[i])*a,strand[i]+(strand[i+1]-strand[i])*b,color);
                    visibleStart=-1;
                }
            }
            if(i==flightTip) {
                float t=Physics.clamp(game.ropeTicks/(float)(game.ropePhase==2?Game.ROPE_RETURN_TICKS:Game.ROPE_FLIGHT_TICKS),0,1),eased=1-(1-t)*(1-t);
                // The loop flares open as the whip snaps, spinning fast, then settles as it lands.
                float open=game.ropePhase==2?1-eased:Physics.clamp(RopeFlight.tip(t),0,1.15f);
                float radius=7+15*open+5*RopeFlight.crack(game);
                MooringView.lasso(p,ex,ey,radius,radius*.66f,game.ropeTicks*.55f,color);
                float crack=RopeFlight.crack(game);
                if(crack>0){
                    // Whip crack at the tip: a white flash with short radial cracks.
                    Painter screen=river?((RiverPainter)p).canvas():p;float s=river?((RiverPainter)p).camera.scale:1;
                    float tx=river?((RiverPainter)p).camera.screenX(ex,ey):ex,ty=river?((RiverPainter)p).camera.screenY(ex,ey):ey;
                    screen.material(tx,ty,46*s*crack,0,0,38*s*crack,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xffffffff,.85f*crack));
                    for(int k=0;k<6;k++){float a=k*1.0472f+.4f,r0=6*s,r1=(6+14*crack)*s;float ca=(float)StrictMath.cos(a),sa=(float)StrictMath.sin(a)*.7f;
                        screen.line(tx+ca*r0,ty+sa*r0,tx+ca*r1,ty+sa*r1,1.6f*s,SoftCue.alpha(0xfffff6d8,crack));}
                }
            }
            } finally {if(clip.length>0)canvas.popClip();}
        }
    }
}
