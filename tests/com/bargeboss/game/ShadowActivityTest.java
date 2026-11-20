package com.bargeboss.game;

/** Independent masks, smooth lamp response and shore-only ambient traffic. */
public final class ShadowActivityTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static boolean near(float a,float b){return Math.abs(a-b)<.00001f;}
    public static void run() {
        for(int atlas:ShadowArt.atlases()) {
            int source=ShadowArt.source(atlas),count=SpriteAtlas.count(source);
            check(SpriteAtlas.count(atlas)==2*count,"each source frame has separate near/soft masks");
            check(!SpriteGrade.applies(atlas),"shadow masks are independent of scenery color grading");
            for(int f=0;f<count;f++)for(int variant=0;variant<2;variant++)for(float uv:new float[]{0,.25f,.7f,1}) {
                int frame=f+variant*count;float mapped=(ShadowArt.PAD+uv*ShadowArt.INNER)/ShadowArt.CELL;
                float sx=(uv-SpriteAtlas.pivotX(source,f))*SpriteAtlas.widthScale(source,f);
                float sy=(uv-SpriteAtlas.pivotY(source,f))*SpriteAtlas.heightScale(source,f);
                check(near(sx,(mapped-SpriteAtlas.pivotX(atlas,frame))*SpriteAtlas.widthScale(atlas,frame)),"independent shadow preserves source X registration");
                check(near(sy,(mapped-SpriteAtlas.pivotY(atlas,frame))*SpriteAtlas.heightScale(atlas,frame)),"independent shadow preserves source Y registration");
            }
        }
        Game g=new Game();g.reset();float[] a=new float[3],b=new float[3];
        for(int y=220;y<800;y++) {
            ShadowView.light(g,300,y,a);ShadowView.light(g,300,y+.01f,b);
            check(near(a[2],Math.min(1,a[2])) && Float.isFinite(a[0]) && Float.isFinite(a[1]),"bounded finite lighting");
            check(Math.abs(a[0]-b[0])+Math.abs(a[1]-b[1])<.002f,"crossing between lamps never snaps shadow direction");
        }
        ShadowView.light(g,300,500,a);g.layout=Layout.focused(1920,600,400,700,2,900,300);ShadowView.light(g,300,500,b);
        for(int i=0;i<3;i++)check(a[i]==b[i],"camera changes do not move the light source");
        check(YardActivity.count()>0,"industrial yard has usable road routes");
        for(int route=0;route<YardActivity.count();route++) {
            float first=Float.NaN,firstY=Float.NaN;boolean moved=false,stopped=false;
            for(int tick=0;tick<3000;tick+=13) {
                YardActivity.position(route,tick,a);YardActivity.position(route,tick,b);
                check(a[0]==b[0] && a[1]==b[1] && a[2]==b[2],"yard movement has no wall-clock or random state");
                if(Float.isNaN(first)){first=a[0];firstY=a[1];}moved|=Physics.length(first-a[0],firstY-a[1])>20;stopped|=a[2]==0;
                for(int end=-1;end<=1;end+=2)for(int side=-1;side<=1;side+=2) {
                    float x=a[0]+end*18,y=a[1]+side*8;
                    check(!TrainingChannel.contains(x,y),"whole hauler remains outside playable water");
                    check(IndustrialRoadView.roadClear((int)StrictMath.floor(x/64),(int)StrictMath.floor(y/64)),"hauler stays on a connected road tile");
                }
            }
            check(moved && stopped,"haulers move and pause at their loading ends");
        }
        Game root=new Game();root.startTutorial();g=root.activeGame();g.tutorial.lesson=Tutorial.HOLD;g.tutorial.setup(g);
        int tick=g.ticks,visual=g.visualTicks;float x=g.tug.x,y=g.tug.y;
        Parity p=new Parity();ShadowView.factories(g,p);YardActivity.draw(g,p);
        check(g.ticks==tick && g.visualTicks==visual && g.tug.x==x && g.tug.y==y,"shadows and factory ambience never mutate gameplay");
        System.out.println(checks+" shadow mask, smooth lighting and dry-road activity assertions passed");
    }
    public static void main(String[] args){run();}
}
