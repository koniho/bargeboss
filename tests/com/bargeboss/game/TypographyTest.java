package com.bargeboss.game;
/** Shared font roles, fitted widths and cached-face bounds. */
public final class TypographyTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void run(){
        for(boolean heading:new boolean[]{false,true}){
            String chars=heading?ArcadeGlyphs.CHARS:BodyGlyphs.CHARS;
            float[][] faces=heading?ArcadeGlyphs.FACES:BodyGlyphs.FACES;
            float[] advances=heading?ArcadeGlyphs.ADVANCES:BodyGlyphs.ADVANCES;
            for(int i=0;i<chars.length();i++){
                check(faces[i].length>0 && faces[i].length%8==0,"complete glyph trapezoids");
                for(int j=0;j<faces[i].length;j+=2){
                    check(faces[i][j]>=-.12f && faces[i][j]<=advances[i]+.01f,"glyph fits advance and cache padding");
                    check(faces[i][j+1]>=-.20f && faces[i][j+1]<=1.4f,"glyph fits vertical cache bounds");
                }
            }
        }
        check(Lettering.width("Ease forward",16)>0 && Lettering.width("Ease forward",16)!=Lettering.width("EASE FORWARD",16),"body supports readable mixed case");
        check(Lettering.headingWidth("Set Sail",24)==Lettering.headingWidth("SET SAIL",24),"display headings share uppercase forms");
        check(Lettering.headingWidth("SET SAIL",24)!=Lettering.width("SET SAIL",24),"body/display metrics remain distinct");
        System.out.println("Shared display/body glyph metrics, case and cache bounds passed");
    }
}
