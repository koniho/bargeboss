package com.bargeboss.game;

/** Nonblocking, bounded rescue chatter. Delivery thanks survive simultaneous arrivals. */
final class ChasRadio {
    static final int SHOW_TICKS=420,FIRST_TOW_TICKS=360,GAP_TICKS=1320;
    // Paired calls: Chas aboard Never Wetter, then Alex aboard Rumrunner 2.
    private static final String[][] CALLS={
        {"THE CHILDREN HAVE FORMED A COMMITTEE.","THEY'VE VOTED THE STEREO OFF THE BOAT.","I HAVE BEEN DEMOTED TO EMOTIONAL BAGGAGE."},
        {"CHAS, THE COOLER GOT MORE VOTES THAN YOU.","IT FLOATS AND DOESN'T CONTROL THE PLAYLIST.","I'M BACKING IT FOR HARBOR MASTER."},
        {"I TRIED CHARGING THE BATTERY WITH REGRET.","THE VOLTMETER SAYS THAT ISN'T ELECTRICITY.","THEN WHY DOES IT FEEL SO POWERFUL, ALEX?"},
        {"I HAVE SIX ENERGY DRINKS AND ZERO VOLTS.","APPARENTLY THAT'S NOT HOW BATTERIES WORK.","THE CAPTAIN IS POWERED. THE BOAT IS NOT."},
        {"A SEAGULL HAS BEEN FOLLOWING US, ALEX.","I THINK IT'S FROM OUR INSURANCE COMPANY.","IT KEEPS TAKING NOTES ON A FRENCH FRY."},
        {"DON'T SIGN ANYTHING, CHAS.","THAT GULL ALREADY TOOK MY DEDUCTIBLE.","IT WAS MOST OF A BREAKFAST SANDWICH."},
        {"THE KIDS SAY THIS COUNTS AS A FIELD TRIP.","TODAY'S SUBJECT IS CONSEQUENCES.","APPARENTLY I'M THE VISUAL AID."},
        {"THEY ASKED IF WE COULD DO THIS AGAIN.","I SAID WE'RE BOOKING A BUS NEXT TIME.","YOU ARE NOT ALLOWED NEAR ITS STEREO."}
    };
    private final InstructionPanel panel=new InstructionPanel();
    private InstructionPanel.Content content;
    private int visible,wait=FIRST_TOW_TICKS,next,serial,pending,thanked;
    private int delivery=-1;
    private boolean assignment;
    void cutLoose(){assignment=true;}
    void delivered(int slot){int bit=1<<slot;if((thanked&bit)==0 && delivery!=slot)pending|=bit;}
    boolean pendingThanks(){return pending!=0 || delivery>=0;}
    InstructionPanel.Content content(){return content;}
    private void start(Game g,String[] lines,Body source,int deliveredSlot){
        if(content==null)panel.reset();
        delivery=deliveredSlot;visible=SHOW_TICKS;serial++;
        content=new InstructionPanel.Content("chas-tow-"+serial,lines[0],lines[1],lines[2],-1,0,
                (source==g.cargo[0]?"ALEX / ":"CHAS / ")+(source==null?"RADIO":MontroseRescue.name(source.berth)),"RADIO / RX");
        g.radio.receiveCrew(source);
    }
    void step(Game g){
        if(!MontroseRescue.cruisers(g) || g.rescue.touring || g.dialogue.active)return;
        if(visible>0 && --visible==0){if(delivery>=0)thanked|=1<<delivery;delivery=-1;content=null;}
        // Assignments and each earned thank-you take priority over casual chatter.
        if(g.radio.progress()>=1 && (visible==0 || delivery<0)){
            if(assignment){assignment=false;wait=FIRST_TOW_TICKS;
                start(g,new String[]{"SEPARATE BOATS. SHARED BAD DECISIONS.","RUMRUNNER 2 GOES TO C-38.","NEVER WETTER GOES TO C-22. THANK YOU!"},g.cargo[1],-1);
            }else if(pending!=0){int slot=(pending&1)!=0?0:1;pending&=~(1<<slot);
                start(g,slot==0?new String[]{"RUMRUNNER 2 IS HOME! YOU LEGEND!","THANK YOU! ENERGY DRINKS ARE ON ME.","THEY WORK ON CAPTAINS. NOT BOAT BATTERIES."}:
                    new String[]{"NEVER WETTER IS SAFE! THANK YOU!","FIREBALL'S ON ME WHEN YOU'RE OFF DUTY.","THE COOLER HAS APPROVED THE EXPENSE."},g.cargo[slot],slot);
                wait=GAP_TICKS;
            }
        }
        if(visible==0 && !assignment && pending==0 && !g.rescue.rafted && g.tug.speed()>6){
            Body source=null;for(int i=0;i<2;i++){Body b=g.cargo[i];if(b!=null && !b.deliveryRecorded && g.tow.connected(b,g.tug)){source=b;break;}}
            if(source!=null && --wait<=0 && g.radio.progress()>=1){int call=next++%CALLS.length;start(g,CALLS[call],g.cargo[call%2==0?1:0],-1);wait=call%2==0?120:GAP_TICKS;}
        }
        panel.step(content);
    }
    void draw(Game g,Painter p){if(serial>0 && MontroseRescue.cruisers(g) && !g.dialogue.active && !g.rescue.touring)panel.draw(g,p);}
}
