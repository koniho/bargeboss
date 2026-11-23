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
            Game menu=new Game();menu.selectLighting(id);
            check(menu.selectedLighting()==id,"programmatic next-entry lighting remains supported");
        }
        int duskBoat=0xff2d3131;
        for(int id=0;id<4;id++){
            LightingProfile profile=LightingProfile.preset(id);
            int[] base={duskBoat};SpriteGrade.applyArgbRow(SpriteAtlas.MOORED_BOATS,base,profile,0,4);
            if(id==1)check(((base[0]>>16)&255)>=170,"daylight boat base loses baked dusk darkness");
            if(id==2)check(base[0]==duskBoat,"accepted dusk boat pixels remain identical");
            for(int band=1;band<4;band++){
                int[] receiver={0x7fc00000,0xff00c000,0xff0000c0};int[] before=receiver.clone();
                SpriteGrade.applyArgbRow(SpriteAtlas.MOORED_BOATS,receiver,profile,band,4);
                check(java.util.Arrays.equals(before,receiver),"RGB receiver bands are data, not graded albedo");
            }
            for(int alpha:new int[]{0,32,128,255}){
                byte[] rgba=new byte[16];
                for(int band=0;band<4;band++){
                    rgba[band*4]=(byte)SpriteGrade.premultiplied(45,alpha);
                    rgba[band*4+1]=(byte)SpriteGrade.premultiplied(49,alpha);
                    rgba[band*4+2]=(byte)SpriteGrade.premultiplied(49,alpha);rgba[band*4+3]=(byte)alpha;
                }
                byte[] expected=rgba.clone();byte[] first=java.util.Arrays.copyOf(rgba,4);
                SpriteGrade.applyPremultipliedRgba(SpriteAtlas.MOORED_BOATS,first,profile);
                System.arraycopy(first,0,expected,0,4);
                SpriteGrade.applySourcePremultipliedRgba(SpriteAtlas.MOORED_BOATS,rgba,profile);
                check(java.util.Arrays.equals(expected,rgba),"native premultiplied source grades base only and preserves coverage");
            }
            if(id!=2)check(!SpriteGrade.cacheKey(SpriteAtlas.MOORED_BOATS,"boats.png",profile).equals(SpriteGrade.cacheKey(SpriteAtlas.MOORED_BOATS,"boats.png",LightingProfile.DUSK)),"moored source cache tracks time selection");
        }
        check(snapshot==LightingProfile.DAYLIGHT,"retired worker snapshot remains unchanged");
        Game live=new Game();live.selectChallenge(Challenges.MONTROSE);live.reset();live.settings.developerWaves=true;
        live.pointerDown(9,30,90);live.pointerUp(9,30,90);
        int ticks=live.ticks;Body tug=live.tug;float x=tug.x,y=tug.y;Layout layout=live.layout;
        FrameGate gate=new FrameGate();gate.rendered(live);
        live.pointerDown(9,250,292);live.pointerUp(9,250,292);
        check(live.lighting()==LightingProfile.NIGHT,"weather panel changes the current stage");
        check(gate.needsFrame(live),"profile replacement redraws without advancing ticks");
        check(live.ticks==ticks && live.tug==tug && tug.x==x && tug.y==y && live.layout==layout,"time selection preserves voyage and camera");
        live.pointerDown(9,250,292);live.pointerCancel(9);live.pointerUp(9,250,292);
        check(live.lighting()==LightingProfile.NIGHT,"cancelled time tap does not switch");
        live.pointerDown(9,250,292);live.pointerUp(9,250,292);
        check(live.lighting()==LightingProfile.DAWN,"next wraps from night to dawn");
        live.pointerDown(9,30,292);live.pointerUp(9,30,292);
        check(live.lighting()==LightingProfile.NIGHT,"previous wraps from dawn to night");
        live.pointerDown(9,200,330);live.pointerUp(9,200,330);
        check(live.lighting()==LightingProfile.DUSK && live.selectedLighting()==2,"reset all restores dusk");
        live.startTutorial();Game practice=live.activeGame();practice.changeLighting(1);
        check(practice.lighting()==LightingProfile.DAYLIGHT && live.lighting()==LightingProfile.DUSK,"live practice changes leave parent untouched");
        System.out.println(checks+" lighting profile ownership, grading, identity and input assertions passed");
    }
}
