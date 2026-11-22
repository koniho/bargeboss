package com.bargeboss.game;

/** Shared cached typography: readable upright body and title-inspired arcade headings. */
public final class Lettering {
    private Lettering() {}
    public static float width(String text,float size){return width(text,size,false);}
    public static float headingWidth(String text,float size){return width(text,size,true);}
    private static float width(String text,float size,boolean heading){
        float width=0;for(int n=0;n<text.length();n++)width+=advance(text.charAt(n),heading);
        return Math.max(0,width-(text.isEmpty()?0:.055f))*size;
    }
    private static int glyph(char ch,boolean heading){return heading?ArcadeGlyphs.CHARS.indexOf(Character.toUpperCase(ch)):BodyGlyphs.CHARS.indexOf(ch);}
    private static float advance(char ch,boolean heading){int i=glyph(ch,heading);return i<0?.40f:(heading?ArcadeGlyphs.ADVANCES:BodyGlyphs.ADVANCES)[i];}
    private static final java.util.LinkedHashMap<Key,TextLayer> CACHE=new java.util.LinkedHashMap<Key,TextLayer>(32,.75f,true) {
        protected boolean removeEldestEntry(java.util.Map.Entry<Key,TextLayer> entry){return size()>128;}
    };
    private static int nextKey=0x60000000;
    private static final class Key {
        final String text;final int color,unit;final boolean heading;
        Key(String text,int color,int unit,boolean heading){this.text=text;this.color=color;this.unit=unit;this.heading=heading;}
        public int hashCode(){return ((text.hashCode()*31+color)*31+unit)*31+(heading?1:0);}
        public boolean equals(Object other){if(!(other instanceof Key))return false;Key k=(Key)other;return color==k.color && unit==k.unit && heading==k.heading && text.equals(k.text);}
    }
    private static final class TextLayer extends StaticLayer {
        final Key text;final int id;
        TextLayer(Key text,int id){this.text=text;this.id=id;}
        public boolean backgroundSafe(){return true;}
        public int key(){return id;}
        public float width(){return Lettering.width(text.text,text.unit*7,text.heading)+text.unit*7*.24f+2;}
        public float height(){return text.unit*7*1.6f+2;}
        public void paint(Painter p){drawGlyphs(p,text.text,1+text.unit*7*.12f,1+text.unit*7*.20f,text.unit*7,0xff000000|text.color,text.heading);}
    }
    private static synchronized TextLayer layer(String text,int color,int unit,boolean heading){
        Key key=new Key(text,color&0xffffff,unit,heading);TextLayer layer=CACHE.get(key);
        if(layer==null){layer=new TextLayer(key,nextKey++);CACHE.put(key,layer);}return layer;
    }
    public static void draw(Painter p,String text,float x,float y,float size,int color){draw(p,text,x,y,size,color,false);}
    /** Dynamic counters reuse fixed words and ten digits, not a sentence atlas per value. */
    public static void changingNumbers(Painter p,String text,float x,float y,float size,int color){
        if(text.isEmpty() || size<=0 || (color>>>24)==0)return;
        float cursor=x;
        for(int start=0;start<text.length();){
            char ch=text.charAt(start);int end=start+1;
            if(ch<'0' || ch>'9')while(end<text.length() && (text.charAt(end)<'0' || text.charAt(end)>'9'))end++;
            draw(p,text.substring(start,end),cursor,y,size,color);
            for(int i=start;i<end;i++)cursor+=advance(text.charAt(i),false)*size;
            start=end;
        }
    }
    public static void heading(Painter p,String text,float x,float y,float size,int color){draw(p,text,x,y,size,color,true);}
    private static void draw(Painter p,String text,float x,float y,float size,int color,boolean heading){
        if(text.isEmpty() || size<=0 || (color>>>24)==0)return;
        int unit=Math.max(1,(int)StrictMath.ceil(size/5));
        TextLayer image=layer(text,color,unit,heading);float scale=size/(unit*7);
        p.layer(image,x-scale-size*.12f,y-scale-size*.20f,scale,(color>>>24)/255f);
    }
    static void drawGlyphs(Painter p,String text,float x,float y,float size,int color){drawGlyphs(p,text,x,y,size,color,false);}
    private static void drawGlyphs(Painter p,String text,float x,float y,float size,int color,boolean heading){
        float cursor=x;
        for(int n=0;n<text.length();n++){
            int index=glyph(text.charAt(n),heading);
            if(index>=0){
                float[] data=(heading?ArcadeGlyphs.FACES:BodyGlyphs.FACES)[index];
                float[][] faces=new float[data.length/8][8];
                for(int i=0;i<faces.length;i++)for(int j=0;j<8;j++)faces[i][j]=(j%2==0?cursor:y)+data[i*8+j]*size;
                p.polygons(faces,faces.length,color);
            }
            cursor+=advance(text.charAt(n),heading)*size;
        }
    }
    public static void center(Painter p,String text,float x,float y,float size,int color){draw(p,text,x-width(text,size)/2,y,size,color);}
    public static void centerHeading(Painter p,String text,float x,float y,float size,int color){heading(p,text,x-headingWidth(text,size)/2,y,size,color);}
}
