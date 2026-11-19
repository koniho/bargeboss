package com.bargeboss.game;

/** Rajdhani SemiBold UI lettering, exported as shared geometry; no host font APIs. */
public final class Lettering {
    private Lettering() {}
    public static float width(String text,float size) {
        float width=0;
        for(int n=0;n<text.length();n++)width+=advance(text.charAt(n));
        return Math.max(0,width-(text.isEmpty()?0:.055f))*size;
    }
    private static int glyph(char ch){return TechnicalGlyphs.CHARS.indexOf(Character.toUpperCase(ch));}
    private static float advance(char ch){int i=glyph(ch);return i<0?.45f:TechnicalGlyphs.ADVANCES[i];}
    private static final java.util.LinkedHashMap<Key,TextLayer> CACHE=new java.util.LinkedHashMap<Key,TextLayer>(32,.75f,true) {
        protected boolean removeEldestEntry(java.util.Map.Entry<Key,TextLayer> entry){return size()>128;}
    };
    private static int nextKey=0x60000000;
    private static final class Key {
        final String text;final int color,unit;
        Key(String text,int color,int unit){this.text=text;this.color=color;this.unit=unit;}
        public int hashCode(){return (text.hashCode()*31+color)*31+unit;}
        public boolean equals(Object other){if(!(other instanceof Key))return false;Key k=(Key)other;return color==k.color && unit==k.unit && text.equals(k.text);}
    }
    private static final class TextLayer extends StaticLayer {
        final Key text;final int id;
        TextLayer(Key text,int id){this.text=text;this.id=id;}
        // paint reads immutable glyph rows and final text fields only.
        public boolean backgroundSafe(){return true;}
        public int key(){return id;}
        public float width(){return Lettering.width(text.text,text.unit*7)+2;}
        public float height(){return text.unit*7*1.3f+2;}
        public void paint(Painter p){drawGlyphs(p,text.text,1,1,text.unit*7,0xff000000|text.color);}
    }
    private static synchronized TextLayer layer(String text,int color,int unit){
        Key key=new Key(text,color&0xffffff,unit);TextLayer layer=CACHE.get(key);
        if(layer==null){layer=new TextLayer(key,nextKey++);CACHE.put(key,layer);}
        return layer;
    }
    /** Cached outline text: pulse alpha/placement stay live, glyph faces are painted once. */
    public static void draw(Painter p,String text,float x,float y,float size,int color) {
        if(text.isEmpty() || size<=0 || (color>>>24)==0)return;
        int unit=Math.max(1,(int)StrictMath.ceil(size/5));
        TextLayer image=layer(text,color,unit);float scale=size/(unit*7);
        p.layer(image,x-scale,y-scale,scale,(color>>>24)/255f);
    }
    static void drawGlyphs(Painter p,String text,float x,float y,float size,int color) {
        float cursor=x;
        for(int n=0;n<text.length();n++) {
            int index=glyph(text.charAt(n));
            if(index>=0) {
                float[] data=TechnicalGlyphs.FACES[index];
                float[][] faces=new float[data.length/8][8];
                for(int i=0;i<faces.length;i++)for(int j=0;j<8;j++)
                    faces[i][j]=(j%2==0?cursor:y)+data[i*8+j]*size;
                p.polygons(faces,faces.length,color);
            }
            cursor+=advance(text.charAt(n))*size;
        }
    }
    public static void center(Painter p,String text,float x,float y,float size,int color) {
        draw(p,text,x-width(text,size)/2,y,size,color);
    }
}
