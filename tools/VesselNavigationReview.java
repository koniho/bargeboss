package com.bargeboss.game;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Actual sprite/fixture renderer, with all 32 headings and source-alpha registration. */
public final class VesselNavigationReview {
    private static void registration(boolean tug)throws Exception {
        int atlas=tug?SpriteAtlas.TUG:SpriteAtlas.BARGE;
        BufferedImage source=ImageIO.read(new File("assets/sprites/"+SpriteAtlas.file(atlas,0)));int checks=0;
        for(int f=0;f<32;f++){
            Body body=new Body(tug?0:1,tug?-1:2,300,500,(f-RiverCamera.DIRECTION_OFFSET)*(float)(StrictMath.PI/16));
            float size=2*(body.half+body.radius)/(tug?TugArt.HULL_FRACTION:BargeArt.HULL_FRACTION),spriteScale=tug?TugArt.SCALE:BargeArt.HULL_SCALE;
            for(int end=tug?1:-1;end<=1;end+=2)for(int index=0;index<(tug?3:4);index++){
                int k=index==3?VesselNavigationLights.SPECIAL:index;
                float dx=end*(VesselNavigationLights.worldX(body,k)-body.x),dy=end*(VesselNavigationLights.worldY(body,k)-body.y);
                float u=(tug?TugArt.pivotX(f):BargeArt.pivotX(false,f))+RiverCamera.Q*(dx+dy)/(size*spriteScale);
                float v=(tug?TugArt.pivotY(f):BargeArt.pivotY(false,f))+(RiverCamera.Q*RiverCamera.DEPTH*(dy-dx)-VesselNavigationLights.height(k)*.81649658f)/(size*spriteScale);
                int l=Math.round(source.getWidth()*SpriteAtlas.sourceLeft(atlas,f)),t=Math.round(source.getHeight()*SpriteAtlas.sourceTop(atlas,f));
                int r=Math.round(source.getWidth()*SpriteAtlas.sourceRight(atlas,f)),b=Math.round(source.getHeight()*SpriteAtlas.sourceBottom(atlas,f));
                int x=l+Math.round(u*(r-l)),y=t+Math.round(v*(b-t));
                if(x<l || x>=r || y<t || y>=b || (source.getRGB(x,y)>>>24)<128)throw new AssertionError("Navigation housing floats outside "+(tug?"tug":"barge")+" hull at frame "+f+" fixture "+k+" pixel "+x+","+y);
                checks++;
            }
        }
        System.out.println(checks+" "+(tug?"tug":"barge")+" directional navigation fittings land on opaque production hull pixels");
    }
    private static void sheet(boolean tug)throws Exception {
        BufferedImage out=new BufferedImage(1920,800,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=out.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Painter p=new DesktopPainter(graphics);p.rect(0,0,1920,800,0xff061d27);Game g=new Game();g.visualTicks=155;
        for(int f=0;f<32;f++){
            int x=f%8*240,y=f/8*200;float scale=2.8f;
            RiverCamera origin=new RiverCamera(0,0,scale);RiverCamera c=new RiverCamera(x+120-origin.screenX(300,500),y+130-origin.screenY(300,500),scale);
            Body b=new Body(tug?0:3,tug?-1:2,300,500,f*(float)(StrictMath.PI/16));b.floatingInitialized=true;
            VesselView.draw(g,new RiverPainter(p,c),b,tug,false);
            Lettering.center(p,"HEADING "+Math.round(f*11.25f),x+120,y+180,10,VisualStyle.CREAM);
        }
        graphics.dispose();ImageIO.write(out,"png",new File("out/vessel-navigation-"+(tug?"tugs":"barges")+".png"));
    }
    private static void turns()throws Exception {
        File directory=new File("out/animation-frames/vessel-navigation-turns");directory.mkdirs();
        Game game=new Game();
        for(int frame=0;frame<128;frame++) {
            BufferedImage out=new BufferedImage(720,360,BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics=out.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Painter painter=new DesktopPainter(graphics);painter.rect(0,0,720,360,0xff061d27);game.visualTicks=frame*3;
            for(int craft=0;craft<2;craft++) {
                float heading=frame*(float)(StrictMath.PI/64),scale=3.5f;
                RiverCamera origin=new RiverCamera(0,0,scale);
                RiverCamera camera=new RiverCamera(180+craft*360-origin.screenX(300,500),235-origin.screenY(300,500),scale);
                Body body=new Body(craft==0?0:3,craft==0?-1:2,300,500,heading);body.floatingInitialized=true;body.engineLoad=.35f;
                VesselView.draw(game,new RiverPainter(painter,camera),body,craft==0,false);
            }
            graphics.dispose();ImageIO.write(out,"png",new File(directory,String.format("%04d.png",frame)));
        }
    }
    public static void main(String[] args)throws Exception {registration(true);registration(false);sheet(true);sheet(false);if(args.length>0 && args[0].equals("--turns"))turns();System.out.println("Vessel navigation sheets exported to out/");}
}
