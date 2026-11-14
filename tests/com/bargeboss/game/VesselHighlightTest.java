package com.bargeboss.game;

public final class VesselHighlightTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void run() {
        for(int atlas:new int[]{SpriteAtlas.BARGE_GOLD,SpriteAtlas.BARGE_MINT,SpriteAtlas.TUG_GOLD}) {
            int source=VesselHighlight.source(atlas);
            check(SpriteAtlas.count(atlas)==32 && SpriteAtlas.columns(atlas)==8 && SpriteAtlas.rows(atlas)==4,"glow preserves every hull heading");
            check(!SpriteGrade.applies(atlas),"gameplay glow retains saturated colors");
            for(int frame=0;frame<32;frame++)for(float uv:new float[]{0,.25f,.5f,1}) {
                float mapped=(VesselHighlight.PAD+uv*VesselHighlight.INNER)/VesselHighlight.CELL;
                float hx=(uv-SpriteAtlas.pivotX(source,frame))*SpriteAtlas.widthScale(source,frame);
                float hy=(uv-SpriteAtlas.pivotY(source,frame))*SpriteAtlas.heightScale(source,frame);
                check(Math.abs(hx-(mapped-SpriteAtlas.pivotX(atlas,frame))*SpriteAtlas.widthScale(atlas,frame))<.000001f,"glow X matches actual hull despite padding");
                check(Math.abs(hy-(mapped-SpriteAtlas.pivotY(atlas,frame))*SpriteAtlas.heightScale(atlas,frame))<.000001f,"glow Y matches actual hull despite padding");
                check(SpriteAtlas.sourceLeft(atlas,frame)==SpriteAtlas.sourceLeft(SpriteAtlas.BARGE,frame) && SpriteAtlas.sourceBottom(atlas,frame)==SpriteAtlas.sourceBottom(SpriteAtlas.BARGE,frame),"directional cells remain registered");
            }
        }
        check(VesselHighlight.opacity(0)>=.35f && VesselHighlight.opacity(45)>.95f,"visible trough and clear peak");
        for(int tick=0;tick<90;tick++) {
            check(VesselHighlight.opacity(tick)==VesselHighlight.opacity(tick+90),"pulse is deterministic and periodic");
            check(Math.abs(VesselHighlight.opacity(tick+1)-VesselHighlight.opacity(tick))<.022f,"smooth pulse including cycle seam");
        }
        Game root=new Game();root.startTutorial();Game g=root.activeGame();g.tutorial.lesson=Tutorial.HOLD;g.tutorial.setup(g);
        Body b=g.cargo[0];check(VesselHighlight.style(g,b)==SpriteAtlas.BARGE_GOLD,"approach target really glows before lasso eligibility");
        check(VesselHighlight.style(g,g.cargo[1])<0,"other teaching barge stays unhighlighted");
        g.tutorial.lesson=Tutorial.TIE;g.tutorial.setup(g);b=g.cargo[0];
        check(g.canTie(b) && VesselHighlight.style(g,b)==SpriteAtlas.BARGE_MINT,"lasso-ready uses mint");
        b.lineAttached=true;check(VesselHighlight.style(g,b)<0,"successful catch removes readiness glow immediately");
        b.lineAttached=false;b.deliveryRecorded=true;check(VesselHighlight.style(g,b)<0,"delivered hull cannot keep glowing");
        check(VesselHighlight.style(g,g.tug)<0,"idle tug does not receive a barge objective pulse");
        g.navigating=true;check(VesselHighlight.style(g,g.tug)==SpriteAtlas.TUG_GOLD,"actively driven tug receives its own silhouette pulse");
        System.out.println(checks+" sprite glow registration, pulse and gameplay-state assertions passed");
    }
    public static void main(String[] args){run();}
}
