package com.bargeboss.game;

/** Six prepared marina hulls. Local +X is bow, +Z up; origin is the water plane.
 * Model topology is shared by every instance; only rigid transforms change. */
final class HarborBoatModel {
    static final int FABRIC=0xff010203;
    private static final HarborBoatModel[] MODELS=new HarborBoatModel[6];
    private final ArticulatedModel.Builder builder=new ArticulatedModel.Builder();
    private final int root=builder.node("hull",-1,0,0,0,0,0);
    final ArticulatedModel.Rigid rigid;
    // Stable hull-local attachment metadata. These idle boats emit no lights/smoke.
    final float[][] lights=new float[0][],smokestacks=new float[0][],tires=new float[0][];
    final float[][] towEyes,fenders;
    static final String[] TOW_IDS={"stern-cleat","bow-cleat"};
    static final String[] FENDER_IDS={"port-aft","port-mid","port-forward","starboard-aft","starboard-mid","starboard-forward"};
    private boolean fabric;private int material=BoatMaterial.PAINT;
    private HarborBoatModel(int kind){
        float l=MooredBoats.length(kind),w=MooredBoats.width(kind);
        towEyes=new float[][]{{-l*.38f,0,4.2f},{l*.38f,0,4.2f}};fenders=new float[6][3];
        for(int side=0;side<2;side++)for(int i=0;i<3;i++)fenders[side*3+i]=new float[]{-l*.26f+i*l*.23f+.9f,(side==0?-1:1)*w*.45f,2.9f};
        model(kind);rigid=builder.build().rigid();
    }
    static void prepare(){for(int i=0;i<6;i++)if(MODELS[i]==null)MODELS[i]=new HarborBoatModel(i);}
    static HarborBoatModel get(int kind){return MODELS[kind];}
    private int color(int c){
        if(fabric)return FABRIC;
        return c;
    }
    private void face(int c,float... xyz){builder.materialPolygon(root,color(c),material,xyz);}
    private void box(float x,float y,float z,float l,float w,float h,int top,int side){
        int mat=fabric || Math.min(l,w)<2?BoatMaterial.METAL:material,c=color(top);
        if(mat==BoatMaterial.PAINT && l>7 && w>5 && h>=.6f){
            float r=Math.min(.55f,h*.35f),zz=z+h-r;
            float[][] bottom={{x,y,z},{x+l,y,z},{x+l,y+w,z},{x,y+w,z}};
            float[][] rim={{x,y,zz},{x+l,y,zz},{x+l,y+w,zz},{x,y+w,zz}};
            float[][] roof={{x+r,y+r,z+h},{x+l-r,y+r,z+h},{x+l-r,y+w-r,z+h},{x+r,y+w-r,z+h}};
            for(int i=0;i<4;i++){int j=(i+1)%4;quad(c,mat,bottom[i],bottom[j],rim[j],rim[i]);quad(c,mat,rim[i],rim[j],roof[j],roof[i]);}
            quad(c,mat,roof[0],roof[1],roof[2],roof[3]);
        }else builder.box(root,x+l*.5f,y+w*.5f,z+h*.5f,l,w,h,c,mat);
    }
    private void quad(int color,int mat,float[] a,float[] b,float[] c,float[] d){builder.materialPolygon(root,color,mat,a[0],a[1],a[2],b[0],b[1],b[2],c[0],c[1],c[2],d[0],d[1],d[2]);}
    void model(int kind){float l=MooredBoats.length(kind),w=MooredBoats.width(kind);int[] hulls={0xffaebfc4,0xffa9b8c8,0xffc0c3b8,0xffb3c2cc,0xffbbc3bd,0xffb4bacb};int base=hulls[kind];
        float[] xs={-.5f,-.38f,.21f,.43f,.52f,.43f,.21f,-.38f},ys={-.30f,-.48f,-.47f,-.25f,0,.25f,.47f,.48f};
        float[] roundedX=new float[16],roundedY=new float[16];
        for(int i=0;i<8;i++){int prev=(i+7)%8,next=(i+1)%8;roundedX[i*2]=xs[i]*.85f+xs[prev]*.15f;roundedY[i*2]=ys[i]*.85f+ys[prev]*.15f;roundedX[i*2+1]=xs[i]*.85f+xs[next]*.15f;roundedY[i*2+1]=ys[i]*.85f+ys[next]*.15f;}
        xs=roundedX;ys=roundedY;material=BoatMaterial.HULL;
        float[] deck=new float[48];for(int i=0;i<16;i++){deck[i*3]=xs[i]*l;deck[i*3+1]=ys[i]*w;deck[i*3+2]=3.5f;
            int j=(i+1)%16;face(base,xs[i]*l*.94f,ys[i]*w*.83f,0,xs[j]*l*.94f,ys[j]*w*.83f,0,xs[j]*l,ys[j]*w,3.5f,xs[i]*l,ys[i]*w,3.5f);}
        material=BoatMaterial.PAINT;face(0xffc3c9c4,deck);
        box(-l*.34f,-w*.31f,3.5f,l*.35f,w*.62f,.7f,0xff435562,0xff222c32);
        if(kind<3){box(-l*.06f,-w*.33f,3.5f,l*(kind==2?.38f:.27f),w*.66f,5.5f,0xffd0d4ca,0xff34434c);
            box(-l*.03f,-w*.34f,6,l*.22f,w*.68f,2.8f,0xff203b4d,0xff1b303b);
            box(-l*.08f,-w*.37f,9,l*.33f,w*.74f,.7f,0xffd5d9d2,0xff3a454b);
            if(kind==1)box(-l*.29f,-w*.35f,5,l*.22f,w*.7f,3,0xff254451,0xff24343c);
            if(kind==2)box(l*.03f,-w*.22f,10,l*.19f,w*.44f,2,0xffb7c5c3,0xff303f48);
        }else{box(-l*.12f,-w*.31f,3.5f,l*.35f,w*.62f,3.2f,0xffcbd2c8,0xff34414a);
            box(l*.08f,-.4f,5,.7f,.8f,kind==5?45:39,0xffa7b9c0,0xff48545d);
            box(-l*.28f,-.9f,10,l*.39f,1.8f,1.7f,0xff81969f,0xff303d49);
            // Furled sails and fine standing rigging, not bright raised sails.
            face(0xff71858e,l*.09f,0,43,-l*.43f,0,4,-l*.43f+.45f,0,4,l*.09f+.35f,0,43);
            face(0xff71858e,l*.09f,0,43,l*.48f,0,4,l*.48f+.45f,0,4,l*.09f+.35f,0,43);
        }
        box(l*.30f,-w*.14f,3.55f,l*.12f,w*.28f,.2f,0xff304c5c,0xff304c5c);
        for(int side=-1;side<=1;side+=2)
            box(-l*.34f,side*w*.46f-.15f,3,.56f*l,.3f,.35f,0xff3c5866,0xff3c5866);
        // Aft cockpit canvas on stainless bows; rigid geometry, never a rotated bitmap.
        if(kind<3){
        for(int end=0;end<2;end++)for(int side=-1;side<=1;side+=2)
            box(l*(-.34f+end*.25f),side*w*.34f-.2f,4,.4f,.4f,5.1f,0xffa2b4b5,0xff48555b);
        fabric=true;
        box(-l*.36f,-w*.37f,9.1f,l*.30f,w*.74f,.45f,0xff747878,0xff525959);
        fabric=false;
        }
        for(int side=-1;side<=1;side+=2)for(int i=0;i<3;i++)box(-l*.26f+i*l*.23f,side*w*.45f-.65f,1.5f,1.8f,1.3f,2.8f,0xff536675,0xff2b373e);
        for(int end=-1;end<=1;end+=2)box(end*l*.38f-.9f,-.6f,3.7f,1.8f,1.2f,.5f,0xff9caeac,0xff39464c);
    }
}
