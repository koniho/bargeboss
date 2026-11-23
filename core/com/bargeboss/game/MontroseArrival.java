package com.bargeboss.game;

/** One continuous south-to-north harbor arrival before Gavin's opening dispatch. */
final class MontroseArrival {
    static final int PAN_TICKS=360;
    static final float X=MontroseHarbor.mapX(453),START_Y=MontroseHarbor.mapY(1500),END_Y=MontroseHarbor.mapY(1240);
    int ticks;boolean entering,welcoming;
    boolean active(){return entering||welcoming;}
    void clear(){ticks=0;entering=welcoming=false;}
    void start(Game g){clear();entering=true;g.cancelInputs();g.tug.x=X;g.tug.y=START_Y;g.tug.angle=-(float)Math.PI/2;g.tug.vx=g.tug.vy=g.tug.spin=0;camera(g);}
    void camera(Game g){
        float u=Math.min(1,ticks/(float)PAN_TICKS),ease=u*u*(3-2*u);
        float y=MontroseHarbor.mapY(1540)+(MontroseHarbor.mapY(1190)-MontroseHarbor.mapY(1540))*ease;
        g.layout=Layout.focused(g.layout.width,g.layout.height,X,y,Game.TRANSIT_ZOOM*MontroseHarbor.ZOOM_MULTIPLIER,g.layout.width*.5f,g.layout.height*.52f);
    }
    void step(Game g){
        if(entering){
            ticks++;float u=Math.min(1,ticks/(float)PAN_TICKS),ease=u*u*(3-2*u);
            g.tug.x=X;g.tug.y=START_Y+(END_Y-START_Y)*ease;g.tug.angle=-(float)Math.PI/2;
            g.tug.vx=0;g.tug.vy=(END_Y-START_Y)*6*u*(1-u)*60/PAN_TICKS;g.tug.spin=0;
            g.tug.engineLoad=Physics.clamp(-g.tug.vy/100,0,.8f);camera(g);
            if(ticks==PAN_TICKS){entering=false;welcoming=true;g.tug.vy=g.tug.engineLoad=0;
                g.dialogue.showFrom(g,RadioAudio.DOCKMASTER,"GAVIN / MONTROSE DOCKMASTER","WELCOME TO MONTROSE HARBOR, CAPTAIN!",
                    "GAVIN HERE. COME ON IN - WE'VE GOT WORK FOR YOU.","GOOD TO BE HERE, GAVIN.");}
        }else if(welcoming && g.dialogue.result()>=0){
            welcoming=false;g.dialogue.close(g);g.cancelInputs();MontroseDockmaster.dispatch(g);
        }
    }
}
