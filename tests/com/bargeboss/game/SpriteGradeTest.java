package com.bargeboss.game;

/** Scene grading cannot darken gameplay craft or change source transparency. */
public final class SpriteGradeTest {
    private static int checks;
    private static void check(boolean ok,String message) {checks++;if(!ok)throw new AssertionError(message);}
    private static int rgb(int r,int g,int b) {return 0xff000000|(r<<16)|(g<<8)|b;}
    public static void run() {
        for(int atlas:new int[]{SpriteAtlas.TUG,SpriteAtlas.BARGE,SpriteAtlas.STEEL,SpriteAtlas.CONTAINER,SpriteAtlas.BULK,
                SpriteAtlas.TITLE_VESSELS,SpriteAtlas.WATER,SpriteAtlas.TITLE_WATER,SpriteAtlas.TITLE_BACKDROP}) {
            check(!SpriteGrade.applies(atlas),"boats, cargo, water and sky preserve original color");
            for(int color:new int[]{0xffffb34f,0xfff9ede0,0xffb9472a,0xff32d8da,0x304080c0,0x00112233})
                check(SpriteGrade.argb(atlas,color)==color,"ungraded source pixel is bit-identical");
        }
        for(int atlas:new int[]{SpriteAtlas.NORTH_QUAY,SpriteAtlas.SOUTH_QUAY,SpriteAtlas.SCENERY,SpriteAtlas.PIERS,
                SpriteAtlas.BRIDGE,SpriteAtlas.TITLE_HARBOR,SpriteAtlas.TITLE_LAND,SpriteAtlas.STATIC_HARBOR,
                SpriteAtlas.TUTORIAL_SCENERY,SpriteAtlas.CHIMNEYS,SpriteAtlas.FACTORY,SpriteAtlas.GROUND,SpriteAtlas.MARINA,
                SpriteAtlas.SHORE_BOLLARDS,SpriteAtlas.SHORE_LOADING,SpriteAtlas.SHORE_TRUCKS,SpriteAtlas.SHORE_TILES,SpriteAtlas.STEELWORKS})
            check(SpriteGrade.applies(atlas),"all industrial scenery uses shared grading");
        for(int a=0;a<256;a++) for(int color:new int[]{0x205080,0xa06742,0xffc020,0xfce0b0,0x808080}) {
            int pixel=(a<<24)|color,graded=SpriteGrade.argb(SpriteAtlas.FACTORY,pixel);
            check((graded>>>24)==a,"grading never creates translucent ghost buildings or erodes alpha edges");
            if(a==0)check(pixel==graded,"transparent pixels are left untouched");
            byte[] rgba={(byte)SpriteGrade.premultiplied((pixel>>>16)&255,a),
                    (byte)SpriteGrade.premultiplied((pixel>>>8)&255,a),(byte)SpriteGrade.premultiplied(pixel&255,a),(byte)a};
            int normalized=(a<<24)|(SpriteGrade.straight(rgba[0]&255,a)<<16)
                    |(SpriteGrade.straight(rgba[1]&255,a)<<8)|SpriteGrade.straight(rgba[2]&255,a);
            int expected=SpriteGrade.argb(SpriteAtlas.FACTORY,normalized);
            SpriteGrade.applyPremultipliedRgba(SpriteAtlas.FACTORY,rgba);
            check((rgba[3]&255)==a,"Core Graphics alpha remains exact");
            for(int c=0;c<3;c++) {
                check((rgba[c]&255)==SpriteGrade.premultiplied((expected>>>(16-c*8))&255,a),"premultiplied host uses the same shared color transform");
                check((rgba[c]&255)<=a,"premultiplied color cannot exceed alpha");
            }
        }
        for(int gray=16;gray<=240;gray+=16) {
            int graded=SpriteGrade.argb(SpriteAtlas.FACTORY,rgb(gray,gray,gray));
            int r=(graded>>>16)&255,g=(graded>>>8)&255,b=graded&255;
            check(r<g && g<b,"neutral scenery has a cool shadow cast");
            check(graded==SceneLighting.sceneryColor(rgb(gray,gray,gray)),"sprite midtones use the same central dusk palette as geometry");
            check(g>=gray*.39f && g<=gray*.46f,"near-night scenery remains dark but legible");
        }
        int window=0xffffc02a;
        check(SpriteGrade.argb(SpriteAtlas.FACTORY,window)==window,"bright warm windows retain source energy against dark walls");
        check(SpriteGrade.argb(SpriteAtlas.FACTORY,0xffaa6844)!=0xffaa6844,"rusty brick is not mistaken for an emissive window");
        check(SpriteGrade.argb(SpriteAtlas.STEELWORKS,window)==window,"steelworks furnace/windows preserve warm emission");
        for(int atlas:new int[]{SpriteAtlas.SHORE_BOLLARDS,SpriteAtlas.SHORE_LOADING,SpriteAtlas.SHORE_TRUCKS,SpriteAtlas.SHORE_TILES,SpriteAtlas.MARINA,SpriteAtlas.GROUND,SpriteAtlas.BRIDGE}) {
            check(SpriteGrade.argb(atlas,window)==SceneLighting.sceneryColor(window),"yellow paint and railings cannot masquerade as glowing windows");
            int[] source={0x00ffcc55,0x7f718293,window};int[] batch=source.clone();SpriteGrade.applyArgb(atlas,batch);
            for(int j=0;j<batch.length;j++)check(batch[j]==SpriteGrade.argb(atlas,source[j]),"new scenery batch grading matches pixel grading");
        }
        int pier=SpriteGrade.argb(SpriteAtlas.PIERS,0xffc0c0c0);
        check(((pier>>>8)&255)<45,"wooden pier source art is almost dark");
        int[] pixels={0xff808080,window};SpriteGrade.applyArgb(SpriteAtlas.FACTORY,pixels);
        check(pixels[0]==SpriteGrade.argb(SpriteAtlas.FACTORY,0xff808080) && pixels[1]==window,"batch decode matches individual pixel grading");
        String path="shared.png";
        check(SpriteGrade.cacheKey(SpriteAtlas.GROUND,path).contains(SceneLighting.VERSION),"lighting version invalidates decoded rasters");
        check(!SpriteGrade.cacheKey(SpriteAtlas.FACTORY,path).equals(SpriteGrade.cacheKey(SpriteAtlas.SHORE_TRUCKS,path)),"window exemption cannot leak between shared source paths");
        check(!SpriteGrade.cacheKey(SpriteAtlas.PIERS,path).equals(SpriteGrade.cacheKey(SpriteAtlas.GROUND,path)),"dark wooden dock grading has a distinct raster key");
        check(!SpriteGrade.cacheKey(SpriteAtlas.GROUND,path).equals(SpriteGrade.cacheKey(SpriteAtlas.TUG,path)),"graded cache cannot contaminate bright craft sharing a source");
        check(SpriteGrade.cacheKey(SpriteAtlas.NORTH_QUAY,path).equals(SpriteGrade.cacheKey(SpriteAtlas.SOUTH_QUAY,path)),"same-grade atlases share one decoded raster");
        System.out.println(checks+" scenery grading, alpha, warm-window and bright-vessel assertions passed");
    }
    public static void main(String[] args) {run();}
}
