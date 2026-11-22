package com.bargeboss.game;
import java.lang.reflect.*;import java.util.*;
/** Golden full-precision commands captured from c943ea9's 64-byte encoder.
 * Runs on the host against android.jar; no Android API or native renderer executes.
 * Covers buffer growth, mixed vertex strides, frame reuse, nested masks and retained relocation. */
public class VulkanEncodingTest {
 static Object read(Object p,String name)throws Exception{Field f=p.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(p);}
 static Painter make(Class<?> c)throws Exception{Constructor<?> ctor=c.getDeclaredConstructor(CanvasPainter.class);ctor.setAccessible(true);Object p=ctor.newInstance(new Object[]{null});Field f=c.getDeclaredField("recording");f.setAccessible(true);f.setBoolean(p,false);return (Painter)p;}
 static void start(Painter p)throws Exception{Method m=p.getClass().getDeclaredMethod("start",float.class,float.class);m.setAccessible(true);m.invoke(p,960f,600f);}
 static ArrayList<Integer> stream(Painter p)throws Exception{
  float[] v=(float[])read(p,"vertices");int[] cmds=(int[])read(p,"commands");int count=(Integer)read(p,"commandUsed");ArrayList<Integer> result=new ArrayList<>();
  for(int i=0;i<count;i+=5){int mode=cmds[i+4],first=cmds[i],n=cmds[i+1];
   if(mode==3 || mode==4){result.add(mode);continue;}
   for(int j=0;j<n;j++){result.add(mode==5?0:mode);for(int k=0;k<16;k++){float x=mode==5?(k<2?v[first+j*6+k]:k>=4&&k<8?v[first+j*6+k-2]:0):v[first+j*16+k];result.add(Float.floatToIntBits(x));}}
  }return result;
 }
 static void draw(Painter p,int frame){
  float[] clip={20,20,900,20,20,550};p.pushClipOutside(clip);
  for(int i=0;i<6000;i++){float x=(i*19+frame*13)%800,y=i%420;int c=(37+i%219)<<24|0x9381ad;
   p.poly(new float[]{x,y,x+16,y+3,x+19,y+12,x-2,y+14},c);
   if(i%7==0){p.material(x,y,12,2,1,15,14,.2f,.3f,c);p.circle(x+7,y+7,3,c);}
   if(i%17==0){p.pushClipOutside(new float[]{x,y,x+5,y,x,y+8});p.poly(new float[]{x,y,x+7,y+1,x+4,y+4},c);p.popClip();}
  }
  p.popClip();
  StaticLayer layer=new StaticLayer(){public int key(){return 123;}public float width(){return 40;}public float height(){return 40;}public void paint(Painter q){q.pushClipOutside(new float[]{0,0,2,0,0,2});q.poly(new float[]{0,0,20,0,20,20,0,20},0x89338172);q.popClip();}};
  p.retainedSurfaces(layer,30+frame,40,2);p.rect(2,3,4,5,0xff987654);
 }
 static void thinPolygons()throws Exception{
  Painter painter=make(VulkanPainter.class);int checked=0;
  for(int direction:new int[]{-1,1})for(int step=40;step<=400;step++){
   float scale=step*.0025f,x=850.125f,y=520.375f;
   float[] q={x,y,x+20*scale,y+2*scale,x+20*scale,y+(2+direction*.12f)*scale,x,y+direction*.12f*scale};
   start(painter);painter.poly(q,0x5083b3b4);
   if((Integer)read(painter,"used")!=36)throw new AssertionError("Thin ripple lost triangles at scale "+scale+" winding "+direction);
   ArrayList<Integer> expected=stream(painter);
   start(painter);painter.convexQuad(q[0],q[1],q[2],q[3],q[4],q[5],q[6],q[7],0x5083b3b4);
   if(!expected.equals(stream(painter)))throw new AssertionError("Direct quad changes ripple vertices at scale "+scale);
   checked++;
  }
  System.out.println(checked+" thin ripple zoom/winding cases retain both triangles");
 }
 public static void main(String[] args)throws Exception{
  thinPolygons();
  String[] expected={"daacc2aa3615cefaec1fc3b98118e8e00be33629cc243ab31be1b46f8a4dc2ea",
   "0e7092c8057cd60ff2f1dfcca91800fdfef50a57f3e843b34ba7b89497a67857",
   "06c5caf79fd850d4d1ae7db836be455780c0676f0c8aa98ec3d839e47eee0276"};
  Painter painter=make(VulkanPainter.class);long checked=0;
  for(int frame=0;frame<expected.length;frame++){
   start(painter);draw(painter,frame);ArrayList<Integer> fields=stream(painter);
   java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
   for(int v:fields){digest.update((byte)(v>>>24));digest.update((byte)(v>>>16));digest.update((byte)(v>>>8));digest.update((byte)v);}
   StringBuilder hash=new StringBuilder();for(byte b:digest.digest())hash.append(String.format("%02x",b&255));
   if(!expected[frame].equals(hash.toString()))throw new AssertionError("Encoded drawing differs on frame "+frame);
   if(((float[])read(painter,"vertices")).length<=262144)throw new AssertionError("Buffer growth not exercised");
   checked+=fields.size();
  }
  System.out.println(checked+" Vulkan vertex fields/clip markers match original encoding");
 }
}
