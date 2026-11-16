package com.bargeboss.game;

/** The rendered rope follows its armed arc, and actual hull loss flashes only the player. */
public final class LassoFeedbackTest {
    private static int checks;
    private static void check(boolean ok,String message) {checks++;if(!ok) throw new AssertionError(message);}
    private static final class Capture implements Painter {
        float[] rope=new float[128];int lines;float tugAlpha=1,ropeWidth=2.8f;
        public void line(float x,float y,float xx,float yy,float w,int color) {}
        public void ropeSegment(float x,float y,float xx,float yy,float w,float from,float to,int color) {
            if(color==RopeMaterial.COLOR && Math.abs(w-ropeWidth)<.001f && lines<32) {rope[lines*4]=x;rope[lines*4+1]=y;rope[lines*4+2]=xx;rope[lines*4+3]=yy;lines++;}
        }
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha) {if(a==SpriteAtlas.TUG) tugAlpha=alpha;}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float blend,float alpha) {sprite(a,f,x,y,w,h,r,alpha);}
        public void rect(float x,float y,float w,float h,int c) {}
        public void circle(float x,float y,float r,int c) {}
        public void poly(float[] xy,int c) {}
    }
    private static Game fixture() {
        Game g=new Game();ReplayInput.controlFixture(g);
        g.cargo[0].x=g.dockX(2);g.cargo[0].y=g.dockY(2);return g;
    }
    public static void run() {
        float[] edge=new float[2],point=new float[2];
        for(int heading=0;heading<32;heading++) for(int direction=0;direction<32;direction++) {
            Body b=new Body(1,2,300,500,heading*(float)Math.PI/16);
            float dx=(float)StrictMath.cos(direction*Math.PI/16),dy=(float)StrictMath.sin(direction*Math.PI/16);
            LassoPath.edge(b,b.x+dx*200,b.y+dy*200,edge);
            float ox=edge[0]-b.x,oy=edge[1]-b.y;
            check(ox*dx+oy*dy>0 && Math.abs(ox*dy-oy*dx)<.001f,"origin is on the side facing the throw");
            float along=ox*(float)StrictMath.cos(b.angle)+oy*(float)StrictMath.sin(b.angle);
            float across=-ox*(float)StrictMath.sin(b.angle)+oy*(float)StrictMath.cos(b.angle);
            float cap=Math.max(0,Math.abs(along)-b.half);
            check(Math.abs(Physics.length(cap,across)-b.radius)<.001f,"origin is on the rounded hull perimeter, not its center");
        }
        Game g=fixture();Body b=g.cargo[0];
        ReplayInput.down(g,b.x,b.y);ReplayInput.move(g,g.bollardX(2),g.bollardY(2));
        RopeAttachment.cast(b,g.aimX(b,g.pointerX,g.pointerY),g.aimY(b,g.pointerX,g.pointerY),false,edge);
        check(g.ropePhase==0,"armed arc never launches before release");
        ReplayInput.up(g,g.bollardX(2),g.bollardY(2));
        check(Math.abs(g.ropeStartX-edge[0])<.001f && Math.abs(g.ropeStartY-edge[1])<.001f,"release preserves the preview origin");
        for(int tick=1;tick<Game.ROPE_FLIGHT_TICKS;tick++) {
            g.step();Capture capture=new Capture();MooringView.draw(g,capture);
            float t=tick/(float)Game.ROPE_FLIGHT_TICKS,fraction=1-(1-t)*(1-t);
            LassoPath.sample(g.ropeStartX,g.ropeStartY,g.ropeEndX,g.ropeEndY,g.ropeStartHeight,fraction,false,point);
            check(capture.lines==32,"paid-out rope draws its complete sampled arc");
            check(Math.abs(capture.rope[126]-point[0])<.002f && Math.abs(capture.rope[127]-point[1])<.002f,"flying loop follows the exact preview curve");
            check(Math.abs(capture.rope[0]-g.ropeOriginX())<.002f
                    && Math.abs(capture.rope[1]-(g.ropeOriginY()-RopeAttachment.height(b)-WaterSurface.lift(g,b)))<.002f,
                    "tether stays on the moving barge's deck edge");
            if(tick>=5 && tick<=15) check(Math.abs(capture.rope[61]-(capture.rope[1]+capture.rope[127])*.5f)>1,"paid-out rope bends over the arc rather than a straight chord");
            RiverCamera camera=g.layout.camera;TowView.Segments flight=new TowView.Segments(g);
            int tip=(Towline.CAPACITY+g.cargo.length)*25+32;
            LassoPath.sample(g.ropeStartX,g.ropeStartY,g.ropeEndX,g.ropeEndY,g.ropeStartHeight,fraction,true,point);
            check(Math.abs(camera.screenX(flight.x[tip],flight.y[tip])-camera.screenX(point[0],point[1]))<.003f
                    && Math.abs(camera.screenY(flight.x[tip],flight.y[tip])-flight.height[tip]*camera.scale-camera.screenY(point[0],point[1]))<.003f,"depth-sorted flight preserves the same isometric arc");
            check(g.ropeTicks==tick,"drawing either projection never advances the throw");
        }
        float localX=g.ropeLocalX,localY=g.ropeLocalY;
        b.x+=12;b.y-=7;b.angle+=.4f;
        check(Math.abs(g.ropeOriginX()-(b.x+localX*(float)StrictMath.cos(b.angle)-localY*(float)StrictMath.sin(b.angle)))<.001f,
                "turning the barge keeps the same local rope fitting");
        g.ropePhase=2;g.ropeTicks=Game.ROPE_RETURN_TICKS;Capture recoil=new Capture();MooringView.draw(g,recoil);
        check(Math.abs(recoil.rope[126]-g.ropeOriginX())<.002f && Math.abs(recoil.rope[127]-(g.ropeOriginY()-RopeAttachment.height(b)-WaterSurface.lift(g,b)))<.002f,
                "a miss retracts all the way to the current edge fitting");
        damage();
        System.out.println(checks+" lasso arc, edge attachment and hull-damage flash assertions passed");
    }
    private static void damage() {
        Game g=new Game();g.reset();g.tug.x=Physics.LEFT+g.tug.half+g.tug.radius+1;g.tug.y=450;g.tug.angle=0;g.tug.vx=-180;
        for(int i=0;i<10 && g.hull==100;i++) g.step();
        check(g.hull<100 && g.damageFlashTicks>0,"actual hull loss starts a tug flash");
        Capture flash=new Capture();VesselView.draw(g,flash,g.tug,true,false);
        check(flash.tugAlpha<.5f,"damage pulses the tug sprite itself");
        Body npc=new Body(-1,-1,300,500,0);Capture normal=new Capture();VesselView.draw(g,normal,npc,true,false);
        check(normal.tugAlpha==1,"a player impact does not flash other boats");
        int remaining=g.damageFlashTicks;g.background();g.advance(1);
        check(g.damageFlashTicks==remaining,"damage flash pauses with the game");
        g.pause();for(int i=0;i<50;i++)g.step();Capture recovered=new Capture();VesselView.draw(g,recovered,g.tug,true,false);
        check(g.damageFlashTicks==0 && recovered.tugAlpha==1,"tug returns to its ordinary sprite after the brief flash");
        g.reset();g.impactTicks=20;Capture contact=new Capture();VesselView.draw(g,contact,g.tug,true,false);
        check(g.damageFlashTicks==0 && contact.tugAlpha==1,"ordinary contact feedback does not imply hull damage");
    }
    public static void main(String[] args) {run();}
}
