package com.bargeboss.game;

/** Player-following overhead navigation, clamped to the play bounds. */
public final class OverviewMap {
    static final float WIDTH=184,HEIGHT=184,EDGE=.5f;
    static final int PLAYER=0xffffcf29,TARGET=0xffffa957,CARGO=0xff77c8cf;
    private static final Chart[] CHARTS={new Chart(0),new Chart(1),new Chart(2),new Chart(3)};
    // Never shrink below the full play-area fit; preserve the world's aspect ratio.
    static final float MIN_ZOOM=1,MAX_ZOOM=4;
    private static final float[] OUTSIDE=outside();
    private float level,zoom=1;
    private int pointerId=-1;
    boolean active;
    OverviewMap(){}
    float level(){return level;}
    float zoom(){return zoom;}
    void reset(){level=0;zoom=1;cancelTouch();}
    boolean owns(int id){return active && pointerId==id;}
    void begin(Game g,int id,float x){pointerId=id;active=true;setFromX(g,x);}
    void cancelTouch(){pointerId=-1;active=false;}
    void setFromX(Game g,float x){
        Layout.Box b=sliderBounds(g);level=Physics.clamp((x-b.x-24)/(b.w-48),0,1);
        zoom=MIN_ZOOM*(float)StrictMath.pow(MAX_ZOOM/MIN_ZOOM,level);
    }
    static Layout.Box sliderBounds(Game g){Layout.Box map=bounds(g);return new Layout.Box(map.x,g.layout.height-48,map.w,44);}
    static boolean sliderHit(Game g,float x,float y){return visible(g) && sliderBounds(g).contains(x,y);}
    // Center on each axis only while the enlarged play area covers the viewport.
    // A shorter axis retains its top/left alignment instead of introducing a gap.
    private static float offset(float player,float span,float view){
        if(span<=view+.001f)return 0;
        return Physics.clamp(view*.5f-player,view-span,0);
    }
    static float offsetX(Game g){Chart c=chart(g);float scale=c.scale*g.overview.zoom();return offset((g.tug.y-c.minY)*scale,(c.maxY-c.minY)*scale,WIDTH-2*EDGE);}
    static float offsetY(Game g){Chart c=chart(g);float scale=c.scale*g.overview.zoom();return offset((c.maxX-g.tug.x)*scale,(c.maxX-c.minX)*scale,HEIGHT-2*EDGE);}
    static float mapX(Game g,float worldY){Chart c=chart(g);return EDGE+(worldY-c.minY)*c.scale*g.overview.zoom()+offsetX(g);}
    static float mapY(Game g,float worldX){Chart c=chart(g);return EDGE+(c.maxX-worldX)*c.scale*g.overview.zoom()+offsetY(g);}
    // Four disjoint rectangles, with consistent triangle winding, work on every
    // host's existing exclusion clip (including Metal's stencil implementation).
    private static float[] outside(){
        float m=32768;float[][] boxes={{-m,-m,m,0},{-m,HEIGHT,m,m},{-m,0,0,HEIGHT},{WIDTH,0,m,HEIGHT}};
        float[] result=new float[48];int at=0;
        for(float[] b:boxes){float[] t={b[0],b[1],b[2],b[1],b[2],b[3],b[0],b[1],b[2],b[3],b[0],b[3]};for(float v:t)result[at++]=v;}
        return result;
    }
    static void slider(Game g,Painter p){
        if(!visible(g))return;
        Layout.Box b=sliderBounds(g);float left=b.x+24,right=b.x+b.w-24,y=b.centerY(),thumb=left+(right-left)*g.overview.level;
        SoftCue.line(p,left,y,right,y,3,0x51678691);
        SoftCue.line(p,left,y,thumb,y,3,0xaac4dedb);
        for(int i=0;i<=3;i++){float x=left+(right-left)*i/3; p.line(x,y-5,x,y+5,1,0x527fa9ad);}
        p.line(b.x+5,y,b.x+13,y,1.5f,0xaac4dedb);
        p.line(b.x+b.w-13,y,b.x+b.w-5,y,1.5f,0xaac4dedb);
        p.line(b.x+b.w-9,y-4,b.x+b.w-9,y+4,1.5f,0xaac4dedb);
        p.circle(thumb,y,g.overview.active?14:11,g.overview.active?0x387fe1db:0x207fe1db);
        p.circle(thumb,y,6,0xddc4dedb);
        SoftCue.ring(p,thumb,y,8,1,0x907fe1db,1);
    }
    public static boolean visible(Game g){return g.mode==Game.PLAY && (g.tutorial==null || g.tutorial.lesson!=Tutorial.DONE);}
    public static Layout.Box bounds(Game g){return new Layout.Box(g.settings.joystickSide()==GameSettings.RIGHT?18:g.layout.width-WIDTH-18,g.layout.height-48-8-HEIGHT,WIDTH,HEIGHT);}
    static Chart chart(Game g){return CHARTS[g.montroseStage()?1:g.indianaStage()?2:g.calumetStage()?3:0];}
    static float pulse(int tick){return .65f+.35f*(float)StrictMath.sin(tick*StrictMath.PI/60);}
    public static void draw(Game g,Painter p){
        if(!visible(g))return;
        Layout.Box box=bounds(g);Chart c=chart(g);
        float zoom=g.overview.zoom();
        p.prepareLayer(c,zoom);
        Painter local=ScreenView.offset(p,box.x,box.y);
        // A faint viewport edge stays readable where zoom crops the world. The
        // stronger actual play boundary is drawn over it with the chart below.
        int frame=0x2696c5ce;
        local.line(.5f,.5f,WIDTH-.5f,.5f,.8f,frame);
        local.line(WIDTH-.5f,.5f,WIDTH-.5f,HEIGHT-.5f,.8f,frame);
        local.line(WIDTH-.5f,HEIGHT-.5f,.5f,HEIGHT-.5f,.8f,frame);
        local.line(.5f,HEIGHT-.5f,.5f,.5f,.8f,frame);
        local.pushClipOutside(OUTSIDE);
        try {
        local.layer(c,EDGE*(1-zoom)+offsetX(g),EDGE*(1-zoom)+offsetY(g),zoom,.53f);
        for(int i=0;i<3;i++)if(g.berthEnabled(i) && !MontroseRescue.swimmers(g) && (g.tutorial==null || g.tutorial.lesson>=Tutorial.HOLD))target(g,local,g.dockX(i),g.dockY(i),TARGET);
        for(Body b:g.cargo)if(b!=null&&b.active&&!b.deliveryRecorded) {
            float x=mapX(g,b.y),y=mapY(g,b.x);local.rect(x-1.7f,y-1.7f,3.4f,3.4f,SoftCue.alpha(CARGO,.7f));
        }
        if(g.trainingChannel() && g.tutorial.lesson==Tutorial.CHANNEL && g.tutorial.channelGate<TrainingChannel.X.length){int i=g.tutorial.channelGate;target(g,local,TrainingChannel.X[i],TrainingChannel.Y[i],TARGET);}
        if(g.tutorial!=null && g.tutorial.lesson==Tutorial.MOVE && g.tutorial.driveMarker<Tutorial.DRIVE_X.length){int i=g.tutorial.driveMarker;target(g,local,Tutorial.DRIVE_X[i],Tutorial.DRIVE_Y[i],TARGET);}
        if(g.calumetStage() && g.voyage.job==2)target(g,local,g.voyage.gateX(),g.voyage.gateY(),TARGET);
        if(g.lockStage())target(g,local,300,g.lock.phase==Lock.READY?Lock.ENTRY_Y+85:g.lock.phase==Lock.EXIT?Lock.EXIT_Y-120:(Lock.ENTRY_Y+Lock.EXIT_Y)*.5f,TARGET);
        if(MontroseRescue.swimmers(g))for(int i=0;i<MontroseRescue.PEOPLE;i++)if(g.rescue.state[i]==0)local.circle(mapX(g,g.rescue.y[i]),mapY(g,g.rescue.x[i]),1.7f,TARGET);
        float x=mapX(g,g.tug.y),y=mapY(g,g.tug.x),pulse=pulse(g.visualTicks);
        local.circle(x,y,6+2*pulse,SoftCue.alpha(PLAYER,.12f+.12f*pulse));
        SoftCue.ring(local,x,y,4+2*pulse,1,SoftCue.alpha(PLAYER,.4f+.35f*pulse),1);
        local.circle(x,y,2.7f,PLAYER);
        } finally {local.popClip();}
        slider(g,p);
    }
    private static void target(Game g,Painter p,float wx,float wy,int color){float x=mapX(g,wy),y=mapY(g,wx);int ink=SoftCue.alpha(color,.8f);p.line(x,y-3,x+3,y,1,ink);p.line(x+3,y,x,y+3,1,ink);p.line(x,y+3,x-3,y,1,ink);p.line(x-3,y,x,y-3,1,ink);}
    static final class Chart extends StaticLayer {
        final int stage;final float minX,maxX,minY,maxY,scale;
        final float[][] water,docks;
        final float[] navigableHoles,boundary;
        Chart(int stage){
            this.stage=stage;
            float[][] extent=stage==1?new float[][]{MontroseHarbor.COAST}:stage==2?new float[][]{IndianaHarbor.COAST}:stage==3?new float[][]{CalumetRiver.COAST}:new float[][]{TrainingChannel.WATER_BOUNDARY};
            // Frame the level's world bounds, not an arbitrary padded shoreline crop.
            float[] bounds=stage==1?PolygonWater.bounds(MontroseHarbor.PLAY_BOUNDS):
                    stage==2?new float[]{IndianaHarbor.MIN_X,IndianaHarbor.MIN_Y,IndianaHarbor.MAX_X,IndianaHarbor.MAX_Y}:
                    stage==3?new float[]{CalumetRiver.MIN_X,CalumetRiver.MIN_Y,CalumetRiver.MAX_X,CalumetRiver.MAX_Y}:
                    PolygonWater.bounds(TrainingChannel.WATER_BOUNDARY);
            minX=bounds[0];minY=bounds[1];maxX=bounds[2];maxY=bounds[3];
            scale=Math.min((WIDTH-2*EDGE)/(maxY-minY),(HEIGHT-2*EDGE)/(maxX-minX));
            boundary=new float[]{x(minY),y(maxX),x(maxY),y(maxX),x(maxY),y(minX),x(minY),y(minX)};
            water=project(stage==1?new float[][]{MontroseHarbor.COAST}:extent);
            docks=project(stage==1?MontroseHarbor.DOCKS:new float[0][]);
            navigableHoles=navigableClip();
        }
        float x(float worldY){return EDGE+(worldY-minY)*scale;}
        float y(float worldX){return EDGE+(maxX-worldX)*scale;}
        private float[][] project(float[][] shapes){float[][] result=new float[shapes.length][];
            float[] clip=boundary;
            for(int j=0;j<shapes.length;j++){float[] a=shapes[j],b=new float[a.length];for(int i=0;i<a.length;i+=2){b[i]=x(a[i+1]);b[i+1]=y(a[i]);}result[j]=PolygonWater.clip(b,clip);}
            return result;
        }
        /** Collect convex navigable pieces once, including the marina's dock cutouts. */
        private float[] navigableClip(){
            final java.util.ArrayList<float[]> pieces=new java.util.ArrayList<float[]>();
            Painter collector=new Painter(){
                public void poly(float[] q,int color){if(q.length>=6 && Math.abs(PolygonWater.area(q))>.001f)pieces.add(q);}
                public void rect(float x,float y,float w,float h,int c){}public void circle(float x,float y,float r,int c){}
                public void line(float x,float y,float xx,float yy,float w,int c){}
                public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
                public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
            };
            float[] region={minX,minY,maxX,minY,maxX,maxY,minX,maxY};
            if(stage==1){
                // Query small cells so each water triangle only subtracts nearby
                // docks; a whole-marina query needlessly fragments distant water.
                for(float xx=minX;xx<maxX;xx+=256)for(float yy=minY;yy<maxY;yy+=256)
                    MontroseHarbor.paintWater(collector,new float[]{xx,yy,Math.min(xx+256,maxX),yy,Math.min(xx+256,maxX),Math.min(yy+256,maxY),xx,Math.min(yy+256,maxY)},0xffffffff);
            }
            else if(stage==2)IndianaHarbor.paintWater(collector,region,0xffffffff);
            else if(stage==3)CalumetRiver.paintWater(collector,region,0xffffffff);
            else for(float[] triangle:PolygonWater.triangles(TrainingChannel.WATER_BOUNDARY))collector.poly(triangle,0xffffffff);
            int count=0;for(float[] q:pieces)count+=(q.length/2-2)*6;
            float[] triangles=new float[count];int at=0;
            for(float[] q:pieces)for(int i=2;i<q.length-2;i+=2){
                triangles[at++]=x(q[1]);triangles[at++]=y(q[0]);
                triangles[at++]=x(q[i+1]);triangles[at++]=y(q[i]);
                triangles[at++]=x(q[i+3]);triangles[at++]=y(q[i+2]);
            }
            return triangles;
        }
        public int key(){return 0x6d000800+stage;}public float width(){return WIDTH;}public float height(){return HEIGHT;}public boolean backgroundSafe(){return true;}
        public void paint(Painter p){
            // Dark, faint water backing; the stronger cyan overlay distinguishes land/docks.
            p.poly(boundary,0x30061119);
            p.pushClipOutside(navigableHoles);
            try{p.poly(boundary,0x5096c5ce);}
            finally{p.popClip();}
            // Opaque strokes share one final layer fade, keeping joins even.
            outline(p,boundary,true,0xff96c5ce,1);
            for(float[] a:water)outline(p,a,stage!=1,0xff96c5ce,1.15f);
            for(float[] a:docks)outline(p,a,true,0xff7eaab5,.7f);
        }
        private static void outline(Painter p,float[] a,boolean closed,int color,float width){
            for(int i=2;i<a.length;i+=2)p.line(a[i-2],a[i-1],a[i],a[i+1],width,color);
            if(closed && a.length>=6)p.line(a[a.length-2],a[a.length-1],a[0],a[1],width,color);
        }
    }
}
