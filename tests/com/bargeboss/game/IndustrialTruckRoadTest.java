package com.bargeboss.game;

/** Static dock trucks occupy whole dry paved footprints, with factory clearance. */
public final class IndustrialTruckRoadTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void run(){
        int trucks=0;
        int width=IndustrialRoadView.MAX_COL-IndustrialRoadView.MIN_COL,height=IndustrialRoadView.MAX_ROW-IndustrialRoadView.MIN_ROW;
        boolean[] reached=new boolean[width*height];int[] queue=new int[reached.length];int head=0,tail=0,total=0;
        for(int row=0;row<height;row++)for(int col=0;col<width;col++)if(IndustrialRoadView.roadClear(col+IndustrialRoadView.MIN_COL,row+IndustrialRoadView.MIN_ROW)){
            total++;if(tail==0){queue[tail++]=row*width+col;reached[row*width+col]=true;}
        }
        while(head<tail){int at=queue[head++];for(int dir=0;dir<4;dir++){
            int n=dir==0?at-1:dir==1?at+1:dir==2?at-width:at+width;
            if(n<0 || n>=reached.length || (dir<2 && n/width!=at/width) || reached[n]
                    || !IndustrialRoadView.roadClear(n%width+IndustrialRoadView.MIN_COL,n/width+IndustrialRoadView.MIN_ROW))continue;
            reached[n]=true;queue[tail++]=n;
        }}
        check(total>0 && tail==total,"every paved road tile belongs to one contiguous network");
        check(TrainingYardCrew.count()>=8 && TrainingYardCrew.count()<=24,"bounded active crews serve the workshop district");
        float[] worker=new float[2];
        for(int id=0;id<TrainingYardCrew.count();id++){
            TrainingYardCrew.position(id,worker);
            check(DockWorkers.dry(worker[0],worker[1],22),"entire work site stays on dry land");
            check(!IndustrialRoadView.roadClear((int)StrictMath.floor(worker[0]/64),(int)StrictMath.floor(worker[1]/64)),"crew works outside traffic lanes");
            float low=1,high=0;for(int tick=0;tick<360;tick+=7){float v=TrainingYardCrew.motion(id,tick);low=Math.min(low,v);high=Math.max(high,v);}
            check(high-low>.5f,"crew alternates active work with inspection pauses");
        }
        for(int i=0;i<CalumetArt.count();i++){
            float[] v=CalumetArt.item(i);if((int)v[0]!=SpriteAtlas.SHORE_TRUCKS)continue;trucks++;
            check(Math.floorMod((int)StrictMath.floor(v[3]/64),5)==4,"truck artwork follows horizontal service lane");
            check(IndustrialRoadView.roadFootprint(v[2],v[3],58,22),"whole truck wheelbase lies on connected road cells");
            for(int dx=-58;dx<=58;dx++)for(int dy=-22;dy<=22;dy++)check(!CalumetMap.contains(v[2]+dx,v[3]+dy),"truck body footprint stays on dry land");
            for(int f=0;f<TrainingChannelView.FACTORY_PLACEMENTS;f++){
                int frame=TrainingChannelView.factoryCell(f);float hx=FactoryArt.footprintX(frame),hy=FactoryArt.footprintY(frame),x=TrainingChannelView.factoryX(f),y=TrainingChannelView.factoryY(f);
                check(x-hx>=v[2]+82 || x+hx<=v[2]-82 || y-hy>=v[3]+46 || y+hy<=v[3]-46,"factory footprint preserves truck body and access apron");
            }
            for(int j=0;j<CalumetArt.count();j++){float[] other=CalumetArt.item(j);if((int)other[0]==SpriteAtlas.STEELWORKS)check(v[2]-82>=other[2]+100 || v[2]+82<=other[2]-100 || v[3]-46>=other[3]+85 || v[3]+46<=other[3]-85,"truck clears fixed steelworks footprint");}
            check(!CalumetArt.clearFactory(v[2]-8,v[3]-8,16,16),"placement rejects factory over parked truck");
        }
        check(trucks==3,"all three authored truck views remain placed");
        check(YardActivity.count()>=8,"unoccupied service lanes retain moving activity");
        float[] at=new float[3],next=new float[3];
        for(int route=0;route<YardActivity.count();route++)for(int tick=0;tick<3200;tick+=11){
            YardActivity.position(route,tick,at);YardActivity.position(route,tick+1,next);
            check(Physics.length(next[0]-at[0],next[1]-at[1])<.32f,"route motion remains continuous through travel, stops and reversals");
            check(IndustrialRoadView.roadFootprint(at[0],at[1],17,17),"moving equipment stays entirely on pavement");
            for(int i=0;i<CalumetArt.count();i++){float[] v=CalumetArt.item(i);if((int)v[0]!=SpriteAtlas.SHORE_TRUCKS)continue;
                check(at[0]-17>=v[2]+82 || at[0]+17<=v[2]-82 || at[1]-8>=v[3]+46 || at[1]+8<=v[3]-46,"moving hauler footprint never enters parked truck access apron");
            }
        }
        Game g=new Game();g.reset();g.configureCanvas(1333,750);g.layout=Layout.focused(1333,750,700,600,1.1f,666,375);
        Capture a=new Capture();IndustrialRoadView.draw(g,a);check(a.surfaces>0 && a.patches>0 && a.cracks>0,"visible roads include material faces, repairs and cracks");
        Capture b=new Capture();IndustrialRoadView.draw(g,b);check(a.hash==b.hash && a.surfaces==b.surfaces,"road material geometry remains deterministic between draws");
        System.out.println(checks+" industrial truck footprint, factory clearance and rugged road assertions passed");
    }
    private static final class Capture implements Painter {
        int surfaces,patches,cracks;long hash=1;
        public void surfacePolygon(float[] xy,float[] uv,int material,int color,float wet){surfaces++;if(wet>.2f)patches++;check(material==SurfaceMaterial.CONCRETE,"roads use shared rough material");for(float v:xy){check(Float.isFinite(v),"finite projected road face");hash=31*hash+Float.floatToIntBits(v);}for(float v:uv)hash=31*hash+Float.floatToIntBits(v);hash=31*hash+color;}
        public void line(float x,float y,float xx,float yy,float w,int c){if(c==0xff171e20)cracks++;}
        public void poly(float[] p,int c){}public void rect(float x,float y,float w,float h,int c){}public void circle(float x,float y,float r,int c){}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int b,float x,float y,float w,float h,float r,float blend,float alpha){}
    }
    public static void main(String[] args){run();}
}
