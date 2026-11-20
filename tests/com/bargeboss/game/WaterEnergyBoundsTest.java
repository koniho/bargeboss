package com.bargeboss.game;

/** Cross-consumer contract and behavior checks for signed, sheltered waves. */
public final class WaterEnergyBoundsTest {
    private static int checks;
    private static void check(boolean yes,String message){checks++;if(!yes)throw new AssertionError(message);}
    public static void run(){
        Game g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();
        float x=MontroseHarbor.mapX(900),y=MontroseHarbor.mapY(1500);
        WaterSurface w=g.water;w.x[0]=x;w.y[0]=y;w.age[0]=80;w.strength[0]=1;w.heading[0]=0;
        float r=WaterSurface.wakeRadius(80),crestX=x-r;
        float calm=SeaWaves.value(g,crestX,y,0,0);
        check(w.height(g,crestX,y,0,null)>calm+.8f,"packet crest directly raises surface at rendered radius");
        check(w.energy(g,x+r,y,null)==0,"front of vessel is outside rear wake fan");
        check(w.energy(g,x-r-27,y,null)==0,"wake has bounded support");
        check(w.height(g,x-r-19,y,0,null)<SeaWaves.value(g,x-r-19,y,0,0),"packet includes signed return trough");
        float[] force=new float[2];w.buoyForce(g,crestX,y,force);
        float expected=(w.height(g,crestX,y,0,null)-calm)*WaterSurface.WAKE_SPEED/Physics.DT*(float)StrictMath.PI/26;
        check(Math.abs(force[0]/1.4f-SeaWaves.value(g,crestX,y,0,1)+expected)<.0001f,"crest orbital flow shares height phase and direction");
        check(Math.abs(force[1]/1.4f-SeaWaves.value(g,crestX,y,0,2))<.0001f,"radial wake has no transverse flow");
        // A packet originating on dry ground cannot influence water across it.
        w.x[0]=MontroseHarbor.mapX(1000);w.y[0]=MontroseHarbor.mapY(1100);w.age[0]=200;
        check(!MontroseHarbor.contains(w.x[0],w.y[0]),"blocking fixture is dry");
        check(w.energy(g,w.x[0]-WaterSurface.wakeRadius(200),w.y[0],null)==0,"shore blocks packet height and force together");
        w.age[0]=WaterSurface.LIFE;
        float innerX=MontroseHarbor.SLIP_X,innerY=MontroseHarbor.SLIP_Y;
        check(SeaWaves.exposure(x,y)>.99f && SeaWaves.exposure(innerX,innerY)<.15f,"lake and slip have distinct exposure");
        float previous=SeaWaves.exposure(MontroseHarbor.mapX(350),MontroseHarbor.mapY(650));
        for(int my=651;my<=1500;my++){
            float next=SeaWaves.exposure(MontroseHarbor.mapX(350),MontroseHarbor.mapY(my));
            check(next>=previous && next-previous<.005f,"entrance shelter changes continuously");previous=next;
        }
        Body head=new Body(0,-1,x,y,-(float)StrictMath.PI/2),beam=new Body(0,-1,x,y,0);
        float headMotion=0,beamMotion=0,turning=0;
        for(int tick=0;tick<600;tick++){
            g.visualTicks=tick;w.affect(g,head);w.affect(g,beam);
            turning+=Math.abs(head.spin)+Math.abs(beam.spin);
            headMotion+=Math.abs(head.vy);beamMotion+=Math.abs(beam.vy);
            head.vx=head.vy=head.spin=beam.vx=beam.vy=beam.spin=0;
        }
        check(beamMotion>headMotion*2,"beam seas exert more lateral response than head seas");
        check(turning>.001f,"spatially different hull forces impart turning torque");
        g.settings.adjustWaves(-4);head.vx=head.vy=head.spin=0;w.affect(g,head);
        check(head.vx==0 && head.vy==0 && head.spin==0,"calm water supplies no ambient push or torque");
        g.settings.resetWaves();g.settings.turnWaves(6);
        float east=0,north=0;
        for(int tick=0;tick<240;tick++){g.visualTicks=tick;east+=Math.abs(SeaWaves.value(g,x,y,tick,1));north+=Math.abs(SeaWaves.value(g,x,y,tick,2));}
        check(east>north*2,"direction control rotates orbital force toward east");
        g.settings.resetWaves();g.settings.adjustWaves(8);
        g.tug.x=x;g.tug.y=y;g.tug.angle=0;
        float peak=0;
        for(int tick=0;tick<240;tick++){
            g.visualTicks=tick;w.floatBoats(g);
            float target=(w.height(g,x+g.tug.half,y,tick,g.tug)+w.height(g,x-g.tug.half,y,tick,g.tug)
                    +w.height(g,x,y+g.tug.radius,tick,g.tug)+w.height(g,x,y-g.tug.radius,tick,g.tug))*.25f;
            check(Math.abs(target-g.tug.heave)<.0001f,"strong-wave boat height follows current surface without temporal lag");peak=Math.max(peak,Math.abs(g.tug.heave));
        }
        check(peak>6,"strong waves are not flattened by a hidden height clamp");
        g.settings.developerWaves=true;g.pointerDown(93,30,90);g.pointerUp(93,30,90);
        g.pointerDown(93,30,140);g.pointerUp(93,30,140);check(g.settings.waveIntensity()==2.75f,"live wave control changes intensity");
        g.pointerDown(93,250,180);g.pointerUp(93,250,180);check(g.settings.waveBearing()==15,"live wave control changes bearing");
        g.pointerDown(93,250,180);g.pointerCancel(93);g.pointerUp(93,250,180);check(g.settings.waveBearing()==15,"cancelled wave tap does not change bearing");
        check(!g.pointer,"wave panel input never starts driving");
        g.settings.turnWaves(-2);check(g.settings.waveBearing()==345,"bearing wraps through north");
        head.lineAttached=true;w.affect(g,head);check(head.vx==0 && head.vy==0 && head.spin==0,"moored gameplay hull stays secured");
        Game a=new Game(),b=new Game();a.selectChallenge(Challenges.MONTROSE);b.selectChallenge(Challenges.MONTROSE);a.reset();b.reset();
        for(int t=0;t<180;t++){a.advance(1.0/60);b.advance(1.0/120);b.advance(1.0/120);}
        check(a.tug.x==b.tug.x && a.tug.y==b.tug.y && a.tug.heave==b.tug.heave && a.tug.spin==b.tug.spin,"render cadence does not change wave simulation");
        float px=a.tug.x,py=a.tug.y,heave=a.tug.heave;a.background();a.advance(.25);
        check(a.tug.x==px && a.tug.y==py && a.tug.heave==heave,"pause freezes wave-driven hull motion");
        System.out.println(checks+" shared wave phase, wake visibility, shelter and response assertions passed");
    }
}
