package com.bargeboss.game;

/** Clean stern-first slip approach and an interactive overview-chart exercise. */
public final class NavigationPractice {
    public static final float SLIP_X=-1940,SLIP_Y=300,APPROACH_X=-1740;
    public static final float OPEN_X=-1740,OPEN_Y=650,TARGET_X=-1320,TARGET_Y=260;
    public int slipPhase,mapPhase,attempts;
    public boolean retry;
    public float reverseTravel;
    private float previousX;
    private int mapEdits;
    void reset(Game g){slipPhase=mapPhase=attempts=0;retry=false;reverseTravel=0;previousX=g.tug.x;}
    boolean slip(Game g){
        Body b=g.tug;float dx=b.x-previousX;previousX=b.x;
        if(b.x<TrainingChannel.BASIN_LEFT+35 && Math.abs(b.y-SLIP_Y)<100 && g.splashes.impactOn(b)>=25){
            if(!retry){attempts++;g.cancelInputs();}retry=true;reverseTravel=0;
        }
        if(retry){
            // Full hull, not just its center, must clear the mouth before another attempt.
            if(b.x-b.half-b.radius>TrainingChannel.BASIN_LEFT+20){retry=false;slipPhase=0;g.haptic(Game.HAPTIC_TICK);}
            return false;
        }
        if(slipPhase==0 && Physics.length(b.x-APPROACH_X,b.y-SLIP_Y)<35){slipPhase=1;g.cancelInputs();}
        if(slipPhase==1 && Math.abs(Physics.angle(b.angle))<.20f && Math.abs(b.y-SLIP_Y)<30){slipPhase=2;reverseTravel=0;g.cancelInputs();}
        if(slipPhase==2 && g.navigating && g.reversing && dx<0 && dx>-10)reverseTravel-=dx;
        return slipPhase==2 && reverseTravel>=90 && b.x<SLIP_X+15 && b.x>SLIP_X-22
                && Math.abs(b.y-SLIP_Y)<20 && Math.abs(Physics.angle(b.angle))<.25f;
    }
    boolean map(Game g){
        if(mapPhase==0 && g.tug.x>TrainingChannel.BASIN_LEFT+50){mapPhase=1;g.cancelInputs();}
        if(mapPhase==1 && Physics.length(g.tug.x-OPEN_X,g.tug.y-OPEN_Y)<45){
            mapPhase=2;g.cancelInputs();g.overview.lessonZoomIn(g);mapEdits=g.overview.edits;
        }
        if(mapPhase==2 && g.overview.edits>mapEdits && g.overview.zoom()<1.7f){mapPhase=3;g.haptic(Game.HAPTIC_TICK);}
        return mapPhase==3 && Physics.length(g.tug.x-TARGET_X,g.tug.y-TARGET_Y)<40;
    }
    float targetX(Game g){
        if(g.tutorial.lesson==Tutorial.SLIP)return retry || slipPhase==0?APPROACH_X:slipPhase==1?g.tug.x+100:SLIP_X;
        return mapPhase==0?APPROACH_X:mapPhase==1?OPEN_X:TARGET_X;
    }
    float targetY(Game g){return g.tutorial.lesson==Tutorial.SLIP || mapPhase==0?SLIP_Y:mapPhase==1?OPEN_Y:TARGET_Y;}
    InstructionPanel.Content content(Game g){
        boolean slip=g.tutorial.lesson==Tutorial.SLIP;
        String title,first,second;
        if(slip){
            title=retry?"PULL CLEAR AND TRY AGAIN":"BACK INTO THE SLIP";
            first=retry?"THAT WAS A HARD BUMP. DRIVE FULLY OUT.":slipPhase==0?"APPROACH THE MARKER OUTSIDE THE SLIP.":slipPhase==1?"POINT YOUR BOW AWAY FROM THE SLIP.":"RELEASE. SELECT AMBER. BACK IN GENTLY.";
            second=retry?"THE NEXT ATTEMPT STARTS OUTSIDE.":"KEEP CLEAR OF THE WALLS. SMALL INPUTS.";
        }else{
            title=mapPhase<2?"HEAD INTO OPEN WATER":mapPhase==2?"USE YOUR OVERVIEW MAP":"FIND THE CHART TARGET";
            first=mapPhase==0?"DRIVE FORWARD OUT OF THE SLIP.":mapPhase==1?"MEET AT THE OPEN-WATER MARKER.":mapPhase==2?"DRAG THE MAP SLIDER LEFT TO ZOOM OUT.":"STEER TO THE AMBER DIAMOND ON THE MAP.";
            second=mapPhase<2?"WE'LL PRACTICE READING THE CHART.":"YOUR TUG IS YELLOW. THE MAP FOLLOWS YOU.";
        }
        return new InstructionPanel.Content("navigation-"+g.tutorial.lesson+":"+slipPhase+":"+retry+":"+mapPhase,title,first,second,g.tutorial.lesson,0);
    }
}
