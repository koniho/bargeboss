package com.bargeboss.game;

/** One registered load, with a shared geometric foreground coaming cutoff. */
public final class CargoView {
    private static final ThreadLocal<float[]> CLIP=new ThreadLocal<float[]>(){
        protected float[] initialValue(){return new float[24];}
    };
    private CargoView(){}
    private static void edge(float[] q,int at,RiverCamera camera,float x,float y,float c,float s,float ax,float ay,float bx,float by){
        float x1=camera.screenX(x+c*ax-s*ay,y+s*ax+c*ay),y1=camera.screenY(x+c*ax-s*ay,y+s*ax+c*ay)-CargoArt.RIM*.81649658f*camera.scale;
        float x2=camera.screenX(x+c*bx-s*by,y+s*bx+c*by),y2=camera.screenY(x+c*bx-s*by,y+s*bx+c*by)-CargoArt.RIM*.81649658f*camera.scale;
        float low=Math.max(y1,y2)+200*camera.scale;
        q[at]=x1;q[at+1]=y1;q[at+2]=x2;q[at+3]=y2;q[at+4]=x2;q[at+5]=low;
        q[at+6]=x1;q[at+7]=y1;q[at+8]=x2;q[at+9]=low;q[at+10]=x1;q[at+11]=low;
    }
    /** Same accepted whole-load art and registration, translated vertically without scaling. */
    static void lifted(Game g,Painter canvas,Body b,float x,float y,float z,float angle){
        RiverPainter p=new RiverPainter(canvas,g.layout.camera);float rise=z*ArticulatedModel.Z_SCALE;
        float xx=x+RiverCamera.riseX(rise),yy=y+RiverCamera.riseY(rise),size=2*(b.half+b.radius)/BargeArt.HULL_FRACTION;
        int atlas=b.berth==0?SpriteAtlas.STEEL:b.berth==2?SpriteAtlas.BULK:SpriteAtlas.CONTAINER;
        boolean rim=z<CargoArt.RIM && b.unloadingTicks<CargoTransfer.LIFT;
        if(rim){float co=(float)StrictMath.cos(angle),si=(float)StrictMath.sin(angle),half=CargoArt.HOLD_LENGTH*.5f,beam=CargoArt.HOLD_WIDTH*.5f;float[] clip=CLIP.get();
            float bob=WaterSurface.lift(g,b),bx=b.x+RiverCamera.riseX(bob),by=b.y+RiverCamera.riseY(bob);
            float nearX=si-co>=0?half:-half,nearY=co+si>=0?beam:-beam;
            edge(clip,0,g.layout.camera,bx,by,co,si,nearX,-beam,nearX,beam);edge(clip,12,g.layout.camera,bx,by,co,si,-half,nearY,half,nearY);canvas.pushClipOutside(clip);
        }
        SceneryOcclusion.nonVessel(canvas,true);
        try{carrier(g,canvas,x,y,z,angle,false);SpriteAtlas.draw(p,atlas,angle,xx,yy,size,size,1);SurfaceLighting.directionalAt(g,p,atlas,angle,xx,yy,size,1,null,x,y);carrier(g,canvas,x,y,z,angle,true);}
        finally{SceneryOcclusion.nonVessel(canvas,false);if(rim)canvas.popClip();}
    }
    /** Rigid steel lifting tray supports bulk material as well as packaged loads. */
    private static void carrier(Game g,Painter p,float x,float y,float z,float angle,boolean front){
        float co=(float)StrictMath.cos(angle),si=(float)StrictMath.sin(angle);RiverCamera c=g.layout.camera;
        float[][] corners={{-31,-12},{31,-12},{31,12},{-31,12}};
        if(!front){float[] q=new float[8];for(int i=0;i<4;i++){float a=corners[i][0],b=corners[i][1],wx=x+co*a-si*b,wy=y+si*a+co*b;q[i*2]=c.screenX(wx,wy);q[i*2+1]=c.screenY(wx,wy)-(z+2)*ArticulatedModel.Z_SCALE*c.scale;}p.poly(q,SceneLighting.sceneryColor(0xff525d5c));return;}
        for(int i=0;i<4;i++){int j=(i+1)%4;float ax=corners[i][0],ay=corners[i][1],bx=corners[j][0],by=corners[j][1];
            float nx=by-ay,ny=ax-bx,wx=co*nx-si*ny,wy=si*nx+co*ny;if(-wx+wy<=0)continue;
            float x1=x+co*ax-si*ay,y1=y+si*ax+co*ay,x2=x+co*bx-si*by,y2=y+si*bx+co*by;
            float sx=c.screenX(x1,y1),sy=c.screenY(x1,y1),tx=c.screenX(x2,y2),ty=c.screenY(x2,y2),lo=z*ArticulatedModel.Z_SCALE*c.scale,hi=(z+4)*ArticulatedModel.Z_SCALE*c.scale;
            p.poly(new float[]{sx,sy-lo,tx,ty-lo,tx,ty-hi,sx,sy-hi},SceneLighting.sceneryColor(0xff777b66));
        }
    }
    public static void draw(Game g,Painter p,Body b,float x,float y,float size){
        float fill=CargoArt.quantity(b);if(fill<=0)return;
        int atlas=b.berth==0?SpriteAtlas.STEEL:b.berth==2?SpriteAtlas.BULK:SpriteAtlas.CONTAINER;
        RiverPainter river=p instanceof RiverPainter?(RiverPainter)p:null;
        if(river!=null){
            float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle),half=CargoArt.HOLD_LENGTH*.5f,beam=CargoArt.HOLD_WIDTH*.5f;
            float[] clip=CLIP.get();
            float nearX=s-c>=0?half:-half,nearY=c+s>=0?beam:-beam;
            edge(clip,0,river.camera,x,y,c,s,nearX,-beam,nearX,beam);
            edge(clip,12,river.camera,x,y,c,s,-half,nearY,half,nearY);
            river.canvas().pushClipOutside(clip);
        }
        try{
            SpriteAtlas.draw(p,atlas,b.angle,x,y,size,size,fill);
            SurfaceLighting.directional(g,p,atlas,b.angle,x,y,size,fill,b);
        }finally{if(river!=null)river.canvas().popClip();}
    }
}
