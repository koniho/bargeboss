package com.bargeboss.game;

/** Visible convoy must fit the title's open-water area throughout its animation. */
public final class TitleLayoutTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static final class Capture implements Painter {
        float left=Float.MAX_VALUE,top=left,right=-left,bottom=-left;
        int vessels,shoreOrder=-1,vesselOrder=-1,order;
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha) {
            order++;
            if(a==SpriteAtlas.TITLE_HARBOR || a==SpriteAtlas.TITLE_LAND)shoreOrder=order;
            if(a>SpriteAtlas.BULK || alpha<.15f)return;
            if(vesselOrder<0)vesselOrder=order;vessels++;
            w*=SpriteAtlas.widthScale(a,f);h*=SpriteAtlas.heightScale(a,f);
            long[] rows=OcclusionMasks.rows(a)[f];
            for(int yy=0;yy<64;yy++)for(int xx=0;xx<64;xx++)if((rows[yy]&(1L<<xx))!=0) {
                float px=x+(xx/64f-SpriteAtlas.pivotX(a,f))*w,py=y+(yy/64f-SpriteAtlas.pivotY(a,f))*h;
                left=Math.min(left,px);top=Math.min(top,py);right=Math.max(right,px+w/64);bottom=Math.max(bottom,py+h/64);
            }
        }
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float blend,float alpha){sprite(a,f,x,y,w,h,r,alpha*(1-blend));sprite(a,ff,x,y,w,h,r,alpha*blend);}
        public void rect(float x,float y,float w,float h,int c){order++;}
        public void circle(float x,float y,float r,int c){order++;}
        public void line(float x,float y,float xx,float yy,float w,int c){order++;}
        public void poly(float[] xy,int c){order++;}
    }
    private static final class WaterCheck implements Painter {
        int commands;
        public void line(float x,float y,float xx,float yy,float w,int c){commands++;check(Math.min(y,yy)-w*.5f>=309.999f,"wake stroke stays below title horizon");}
        public void poly(float[] xy,int c){commands++;for(int i=1;i<xy.length;i+=2)check(xy[i]>=310,"wake polygons stay out of the sky");}
        public void rect(float x,float y,float w,float h,int c){commands++;check(y>=310,"water rectangle clips to horizon");}
        public void circle(float x,float y,float r,int c){commands++;check(y-r>=310,"water circle stays below horizon");}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){throw new AssertionError("water cannot contain sprite artwork");}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float blend,float alpha){throw new AssertionError("water cannot contain sprite artwork");}
    }
    public static void run() {
        for(int[] size:new int[][]{{960,600},{1243,600},{1333,600},{960,720}}) {
            Game g=new Game();g.configureCanvas(size[0],size[1]);
            for(int tick=0;tick<=720;tick+=37) {
                g.visualTicks=tick;Capture p=new Capture();TitleView.voyage(g,p);
                float menuLeft=g.layout.width-352;
                check(p.vessels>=3,"title retains both watercraft and the complete three-container load layer");
                check(p.left>=24 && p.right<=menuLeft-16,"complete convoy stays left of menu panel");
                check(p.top>=238,"mast and cargo stay below the compact logo");
                check(p.bottom<=g.layout.height-76,"convoy clears the rank bar throughout heave");
                check(p.shoreOrder>0 && p.vesselOrder>p.shoreOrder,"shoreline covers wake while vessels remain in front");
            }
            check(g.controlBox(Layout.START).x==size[0]-328,"title action is anchored to the actual right edge");
            g.mode=Game.HELP;
            check(g.controlBox(Layout.START).x==Layout.START.x+g.layout.offsetX,"help action keeps its centered rendered position");
            g.mode=Game.TITLE;
            int ticks=g.ticks;float tx=g.tug.x,ty=g.tug.y;TitleView.voyage(g,new Parity());
            check(g.ticks==ticks && g.tug.x==tx && g.tug.y==ty,"title framing cannot move the live player tug");
            for(Layout.Box button:new Layout.Box[]{Layout.LEARN,Layout.START,Layout.CHALLENGES}) {
                Game menu=new Game();menu.configureCanvas(size[0],size[1]);ReplayInput.tap(menu,button);
                check(button==Layout.LEARN?menu.activeGame()!=menu:button==Layout.START?menu.mode==Game.PLAY:menu.mode==Game.CHALLENGES,"title buttons retain their responsive hit targets");
            }
        }
        WaterCheck check=new WaterCheck();Painter water=TitleWaterView.below(check,310);
        water.line(0,290,20,340,4,-1);water.line(0,340,20,290,4,-1);
        water.poly(new float[]{0,300,30,300,30,330,0,330},-1);water.circle(20,310,8,-1);water.rect(0,300,40,20,-1);
        check(check.commands==5,"crossing water primitives are clipped, not dropped");
        int count=check.commands;water.line(0,280,20,290,4,-1);water.circle(20,280,8,-1);
        check(check.commands==count,"water entirely above the horizon is omitted");
        System.out.println(checks+" title convoy framing, layer order, water clipping and touch target assertions passed");
    }
    public static void main(String[] args){run();}
}
