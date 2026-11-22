package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Typography specimen rendered through the same shared geometry and cache as gameplay. */
public final class TypographyReview{
 public static void main(String[] args)throws Exception{
  BufferedImage image=new BufferedImage(1100,660,BufferedImage.TYPE_INT_RGB);
  Graphics2D graphics=image.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
  Painter p=new DesktopPainter(graphics);p.rect(0,0,1100,660,PilotGlass.NAVY);
  p.sprite(SpriteAtlas.TITLE_LOGO,0,210,112,360,180,0,1);
  Lettering.draw(p,"TWO TYPOGRAPHY ROLES",440,50,14,PilotGlass.AMBER);
  Lettering.heading(p,"BARGE DISPLAY",440,83,38,PilotGlass.TEXT);
  Lettering.draw(p,"Bold, slanted headings inspired by the title.",440,142,16,PilotGlass.TEXT);
  Lettering.heading(p,"READY TO CAST OFF?",48,236,34,PilotGlass.AMBER);
  Lettering.heading(p,"ABCDEFGHIJKLMNOPQRSTUVWXYZ",48,294,23,PilotGlass.TEXT);
  Lettering.heading(p,"0123456789 / + - : ! ?",48,340,24,PilotGlass.TEXT);
  p.line(48,399,1052,399,1,PilotGlass.TRACK);
  Lettering.draw(p,"READABLE BODY / Barlow Semi Condensed",48,430,20,PilotGlass.AMBER);
  Lettering.draw(p,"Ease forward, turn toward the channel, and release to coast.",48,480,22,PilotGlass.TEXT);
  Lettering.draw(p,"DRAG A LITTLE INTO GREEN. MOVE SLOWLY.",48,530,16,PilotGlass.TEXT);
  Lettering.draw(p,"ABCDEFGHIJKLMNOPQRSTUVWXYZ  abcdefghijklmnopqrstuvwxyz",48,568,16,PilotGlass.TEXT);
  Lettering.draw(p,"0123456789  /  Port  /  Starboard  /  Captain's instructions",48,603,16,PilotGlass.MUTED);
  graphics.dispose();new File("out/ui-type-v3").mkdirs();ImageIO.write(image,"png",new File("out/ui-type-v3/typography.png"));
 }
}
