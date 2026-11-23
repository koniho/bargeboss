package com.bargeboss.game;
import java.awt.*;import java.awt.image.*;
public final class VertexShadePixelTest {
 public static void main(String[] args){
  BufferedImage image=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
  Painter p=new DesktopPainter(g);p.shadedTriangle(0,0,0xffff0000,120,0,0xff00ff00,0,120,0xff0000ff);
  int c=image.getRGB(39,39);if(Math.abs((c>>16&255)-87)>2||Math.abs((c>>8&255)-84)>2||Math.abs((c&255)-84)>2)throw new AssertionError("linear barycentric reference color");
  g.setComposite(AlphaComposite.Clear);g.fillRect(0,0,128,128);g.setComposite(AlphaComposite.SrcOver);
  p.shadedTriangle(0,0,0x00ff0000,120,0,0xff0000ff,0,120,0xff0000ff);c=image.getRGB(39,39);
  if((c>>16&255)!=0 || (c&255)!=255 || Math.abs((c>>>24)-168)>2)throw new AssertionError("transparent red cannot bleed into premultiplied blue");g.dispose();
  System.out.println("Vertex gradient reference pixels and transparent-edge interpolation passed");
 }
}
