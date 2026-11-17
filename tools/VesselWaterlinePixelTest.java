package com.bargeboss.game;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
/** Real atlas pixels must disappear below the animated water intersection. */
public final class VesselWaterlinePixelTest {
    public static void main(String[] args) {
        int removed=0,retained=0,checks=0;
        for(int kind=0;kind<3;kind++)for(int heading=0;heading<32;heading++)for(int phase=0;phase<3;phase++) {
            Game g=new Game();g.mode=Game.TITLE;g.visualTicks=phase*35;
            Body b=new Body(kind,kind-1,300,400,heading*(float)Math.PI/16,kind==2);
            b.floatingInitialized=true;b.heave=(phase-1)*1.2f;
            float scale=2,size=2*(b.half+b.radius)/(b.yacht?YachtArt.HULL_FRACTION:kind==0?TugArt.HULL_FRACTION:BargeArt.HULL_FRACTION);
            RiverCamera base=new RiverCamera(0,0,scale),c=new RiverCamera(200-base.screenX(b.x,b.y),230-base.screenY(b.x,b.y),scale);
            BufferedImage[] images={new BufferedImage(400,400,2),new BufferedImage(400,400,2)};
            float[] edge=null;
            for(int pass=0;pass<2;pass++) {
                Graphics2D graphics=images[pass].createGraphics();Painter canvas=new DesktopPainter(graphics),p=new RiverPainter(canvas,c);
                edge=VesselWaterline.boundary(g,p,b,kind==0,size);
                if(pass==1)canvas.pushClipOutside(VesselWaterline.submerged(edge,size*scale));
                SpriteAtlas.draw(p,b.yacht?SpriteAtlas.YACHT:kind==0?SpriteAtlas.TUG:SpriteAtlas.BARGE,b.angle,b.x+RiverCamera.riseX(b.heave),b.y+RiverCamera.riseY(b.heave),size,size,1);
                if(pass==1)canvas.popClip();graphics.dispose();
            }
            for(int i=0;i<edge.length/2-1;i++) {
                int j=i*2;
                for(int x=(int)Math.ceil(edge[j]);x<edge[j+2] && x<400;x++) {
                    if(x<0)continue;
                    float y=edge[j+1]+(edge[j+3]-edge[j+1])*(x+.5f-edge[j])/(edge[j+2]-edge[j]);
                    for(int yy=Math.max(0,(int)Math.ceil(y+2));yy<Math.min(399,y+24);yy++) {
                        checks++;if((images[1].getRGB(x,yy)>>>24)!=0)throw new AssertionError("Submerged hull survived clip");
                        if((images[0].getRGB(x,yy)>>>24)>80)removed++;
                    }
                }
            }
            for(int y=0;y<400;y+=2)for(int x=0;x<400;x+=2)if((images[1].getRGB(x,y)>>>24)>80)retained++;
        }
        if(removed<1000 || retained<10000)throw new AssertionError("Need both submerged and retained production pixels");
        System.out.println(checks+" submerged pixel checks; "+removed+" hidden hull pixels; "+retained+" retained samples across three vessel types, 32 headings and three bob phases");
    }
}
