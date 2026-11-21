package com.bargeboss.game;

/** Visible reverse sectors match the real projected thrust boundary and fade on fixed steps. */
public final class JoystickReverseTest {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static boolean shown(float heading,float angle){float relative=angle-Joystick.reverseStart(heading),tau=(float)(Math.PI*2);relative=(relative%tau+tau)%tau;return relative<Joystick.reverseSweep(heading);}
    public static void run(){
        for(int pose=0;pose<64;pose++){
            float heading=pose*(float)Math.PI/32;
            for(int ray=0;ray<720;ray++){
                float angle=(ray+.237f)*(float)Math.PI/360,x=(float)StrictMath.cos(angle),y=(float)StrictMath.sin(angle);
                float wx=(x-y/RiverCamera.DEPTH)/(2*RiverCamera.Q),wy=(x+y/RiverCamera.DEPTH)/(2*RiverCamera.Q);
                float dot=(wx*(float)StrictMath.cos(heading)+wy*(float)StrictMath.sin(heading))/Physics.length(wx,wy);
                if(Math.abs(dot+.5f)<.00001f)continue;
                float forward=(angle-Joystick.forwardStart(heading))%(float)(2*Math.PI);if(forward<0)forward+=(float)(2*Math.PI);
                check((forward<Joystick.forwardSweep(heading))==(dot>=-.5f),"Forward segments complement the projected reverse cone");
                check(shown(heading,angle)==(dot<-.5f),"Marked arc equals the actual reverse cone under isometric projection");
                check(Joystick.reverseScreen(heading,x,y)==(dot<-.5f),"Screen predicate agrees with independent world dot-product");
            }
            for(int side:new int[]{GameSettings.LEFT,GameSettings.RIGHT})for(boolean reverse:new boolean[]{false,true}){
                Game g=new Game();ReplayInput.startPractice(g);g=g.activeGame();g.settings.setJoystickSide(side);g.tug.angle=heading;
                float angle=heading+(reverse?(float)Math.PI:0),x=(float)StrictMath.cos(angle),y=(float)StrictMath.sin(angle);
                float sx=RiverCamera.Q*(x+y),sy=RiverCamera.Q*RiverCamera.DEPTH*(y-x),len=Physics.length(sx,sy);
                g.pointerDown(0,Joystick.centerX(g),Joystick.centerY(g));g.pointerMove(0,Joystick.centerX(g)+sx/len*42,Joystick.centerY(g)+sy/len*42);g.step();
                check(g.reversing==reverse,"Native joystick input uses the same forward/reverse regions on either side");
            }
        }
        Game g=new Game();g.reset();float last=g.joystick.guideAlpha();check(last==1,"Released guide starts visible");
        g.pointerDown(0,Joystick.centerX(g),Joystick.centerY(g));
        check(g.joystick.guideAlpha()==last,"Press does not snap guide opacity");
        for(int i=0;i<6;i++){g.step();float now=g.joystick.guideAlpha();check(now<last && now>0,"Hold fades monotonically instead of hiding abruptly");last=now;}
        g.pointerUp(0,Joystick.centerX(g),Joystick.centerY(g));check(g.joystick.guideAlpha()==last,"Release preserves interrupted fade position");
        g.step();check(g.joystick.guideAlpha()>last && g.joystick.guideAlpha()<1,"Release eases back in");
        for(int i=0;i<60;i++)g.step();check(g.joystick.guideAlpha()==1,"Released guide fully reappears");
        ReplayInput.down(g,g.tug.x,g.tug.y);for(int i=0;i<60;i++)g.step();check(g.joystick.guideAlpha()==0,"Direct boat holding fades the joystick guide too");
        g.cancelInputs();for(int i=0;i<60;i++)g.step();check(g.joystick.guideAlpha()==1,"Cancellation restores guide through normal fade");
        float alpha=g.joystick.guideAlpha();int signature=Parity.frame(g);check(signature==Parity.frame(g) && g.joystick.guideAlpha()==alpha,"Repeated drawing never advances opacity");
        for(int side:new int[]{GameSettings.LEFT,GameSettings.RIGHT})for(boolean reverse:new boolean[]{false,true}){
            g=new Game();g.reset();g.settings.setJoystickSide(side);g.tug.angle=0;
            float cx=Joystick.centerX(g),cy=Joystick.centerY(g),sign=reverse?-1:1;
            check(g.joystick.throttleAlpha()==0,"Idle throttle sector starts hidden");
            g.pointerDown(0,cx,cy);g.pointerMove(0,cx+sign*50,cy-sign*29);
            g.step();float first=g.joystick.throttleAlpha();check(first>0 && first<1,"Thrust fades sector in");
            for(int i=0;i<40;i++)g.step();check(g.joystick.throttleAlpha()==1,"Sustained thrust fully reveals sector");
            g.pointerMove(0,cx,cy);g.step();last=g.joystick.throttleAlpha();
            check(last>0 && last<1,"Held neutral fades sector instead of keeping empty outline");
            g.pointerMove(0,cx+sign*50,cy-sign*29);g.step();check(g.joystick.throttleAlpha()>last,"Reapplying thrust reverses fade continuously");
            g.pointerUp(0,cx+sign*50,cy-sign*29);last=g.joystick.throttleAlpha();
            g.step();check(g.joystick.throttleAlpha()<last,"Release fades out even while boat coasts");
            g.back();last=g.joystick.throttleAlpha();g.advance(.25);check(g.joystick.throttleAlpha()==last,"Pause freezes throttle fade");g.back();
            for(int i=0;i<60;i++)g.step();check(g.joystick.throttleAlpha()==0,"Released sector disappears completely");
            ReplayInput.down(g,g.tug.x,g.tug.y);ReplayInput.move(g,g.downX+100,g.downY);
            for(int i=0;i<20;i++)g.step();check(g.joystick.throttleAlpha()==0 && !g.navigating,"Removed tug drag never shows powered throttle");
            g.cancelInputs();for(int i=0;i<60;i++)g.step();check(g.joystick.throttleAlpha()==0,"Cancel fades direct-drag sector away");
        }
        System.out.println(checks+" projected reverse-sector, real input and continuous fade assertions passed");
    }
    public static void main(String[] args){run();}
}
