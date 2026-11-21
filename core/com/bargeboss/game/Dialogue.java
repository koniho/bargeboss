package com.bargeboss.game;

/** Shared modal radio conversation. Choices commit on release; no world input leaks through. */
public final class Dialogue {
    private final InstructionPanel panel=new InstructionPanel();
    private String speaker,title,first,second;
    private String[] choices=new String[0];
    private int speakerKind=RadioAudio.TEACHER;
    boolean instructorConversation(){return active && speakerKind==RadioAudio.TEACHER;}
    private int pointer=-1,pressed=-1,result=-1,revision,ticks;
    public boolean active;
    public void show(Game g,String speaker,String prompt,String detail,String... answers){
        showFrom(g,RadioAudio.TEACHER,speaker,prompt,detail,answers);
    }
    public void showFrom(Game g,int transmitter,String speaker,String prompt,String detail,String... answers){
        if(answers==null || answers.length<1 || answers.length>4)throw new IllegalArgumentException("Dialogue needs 1 to 4 choices");
        for(String answer:answers)if(answer==null || answer.trim().isEmpty())throw new IllegalArgumentException("Empty choice");
        g.cancelInputs();active=true;speakerKind=transmitter;this.speaker=speaker;title=prompt;first=detail;second="SELECT A REPLY TO TRANSMIT.";
        choices=answers.clone();result=-1;ticks=0;revision++;cancel();panel.reset();
        panel.step(content());if(transmitter==RadioAudio.BRIDGE)g.radio.receiveBridge();else g.radio.transmit(RadioAudio.TEACHER);
    }
    public void close(Game g){active=false;cancel();g.cancelInputs();}
    public void cancel(){pointer=pressed=-1;}
    public int count(){return choices.length;}
    public int result(){return result;}
    public boolean ready(){return active && result<0 && ticks>=InstructionPanel.OPEN_TICKS;}
    InstructionPanel.Content content(){return new InstructionPanel.Content("radio-"+revision,title,first,second,-1,0,speaker,"RADIO / RX",active && result<0);}
    public void step(){if(!active)return;ticks++;panel.step(content());}
    public Layout.Box choiceBox(Game g,int i){
        float width=(g.layout.width-52)*.5f,left=18;
        int rows=(count()+1)/2;
        return new Layout.Box(left+(i%2)*(width+16),g.layout.height-32-rows*62+(i/2)*62,width,50);
    }
    private int hit(Game g,float x,float y){
        for(int i=0;i<count();i++){
            Layout.Box b=choiceBox(g,i);
            if(b.contains(x,y)){
                float edge=14*Math.abs((y-b.y-b.h/2)/(b.h/2));
                if(i%2==0?x<=b.x+b.w-edge:x>=b.x+edge)return i;
            }
        }return -1;
    }
    public void down(Game g,int id,float x,float y){if(!ready() || pointer!=-1)return;pointer=id;pressed=hit(g,x,y);}
    public void move(Game g,int id,float x,float y){if(id==pointer && hit(g,x,y)!=pressed)pressed=-1;}
    public void up(Game g,int id,float x,float y){
        if(id!=pointer)return;
        if(ready() && pressed>=0 && hit(g,x,y)==pressed){result=pressed;g.radio.transmit(RadioAudio.REPLY);g.haptic(Game.HAPTIC_TICK);}
        cancel();
    }
    public void cancel(int id){if(id==pointer)cancel();}
    public void draw(Game g,Painter p){
        if(!active)return;panel.draw(g,p);
        float alpha=Physics.clamp(ticks/18f,0,1);
        for(int i=0;i<count();i++){
            Layout.Box b=choiceBox(g,i);boolean selected=result==i || pressed==i;
            Painter glass=new OpacityPainter(p,alpha*(result>=0 && result!=i?.35f:1));
            boolean left=i%2==0;
            float rear=left?b.x:b.x+b.w,tip=left?b.x+b.w:b.x,shoulder=tip+(left?-14:14);
            float[] q={rear,b.y,shoulder,b.y,tip,b.y+b.h/2,shoulder,b.y+b.h,rear,b.y+b.h};
            PilotGlass.plate(glass,q,selected?PilotGlass.AMBER:PilotGlass.FILL,selected?0xffad7730:PilotGlass.LINE,1.5f);
            int ink=PilotGlass.buttonText(selected);
            glass.circle(b.x+32,b.y+25,14,selected?PilotGlass.NAVY:0xff26334a);
            Lettering.draw(glass,"0"+(i+1),b.x+26,b.y+18,11,PilotGlass.AMBER);
            float size=Math.min(14,14*(b.w-92)/Math.max(1,Lettering.width(choices[i],14)));
            Lettering.draw(glass,choices[i],b.x+61,b.y+17,size,ink);
        }
    }
}
