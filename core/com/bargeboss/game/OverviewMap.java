package com.bargeboss.game;

/** Fixed overhead navigation: world +X points up, world +Y points right. */
public final class OverviewMap {
    static final float WIDTH=184,HEIGHT=184,PAD=10,TOP=PAD,BOTTOM=PAD;
    static final int PLAYER=0xffffcf29,TARGET=0xffffa957,CARGO=0xff77c8cf;
    private static final Chart[] CHARTS={new Chart(0),new Chart(1),new Chart(2),new Chart(3)};
    private OverviewMap(){}
    public static boolean visible(Game g){return g.mode==Game.PLAY && (g.tutorial==null || g.tutorial.lesson!=Tutorial.DONE);}
    public static Layout.Box bounds(Game g){return new Layout.Box(g.settings.joystickSide()==GameSettings.RIGHT?18:g.layout.width-WIDTH-18,Joystick.centerY(g)-HEIGHT*.5f,WIDTH,HEIGHT);}
    static Chart chart(Game g){return CHARTS[g.montroseStage()?1:g.indianaStage()?2:g.calumetStage()?3:0];}
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
        if(g.calumetStage() && g.voyage.job==2)target(local,c,g.voyage.gateX(),g.voyage.gateY(),TARGET);
        if(g.lockStage())target(local,c,300,g.lock.phase==Lock.READY?Lock.ENTRY_Y+85:g.lock.phase==Lock.EXIT?Lock.EXIT_Y-120:(Lock.ENTRY_Y+Lock.EXIT_Y)*.5f,TARGET);
        if(MontroseRescue.swimmers(g))for(int i=0;i<MontroseRescue.PEOPLE;i++)if(g.rescue.state[i]==0)local.circle(c.x(g.rescue.y[i]),c.y(g.rescue.x[i]),1.7f,TARGET);
        float x=c.x(g.tug.y),y=c.y(g.tug.x),pulse=pulse(g.visualTicks);
        local.circle(x,y,6+2*pulse,SoftCue.alpha(PLAYER,.12f+.12f*pulse));
        SoftCue.ring(local,x,y,4+2*pulse,1,SoftCue.alpha(PLAYER,.4f+.35f*pulse),1);
        local.circle(x,y,2.7f,PLAYER);
    }
    private static void target(Painter p,Chart c,float wx,float wy,int color){float x=c.x(wy),y=c.y(wx);int ink=SoftCue.alpha(color,.8f);p.line(x,y-3,x+3,y,1,ink);p.line(x+3,y,x,y+3,1,ink);p.line(x,y+3,x-3,y,1,ink);p.line(x-3,y,x,y-3,1,ink);}
    static final class Chart extends StaticLayer {
        final int stage;final boolean enclosed;final float margin,minX,maxX,minY,maxY,scale,cx,cy;
        final float[][] water,docks,waterFill;
        final float[] dockHoles,boundary;
        Chart(int stage){
            this.stage=stage;
            float[][] extent=stage==1?new float[][]{MontroseHarbor.COAST}:stage==2?new float[][]{IndianaHarbor.COAST}:stage==3?new float[][]{CalumetRiver.COAST}:new float[][]{TrainingChannel.WATER_BOUNDARY};
            // Frame the level's world bounds, not an arbitrary padded shoreline crop.
            float[] bounds=stage==1?PolygonWater.bounds(MontroseHarbor.PLAY_BOUNDS):
                    stage==2?new float[]{IndianaHarbor.MIN_X,IndianaHarbor.MIN_Y,IndianaHarbor.MAX_X,IndianaHarbor.MAX_Y}:
                    stage==3?new float[]{CalumetRiver.MIN_X,CalumetRiver.MIN_Y,CalumetRiver.MAX_X,CalumetRiver.MAX_Y}:
                    PolygonWater.bounds(TrainingChannel.WATER_BOUNDARY);
            // The practice canal/basins have solid banks all around. The three
            // geographic locations connect to open lake water at their outer limits.
            enclosed=stage==0;
            margin=enclosed?Math.max(bounds[2]-bounds[0],bounds[3]-bounds[1])*.06f:0;
            minX=bounds[0]-margin;minY=bounds[1]-margin;maxX=bounds[2]+margin;maxY=bounds[3]+margin;
            scale=Math.min((WIDTH-2*PAD)/(maxY-minY),(HEIGHT-TOP-BOTTOM)/(maxX-minX));
            cx=WIDTH*.5f;cy=(TOP+HEIGHT-BOTTOM)*.5f;
            boundary=new float[]{x(minY),y(maxX),x(maxY),y(maxX),x(maxY),y(minX),x(minY),y(minX)};
            water=project(stage==1?new float[][]{MontroseHarbor.COAST}:extent);
            docks=project(stage==1?MontroseHarbor.DOCKS:new float[0][]);
            waterFill=project(stage==1?new float[][]{MontroseHarbor.WATER_POLYGON}:extent);
            int count=0;for(float[] dock:docks)count+=Math.max(0,dock.length/2-2)*6;
            dockHoles=new float[count];int at=0;
            for(float[] dock:docks)for(int i=2;i<dock.length-2;i+=2){dockHoles[at++]=dock[0];dockHoles[at++]=dock[1];dockHoles[at++]=dock[i];dockHoles[at++]=dock[i+1];dockHoles[at++]=dock[i+2];dockHoles[at++]=dock[i+3];}
        }
        float x(float worldY){return cx+(Physics.clamp(worldY,minY,maxY)-(minY+maxY)*.5f)*scale;}
        float y(float worldX){return cy-(Physics.clamp(worldX,minX,maxX)-(minX+maxX)*.5f)*scale;}
        private float[][] project(float[][] shapes){float[][] result=new float[shapes.length][];
            float[] clip=boundary;
            for(int j=0;j<shapes.length;j++){float[] a=shapes[j],b=new float[a.length];for(int i=0;i<a.length;i+=2){b[i]=cx+(a[i+1]-(minY+maxY)*.5f)*scale;b[i+1]=cy-(a[i]-(minX+maxX)*.5f)*scale;}result[j]=PolygonWater.clip(b,clip);}
            return result;
        }
        public int key(){return 0x6d000500+stage;}public float width(){return WIDTH;}public float height(){return HEIGHT;}public boolean backgroundSafe(){return true;}
        public void paint(Painter p){
            // Water is softer than contours; dry shore and dock interiors remain clear.
            if(dockHoles.length>0)p.pushClipOutside(dockHoles);
            try{for(float[] a:waterFill)if(a.length>=6)p.poly(a,0x5096c5ce);}
            finally{if(dockHoles.length>0)p.popClip();}
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
