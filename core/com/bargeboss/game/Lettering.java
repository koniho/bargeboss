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
    public static float width(String text,float size) { return Math.max(0,text.length()*6-1)*size/7f; }
    public static void draw(Painter p,String text,float x,float y,float size,int color) {
        float unit=size/7f;
        for(int n=0;n<text.length();n++) {
            char ch=text.charAt(n); if(ch>='a' && ch<='z') ch=(char)(ch-32);
            int index=CHARS.indexOf(ch);
            if(index>=0) {
                String data=GLYPHS[index];
                for(int row=0;row<7;row++) {
                    int bits=Integer.parseInt(data.substring(row*2,row*2+2),16);
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
