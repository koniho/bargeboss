package com.bargeboss.game;

/** Isometric industrial practice yard, registered to the exact right-angle canal banks. */
public final class TrainingChannelView {
    private TrainingChannelView() {}
    public static void draw(Game g,Painter canvas) {
        RiverCamera c=g.layout.camera;Painter p=new RiverPainter(canvas,c);
        WaveView.draw(g,canvas);
        g.water.draw(g,p);
        float low=Float.MAX_VALUE,high=-Float.MAX_VALUE,minY=Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
        for(int corner=0;corner<4;corner++) {
            float sx=(corner%2)*g.layout.width,sy=(corner/2)*g.layout.height;
            low=Math.min(low,c.worldX(sx,sy)-35);high=Math.max(high,c.worldX(sx,sy)+35);
            minY=Math.min(minY,c.worldY(sx,sy)-35);maxY=Math.max(maxY,c.worldY(sx,sy)+35);
        }
        float outerLow=Math.min(low,TrainingChannel.MIN)-80,outerHigh=Math.max(high,TrainingChannel.MAX)+80;
        minY=Math.min(minY,200);maxY=Math.max(maxY,800);
        for(int side=-1;side<=1;side+=2) {
            float[] bank=TrainingChannel.bank(side);float outer=side<0?minY:maxY;
            float[] land=new float[bank.length+8];land[0]=outerLow;land[1]=outer;
            land[2]=outerLow;land[3]=bank[1];
            System.arraycopy(bank,0,land,4,bank.length);
            land[bank.length+4]=outerHigh;land[bank.length+5]=bank[bank.length-1];
            land[bank.length+6]=outerHigh;land[bank.length+7]=outer;
            p.poly(land,side<0?HarborAtmosphere.SHORE_LIT:HarborAtmosphere.SHORE_DARK);
            for(int i=0;i<bank.length-2;i+=2) {
                float x=bank[i],y=bank[i+1],xx=bank[i+2],yy=bank[i+3];
                p.line(x,y,xx,yy,6,VisualStyle.OUTLINE);
                p.line(x,y,xx,yy,2,HarborAtmosphere.QUAY_EDGE);
                float length=Physics.length(xx-x,yy-y);
                for(float d=14;d<length-6;d+=38) {
                    float t=d/length,bx=x+(xx-x)*t,by=y+(yy-y)*t;
                    p.circle(bx,by,3.5f,VisualStyle.OUTLINE);
                    p.circle(bx,by,1.5f,0xff6e6252);
                }
            }
        }
        p.rect(outerLow,minY,TrainingChannel.MIN-outerLow,maxY-minY,HarborAtmosphere.SHORE_DARK);
        p.rect(TrainingChannel.MAX,minY,outerHigh-TrainingChannel.MAX,maxY-minY,HarborAtmosphere.SHORE_DARK);
        int gate=g.tutorial.channelGate;
        for(int i=1;i<TrainingChannel.X.length;i++) {
            float x=TrainingChannel.X[i],y=TrainingChannel.Y[i];
            if(x<low || x>high) continue;
            int color=i<gate?ControlView.GREEN:i==gate?VisualStyle.GOLD:VisualStyle.MUTED;
            for(int side=-1;side<=1;side+=2) {
                float yy=y+side*43;
                p.circle(x,yy,6,VisualStyle.OUTLINE);p.circle(x,yy,4,color);
                p.line(x,yy,x+13,yy,2,VisualStyle.CREAM);
                p.poly(new float[]{x+13,yy,x+7,yy,x+10,yy+side*10},color);
            }
            for(int j=-5;j<=5;j++) p.line(x,y+j*10-3,x,y+j*10+3,2,color);
            RiverPainter.center(p,i==TrainingChannel.X.length-1?"HARBOR":"GATE "+i,x+20,y,9,color);
        }
        gateBurst(g,p);
        propsAndTug(g,canvas,low,high);
        int next=Math.min(gate,TrainingChannel.X.length-1);
        float gx=TrainingChannel.X[next],gy=TrainingChannel.Y[next];
        float sy=c.screenY(gx,gy);
        if(sy<82) {
            float sx=Physics.clamp(c.screenX(gx,gy),80,g.layout.width-80);
            ControlView.arrow(canvas,sx,122,sx,88,VisualStyle.GOLD);
            Lettering.center(canvas,"NEXT GATE",sx,131,10,VisualStyle.CREAM);
        }
    }
    private static void gateBurst(Game g,Painter world) {
        Tutorial t=g.tutorial;
        if(t.gateClearTicks<=0 || t.clearedGate<1) return;
        Painter water=new WaterEffectsPainter(g,world);
        float x=TrainingChannel.X[t.clearedGate],y=TrainingChannel.Y[t.clearedGate];
        float age=1-t.gateClearTicks/90f,fade=(1-age)*(1-age);
        int mint=((int)(220*fade)<<24)|0x99ffcb;
        int white=((int)(245*fade)<<24)|0xfff0c5;
        ControlView.ring(water,x,y,12+100*age,1,mint,4*(1-age)+1);
        ControlView.ring(water,x,y,6+65*age,1,white,2);
        water.line(x,y-TrainingChannel.HALF_WIDTH,x,y+TrainingChannel.HALF_WIDTH,2+7*fade,mint);
        for(int side=-1;side<=1;side+=2) for(int i=0;i<5;i++) {
            double angle=(i+1)*Math.PI/6;
            float distance=8+55*age;
            float sx=x+(float)StrictMath.cos(angle)*distance;
            float sy=y+side*(43-(float)StrictMath.sin(angle)*distance);
            water.circle(sx,sy,1+2*fade,i%2==0?white:mint);
        }
    }
    /** Keep the entire footprint on land even at a steep bend or turning point. */
    static float propY(float x,int side,float half) {
        float edge=side>0?-Float.MAX_VALUE:Float.MAX_VALUE;
        for(float[] r:TrainingChannel.RECTANGLES) if(r[2]>=x-half && r[0]<=x+half)
            edge=side>0?Math.max(edge,r[3]):Math.min(edge,r[1]);
        if(edge==Float.MAX_VALUE || edge==-Float.MAX_VALUE) edge=TrainingChannel.center(x)+side*TrainingChannel.HALF_WIDTH;
        return edge+side*(half+18);
    }
    private static void propsAndTug(Game g,Painter canvas,float low,float high) {
        RiverCamera c=g.layout.camera;
        int spacing=220;
        int first=(int)StrictMath.floor((low-spacing-50)/spacing),last=(int)StrictMath.ceil((high+spacing-50)/spacing);
        int count=(last-first+1)*2+2;
        float[] xs=new float[count],ys=new float[count],depth=new float[count];int[] cells=new int[count];
        int n=0;
        for(int i=first;i<=last;i++) for(int side=-1;side<=1;side+=2) {
            int cell=Math.abs(i*3+(side>0?2:0))%8;
            float x=i*spacing+50+(side>0?55:0);
            float half=cell==0 || cell==1?StaticHarborArt.FOOTPRINT_HALF:TutorialSceneryArt.footprintHalf(TutorialSceneryArt.fromLegacy(cell));
            xs[n]=x;ys[n]=propY(x,side,half);cells[n]=cell;depth[n]=c.screenY(x,ys[n]);n++;
        }
        // Two fixed industrial landmarks break up repeated storage props. Their
        // complete bases stay inland; plume and lamp anchors belong to their art.
        for(int stack=0;stack<2;stack++) {
            float x=stack==0?610:1270;
            xs[n]=x;ys[n]=propY(x,-1,ChimneyArt.FOOTPRINT_HALF);
            cells[n]=-1-stack;depth[n]=c.screenY(x,ys[n]);n++;
        }
        // Banks reverse their screen-depth relationship as the course bends.
        // Sort both banks together and insert the vessel at its waterline depth.
        for(int i=1;i<count;i++) {
            float x=xs[i],y=ys[i],d=depth[i];int cell=cells[i],j=i-1;
            while(j>=0 && depth[j]>d) { xs[j+1]=xs[j];ys[j+1]=ys[j];depth[j+1]=depth[j];cells[j+1]=cells[j];j--; }
            xs[j+1]=x;ys[j+1]=y;depth[j+1]=d;cells[j+1]=cell;
        }
        float tugDepth=c.screenY(g.tug.x,g.tug.y);boolean tugDrawn=false;
        Painter world=new RiverPainter(canvas,c);
        for(int i=0;i<count;i++) {
            if(!tugDrawn && depth[i]>=tugDepth) { VesselView.draw(g,world,g.tug,true);tugDrawn=true; }
            if(cells[i]<0) ChimneyArt.draw(g,canvas,-cells[i]-1,xs[i],ys[i]);
            else SceneryView.prop(g,canvas,cells[i],xs[i],ys[i]);
        }
        if(!tugDrawn) VesselView.draw(g,world,g.tug,true);
    }
}
