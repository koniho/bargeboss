package com.bargeboss.game;

/** Layered, drifting surface facets and broken wave crests; no tiled water strips. */
public final class WaveView {
    private WaveView() {}
    private static float noise(int seed) {
        int hash=seed*0x45d9f3b;hash=(hash^(hash>>>16))*0x45d9f3b;hash^=hash>>>16;
        return (hash&65535)/65535f;
    }
    /** World-anchored detail moves with the river during camera follow and zoom. */
    public static void draw(Game g,Painter canvas) {
        canvas.rect(0,0,g.layout.width,g.layout.height,HarborAtmosphere.WATER_BASE);
        surface(g,canvas);
    }
    /** Wave detail without clearing a scene that has already drawn its land/water mask. */
    public static void surface(Game g,Painter canvas) {
        RiverCamera c=g.layout.camera;
        Painter p=new WaterEffectsPainter(g,new RiverPainter(canvas,c));
        float minX=Float.MAX_VALUE,minY=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
        for(int corner=0;corner<4;corner++) {
            float sx=corner%2*g.layout.width,sy=corner/2*g.layout.height;
            float x=c.worldX(sx,sy),y=c.worldY(sx,sy);
            minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
        }
        shorelines(g,p,minY,maxY);
        int tick=g.visualTicks;
        // Cell identities stay in world space. Slow oscillation gives currents
        // motion without a periodic tile jump when the camera crosses a cell.
        int firstX=(int)StrictMath.floor((minX-50)/38),lastColumn=(int)StrictMath.ceil((maxX+50)/38);
        int firstY=(int)StrictMath.floor((minY-50)/30),lastRow=(int)StrictMath.ceil((maxY+50)/30);
        for(int row=firstY;row<=lastRow;row++) for(int col=firstX;col<=lastColumn;col++) {
            int seed=col*1973+row*9277+19037;
            float phase=tick*(.016f+noise(seed+11)*.009f)+noise(seed)*6.283f;
            float life=.5f+.5f*(float)StrictMath.sin(phase),fade=life*life;
            float x=(col+.15f+noise(seed+2)*.7f)*38+(float)StrictMath.sin(tick*.008+seed)*4;
            float y=(row+.15f+noise(seed+5)*.7f)*30+(float)StrictMath.sin(tick*.006+seed*.7)*3;
            if(!WaterEffectsPainter.contains(g,x,y)) continue;
            float sx=c.screenX(x,y),sy=c.screenY(x,y);
            if(sx<-45*c.scale || sx>g.layout.width+45*c.scale || sy<-25*c.scale || sy>g.layout.height+25*c.scale) continue;
            float span=10+noise(seed+7)*23,rise=.8f+noise(seed+13)*2.3f;
            // Broad dark trough and a discontinuous highlight, each with a
            // slightly different phase. No evenly spaced full-width bands.
            float lastX=0,lastY=0;
            for(int part=0;part<=5;part++) {
                float t=part/5f,along=(t-.5f)*span;
                float bend=(float)StrictMath.sin(t*StrictMath.PI)*(rise+.3f*(float)StrictMath.sin(t*9+phase));
                float xx=x+(along-bend)*RiverCamera.Q,yy=y+(along+bend)*RiverCamera.Q;
                if(part>0) {
                    p.line(lastX+1,lastY-1,xx+1,yy-1,2.4f,0x16102028);
                    int alpha=(int)((12+fade*68)*(float)StrictMath.sin((t-.1f)*StrictMath.PI));
                    if(part!=2 || noise(seed+29)>.35f) p.line(lastX,lastY,xx,yy,.9f+fade*.5f,(alpha<<24)|0x83b3b4);
                }
                lastX=xx;lastY=yy;
            }
            if(life>.58f && noise(seed+31)>.35f) {
                float arm=2+noise(seed+37)*6;
                float xx=x+4,yy=y-4;
                p.line(xx-arm*RiverCamera.Q,yy-arm*RiverCamera.Q,xx+arm*RiverCamera.Q,yy+arm*RiverCamera.Q,
                        .8f,((int)((life-.58f)*175)<<24)|0xb3d5cb);
            }
        }
    }

    private static void shorelines(Game g,Painter p,float minY,float maxY) {
        if(g.tutorial!=null) {
            float[] bank=TrainingChannel.WATER_BOUNDARY;
            for(int i=0;i<bank.length;i+=2) {
                int next=(i+2)%bank.length;
                edge(g,p,bank[i],bank[i+1],bank[next],bank[next+1]);
            }
        } else {
            edge(g,p,Physics.LEFT,minY,Physics.LEFT,maxY);
            edge(g,p,Physics.RIGHT,minY,Physics.RIGHT,maxY);
            if(g.challengeId==2) for(float[] r:HarborGeometry.PIERS) {
                edge(g,p,r[0],r[1],r[2],r[1]);edge(g,p,r[2],r[1],r[2],r[3]);
                edge(g,p,r[2],r[3],r[0],r[3]);edge(g,p,r[0],r[3],r[0],r[1]);
            }
        }
    }
    /** Small bank laps brighten when an existing travelling wake reaches shore. */
    private static void edge(Game g,Painter p,float x,float y,float xx,float yy) {
        float dx=xx-x,dy=yy-y,length=Physics.length(dx,dy);if(length<1) return;
        dx/=length;dy/=length;float nx=-dy,ny=dx;
        if(!WaterEffectsPainter.contains(g,(x+xx)*.5f+nx*2,(y+yy)*.5f+ny*2)) {nx=-nx;ny=-ny;}
        for(float along=8;along<length-4;along+=24) {
            float wx=x+along*dx+nx*2.2f,wy=y+along*dy+ny*2.2f;
            if(!WaterEffectsPainter.contains(g,wx,wy) || !HarborAtmosphere.visible(g,wx,wy,20)) continue;
            float phase=g.visualTicks*.032f+wx*.09f+wy*.07f;
            float pulse=.5f+.5f*(float)StrictMath.sin(phase);
            float wake=g.water.energy(wx,wy,null),inset=1.4f+pulse*.8f;
            int alpha=(int)((13+Math.min(1,wake)*54)*pulse);
            p.line(wx-dx*5+nx*inset,wy-dy*5+ny*inset,wx+dx*(4+pulse*4)+nx*inset,
                    wy+dy*(4+pulse*4)+ny*inset,.8f+Math.min(1,wake)*.8f,(alpha<<24)|0x91cdc3);
        }
    }

    public static void draw(Painter p,int tick,float left,float top,float width,float height,float scale,float origin) {
        p.rect(left,top,width,height,HarborAtmosphere.WATER_BASE);
        if(height<=0 || width<=0) return;
        // Broad low-contrast facets give the surface depth without covering vessels in fog.
        float broad=160*scale;
        int broadColumns=(int)(width/broad)+3,broadRows=(int)(height/(broad*.55f))+3;
        for(int row=-1;row<broadRows;row++) for(int col=-1;col<broadColumns;col++) {
            int seed=col*917+row*353+9281;
            float x=left+(col+noise(seed))*broad+(origin-tick*.09f*scale)%broad;
            float y=top+(row+noise(seed+17))*broad*.55f;
            float sway=(float)StrictMath.sin(tick*.009+seed)*7*scale;
            float[] facet={x-broad*.6f,y+sway,x-broad*.15f,y-broad*.16f+sway,
                    x+broad*.65f,y-broad*.08f+sway,x+broad*.45f,y+broad*.15f+sway,x-broad*.3f,y+broad*.19f+sway};
            for(int i=1;i<facet.length;i+=2) facet[i]=Physics.clamp(facet[i],top,top+height);
            p.poly(facet,(noise(seed+3)>.5f?0x0810202b:0x05598086));
        }
        float cellX=58*scale,cellY=27*scale;
        int columns=(int)(width/cellX)+3,rows=(int)(height/cellY)+3;
        float drift=(origin-tick*.17f*scale)%cellX;
        for(int row=-1;row<rows;row++) for(int col=-1;col<columns;col++) {
            int seed=col*1973+row*9277+19037;
            float phase=tick*(.024f+noise(seed+11)*.014f)+noise(seed)*6.283f;
            float life=.5f+.5f*(float)StrictMath.sin(phase);
            float x=left+(col+noise(seed+2)*.95f)*cellX+drift;
            float y=top+(row+noise(seed+5)*.95f)*cellY+(float)StrictMath.sin(phase*.7f)*2*scale;
            float span=(10+noise(seed+7)*32)*scale;
            float rise=(1.2f+noise(seed+13)*2.7f)*scale;
            if(y<top+rise+3*scale || y>top+height-4*scale) continue;
            float slope=(noise(seed+19)-.5f)*.12f;
            float lastX=x-span*.5f,lastY=y-span*.5f*slope;
            for(int part=1;part<=5;part++) {
                float t=part/5f,xx=x+(t-.5f)*span;
                float yy=y-(float)StrictMath.sin(t*StrictMath.PI)*(rise+.45f*scale*(float)StrictMath.sin(t*7+seed))+(t-.5f)*span*slope;
                float edge=(float)StrictMath.sin((t-.1f)*StrictMath.PI);
                int alpha=(int)((13+life*51)*edge);
                p.line(lastX,lastY+2*scale,xx,yy+2*scale,2.0f*scale,0x2010202c);
                if(part!=2 || noise(seed+29)>.33f)
                    p.line(lastX,lastY,xx,yy,(.65f+life*.5f)*scale,(alpha<<24)|0x7faaa9);
                lastX=xx;lastY=yy;
            }
            if(life>.55f && noise(seed+31)>.48f) {
                float glint=span*(.12f+noise(seed+37)*.17f),gy=y+5*scale;
                int alpha=(int)((life-.55f)*120);
                p.poly(new float[]{x-glint,gy,x+glint*.4f,gy-.6f*scale,
                        x+glint,gy+.2f*scale,x-glint*.2f,gy+1.1f*scale},(alpha<<24)|0xa6c4bb);
            }
            // Occasional short silver flecks prevent every crest becoming a complete crescent.
            if(life>.75f && noise(seed+23)>.62f) p.line(x-span*.13f,y-rise-.3f*scale,
                    x+span*.05f,y-rise-.1f*scale,.75f*scale,0x426d999d);
        }
    }
}
