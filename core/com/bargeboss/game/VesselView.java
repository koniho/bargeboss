package com.bargeboss.game;

/** Rotating hulls with screen-up superstructures: shared 2.5D arcade vessel art. */
public final class VesselView {
    private static final int INK=VisualStyle.OUTLINE, CREAM=VisualStyle.CREAM;
    private final Painter p;
    private final Body b;
    private final float c,s,x,y;
    private VesselView(Painter p,Body b,float bob) {
        this.p=p; this.b=b; c=(float)StrictMath.cos(b.angle); s=(float)StrictMath.sin(b.angle);
        x=b.x; y=b.y+bob;
    }
    public static void draw(Game g,Painter p,Body b,boolean tug) {
        int tick=g.visualTicks;
        float bob=b.lineAttached?0:(float)StrictMath.sin(tick*.055+b.id*1.7)*(tug?.55f:.35f);
        VesselView v=new VesselView(p,b,bob);
        ArcadeView.wake(p,b,tick);
        if(tug) v.tug(g,tick); else v.barge(g,tick);
    }
    private float px(float xx,float yy) { return x+xx*c-yy*s; }
    private float py(float xx,float yy,float height) { return y+xx*s+yy*c-height; }
    private float[] points(float[] local,float height,float ox,float oy) {
        float[] result=new float[local.length];
        for(int i=0;i<local.length;i+=2) { result[i]=px(local[i],local[i+1])+ox; result[i+1]=py(local[i],local[i+1],height)+oy; }
        return result;
    }
    private void shape(float[] local,float h,int color) { p.poly(points(local,h,0,0),color); }
    private void line(float ax,float ay,float bx,float by,float h,float width,int color) {
        p.line(px(ax,ay),py(ax,ay,h),px(bx,by),py(bx,by,h),width,color);
    }
    private void dot(float xx,float yy,float h,float radius,int color) { p.circle(px(xx,yy),py(xx,yy,h),radius,color); }
    private void slab(float[] local,float h,int top,int side) {
        float[] bottom=points(local,0,0,0),upper=points(local,h,0,0);
        p.poly(bottom,INK);
        for(int i=0;i<local.length;i+=2) {
            int j=(i+2)%local.length;
            p.poly(new float[]{bottom[i],bottom[i+1],bottom[j],bottom[j+1],upper[j],upper[j+1],upper[i],upper[i+1]},side);
        }
        p.poly(upper,top);
    }
    private void box(float xx,float yy,float w,float d,float h,int top,int side) {
        slab(new float[]{xx,yy,xx+w,yy,xx+w,yy+d,xx,yy+d},h,top,side);
        line(xx,yy,xx+w,yy,h,1,0x88fff3ce);
    }
    private void raisedBox(float xx,float yy,float w,float d,float base,float h,int top,int side) {
        float[] local={xx,yy,xx+w,yy,xx+w,yy+d,xx,yy+d};
        float[] bottom=points(local,base,0,0),upper=points(local,h,0,0);
        for(int i=0;i<local.length;i+=2) {
            int j=(i+2)%local.length;
            p.poly(new float[]{bottom[i],bottom[i+1],bottom[j],bottom[j+1],upper[j],upper[j+1],upper[i],upper[i+1]},side);
        }
        p.poly(upper,top);
    }
    private void tug(Game g,int tick) {
        boolean working=(b==g.tug && (g.pushing || g.navigating)) || b.speed()>8;
        boolean pushing=working && (g.control==Game.PUSHING || g.control==Game.STEERING || g.control==Game.ALIGNING);
        float[] hull={-23,-10,-18,-13,11,-13,21,-9,25,0,21,9,11,13,-18,13,-23,9};
        p.poly(points(hull,0,4,6),0x55030e20);
        // Far-side fenders sit behind the deck; near-side ones cover its shaded wall.
        tires(false);
        slab(hull,3,0xff8b251e,INK);
        shape(new float[]{-21,-9,-17,-11,11,-11,20,-7,23,0,20,7,11,11,-17,11,-21,8},4,VisualStyle.RED);
        line(-18,-11,10,-11,4,2,0xffffae46);
        line(11,-11,21,-6,4,2,0xffffcc57);
        line(-18,11,11,11,3,1.5f,0xfffa713f);
        // Timber foredeck, stout pushing bumper, cleats and lit navigation lamps.
        box(12,-7,7,14,4,0xffc8924e,0xff80552f);
        for(int i=13;i<20;i+=2) line(i,-6,i,6,4,.6f,0xff71573b);
        line(22,-7,22,7,5,4.5f,INK);
        line(23,-5,23,5,6,1.4f,0xff687888);
        for(int side=-1;side<=1;side+=2) {
            line(-18,side*8,-13,side*8,5,1.6f,INK);
            dot(15,side*10,5,2.5f,0x44fff1a0);
            dot(15,side*10,5,1.3f,side<0?0xff7fffc5:0xffff7751);
        }
        // Raised cream accommodation deck and wheelhouse with dark glazed windows.
        box(-9,-7,20,14,9,0xffe1ceb1,0xffb1a08d);
        box(-8,-6,18,12,10,CREAM,0xffe9d9b7);
        box(-4,-6,13,12,17,0xfff2e5ca,0xffe0d1b6);
        for(int side=-1;side<=1;side+=2) {
            // Glazing occupies the wall face rather than a broad round-capped stroke.
            for(int i=0;i<3;i++) {
                float xx=-3+i*4,yy=side*6;
                p.poly(new float[]{px(xx,yy),py(xx,yy,16),px(xx+2.6f,yy),py(xx+2.6f,yy,16),
                        px(xx+2.6f,yy),py(xx+2.6f,yy,11.5f),px(xx,yy),py(xx,yy,11.5f)},INK);
                p.line(px(xx+.4f,yy),py(xx+.4f,yy,15),px(xx+1.6f,yy),py(xx+1.6f,yy,15),.8f,0xff8fc6d2);
            }
            line(-8,side*7,10,side*7,3,3,VisualStyle.RED);
            dot(-5,side*7,7,1.1f,0xff637c8b);
            dot(5,side*7,7,1.1f,0xffefc878);
        }
        for(int i=0;i<2;i++) {
            float yy=-4+i*4.5f;
            p.poly(new float[]{px(9,yy),py(9,yy,16),px(9,yy+3),py(9,yy+3,16),
                    px(9,yy+3),py(9,yy+3,11.5f),px(9,yy),py(9,yy,11.5f)},INK);
            line(9,yy+.4f,9,yy+2.5f,15,.8f,0xffa4d7dc);
        }
        raisedBox(-5,-7,15,14,17,19,0xffc63825,0xff6f251f);
        raisedBox(-4,-6,13,12,19,20,CREAM,0xffe0c49e);
        for(int i=0;i<3;i++) line(-2+i*2,-3,-2+i*2,0,20,.8f,0xff61717b);
        line(-3,-5,7,-5,20,1.2f,0xffffffff);
        // Rear stack has a red collar, black cap and phase-dependent exhaust.
        box(-17,-4,6,7,15,0xff223347,INK);
        box(-17.5f,-4.5f,7,8,17,0xff172334,INK);
        line(-17,-4,-11,-4,11,3,VisualStyle.RED);
        float mastX=px(0,0),mastY=py(0,0,20);
        p.line(mastX,mastY,mastX,mastY-13,1.2f,INK);
        p.line(mastX-5,mastY-7,mastX+5,mastY-7,1.1f,CREAM);
        p.rect(mastX-2,mastY-16,4,3,CREAM);
        p.rect(mastX+1,mastY-12,5,3,VisualStyle.RED);
        dot(6,0,22,1.5f,VisualStyle.GOLD);
        tires(true);
        if(g.damageCooldown>50 && b==g.tug && (tick/5)%2==0)
            shape(hull,4,0x55ff7049);
        exhaust(tick,pushing?1.3f:working?1:.35f,pushing);
        if(pushing) {
            for(int i=0;i<4;i++) {
                float t=((tick+i*9)%30)/30f;
                float xx=px(22+t*9,(i%2==0?-1:1)*(6+t*10));
                float yy=py(22+t*9,(i%2==0?-1:1)*(6+t*10),0);
                p.rect(xx,yy,2+t*2,1.5f,0xbbe5ffff);
            }
        }
        if(b==g.tug && g.braking && b.speed()>8) {
            // Reverse wash near the stern makes braking visibly different from coasting.
            for(int i=0;i<5;i++) {
                float t=((tick+i*7)%25)/25f;
                float xx=px(-22-t*15,(i-2)*3),yy=py(-22-t*15,(i-2)*3,0);
                p.rect(xx,yy,2+3*t,1.5f,0xaae5ffff);
            }
        }
    }
    private void tires(boolean front) {
        for(int side=-1;side<=1;side+=2) for(int i=0;i<4;i++) {
            float xx=-18+i*10,yy=side*13;
            if((xx*s+yy*c>=0)!=front) continue;
            float tx=px(xx,yy),ty=py(xx,yy,1);
            p.circle(tx+.5f,ty+1,4.6f,INK); p.circle(tx,ty,3.6f,0xff344255);
            p.circle(tx,ty,2.2f,0xff091120); p.rect(tx-1,ty-3,2,1,0xff6f8193);
        }
    }
    private void exhaust(int tick,float power,boolean strain) {
        float sx=px(-14,0),sy=py(-14,0,19);
        for(int i=5;i>=0;i--) {
            float t=((tick*(strain?2:1)+i*13)%84)/84f;
            float radius=1.3f+t*(3+power*3);
            float xx=sx-5-t*(12+power*12)+(float)StrictMath.sin(t*7)*2;
            float yy=sy-t*(13+power*16);
            int alpha=(int)((1-t)*(strain?155:105));
            int color=(alpha<<24)|(strain?0x708493:0xc4d2d2);
            // Cross-shaped pixel clusters avoid soft particle gradients.
            p.rect(xx-radius,yy-radius*.45f,radius*2,radius*.9f,color);
            p.rect(xx-radius*.5f,yy-radius,radius,radius*2,color);
        }
    }
    private void barge(Game g,int tick) {
        float[] outline={-32,-12,-28,-15,26,-15,33,-8,33,8,26,15,-28,15,-32,11};
        p.poly(points(outline,0,4,5),0x66030c1a);
        slab(outline,3,0xffb36d3d,INK);
        shape(new float[]{-29,-11,-26,-12,25,-12,29,-7,29,7,25,12,-26,12,-29,9},4,0xff683e2e);
        box(-26,-10,51,20,4,0xff9a643f,0xff6b4630);
        for(int i=-24;i<25;i+=7) line(i,-9,i,9,4,1,0xff73472e);
        line(-28,-13,26,-13,5,2,0xffe3a65c);
        line(-29,13,26,13,4,2,0xffe7a553);
        line(-30,-9,-30,9,5,2,0xffc88a43);
        line(31,-7,31,7,5,2,0xffedb468);
        for(int side=-1;side<=1;side+=2) for(int i=0;i<5;i++) {
            float xx=-26+i*13;
            line(xx,side*12,xx,side*15,5,1.4f,INK);
            dot(xx,side*13,5,.9f,0xffffdc88);
        }
        int remaining=b.unloadingTicks==0?3:Math.max(0,3-(b.unloadingTicks+50)/60);
        if(b.berth==0) steel(remaining);
        else if(b.berth==1) containers(remaining);
        else if(b.berth==2) bulk(remaining);
        else { // Crossing workboats are cargo ferries, not player cargo contracts.
            box(-21,-9,33,18,7,0xff618284,0xff324a55);
            box(-18,-7,11,14,12,0xffe5d8b8,0xff9c9d95);
        }
        if(b.lineAttached) {
            float blink=(tick/18)%2==0?1:.6f;
            dot(-28,-9,6,2.1f,0xffd3e8a1);
            dot(28,8,6,1.5f,blink==1?0xffffe589:0xffac975d);
        } else if(b.berth>=0 && g.canTie(b)) {
            float pulse=(float)StrictMath.sin(tick*.12)*.5f+.5f;
            dot(-28,-9,6,1.7f+pulse,0xffffd34e);
        }
    }
    private void containers(int count) {
        int[] tops={0xffdf5038,0xff319cb2,0xffe5b347};
        int[] sides={0xff922a28,0xff235e83,0xff9b6c32};
        for(int i=0;i<count;i++) {
            float xx=-25+i*17;
            box(xx,-9,15,18,11,tops[i],sides[i]);
            for(int rib=3;rib<15;rib+=3) line(xx+rib,-8,xx+rib,8,11,1,0x66ffe0aa);
            line(xx+1,8,xx+14,8,8,1,0xff412d30);
            dot(xx+2,-6,11,.7f,CREAM); dot(xx+12,6,11,.7f,CREAM);
        }
    }
    private void steel(int count) {
        for(int i=0;i<count;i++) {
            float yy=-7+i*6;
            // Long bundled rolled-steel cylinders catch cool highlights across their upper edge.
            box(-23,yy,45,4,8,0xffa9bdc3,0xff4e657b);
            line(-22,yy+1,21,yy+1,9,1.3f,0xffdce3d9);
            dot(-23,yy+2,8,2.8f,0xff263a51); dot(-23,yy+2,8,1.3f,0xff8595a4);
            line(-11,yy,-11,yy+4,9,1.4f,0xffd1a252);
            line(12,yy,12,yy+4,9,1.4f,0xffd1a252);
        }
    }
    private void bulk(int count) {
        for(int i=0;i<count;i++) {
            float xx=-24+i*17;
            shape(new float[]{xx,-8,xx+7,-10,xx+15,-6,xx+16,8,xx+2,10},5,0xffa96731);
            shape(new float[]{xx,-7,xx+7,-7,xx+10,-1,xx+15,8,xx+2,8},10,0xffe4b865);
            shape(new float[]{xx+2,-6,xx+7,-7,xx+10,-1,xx+6,4},11,0xffffd88a);
            for(int j=0;j<6;j++) dot(xx+3+(j*7)%11,-5+(j*5)%12,11,.7f,j%2==0?0xffbf914c:0xffffe3a6);
        }
    }
}
