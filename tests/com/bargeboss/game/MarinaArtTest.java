package com.bargeboss.game;

/** Marina trees keep their registered root while wind bends their upright image. */
public final class MarinaArtTest {
    private static int assertions;
    private static void yes(boolean value){assertions++;if(!value)throw new AssertionError("Marina art check "+assertions);}
    private static final class Capture implements Painter {
        int sprites,affines;float x,y,ax,ay,bx,by;
        public void spriteAffine(int atlas,int frame,float x,float y,float ax,float ay,float bx,float by,float alpha){affines++;this.x=x;this.y=y;this.ax=ax;this.ay=ay;this.bx=bx;this.by=by;}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){sprites++;}
        public void spriteBlend(int a,int f,int h,float x,float y,float w,float ht,float r,float b,float alpha){}
        public void rect(float x,float y,float w,float h,int c){}
        public void circle(float x,float y,float r,int c){}
        public void poly(float[] p,int c){}
        public void line(float x,float y,float a,float b,float w,int c){}
    }
    public static int run(){assertions=0;Game g=new Game();RiverCamera c=new RiverCamera(20,35,1.7f);Capture p=new Capture();
        for(int frame=0;frame<8;frame++){
            yes(MarinaArt.pivotX(frame)>0 && MarinaArt.pivotX(frame)<1);
            yes(MarinaArt.pivotY(frame)>0 && MarinaArt.pivotY(frame)<1);
            yes((MarinaArt.height(frame,100)>0)==(frame>=4));
            for(int ticks=0;ticks<3600;ticks+=7){g.visualTicks=ticks;int beforeSprites=p.sprites,beforeAffines=p.affines;
                MarinaArt.drawAnimated(g,p,c,frame,120,230,140,19);
                yes(g.visualTicks==ticks);yes(g.ticks==0);
                if(MarinaArt.tree(frame)){yes(p.affines==beforeAffines+1 && p.sprites==beforeSprites);yes(p.x==c.screenX(120,230) && p.y==c.screenY(120,230));yes(p.ay==0 && p.ax==p.by && p.by==140*c.scale);yes(Math.abs(p.bx)<=.028f*140*c.scale);float old=p.bx;MarinaArt.drawAnimated(g,p,c,frame,120,230,140,19);yes(old==p.bx);}
                else yes(p.sprites==beforeSprites+1 && p.affines==beforeAffines);
            }
        }
        yes(MarinaArt.sway(91,4,2)!=MarinaArt.sway(91,4,3));yes(MarinaArt.height(4,100)>MarinaArt.height(5,100));
        return assertions;
    }
    public static void main(String[] args){System.out.println("MarinaArt: "+run()+" deterministic root/sway/height assertions");}
}
