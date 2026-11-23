package com.bargeboss.game;
/** Continuous vessel transforms, bounded preparation, water clipping and LOD routes. */
public final class LiveHarborBoatsTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void run(){
        Game g=ReplayInput.montroseJob(Challenges.HAULOUT);g.dialogue.close(g);HarborBoatModel.prepare();
        g.layout=Layout.focused(1333,600,0,0,3.1f,666,300);g.tug.x=g.tug.y=0;
        check(MooredBoats.liveWeight(g,47,0,0)==1,"near large vessels use geometry only");
        check(MooredBoats.liveWeight(g,47,400,0)==0,"distant vessels retain sprite path");
        g.layout=Layout.focused(1333,600,0,0,.5f,666,300);
        check(MooredBoats.liveWeight(g,47,0,0)==0,"wide view keeps sprite rendering");
        g.layout=Layout.focused(1333,600,0,0,3.1f,666,300);
        for(int kind=0;kind<6;kind++){
            ArticulatedModel.Rigid model=HarborBoatModel.get(kind).rigid;int prepared=model.fragmentCount();
            check(prepared>0 && prepared<1000,"bounded prepared mesh");
            long previous=0;
            for(int i=0;i<=128;i++){
                Probe p=new Probe();float angle=i*(float)StrictMath.PI/64;
                model.draw(g,p,0,0,0,angle,.012f,.02f,(a,b,x,y,z,nx,ny,nz)->{
                    check(Float.isFinite(x+y+z+nx+ny+nz),"finite transformed surfaces");
                    check(Math.abs(nx*nx+ny*ny+nz*nz-1)<.002f,"rigid transform preserves normals");return b;});
                check(p.faces>15,"hull present at every heading");
                check(i==0 || p.hash!=previous,"live geometry never holds a sampled heading");previous=p.hash;
                check(model.fragmentCount()==prepared,"rotation never grows geometry cache");
                if(kind>=3)check(p.fabric==0,"sailboats have no bimini faces");
                else check(p.fabric>0,"motorboats retain colored canopies");
            }
        }
        int tick=g.ticks;float x=g.mooredFleet.boats[0].x;
        int frame=Parity.frame(g);check(frame==Parity.frame(g) && g.ticks==tick && g.mooredFleet.boats[0].x==x,"render does not advance fleet");
        System.out.println("774 continuous live boat headings, normal lengths, sailboat canopy and bounded LOD checks passed");
    }
    private static final class Probe implements Painter {
        long hash=1;int faces,fabric,boatSprites;
        public void poly(float[] xy,int c){faces++;if(c==HarborBoatModel.FABRIC)fabric++;for(float v:xy){check(Float.isFinite(v),"finite projection");hash=hash*31+Float.floatToIntBits(v);}}
        public void rect(float x,float y,float w,float h,int c){}
        public void circle(float x,float y,float r,int c){}
        public void line(float x,float y,float xx,float yy,float w,int c){}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){if(a==SpriteAtlas.MOORED_BOATS)boatSprites++;}
        public void spriteBlend(int a,int f,int b,float x,float y,float w,float h,float r,float mix,float alpha){}
    }
    public static void main(String[] args){run();}
}
