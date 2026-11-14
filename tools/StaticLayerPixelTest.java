package com.bargeboss.game;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Map;

/** Native cache pixels, warming, reuse and bounded LRU retention without production art. */
public final class StaticLayerPixelTest {
    private static int checks;
    private static void check(boolean ok,String message) {checks++;if(!ok) throw new AssertionError(message);}
    private static final class TestLayer extends StaticLayer {
        final int id;final float w,h;int paints;
        TestLayer(int id,float width,float height) {this.id=id;w=width;h=height;}
        public int key() {return id;}
        public float width() {return w;}
        public float height() {return h;}
        public void paint(Painter p) {
            paints++;p.rect(2,2,18,13,0xffdb8436);p.circle(16,17,8,0x8861aabb);
            p.poly(new float[]{5,26,14,19,29,26},0xff426876);p.line(2,31,30,33,1.5f,0xffe9d5ad);
            p.rect(-5,8,12,9,0xff843626);p.circle(w-2,h-2,7,0x88e0d8b8);
        }
    }
    private static Graphics2D graphics(BufferedImage image) {
        Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);return g;
    }
    private static Object cache() throws Exception {
        Field field=DesktopPainter.class.getDeclaredField("LAYERS");field.setAccessible(true);return ((ThreadLocal<?>)field.get(null)).get();
    }
    @SuppressWarnings("unchecked") private static Map<Long,BufferedImage> images() throws Exception {
        Field field=cache().getClass().getDeclaredField("images");field.setAccessible(true);return (Map<Long,BufferedImage>)field.get(cache());
    }
    private static long bytes() throws Exception {
        Field field=cache().getClass().getDeclaredField("bytes");field.setAccessible(true);return field.getLong(cache());
    }
    private static void clear() throws Exception {
        for(BufferedImage image:images().values()) image.flush();images().clear();
        Field field=cache().getClass().getDeclaredField("bytes");field.setAccessible(true);field.setLong(cache(),0);
    }
    private static BufferedImage cached(TestLayer layer,float scale,float alpha) {
        BufferedImage image=new BufferedImage(192,192,BufferedImage.TYPE_INT_ARGB_PRE);Graphics2D g=graphics(image);
        DesktopPainter p=new DesktopPainter(g);p.beginFrame();p.layer(layer,20,16,scale,alpha);g.dispose();return image;
    }
    private static void pixels() throws Exception {
        clear();TestLayer layer=new TestLayer(1101,36,36);
        for(float scale:new float[]{StaticLayer.rasterScale(1),StaticLayer.rasterScale(4)}) {
            if(StaticLayer.rasterScale(scale)!=scale) continue;
            BufferedImage expected=new BufferedImage(192,192,BufferedImage.TYPE_INT_ARGB_PRE);Graphics2D g=graphics(expected);
            g.translate(20,16);g.scale(scale,scale);g.clip(new java.awt.geom.Rectangle2D.Float(0,0,layer.width(),layer.height()));layer.paint(new DesktopPainter(g));g.dispose();
            BufferedImage actual=cached(layer,scale,1);
            for(int y=0;y<192;y++) for(int x=0;x<192;x++) check(expected.getRGB(x,y)==actual.getRGB(x,y),"integer-density cache matches direct primitive pixels");
        }
        BufferedImage opaque=cached(layer,2,1),expected=new BufferedImage(192,192,BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g=expected.createGraphics();g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,.5f));g.drawImage(opaque,0,0,null);g.dispose();
        BufferedImage faded=cached(layer,2,.5f);
        for(int y=0;y<192;y++) for(int x=0;x<192;x++) check(expected.getRGB(x,y)==faded.getRGB(x,y),"cached layer alpha composites once over overlapping primitives");
    }
    private static void coldFallback() throws Exception {
        float scale=StaticLayer.rasterScale(4);
        for(float alpha:new float[]{1,.5f}) {
            clear();TestLayer layer=new TestLayer(1151,36,36);
            BufferedImage expected=cached(layer,scale,alpha);
            clear();BufferedImage actual=new BufferedImage(192,192,BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g=graphics(actual);DesktopPainter painter=new DesktopPainter(g);painter.beginFrame();
            painter.layer(new TestLayer(1152,36,36),150,150,1,1);
            // Remove the warming layer's pixels while leaving its cache entry.
            g.setComposite(AlphaComposite.Clear);g.fillRect(0,0,192,192);g.setComposite(AlphaComposite.SrcOver);
            painter.layer(layer,20,16,scale,alpha);g.dispose();
            check(images().size()==1,"throttled fallback never adds an extra retained bitmap");
            for(int y=0;y<192;y++) for(int x=0;x<192;x++)
                check(expected.getRGB(x,y)==actual.getRGB(x,y),"cold and warm layers share clipping and whole-group opacity");
        }
    }
    private static void warming() throws Exception {
        clear();TestLayer a=new TestLayer(1201,36,36),b=new TestLayer(1202,36,36);
        BufferedImage output=new BufferedImage(192,192,BufferedImage.TYPE_INT_ARGB_PRE);Graphics2D g=graphics(output);
        DesktopPainter painter=new DesktopPainter(g);painter.beginFrame();
        painter.layer(a,10,10,1,1);painter.layer(b,50,10,1,1);
        check(images().size()==1 && a.paints==1 && b.paints==1,"one visible miss warms while later misses draw directly");
        painter=new DesktopPainter(g);painter.beginFrame();
        painter.layer(a,12,15,1.1f,.7f);painter.layer(b,50,10,1,1);
        check(a.paints==1 && b.paints==2 && images().size()==2,"new painter instance reuses prior frame cache through pan zoom and fade");
        painter.beginFrame();painter.layer(new TestLayer(1203,36,36),500,500,1,1);
        check(images().size()==2,"invisible misses do not allocate or consume cache space");
        TestLayer huge=new TestLayer(1204,5000,5000);painter.layer(huge,0,0,1,1);
        check(huge.paints==1 && images().size()==2 && bytes()<=StaticLayer.cacheBudgetBytes(),"oversized images use primitives without cache allocation");
        g.dispose();
    }
    private static void eviction() throws Exception {
        clear();BufferedImage output=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB_PRE);Graphics2D g=graphics(output);
        DesktopPainter p=new DesktopPainter(g);TestLayer[] layers=new TestLayer[20];
        int pixels=(int)StrictMath.ceil(512*StaticLayer.rasterScale(1));
        int count=StaticLayer.cacheBudgetBytes()/(pixels*pixels*4);
        check(count>=2 && count+1<layers.length,"fixture spans the shared cache budget");
        for(int i=0;i<count;i++) {
            layers[i]=new TestLayer(1300+i,512,512);p.beginFrame();p.layer(layers[i],0,0,1,1);
            check(bytes()<=StaticLayer.cacheBudgetBytes(),"cache bytes remain bounded while warming");
        }
        p.beginFrame();p.layer(layers[0],0,0,1,1);
        layers[count]=new TestLayer(1300+count,512,512);p.layer(layers[count],0,0,1,1);
        check(images().size()==count && bytes()<=StaticLayer.cacheBudgetBytes(),"new raster evicts before exceeding pixel budget");
        p.beginFrame();p.layer(layers[0],0,0,1,1);check(layers[0].paints==1,"recently touched first entry survives LRU eviction");
        p.layer(layers[1],0,0,1,1);check(layers[1].paints==2,"least recently used entry is rebuilt on demand");
        g.dispose();clear();
    }
    public static void main(String[] args) throws Exception {
        pixels();coldFallback();warming();eviction();System.out.println(checks+" static layer cache pixel, warming, reuse and LRU assertions passed");
    }
}
