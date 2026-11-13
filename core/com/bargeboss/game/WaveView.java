package com.bargeboss.game;

/** Moving, curved wavelets shared by the harbor and title; no repeated water strips. */
public final class WaveView {
    private WaveView() {}
    public static void draw(Painter p,int tick,float left,float top,float width,float height,float scale,float origin) {
        p.rect(left,top,width,height,0xff205d6a);
        if(height<=0) return;
        float cellX=100*scale,cellY=46*scale;
        float drift=(origin-tick*.18f*scale)%cellX;
        int columns=(int)(width/cellX)+3,rows=(int)(height/cellY)+2;
        for(int row=-1;row<rows;row++) for(int col=-1;col<columns;col++) {
            int seed=(col+19)*73+(row+23)*137;
            float phase=tick*.032f+seed*.37f;
            float life=.5f+.5f*(float)StrictMath.sin(phase);
            float x=left+col*cellX+drift+(row%2)*cellX*.5f;
            float y=top+(row+.6f)*cellY+(float)StrictMath.sin(phase*.61f)*7*scale;
            float span=(42+seed%31)*scale,amplitude=(4+life*6)*scale;
            float slope=(float)StrictMath.sin(seed)*.08f;
            float[] body=new float[52];
            for(int i=0;i<=12;i++) {
                float t=i/12f,xx=x+(t-.5f)*span;
                float crest=y-(float)StrictMath.sin(t*Math.PI)*amplitude+(t-.5f)*span*slope;
                float thickness=(float)StrictMath.sin(t*Math.PI)*(5+life*4)*scale;
                body[i*2]=xx;body[i*2+1]=Physics.clamp(crest,top,top+height);
                body[(25-i)*2]=xx;body[(25-i)*2+1]=Physics.clamp(crest+thickness,top,top+height);
                if(i>0 && y>top+12*scale && y<top+height-10*scale) {
                    float prev=(i-1)/12f;
                    float yy=y-(float)StrictMath.sin(prev*Math.PI)*amplitude+(prev-.5f)*span*slope;
                    int alpha=(int)((28+65*life)*(float)StrictMath.sin((t+prev)*.5*Math.PI));
                    p.line(x+(prev-.5f)*span,yy,xx,crest,1.4f*scale,(alpha<<24)|0x8bd4ce);
                }
            }
            p.poly(body,((int)(24+23*life)<<24)|0x123d50);
        }
    }
}
