package com.bargeboss.game;

/** Player-following overhead navigation, clamped to the chart coverage. */
public final class OverviewMap {
    static final float WIDTH=184,HEIGHT=184,EDGE=.5f;
    static final int PLAYER=0xffffcf29,TARGET=0xffffa957,CARGO=0xff77c8cf;
    private static final Chart[] CHARTS={new Chart(0),new Chart(1)};
    /** Arena charts (tag circuit, Thousand Islands), built on first use. */
    private static Chart loopChart,islandChart;
    // Never shrink below the full chart fit; preserve the world's aspect ratio.
    static final float MIN_ZOOM=1,MAX_ZOOM=4;
    private static final float[] OUTSIDE=outside();
    private float level,zoom=1;
    private int pointerId=-1;
    boolean active;
    int edits;
    void lessonZoomIn(Game g){level=1;zoom=maxZoom(g);cancelTouch();}
    /** Training chart detail at full zoom: five times the fit of the original 3392-unit yard.
     * Fixed in chart pixels per world unit so enlarging the yard (challenge basin) keeps the
     * chart lesson's close view, and its distant target, unchanged. */
    static final float TRAINING_DETAIL=5*(WIDTH-2*EDGE)/3392f;
    static float maxZoom(Game g){Chart c=chart(g);return c.stage==0?Math.max(5,TRAINING_DETAIL/c.scale):MAX_ZOOM;}
    OverviewMap(){}
    float level(){return level;}
    float zoom(){return zoom;}
    void reset(){level=0;zoom=1;cancelTouch();}
    boolean owns(int id){return active && pointerId==id;}
    void begin(Game g,int id,float x){pointerId=id;active=true;setFromX(g,x);}
    void cancelTouch(){pointerId=-1;active=false;}
    void setFromX(Game g,float x){
        edits++;Layout.Box b=sliderBounds(g);level=Physics.clamp((x-b.x-24)/(b.w-48),0,1);
        zoom=MIN_ZOOM*(float)StrictMath.pow(maxZoom(g)/MIN_ZOOM,level);
    }
    static Layout.Box sliderBounds(Game g){Layout.Box map=bounds(g);return new Layout.Box(map.x,g.layout.height-48,map.w,44);}
    static boolean sliderHit(Game g,float x,float y){return visible(g) && !g.abilities.replacesMap() && sliderBounds(g).contains(x,y);}
    // Center on each axis only while the enlarged play area covers the viewport.
    // A shorter axis retains its top/left alignment instead of introducing a gap.
    private static float offset(float player,float span,float view){
        if(span<=view+.001f)return 0;
        return Physics.clamp(view*.5f-player,view-span,0);
    }
    static float offsetX(Game g){Chart c=chart(g);float scale=c.scale*g.overview.zoom();return offset((c.upright?g.tug.x-c.minX:g.tug.y-c.minY)*scale,(c.upright?c.maxX-c.minX:c.maxY-c.minY)*scale,WIDTH-2*EDGE);}
    static float offsetY(Game g){Chart c=chart(g);float scale=c.scale*g.overview.zoom();return offset((c.upright?g.tug.y-c.minY:c.maxX-g.tug.x)*scale,(c.upright?c.maxY-c.minY:c.maxX-c.minX)*scale,HEIGHT-2*EDGE);}
    static float mapX(Game g,float axis){Chart c=chart(g);return EDGE+(axis-(c.upright?c.minX:c.minY))*c.scale*g.overview.zoom()+offsetX(g);}
    static float mapY(Game g,float axis){Chart c=chart(g);return EDGE+(c.upright?axis-c.minY:c.maxX-axis)*c.scale*g.overview.zoom()+offsetY(g);}
    static float mapX(Game g,float wx,float wy){return mapX(g,chart(g).upright?wx:wy);}
    static float mapY(Game g,float wx,float wy){return mapY(g,chart(g).upright?wy:wx);}
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
        Layout.Box b=sliderBounds(g);float left=b.x+24,right=b.x+b.w-24,y=b.y+28,thumb=left+(right-left)*g.overview.level;
        PilotGlass.panel(p,b.x+2,b.y+2,b.w-4,b.h-4,0xa8101c30);
        Lettering.draw(p,"CHART ZOOM",b.x+12,b.y+7,7,PilotGlass.MUTED);
        SoftCue.line(p,left,y,right,y,3,0x51678691);
        SoftCue.line(p,left,y,thumb,y,3,0xbaffc52f);
        for(int i=0;i<=3;i++){float x=left+(right-left)*i/3; p.line(x,y-5,x,y+5,1,0x52fff0ce);}
        p.line(b.x+5,y,b.x+13,y,1.5f,0xbaffc52f);
        p.line(b.x+b.w-13,y,b.x+b.w-5,y,1.5f,0xbaffc52f);
        p.line(b.x+b.w-9,y-4,b.x+b.w-9,y+4,1.5f,0xbaffc52f);
        p.poly(new float[]{thumb-8,y-9,thumb+5,y-9,thumb+8,y-6,thumb+8,y+9,thumb-5,y+9,thumb-8,y+6},PilotGlass.AMBER);
        p.line(thumb-2,y-4,thumb-2,y+4,1,PilotGlass.NAVY);
        p.line(thumb+2,y-4,thumb+2,y+4,1,PilotGlass.NAVY);
    }
    public static boolean visible(Game g){return g.mode==Game.PLAY && (g.tutorial==null || g.tutorial.lesson>=Tutorial.MAP && g.tutorial.lesson<Tutorial.DONE);}
    public static Layout.Box bounds(Game g){return new Layout.Box(g.settings.joystickSide()==GameSettings.RIGHT?18:g.layout.width-WIDTH-18,g.layout.height-48-8-HEIGHT,WIDTH,HEIGHT);}
    /** Survey charts are built on first use by their own level, never for other stages. */
    static Chart chart(Game g){
        SurveyLevel level=g.level();if(level!=null)return level.chart();
        if(g.arenaStage() && g.arena!=null){
            if(g.arena.course instanceof ArenaCourse.Islands){if(islandChart==null)islandChart=new Chart(3);return islandChart;}
            if(loopChart==null)loopChart=new Chart(2);return loopChart;
        }
        return CHARTS[g.montroseStage()?1:0];
    }
    static float pulse(int tick){return .65f+.35f*(float)StrictMath.sin(tick*StrictMath.PI/60);}
    public static void draw(Game g,Painter p){
        if(!visible(g))return;
        Layout.Box box=bounds(g);Chart c=chart(g);
        float zoom=g.overview.zoom();
        p.prepareLayer(c,zoom);
        Painter local=ScreenView.offset(p,box.x,box.y);
        // A faint viewport edge stays readable where zoom crops the world. The
        // stronger actual play boundary is drawn over it with the chart below.
        int frame=0x26fff0ce;
        local.line(.5f,.5f,WIDTH-.5f,.5f,.8f,frame);
        local.line(WIDTH-.5f,.5f,WIDTH-.5f,HEIGHT-.5f,.8f,frame);
        local.line(WIDTH-.5f,HEIGHT-.5f,.5f,HEIGHT-.5f,.8f,frame);
        local.line(.5f,HEIGHT-.5f,.5f,.5f,.8f,frame);
        local.pushClipOutside(OUTSIDE);
        try {
        local.layer(c,EDGE*(1-zoom)+offsetX(g),EDGE*(1-zoom)+offsetY(g),zoom,.53f);
        for(int i=0;i<3;i++)if(g.berthEnabled(i) && (!MontroseRescue.cruisers(g) || g.rescue.slipsRevealed()) && !MontroseRescue.swimmers(g) && (g.tutorial==null || g.tutorial.lesson>=Tutorial.CONNECT && g.tutorial.lesson<Tutorial.MOOR && g.tutorial.lesson!=Tutorial.CHALLENGE))target(g,local,g.dockX(i),g.dockY(i),TARGET);
        if(g.tutorial!=null && g.tutorial.lesson==Tutorial.CHALLENGE)target(g,local,TowingChallenge.BERTH_X,(TowingChallenge.rowY(0)+TowingChallenge.rowY(TowingChallenge.BARGES-1))*.5f,TARGET);
        for(Body b:g.cargo)if(b!=null&&b.active&&!b.deliveryRecorded) {
            float x=mapX(g,b.x,b.y),y=mapY(g,b.x,b.y);local.rect(x-1.7f,y-1.7f,3.4f,3.4f,SoftCue.alpha(CARGO,.7f));
        }
        if(g.trainingChannel() && g.tutorial.lesson==Tutorial.CHANNEL && g.tutorial.channelGate<TrainingChannel.X.length){int i=g.tutorial.canal.guideGate(g);target(g,local,TrainingChannel.X[i],TrainingChannel.Y[i],TARGET);}
        if(g.tutorial!=null && (g.tutorial.lesson==Tutorial.FIGURE_EIGHT || g.tutorial.lesson==Tutorial.REVERSE) && g.tutorial.driveMarker<Tutorial.DRIVE_X.length){int i=g.tutorial.driveMarker;target(g,local,Tutorial.DRIVE_X[i],Tutorial.DRIVE_Y[i],TARGET);}
        if(g.tutorial!=null && g.tutorial.lesson==Tutorial.MOVE && g.tutorial.helm.phase==HelmPractice.RENDEZVOUS)target(g,local,HelmPractice.MEET_X,HelmPractice.MEET_Y,TARGET);
        if(g.tutorial!=null && (g.tutorial.lesson==Tutorial.SLIP || g.tutorial.lesson==Tutorial.MAP))target(g,local,g.tutorial.navigation.targetX(g),g.tutorial.navigation.targetY(g),TARGET);
        if(g.calumetStage() && g.voyage.job==2)target(g,local,g.voyage.gateX(),g.voyage.gateY(),TARGET);
        if(g.lockStage())target(g,local,300,g.lock.phase==Lock.READY?Lock.ENTRY_Y+85:g.lock.phase==Lock.EXIT?Lock.EXIT_Y-120:(Lock.ENTRY_Y+Lock.EXIT_Y)*.5f,TARGET);
        if(MontroseRescue.swimmers(g))for(int i=0;i<MontroseRescue.PEOPLE;i++)if(g.rescue.state[i]<4 && g.rescue.found[i])local.circle(mapX(g,g.rescue.x[i],g.rescue.y[i]),mapY(g,g.rescue.x[i],g.rescue.y[i]),1.7f,TARGET);
        if(g.tutorial!=null)for(int i=0;i<g.passages.bridges.length;i++){
            LandmarkBridge b=g.passages.bridges[i];int ink=g.passages.passable(i)?0x9995d7d1:0xbbffc170;
            local.line(mapX(g,b.x-b.dx*b.left,b.y-b.dy*b.left),mapY(g,b.x-b.dx*b.left,b.y-b.dy*b.left),
                    mapX(g,b.x+b.dx*b.right,b.y+b.dy*b.right),mapY(g,b.x+b.dx*b.right,b.y+b.dy*b.right),1,ink);
        }
        if(g.arenaStage() && g.arena!=null){
            TagArena a=g.arena;
            if(a.mode==TagArena.CHASE)for(int k=0;k<a.penX.length;k++)target(g,local,a.penX[k],a.penY[k],TARGET);
            for(int k=0;k<a.powerLive.length;k++)if(a.powerLive[k])local.circle(mapX(g,a.powerX[k],a.powerY[k]),mapY(g,a.powerX[k],a.powerY[k]),1.6f,0xff00f0ff);
            if(a.mode==TagArena.TAG && a.it>=0){Body it=a.npcs[a.it];local.circle(mapX(g,it.x,it.y),mapY(g,it.x,it.y),3.2f,0xffff4a3a);}
            if(a.mode==TagArena.CHASE)for(int i=0;i<a.npcs.length;i++)if(a.corralled[i]){Body b=a.npcs[i];local.rect(mapX(g,b.x,b.y)-1.2f,mapY(g,b.x,b.y)-1.2f,2.4f,2.4f,0x8896c5ce);}
        }
        float x=mapX(g,g.tug.x,g.tug.y),y=mapY(g,g.tug.x,g.tug.y),pulse=pulse(g.visualTicks);
        local.circle(x,y,6+2*pulse,SoftCue.alpha(PLAYER,.12f+.12f*pulse));
        SoftCue.ring(local,x,y,4+2*pulse,1,SoftCue.alpha(PLAYER,.4f+.35f*pulse),1);
        local.circle(x,y,2.7f,PLAYER);
        } finally {local.popClip();}
        slider(g,p);
    }
    private static void target(Game g,Painter p,float wx,float wy,int color){float x=mapX(g,wx,wy),y=mapY(g,wx,wy);int ink=SoftCue.alpha(color,.8f);p.line(x,y-3,x+3,y,1,ink);p.line(x+3,y,x,y+3,1,ink);p.line(x,y+3,x-3,y,1,ink);p.line(x-3,y,x,y-3,1,ink);}
    static final class Chart extends StaticLayer {
        final int stage;final float minX,maxX,minY,maxY,scale;final SurveyLevel survey;
        /** Upright charts keep world x across and y down (Montrose, arenas); others are rotated. */
        final boolean upright;final boolean arena;
        final float[][] water,docks;
        final float[] navigableHoles,boundary;
        Chart(int stage){this(stage,null);}
        /** Geographic level chart; stage 10+id keeps layer keys unique per level. */
        Chart(SurveyLevel level){this(10+level.id,level);}
        private Chart(int stage,SurveyLevel survey){
            this.stage=stage;this.survey=survey;upright=stage>=1 && stage<=3;arena=stage==2 || stage==3;
            ArenaCourse course=stage==2?new ArenaCourse.Loop():stage==3?new ArenaCourse.Islands():null;
            float[][] extent=arena?arenaWater(course):stage==1?new float[][]{MontroseHarbor.COAST}:survey!=null?new float[][]{survey.coast()}:new float[][]{TrainingChannel.WATER_BOUNDARY};
            // The tutorial includes its authored scenery margin; geographic levels retain play bounds.
            float[] bounds=arena?new float[]{-60,-60,course.w+60,course.h+60}:stage==1?PolygonWater.bounds(MontroseHarbor.PLAY_BOUNDS):
                    survey!=null?new float[]{survey.minX(),survey.minY(),survey.maxX(),survey.maxY()}:
                    TrainingScenery.bounds();
            minX=bounds[0];minY=bounds[1];maxX=bounds[2];maxY=bounds[3];
            scale=Math.min((WIDTH-2*EDGE)/(maxY-minY),(HEIGHT-2*EDGE)/(maxX-minX));
            boundary=upright?new float[]{x(minX),y(minY),x(maxX),y(minY),x(maxX),y(maxY),x(minX),y(maxY)}
                    :new float[]{x(minY),y(maxX),x(maxY),y(maxX),x(maxY),y(minX),x(minY),y(minX)};
            water=project(stage==1?new float[][]{MontroseHarbor.COAST}:extent);
            docks=project(stage==1?MontroseHarbor.DOCKS:arena?arenaLand(course):new float[0][]);
            navigableHoles=arena?new float[0]:navigableClip();
        }
        float x(float worldY){return EDGE+(worldY-(upright?minX:minY))*scale;}
        float y(float worldX){return EDGE+(upright?worldX-minY:maxX-worldX)*scale;}
        private float[][] project(float[][] shapes){float[][] result=new float[shapes.length][];
            float[] clip=boundary;
            for(int j=0;j<shapes.length;j++){float[] a=shapes[j],b=new float[a.length];for(int i=0;i<a.length;i+=2){b[i]=x(a[i+(upright?0:1)]);b[i+1]=y(a[i+(upright?1:0)]);}result[j]=PolygonWater.clip(b,clip);}
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
            else if(survey!=null)survey.paintWater(collector,region,0xffffffff);
            else for(float[] triangle:PolygonWater.triangles(TrainingChannel.WATER_BOUNDARY))collector.poly(triangle,0xffffffff);
            int count=0;for(float[] q:pieces)count+=(q.length/2-2)*6;
            float[] triangles=new float[count];int at=0;
            for(float[] q:pieces)for(int i=2;i<q.length-2;i+=2){
                triangles[at++]=x(q[stage==1?0:1]);triangles[at++]=y(q[stage==1?1:0]);
                triangles[at++]=x(q[i+(stage==1?0:1)]);triangles[at++]=y(q[i+(stage==1?1:0)]);
                triangles[at++]=x(q[i+(stage==1?2:3)]);triangles[at++]=y(q[i+(stage==1?3:2)]);
            }
            return triangles;
        }
        public int key(){return 0x6d000a00+stage;}public float width(){return WIDTH;}public float height(){return HEIGHT;}public boolean backgroundSafe(){return true;}
        /** Arena water as polygons: the circuit's channels, or the open river inside its banks. */
        private static float[][] arenaWater(ArenaCourse c){
            if(c instanceof ArenaCourse.Islands){float e=ArenaCourse.Islands.SHORE;return new float[][]{{e,e,c.w-e,e,c.w-e,c.h-e,e,c.h-e}};}
            float[][] out=new float[TagArena.WATER.length][];
            for(int i=0;i<out.length;i++){float[] r=TagArena.WATER[i];out[i]=new float[]{r[0],r[1],r[2],r[1],r[2],r[3],r[0],r[3]};}
            return out;
        }
        /** Arena land inside the water: the circuit's islands, or each granite islet. */
        private static float[][] arenaLand(ArenaCourse c){
            if(c instanceof ArenaCourse.Islands){float[][] isles=ArenaCourse.Islands.ISLES,out=new float[isles.length][];
                for(int i=0;i<isles.length;i++){out[i]=new float[24];for(int k=0;k<12;k++){double t=k*Math.PI/6;out[i][k*2]=isles[i][0]+(float)Math.cos(t)*isles[i][2];out[i][k*2+1]=isles[i][1]+(float)Math.sin(t)*isles[i][2];}}
                return out;}
            float[][] out=new float[TagArena.ISLANDS.length][];
            for(int i=0;i<out.length;i++){float[] r=TagArena.ISLANDS[i];out[i]=new float[]{r[0],r[1],r[2],r[1],r[2],r[3],r[0],r[3]};}
            return out;
        }
        public void paint(Painter p){
            if(arena){
                // Land backing, water polygons, then the islands drawn back over as land.
                p.poly(boundary,0x5096c5ce);
                for(float[] a:water)if(a.length>=6){p.poly(a,0xff0a1a24);outline(p,a,true,0xff96c5ce,1);}
                for(float[] a:docks)if(a.length>=6){p.poly(a,0x9096c5ce);outline(p,a,true,0xff96c5ce,.8f);}
                return;
            }
            // Dark, faint water backing; the stronger cyan overlay distinguishes land/docks.
            p.poly(boundary,0x30061119);
            p.pushClipOutside(navigableHoles);
            try{p.poly(boundary,0x5096c5ce);}
            finally{p.popClip();}
            if(stage==0)scenery(p);
            // Opaque strokes share one final layer fade, keeping joins even.
            outline(p,boundary,true,0xff96c5ce,1);
            for(float[] a:water)outline(p,a,stage!=1,0xff96c5ce,1.15f);
            for(float[] a:docks)outline(p,a,true,0xff7eaab5,.7f);
        }
        private void scenery(Painter p){
            for(int row=IndustrialRoadView.MIN_ROW;row<IndustrialRoadView.MAX_ROW;row++)
                for(int col=IndustrialRoadView.MIN_COL;col<IndustrialRoadView.MAX_COL;col++)
                    if(IndustrialRoadView.roadClear(col,row))
                        p.rect(x(row*64),y((col+1)*64),64*scale,64*scale,0x50708083);
            for(int i=0;i<TrainingChannelView.FACTORY_PLACEMENTS;i++){
                int cell=TrainingChannelView.factoryCell(i);
                float wx=TrainingChannelView.factoryX(i),wy=TrainingChannelView.factoryY(i);
                float hx=FactoryArt.footprintX(cell),hy=FactoryArt.footprintY(cell);
                p.rect(x(wy-hy),y(wx+hx),2*hy*scale,2*hx*scale,0xaa9bb2b5);
            }
        }
        private static void outline(Painter p,float[] a,boolean closed,int color,float width){
            for(int i=2;i<a.length;i+=2)p.line(a[i-2],a[i-1],a[i],a[i+1],width,color);
            if(closed && a.length>=6)p.line(a[a.length-2],a[a.length-1],a[0],a[1],width,color);
        }
    }
}
