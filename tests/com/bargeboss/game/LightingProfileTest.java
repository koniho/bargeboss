package com.bargeboss.game;

/** Stage ownership, navigation identity, alpha and immutable preparation inputs. */
public final class LightingProfileTest {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    public static void run(){
        Game game=new Game();check(game.lighting()==LightingProfile.DUSK,"default/title is dusk");
        game.selectLighting(1);check(game.lighting()==LightingProfile.DUSK,"selection leaves title artwork dusk");
        game.reset();LightingProfile snapshot=game.lighting();check(snapshot==LightingProfile.DAYLIGHT,"entry snapshots selection");
        game.selectLighting(3);check(game.lighting()==snapshot,"future selection cannot recolor active stage");
        game.startTutorial();check(game.activeGame().lighting()==LightingProfile.NIGHT,"practice owns its selected profile");
        check(game.lighting()==snapshot,"practice cannot modify parent profile");
        Game other=new Game();other.reset();check(other.lighting()==LightingProfile.DUSK,"independent game defaults stay isolated");
        for(int id=0;id<4;id++){
            LightingProfile p=LightingProfile.preset(id);
            for(int a=0;a<256;a+=7){
                int color=(a<<24)|0xb6a381;
                check(p.scenery(color)>>>24==a,"scenery alpha preserved");
                check(p.surface(color,SurfaceMaterial.EMISSIVE)==color,"lamp/navigation identity preserved");
                for(int atlas:new int[]{SpriteAtlas.FACTORY,SpriteAtlas.PIERS,SpriteAtlas.TUG,SpriteAtlas.TEACHER_TUG}){
                    int[] pixels={color};SpriteGrade.applyArgb(atlas,pixels,p);
                    check(pixels[0]>>>24==a,"sprite alpha preserved");
                    if(atlas==SpriteAtlas.TUG)check(pixels[0]==color,"active craft source saturation preserved");
                    if(atlas==SpriteAtlas.TEACHER_TUG)check(pixels[0]==SpriteGrade.argb(atlas,color),"teacher livery independent of ambient profile");
                }
            }
            for(int j=id+1;j<4;j++)check(!SpriteGrade.cacheKey(SpriteAtlas.FACTORY,"same.png",p).equals(SpriteGrade.cacheKey(SpriteAtlas.FACTORY,"same.png",LightingProfile.preset(j))),"profile source identities cannot collide");
            Game menu=new Game();menu.configureCanvas(1333,600);Layout.Box b=LightingChoices.box(menu,id);
            menu.pointerDown(4,b.centerX(),b.centerY());menu.pointerUp(4,b.centerX(),b.centerY());
            check(menu.selectedLighting()==id,"title selection uses real screen taps on wide canvas");
        }
        check(snapshot==LightingProfile.DAYLIGHT,"retired worker snapshot remains unchanged");
        System.out.println(checks+" lighting profile ownership, grading, identity and input assertions passed");
    }
}
