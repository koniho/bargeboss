package com.bargeboss.game;

import java.util.ArrayList;
import java.util.List;

/** Geographic scene entry points must submit the existing local shore receivers. */
public final class GeographicShoreLightTest {
    static final class Ink implements Painter {
        final List<String> patches=new ArrayList<String>();
        public void poly(float[] q,int color){
            if((color&0xffffff)!=0xffbc6b || (color>>>24)==0)return;
            String key=""+color;for(float v:q)key+=":"+Float.floatToIntBits(v);patches.add(key);
        }
        public void rect(float x,float y,float w,float h,int c){}public void circle(float x,float y,float r,int c){}
        public void line(float x,float y,float xx,float yy,float w,int c){}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
        public void material(float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color){}
    }
    static Game fixture(int id){
        Game g=new Game();g.challengeId=id;g.reset();
        for(Body b:g.cargo)if(b!=null)b.active=false;
        for(Feeder f:g.feeders)f.phase=Feeder.IDLE;
        for(ShorelineView.Segment s:ShorelineView.segments(g)){
            if(!s.wallVisible)continue;
            float x=(s.ax+s.bx)*.5f+s.nx*24,y=(s.ay+s.by)*.5f+s.ny*24;
            if(!WaterEffectsPainter.contains(g,x,y))continue;
            g.tug.x=x;g.tug.y=y;g.tug.angle=(float)StrictMath.atan2(-s.ny,-s.nx);
            g.layout=Layout.focused(960,600,x,y,2,480,320);
            Ink source=new Ink();VesselShoreLight.draw(g,source);
            if(source.patches.size()>3)return g;
        }
        throw new AssertionError("no lit geographic shore fixture: "+id);
    }
    static void run(){
        for(int id:new int[]{Challenges.INDIANA,Challenges.TWO_BANKS}){
            Game g=fixture(id);Ink expected=new Ink();VesselShoreLight.draw(g,expected);
            Ink actual=new Ink();SurveyLevelView.draw(g,actual,g.level());
            if(!actual.patches.containsAll(expected.patches))throw new AssertionError("geographic scene omitted vessel-to-shore lighting: "+id);
            g.tug.active=false;Ink dark=new Ink();VesselShoreLight.draw(g,dark);
            if(!dark.patches.isEmpty())throw new AssertionError("inactive source still lights shore");
        }
        System.out.println("Indiana/Calumet scene entry points include local vessel-to-shore receivers");
    }
    public static void main(String[] args){run();}
}
