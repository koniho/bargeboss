package com.bargeboss.game;

/** Cached UI lettering remains present, while world-space labels submit no text. */
public final class OverlayTextTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static final class Count implements Painter {
        int text;StaticLayer last;float alpha;
        public void layer(StaticLayer l,float x,float y,float s,float a){if((l.key()&0xf0000000)==0x60000000){text++;last=l;alpha=a;}}
        public void poly(float[] q,int c){}public void rect(float x,float y,float w,float h,int c){}public void circle(float x,float y,float r,int c){}
        public void line(float x,float y,float xx,float yy,float w,int c){}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
    }
    public static void run(){
        check(Lettering.width("WWW",14)>Lettering.width("III",14),"technical font uses proportional metrics");
        check(Lettering.width("ready",14)==Lettering.width("READY",14),"uppercase normalization has matching metrics");
        check(Lettering.width("",14)==0,"empty text has zero width");
        check(LogoLettering.width("BARGE",7)==29,"title logo keeps original geometry");
        InstructionPanel panel=new InstructionPanel();
        InstructionPanel.Content old=new InstructionPanel.Content("old","OLD","FIRST","SECOND",0,0);
        InstructionPanel.Content next=new InstructionPanel.Content("new","NEW","FIRST","SECOND",1,0);
        check(panel.open()==0,"CRT starts collapsed");
        for(int i=0;i<InstructionPanel.OPEN_TICKS;i++)panel.step(old);
        check(panel.open()==1 && panel.textAlpha()==1,"CRT settles fully open");
        panel.step(next);check(panel.clearing() && panel.open()==1,"lesson change wipes contents with frame fully open");
        float wipe=panel.wipe();panel.step(next);check(panel.wipe()>wipe,"clear moves left to right");
        for(int i=2;i<InstructionPanel.WIPE_TICKS;i++)panel.step(next);
        check(panel.dropping() && panel.starX()==414 && panel.starY()==39,"clear star reaches top right before dropping");
        for(int i=0;i<InstructionPanel.DROP_TICKS;i++){check(panel.starX()==414,"star drops straight down");panel.step(next);}
        check(panel.returning() && panel.starY()==104,"star reaches lower right before returning");
        float starX=panel.starX(),starY=panel.starY();
        for(int i=0;i<InstructionPanel.RETURN_TICKS;i++){
            panel.step(next);check(panel.starX()<=starX && panel.starY()<=starY,"return travels toward top left");starX=panel.starX();starY=panel.starY();
        }
        check(panel.starX()==18 && panel.starY()==39,"new wipe begins at top left");
        check(panel.revealing() && panel.wipe()==0,"new content reveal begins only after clearing finishes");
        panel.step(next);check(panel.wipe()>0,"new content reveals left to right");
        for(int i=1;i<InstructionPanel.WIPE_TICKS;i++)panel.step(next);
        check(!panel.revealing() && panel.open()==1,"wipe settles without resizing frame");
        check(panel.shimmering(),"new text completion starts brief shimmer");
        for(int i=0;i<InstructionPanel.SHIMMER_TICKS;i++)panel.step(next);
        check(!panel.shimmering(),"shimmer settles and stays off while idle");
        panel.step(new InstructionPanel.Content("new","NEW","UPDATED COUNT","SECOND",1,0));
        check(!panel.clearing(),"live counters update without restarting wipe");
        for(int i=0;i<InstructionPanel.CLOSE_TICKS;i++)panel.step(null);
        check(panel.hidden() && panel.open()==0,"completion shuts instruction panel down");
        panel.reset();check(!panel.hidden() && panel.open()==0,"reset restarts CRT reveal");
        Count p=new Count();Lettering.draw(p,"READY",10,20,14,0xffabcdef);StaticLayer first=p.last;
        Lettering.draw(p,"READY",40,70,14,0x44abcdef);
        check(p.last==first && Math.abs(p.alpha-68/255f)<.00001f,"pulse and location reuse immutable text raster");
        Lettering.draw(p,"READY",40,70,14,0xff112233);check(p.last!=first,"color change gets distinct glyph raster");
        Lettering.draw(p,"WAIT",40,70,14,0xffabcdef);check(p.last!=first,"content change gets distinct glyph raster");
        for(int i=0;i<160;i++)Lettering.draw(p,"VALUE "+i,0,0,14,0xffabcdef);
        Lettering.draw(p,"READY",0,0,14,0xffabcdef);check(p.last.key()!=first.key(),"evicted text never reuses an older native cache key");
        Game g=new Game();ReplayInput.controlFixture(g);p=new Count();Painter world=new RiverPainter(p,g.layout.camera);
        SoftCue.caption(world,"NO WORLD LABEL",300,400,12,0xffffffff);RiverPainter.center(world,"NO LABEL",300,400,12,0xffffffff);
        HarborView.draw(g,world);Renderer.body(g,world,g.tug);for(Body b:g.cargo)if(b!=null)Renderer.body(g,world,b);
        for(int crane:DockView.ordered(g))DockView.draw(g,p,crane);MooringView.draw(g,world);check(p.text==0,"vessels, berths, dock machinery and mooring cues contain no world text");
        ScreenView.hud(g,p);check(p.text>0,"HUD still has readable cached text");
        g.startTutorial();g=g.activeGame();for(int i=0;i<InstructionPanel.OPEN_TICKS;i++)g.tutorial.instructionPanel.step(TutorialView.instructionContent(g));p=new Count();TutorialView.draw(g,p);check(p.text>0,"tutorial instruction overlay remains readable");
        System.out.println(checks+" cached UI and world-text removal assertions passed");
    }
    public static void main(String[] args){run();}
}
