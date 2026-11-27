package com.bargeboss.game;
/** Nonblocking, once-per-speeder calls. A bounded queue preserves simultaneous handovers. */
final class PoliceRadio {
    static final int SHOW_TICKS=300;
    private final InstructionPanel panel=new InstructionPanel();
    private final int[] queue=new int[WakePatrolJob.SPEEDERS*2];
    private int read,write,count,seen,visible,serial;
    private InstructionPanel.Content content;
    void lassoed(int boat){enqueue(boat);}
    void delivered(int boat){enqueue(WakePatrolJob.SPEEDERS+boat);}
    private void enqueue(int event){int bit=1<<event;if((seen&bit)!=0)return;seen|=bit;queue[write]=event;write=(write+1)%queue.length;count++;}
    boolean busy(){return count>0 || visible>0;}
    int pending(){return count;}
    InstructionPanel.Content content(){return content;}
    void step(Game g){
        if(g.dialogue.active)return;
        if(visible>0 && --visible==0)content=null;
        if(visible==0 && count>0 && g.radio.progress()>=1){
            int event=queue[read];read=(read+1)%queue.length;count--;boolean drop=event>=WakePatrolJob.SPEEDERS;int boat=event%WakePatrolJob.SPEEDERS;
            String[] caught={"NICE CATCH. BRING THAT ONE TO M3.","LINE SECURE. WE HAVE ROOM FOR ONE MORE.","GOOD CATCH. THEIR SPEEDING DAYS ARE OVER."};
            String[] received={"WE'LL TAKE IT FROM HERE, CAPTAIN.","ANOTHER ONE OFF THE FAST LANE. THANK YOU.","HANDOVER COMPLETE. NICE WORK OUT THERE."};
            panel.reset();
            content=new InstructionPanel.Content("police-"+(++serial),drop?received[boat%3]:caught[boat%3],
                    drop?"OUR CREW HAS THE BOAT. YOU'RE CLEAR TO GO.":"TOW THEM INTO THE BLUE IMPOUND RING.",
                    drop?"M3 / CHICAGO POLICE MARINE UNIT":"KEEP IT SLOW THROUGH THE MOORINGS.",-1,0,"POLICE CAPTAIN / M3","RADIO / RX");
            visible=SHOW_TICKS;g.radio.receivePolice();
        }
        panel.step(content);
    }
    void draw(Game g,Painter p){if(serial>0 && !g.dialogue.active)panel.draw(g,p);}
}
