package com.bargeboss.game;

/** A small authored 5x7 alphabet. Glyph geometry is identical on every host. */
public final class Lettering {
    private Lettering() {}
    private static final String CHARS="ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-+:/.,!?%()'";
    private static final String[] GLYPHS={
        "0E11111F111111","1E11111E11111E","0F10101010100F","1E11111111111E","1F10101E10101F","1F10101E101010",
        "0F10101711110F","1111111F111111","0E04040404040E","0702020212120C","11121418141211","1010101010101F",
        "111B1515111111","11191513111111","0E11111111110E","1E11111E101010","0E11111115120D","1E11111E141211",
        "0F10100E01011E","1F040404040404","1111111111110E","11111111110A04","11111115151B11","11110A040A1111",
        "11110A04040404","1F01020408101F","0E11131519110E","040C040404040E","0E11010204081F","1E01010601011E",
        "02060A121F0202","1F10101E01011E","0E10101E11110E","1F010204080808","0E11110E11110E","0E11110F01010E",
        "0000001F000000","0004041F040400","00040000040000","01010204081010","00000000000C0C","00000000000408",
        "04040404040004","0E110102040004","19190204081313","02040808080402","08040202020408","04040800000000"
    };
    private static final int[][] ROWS=decode();
    private static int[][] decode() {
        int[][] rows=new int[GLYPHS.length][7];
        for(int i=0;i<GLYPHS.length;i++)for(int row=0;row<7;row++)
            rows[i][row]=Integer.parseInt(GLYPHS[i].substring(row*2,row*2+2),16);
        return rows;
    }
    public static float width(String text,float size) { return Math.max(0,text.length()*6-1)*size/7f; }
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
        public float height(){return text.unit*7+2;}
        public void paint(Painter p){drawGlyphs(p,text.text,1,1,text.unit*7,0xff000000|text.color);}
    }
    private static synchronized TextLayer layer(String text,int color,int unit){
        Key key=new Key(text,color&0xffffff,unit);TextLayer layer=CACHE.get(key);
        if(layer==null){layer=new TextLayer(key,nextKey++);CACHE.put(key,layer);}
        return layer;
    }
    /** Cached authored text: pulse alpha/placement stay live, glyph cells are painted once. */
    public static void draw(Painter p,String text,float x,float y,float size,int color) {
        if(text.isEmpty() || size<=0 || (color>>>24)==0)return;
        int unit=Math.max(1,(int)StrictMath.ceil(size/5));
        TextLayer image=layer(text,color,unit);float scale=size/(unit*7);
        p.layer(image,x-scale,y-scale,scale,(color>>>24)/255f);
    }
    static void drawGlyphs(Painter p,String text,float x,float y,float size,int color) {
        float unit=size/7f;
        for(int n=0;n<text.length();n++) {
            char ch=text.charAt(n); if(ch>='a' && ch<='z') ch=(char)(ch-32);
            int index=CHARS.indexOf(ch);
            if(index>=0) {
                int[] data=ROWS[index];
                for(int row=0;row<7;row++) {
                    int bits=data[row];
                    for(int col=0;col<5;col++) if((bits&(1<<(4-col)))!=0)
                        p.rect(x+(n*6+col)*unit,y+row*unit,unit,unit,color);
                }
            }
        }
    }
    public static void center(Painter p,String text,float x,float y,float size,int color) {
        draw(p,text,x-width(text,size)/2,y,size,color);
    }
}
