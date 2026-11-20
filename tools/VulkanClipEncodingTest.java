package com.bargeboss.game;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/** Host-only encoder check; no Android runtime or Vulkan driver is invoked. */
public final class VulkanClipEncodingTest {
    private static Object field(VulkanPainter p,String name)throws Exception{Field f=VulkanPainter.class.getDeclaredField(name);f.setAccessible(true);return f.get(p);}
    public static void main(String[] args)throws Exception{
        Constructor<VulkanPainter> ctor=VulkanPainter.class.getDeclaredConstructor(CanvasPainter.class);ctor.setAccessible(true);
        VulkanPainter p=ctor.newInstance((Object)null);p.start(960,600);
        float[] outer={0,0,100,0,0,100},inner={10,10,20,10,10,20};
        p.pushClipOutside(outer);p.rect(0,0,100,100,0xffffffff);
        p.pushClipOutside(inner);p.rect(10,10,20,20,0xffffffff);
        java.util.Arrays.fill(outer,999);java.util.Arrays.fill(inner,999);
        int used=(Integer)field(p,"used");p.popClip();p.popClip();
        if((Integer)field(p,"used")!=used)throw new AssertionError("clip restoration duplicated captured vertices");
        int[] commands=(int[])field(p,"commands");float[] vertices=(float[])field(p,"vertices");
        int count=(Integer)field(p,"commandUsed"),depth=0,restores=0;int[] first=new int[64],sizes=new int[64];
        for(int i=0;i<count;i+=5){int mode=commands[i+4];
            if(mode==1){first[depth]=commands[i];sizes[depth++]=commands[i+1];if(vertices[commands[i]]==999)throw new AssertionError("caller changed captured mask");}
            if(mode==2){--depth;if(commands[i]!=first[depth] || commands[i+1]!=sizes[depth])throw new AssertionError("nested restore used another mask");restores++;}
        }
        if(depth!=0 || restores!=2)throw new AssertionError("unbalanced nested masks");
        boolean rejected=false;try{p.popClip();}catch(IllegalStateException expected){rejected=true;}if(!rejected)throw new AssertionError("underflow accepted");
        p.start(960,600);for(int i=0;i<64;i++)p.pushClipOutside(new float[0]);
        rejected=false;try{p.pushClipOutside(new float[0]);}catch(IllegalStateException expected){rejected=true;}if(!rejected)throw new AssertionError("nesting limit lost");
        for(int i=0;i<64;i++)p.popClip();
        System.out.println("Vulkan clip snapshot, nested ranges, zero-copy restore and depth limits passed");
    }
}
