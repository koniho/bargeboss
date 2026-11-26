package com.bargeboss.game;

/** Nonblocking emergency chatter, followed by a player-led relief exchange. */
final class FireRescueRadio {
    private int wait=360,visible,serial,line,category,ending,ticks,reply;
    private final int[] next=new int[3];
    private static final String[][] CALLS={
        {"CAPTAIN! THE WOOD IS STILL VERY FLAMMABLE!","FETCH THE FIRE BARGE! PREFERABLY YESTERDAY!","MY CALM RADIO VOICE HAS LEFT THE HARBOR!"},
        {"YES! BRING THE WET BOAT BACK. QUICKLY!","THAT IS OUR ONLY FIRE BARGE. NO PRESSURE!","FASTER, CAPTAIN! SAFELY FASTER!"},
        {"WATER ON THE WOOD! ALL OF THE WOOD!","KEEP SPRAYING! IT IS STILL TOO FESTIVE!","LESS BONFIRE! MORE EXTREMELY WET TIMBER!"}
    };
    void reset(){wait=360;visible=serial=line=category=ending=ticks=reply=0;for(int i=0;i<next.length;i++)next[i]=0;}
    void step(Game g){
        FireworksLesson f=g.tutorial.fireworks;
        if(f.done){finish(g);return;}
        if(visible>0)visible--;
        if(f.heat<=0 || g.tutorial.lesson<Tutorial.FIRE_TOW || g.tutorial.lesson>Tutorial.EXTINGUISH)return;
        if(g.dialogue.active || g.tutorial.instructorRadio.phase!=0 || g.tutorial.feedback.active() || g.radio.progress()<1)return;
        if(--wait>0)return;
        category=g.tutorial.lesson==Tutorial.EXTINGUISH?2:Tutorial.direct(g,g.cargo[3])?1:0;
        line=next[category]++%CALLS[category].length;serial++;visible=360;wait=720;
        g.radio.transmit(RadioAudio.TEACHER);
    }
    private void finish(Game g){
        if(ending==4)return;
        if(ending==0){
            if(g.dialogue.active || g.tutorial.instructorRadio.phase!=0)return;
            visible=0;ending=1;ticks=0;g.radio.transmit(RadioAudio.TEACHER);
        }else if(ending==1 && ++ticks>=210){
            ending=2;ticks=0;
            g.dialogue.show(g,"INSTRUCTOR / GREEN TUG","EVERYONE STILL HAS EYEBROWS?","FIRE OUT. WOOD DAMP. HOW ARE WE FEELING?",
                    "A PERFECTLY NORMAL GRADUATION.","NEXT TIME, JUST CONFETTI.","DO I GET A FIREFIGHTER STRIPE?");
        }else if(ending==2 && g.dialogue.result()>=0 && ++ticks>=45){
            reply=g.dialogue.result();g.dialogue.close(g);ending=3;ticks=0;g.radio.transmit(RadioAudio.TEACHER);
        }else if(ending==3 && ++ticks>=240)ending=4;
    }
    boolean complete(){return ending==4;}
    void conclude(){visible=0;ending=4;}
    InstructionPanel.Content content(Game g,String instruction){
        if(ending==1 || ending==3){
            String title=ending==1?"IT'S OUT! OH, THANK GOODNESS.":"WELCOME ABOARD, CAPTAIN.";
            String detail=ending==1?"I CAN STOP HOLDING MY BREATH NOW.":reply==0?"YES. COMPLETELY ROUTINE. NO NOTES.":reply==1?"PAPER CONFETTI. ACTUALLY... NO CONFETTI.":"ONE STRIPE. SLIGHTLY SINGED. WELL EARNED.";
            return new InstructionPanel.Content("fire-relief-"+ending,title,detail,ending==1?"YOU SAVED THE DOCK. AND MY EYEBROWS.":"GOOD WORK. LET'S NEVER DO THAT AGAIN.",-1,0);
        }
        if(visible==0 || ending!=0)return null;
        return new InstructionPanel.Content("fire-hurry-"+serial,"CAPTAIN! A SMALL EMERGENCY!",CALLS[category][line],instruction,-1,0,"INSTRUCTOR / CHANNEL 08","FIRE RESPONSE");
    }
}
