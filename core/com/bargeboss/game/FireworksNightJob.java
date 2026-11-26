package com.bargeboss.game;

/** FIREWORKS NIGHT: the finale. Tow the fireworks barge from the inner harbor out through the inlet to
 * the show mark on the lake before showtime; it anchors on station. Then tap the barge and fire three
 * volleys for the crowd on the beach. Dusk falls as the job runs. */
final class FireworksNightJob extends MontroseJob {
    static final float PICKUP_X=590,PICKUP_Y=-600,MARK_X=1000,MARK_Y=920,MARK_REACH=90;
    static final int VOLLEYS=3,SHOWTIME=210*60;
    Body barge;boolean onStation;int launchesAtStation;

    void setup(Game g){
        limit=SHOWTIME;
        barge=new Body(g.nextId++,2,PICKUP_X,PICKUP_Y,(float)Math.PI*.15f);barge.ability=BargeAbilities.FIREWORKS;
        barge.awaitingPickup=true;barge.tiedX=barge.x;barge.tiedY=barge.y;barge.tiedAngle=barge.angle;g.cargo[0]=barge;
        g.weather.evening=true;g.weather.eveningTicks=StageWeather.EVENING_TICKS/2;
        start(g,PICKUP_X-40,PICKUP_Y+120,-(float)Math.PI/2);
    }
    void objective(Game g,Painter canvas){
        if(onStation){if(g.delivered<VOLLEYS)arrow(g,canvas,barge.x,barge.y,"LAUNCH");return;}
        if(g.tow.connected(barge,g.tug))arrow(g,canvas,MARK_X,MARK_Y,"SHOW MARK");else arrow(g,canvas,barge.x,barge.y,"FIREWORKS BARGE");
    }
    String status(Game g){
        if(!onStation)return g.tow.connected(barge,g.tug)?"TOW TO THE SHOW MARK ON THE LAKE / RELEASE IN THE RING":"TOW THE FIREWORKS BARGE OUT TO THE SHOW MARK";
        return "SHOWTIME! TAP THE BARGE AND LAUNCH / "+g.delivered+" OF "+VOLLEYS+" VOLLEYS";
    }
    void step(Game g){
        super.step(g);
        if(!onStation){
            if(!g.tow.connected(barge,g.tug) && Physics.length(barge.x-MARK_X,barge.y-MARK_Y)<MARK_REACH && barge.speed()<30){
                onStation=true;g.tow.detachBody(g,barge);
                barge.awaitingPickup=true;barge.tiedX=barge.x;barge.tiedY=barge.y;barge.tiedAngle=barge.angle;barge.vx=barge.vy=barge.spin=0;
                launchesAtStation=g.abilities.launches;limit=Math.max(limit,ticks+90*60);
                g.weather.eveningTicks=StageWeather.EVENING_TICKS;g.haptic(Game.HAPTIC_SUCCESS);
                pop(barge.x,barge.y,"ON STATION!");g.tell("ON STATION! TAP THE BARGE / LIGHT IT UP");
            }
            return;
        }
        // Every launch from the anchored barge is a volley for the crowd.
        while(launchesAtStation<g.abilities.launches && g.delivered<VOLLEYS){
            launchesAtStation++;
            score(g,barge.x,barge.y,g.delivered+1==VOLLEYS?1500:600,g.delivered+1==VOLLEYS?"GRAND FINALE! THE BEACH GOES WILD":"OOOH! AAAH!");
        }
    }
    boolean towable(Body b){return b!=barge || !onStation;}
    void water(Game g,Painter p){
        if(!onStation)ring(g,p,MARK_X,MARK_Y,MARK_REACH*.7f,0xffff6fd8,g.tow.connected(barge,g.tug)?1:.5f);
    }
}
