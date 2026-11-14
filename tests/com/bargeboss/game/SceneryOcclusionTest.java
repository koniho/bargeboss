package com.bargeboss.game;

/** Regression cases for proximity, transparent padding and actual drawing order. */
public final class SceneryOcclusionTest {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static float rectangle(Painter p,float l,float t,float r,float b) {
        return SceneryOcclusion.polygonOpacity(p,new float[]{l,t,r,t,r,b,l,b});
    }
    public static void run() {
        for(int atlas=0;atlas<=4;atlas++)for(int frame=0;frame<32;frame++) {
            SceneryOcclusion p=new SceneryOcclusion(new Parity());
            check(rectangle(p,0,0,1000,1000)==1,"scenery painted before vessels stays opaque");
            float w=120*SpriteAtlas.widthScale(atlas,frame),h=120*SpriteAtlas.heightScale(atlas,frame);
            float l=300-SpriteAtlas.pivotX(atlas,frame)*w,t=300-SpriteAtlas.pivotY(atlas,frame)*h;
            p.sprite(atlas,frame,300,300,120,120,0,1);
            check(rectangle(p,l-20,t,l-.01f,t+h)==1,"nearby scenery with a positive gap never fades at any heading");
            check(rectangle(p,l,t+h+.01f,l+w,t+h+20)==1,"no oversized vertical protection zone");
            check(rectangle(p,l,t,l+w,t+h)<=.29f,"foreground covering the painted vessel fades at every heading");
            long[] rows=OcclusionMasks.rows(atlas)[frame];int empty=-1,solid=-1;
            for(int y=2;y<62;y++)for(int x=2;x<62;x++) {
                if((rows[y]&(1L<<x))==0 && empty<0)empty=y*64+x;
                if((rows[y]&(1L<<x))!=0 && solid<0)solid=y*64+x;
            }
            check(empty>=0 && solid>=0,"sprite has both transparent padding and opaque hull/cargo");
            float ex=l+(empty%64+.5f)*w/64,ey=t+(empty/64+.5f)*h/64;
            check(rectangle(p,ex-.1f,ey-.1f,ex+.1f,ey+.1f)==1,"transparent vessel padding cannot hide a boat");
            // A diagonal empty triangle shares the vessel's broad bounding rectangle.
            check(SceneryOcclusion.polygonOpacity(p,new float[]{l-30,t-30,l-20,t+h,l-10,t-30})==1,"polygon bounds alone do not trigger fading");
        }
        SceneryOcclusion p=new SceneryOcclusion(new Parity());
        p.circle(300,300,30,0xffffffff);p.line(250,300,350,300,10,0xffffffff);
        p.sprite(SpriteAtlas.FACTORY,0,300,300,200,200,0,1);
        check(rectangle(p,200,200,400,400)==1,"wake, ropes, lights and scenery are not registered as watercraft");
        for(int frame=0;frame<32;frame++)for(float blend:new float[]{0,.25f,.5f,.75f,1}) {
            p=new SceneryOcclusion(new Parity());
            p.spriteBlend(SpriteAtlas.TUG,frame,(frame+1)%32,300,300,100,100,0,blend,1);
            check(rectangle(p,150,150,450,450)<.3f,"both visible blend frames retain obstruction coverage");
            check(rectangle(p,100,100,110,110)==1,"blending creates no distant protection radius");
        }
        // Scenery has its own transparent padding: occupying the same image rectangle
        // does not imply its pixels cover the vessel. Search a clear corner and a solid region.
        boolean clear=false,blocked=false;
        for(int y=80;y<360;y+=20)for(int x=80;x<360;x+=20) {
            p=new SceneryOcclusion(new Parity());p.sprite(SpriteAtlas.TUG,0,x,y,18,18,0,1);
            float alpha=SceneryOcclusion.spriteOpacity(p,SpriteAtlas.FACTORY,3,240,340,300,300);
            float l=240-FactoryArt.pivotX(3)*300,t=340-FactoryArt.pivotY(3)*300;
            if(x>l+20 && x<l+280 && y>t+20 && y<t+280){clear|=alpha==1;blocked|=alpha<.5f;}
        }
        check(clear && blocked,"factory transparent corners stay opaque while real building overlap fades");
        System.out.println(checks+" actual silhouette, foreground order and no-proximity fading assertions passed");
    }
    public static void main(String[] args){run();}
}
