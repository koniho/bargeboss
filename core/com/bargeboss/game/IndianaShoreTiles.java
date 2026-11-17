package com.bargeboss.game;

/** Sparse industrial shore modules on the existing dry footprint, prepared with the ground. */
final class IndianaShoreTiles {
    private static final float RISE=GroundArt.LAND_RISE/ArticulatedModel.Z_SCALE;
    private static final int COPING=0xff8b8075,SHEET=0xff675b55,ASPHALT=0xff49413b,FILL=0xff73675a;
    private final ArticulatedModel.Builder builder;
    private int serial;
    private IndianaShoreTiles(ArticulatedModel.Builder b){builder=b;}
    static void append(ArticulatedModel.Builder b){new IndianaShoreTiles(b).build();}
    private void build(){
        // Inner working quays: continuous coping and sheet-pile wall material, not a post grid.
        float[] coast=IndianaHarbor.COAST;
        for(int i=0;i<coast.length;i+=2){int j=(i+2)%coast.length;
            float x=coast[i],y=coast[i+1],xx=coast[j],yy=coast[j+1];
            if(Math.min(y,yy)<-820)continue; // Outer margins use rough fill/stone below.
            float dx=xx-x,dy=yy-y,length=Physics.length(dx,dy),nx=-dy/length,ny=dx/length;
            if(IndianaHarbor.contains((x+xx)*.5f+nx*3,(y+yy)*.5f+ny*3)){nx=-nx;ny=-ny;}
            float a=x+nx*2,b=y+ny*2,c=xx+nx*2,d=yy+ny*2;
            patch("quay-coping",Math.min(a,c)-Math.abs(nx)*2,Math.min(b,d)-Math.abs(ny)*2,
                    Math.max(a,c)+Math.abs(nx)*2,Math.max(b,d)+Math.abs(ny)*2,RISE-.7f,1.4f,COPING,SurfaceMaterial.CONCRETE);
            // The wall skin is inside the bank and ends at the same physical shore elevation.
            a=x+nx*.35f;b=y+ny*.35f;c=xx+nx*.35f;d=yy+ny*.35f;
            patch("sheet-pile",Math.min(a,c)-Math.abs(nx)*.35f,Math.min(b,d)-Math.abs(ny)*.35f,
                    Math.max(a,c)+Math.abs(nx)*.35f,Math.max(b,d)+Math.abs(ny)*.35f,RISE*.45f,RISE*.9f,SHEET,SurfaceMaterial.CORRUGATED);
        }
        // Long parallel industrial strips read like the surveyed rail/mill yards.
        surface("west-basin-apron",-48,-785,94,100,0xff75685d);
        surface("east-basin-apron",1156,-785,1330,100,0xff7a6a5c);
        surface("south-mill-apron",506,1120,690,1720,0xff716356);
        surface("west-fork-yard",-470,1106,220,1250,0xff6c6056);
        road(-98,-785,-66,100);road(1342,-785,1374,100);
        road(510,160,538,1800);road(-470,1274,220,1306);
        surface("hick-ballast",165,170,235,740,FILL);
        rails(205,185,700);rails(670,210,630);
        // Dry rough aggregate at the lake-facing shoulders, never into the navigable mouth.
        surface("west-breakwater-fill",-260,-955,394,-835,FILL);
        surface("east-breakwater-fill",806,-955,1150,-835,0xff7b6d5e);
        rockMargin(-230,375,-946,8);rockMargin(825,1135,-946,5);
    }
    private void surface(String name,float x,float y,float xx,float yy,int color){
        patch(name,x,y,xx,yy,RISE+.025f,.05f,color,SurfaceMaterial.CONCRETE);
    }
    private void road(float x,float y,float xx,float yy){
        surface("service-road",x,y,xx,yy,ASPHALT);
        // Sparse irregular repair rectangles rather than a repeating checkerboard.
        float midY=(y+yy)*.5f;
        patch("asphalt-repair",x+4,midY-12,Math.min(xx-4,x+22),midY+19,RISE+.06f,.04f,0xff383634,SurfaceMaterial.CONCRETE);
    }
    private void rails(float x,float y,float yy){
        patch("rail-bed",x-10,y,x+28,yy,RISE+.07f,.08f,0xff514a40,SurfaceMaterial.CONCRETE);
        for(int i=0;i<2;i++)patch("rail",x+i*18-.8f,y,x+i*18+.8f,yy,RISE+.25f,.4f,0xff70645a,SurfaceMaterial.STEEL);
    }
    private void rockMargin(float x,float xx,float y,int count){
        for(int i=0;i<count;i++){
            float width=18+(i*7)%13,depth=14+(i*11)%17,cx=x+(xx-x)*(i+.5f)/count;
            // Low irregular slabs retain the existing cap height and collision footprint.
            patch("breakwater-stone",cx-width*.5f,y+(i%3)*5,cx+width*.5f,y+(i%3)*5+depth,
                    RISE+.4f,.8f,i%2==0?0xff918273:0xff7d756a,SurfaceMaterial.CONCRETE);
        }
    }
    /** Intersect each authored module with the immutable disjoint land partition. */
    private void patch(String name,float x,float y,float xx,float yy,float z,float height,int color,int material){
        for(float[] land:IndianaHarbor.LAND){
            float left=Math.max(x,land[0]),top=Math.max(y,land[1]),right=Math.min(xx,land[2]),bottom=Math.min(yy,land[3]);
            if(right-left<.01f || bottom-top<.01f)continue;
            int n=builder.node("indiana-"+name+"-"+serial++,-1,(left+right)*.5f,(top+bottom)*.5f,z,0,0);
            builder.box(n,0,0,0,right-left,bottom-top,height,color,material);
        }
    }
}
