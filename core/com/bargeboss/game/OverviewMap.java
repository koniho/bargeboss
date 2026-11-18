package com.bargeboss.game;

/** Fixed overhead navigation: world +X points up, world +Y points right. */
public final class OverviewMap {
    static final float WIDTH=184,HEIGHT=164,PAD=10,TOP=22,BOTTOM=14;
    static final int PLAYER=0xffffcf29,TARGET=0xffffa957,CARGO=0xff77c8cf;
    private static final Chart[] CHARTS={new Chart(0),new Chart(1),new Chart(2)};
    private OverviewMap(){}
    public static boolean visible(Game g){return g.mode==Game.PLAY && (g.tutorial==null || g.tutorial.lesson!=Tutorial.DONE);}
    public static Layout.Box bounds(Game g){return new Layout.Box(g.settings.joystickSide()==GameSettings.RIGHT?18:g.layout.width-WIDTH-18,Joystick.centerY(g)-HEIGHT*.5f,WIDTH,HEIGHT);}
    static Chart chart(Game g){return CHARTS[g.montroseStage()?1:g.indianaStage()?2:0];}
    static float pulse(int tick){return .65f+.35f*(float)StrictMath.sin(tick*StrictMath.PI/60);}
    public static void draw(Game g,Painter p){
        if(!visible(g))return;
        Layout.Box box=bounds(g);Chart c=chart(g);
        p.prepareLayer(c,1);p.layer(c,box.x,box.y,1,.53f);
        Painter local=ScreenView.offset(p,box.x,box.y);
        for(int i=0;i<3;i++)if(g.berthEnabled(i) && !MontroseRescue.swimmers(g) && (g.tutorial==null || g.tutorial.lesson>=Tutorial.HOLD))target(local,c,g.dockX(i),g.dockY(i),TARGET);
        for(Body b:g.cargo)if(b!=null&&b.active&&!b.deliveryRecorded) {
            float x=c.x(b.y),y=c.y(b.x);local.rect(x-1.7f,y-1.7f,3.4f,3.4f,SoftCue.alpha(CARGO,.7f));
        }
        if(g.trainingChannel() && g.tutorial.lesson==Tutorial.CHANNEL && g.tutorial.channelGate<TrainingChannel.X.length){int i=g.tutorial.channelGate;target(local,c,TrainingChannel.X[i],TrainingChannel.Y[i],TARGET);}
        if(g.tutorial!=null && g.tutorial.lesson==Tutorial.MOVE && g.tutorial.driveMarker<Tutorial.DRIVE_X.length){int i=g.tutorial.driveMarker;target(local,c,Tutorial.DRIVE_X[i],Tutorial.DRIVE_Y[i],TARGET);}
        if(g.lockStage())target(local,c,300,g.lock.phase==Lock.READY?Lock.ENTRY_Y+85:g.lock.phase==Lock.EXIT?Lock.EXIT_Y-120:(Lock.ENTRY_Y+Lock.EXIT_Y)*.5f,TARGET);
        if(MontroseRescue.swimmers(g))for(int i=0;i<MontroseRescue.PEOPLE;i++)if(g.rescue.state[i]==0)local.circle(c.x(g.rescue.y[i]),c.y(g.rescue.x[i]),1.7f,TARGET);
        float x=c.x(g.tug.y),y=c.y(g.tug.x),pulse=pulse(g.visualTicks);
        local.circle(x,y,6+2*pulse,SoftCue.alpha(PLAYER,.12f+.12f*pulse));
        SoftCue.ring(local,x,y,4+2*pulse,1,SoftCue.alpha(PLAYER,.4f+.35f*pulse),1);
        local.circle(x,y,2.7f,PLAYER);
    }
    private static void target(Painter p,Chart c,float wx,float wy,int color){float x=c.x(wy),y=c.y(wx);int ink=SoftCue.alpha(color,.8f);p.line(x,y-3,x+3,y,1,ink);p.line(x+3,y,x,y+3,1,ink);p.line(x,y+3,x-3,y,1,ink);p.line(x-3,y,x,y-3,1,ink);}
    static final class Chart extends StaticLayer {
        final int stage;final float minX,maxX,minY,maxY,scale,cx,cy;
        final float[][] water,docks;
        Chart(int stage){
            this.stage=stage;
            float[][] extent=stage==1?new float[][]{MontroseHarbor.COAST}:stage==2?rectangles(IndianaHarbor.WATER):new float[][]{TrainingChannel.WATER_BOUNDARY};
            float lx=Float.MAX_VALUE,ly=lx,hx=-lx,hy=-lx;
            for(float[] a:extent)for(int i=0;i<a.length;i+=2){lx=Math.min(lx,a[i]);hx=Math.max(hx,a[i]);ly=Math.min(ly,a[i+1]);hy=Math.max(hy,a[i+1]);}
            float margin=stage==1?200:80;minX=lx-margin;maxX=hx+margin;minY=ly-margin;maxY=hy+margin;
            scale=Math.min((WIDTH-2*PAD)/(maxY-minY),(HEIGHT-TOP-BOTTOM)/(maxX-minX));
            cx=WIDTH*.5f;cy=(TOP+HEIGHT-BOTTOM)*.5f;
            water=project(stage==1?new float[][]{MontroseHarbor.COAST}:stage==2?new float[][]{indianaCoast()}:extent);
            docks=project(stage==1?MontroseHarbor.DOCKS:new float[0][]);
        }
        float x(float worldY){return Physics.clamp(cx+(worldY-(minY+maxY)*.5f)*scale,PAD,WIDTH-PAD);}
        float y(float worldX){return Physics.clamp(cy-(worldX-(minX+maxX)*.5f)*scale,TOP,HEIGHT-BOTTOM);}
        private float[][] project(float[][] shapes){float[][] result=new float[shapes.length][];
            float[] clip={PAD,TOP,WIDTH-PAD,TOP,WIDTH-PAD,HEIGHT-BOTTOM,PAD,HEIGHT-BOTTOM};
            for(int j=0;j<shapes.length;j++){float[] a=shapes[j],b=new float[a.length];for(int i=0;i<a.length;i+=2){b[i]=cx+(a[i+1]-(minY+maxY)*.5f)*scale;b[i+1]=cy-(a[i]-(minX+maxX)*.5f)*scale;}result[j]=PolygonWater.clip(b,clip);}
            return result;
        }
        public int key(){return 0x6d000200+stage;}public float width(){return WIDTH;}public float height(){return HEIGHT;}public boolean backgroundSafe(){return true;}
        public void paint(Painter p){
            // Rasterize opaque strokes, then fade the whole chart once. Segment joins
            // must not accumulate translucency and turn into bright dots.
            for(float[] a:water)outline(p,a,stage!=1,0xff96c5ce,1.15f);
            for(float[] a:docks)outline(p,a,true,0xff7eaab5,.7f);
        }
        private static void outline(Painter p,float[] a,boolean closed,int color,float width){
            for(int i=2;i<a.length;i+=2)p.line(a[i-2],a[i-1],a[i],a[i+1],width,color);
            if(closed && a.length>=6)p.line(a[a.length-2],a[a.length-1],a[0],a[1],width,color);
        }
        // Exterior of the six connected Indiana water rectangles, without internal seams.
        private static float[] indianaCoast(){return new float[]{-700,-1400,1400,-1400,1400,-960,800,-960,800,-820,1150,-820,1150,150,500,150,500,1800,250,1800,250,1100,-500,1100,-500,850,250,850,250,150,100,150,100,-820,400,-820,400,-960,-700,-960};}

        private static float[][] rectangles(float[][] r){float[][] result=new float[r.length][];for(int i=0;i<r.length;i++){float[] a=r[i];result[i]=new float[]{a[0],a[1],a[2],a[1],a[2],a[3],a[0],a[3]};}return result;}
    }
}
