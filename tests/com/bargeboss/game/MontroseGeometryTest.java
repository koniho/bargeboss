package com.bargeboss.game;

/** Raised receivers obey physical ground depth; exact edge contacts have a useful normal. */
public final class MontroseGeometryTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void collision(){
        float[] square={-200,-200,200,-200,200,200,-200,200};
        for(boolean reverse:new boolean[]{false,true})for(boolean inside:new boolean[]{false,true})for(boolean yacht:new boolean[]{false,true})for(int edge=0;edge<4;edge++){
            float[] p=square.clone();if(reverse)for(int i=0;i<4;i++){p[i*2]=square[(3-i)*2];p[i*2+1]=square[(3-i)*2+1];}
            int a=edge*2,b=(a+2)%8;float dx=p[b]-p[a],dy=p[b+1]-p[a+1],len=Physics.length(dx,dy);
            float nx=-dy/len,ny=dx/len,sign=(PolygonWater.area(p)>0?1:-1)*(inside?1:-1);nx*=sign;ny*=sign;
            float x=(p[a]+p[b])/2,y=(p[a+1]+p[b+1])/2;
            Body body=new Body(0,-1,x,y,(float)StrictMath.atan2(dy,dx),yacht);body.vx=-80*nx+10*dx/len;body.vy=-80*ny+10*dy/len;
            float impact=PolygonWater.collide(body,p,inside,null);
            check(Float.isFinite(body.x) && Float.isFinite(body.y) && Float.isFinite(body.vx) && Float.isFinite(body.vy),"Exact-edge contact remains finite");
            check(impact>79 && body.vx*nx+body.vy*ny>=0,"Exact-edge normal rejects inward momentum for either winding/constraint");
            check(Math.abs(body.vx*dx/len+body.vy*dy/len-10)<.001f,"Wall collision preserves tangential motion");
            for(int disc=0;disc<3;disc++)check((body.discX(disc)-x)*nx+(body.discY(disc)-y)*ny>=body.radius-.001f,"Every hull disc is separated onto allowed side");
            float xx=body.x,yy=body.y;PolygonWater.collide(body,p,inside,null);
            check(Physics.length(body.x-xx,body.y-yy)<.001f,"Resolved contact does not creep on repeated collision");
        }
    }
    private static float[] clip(RiverCamera c,float x,float y,float depth){return MontroseHarbor.shore(c,x-36,y-36,x+36,y+36,depth);}
    private static void face(RiverCamera c,float ax,float ay,float bx,float by,float rise,boolean visible){
        float x=(ax+bx)/2,y=(ay+by)/2,sx=c.screenX(x,y),depth=c.screenY(x,y),h=rise*c.scale;
        // For a camera-facing wall, this pixel is below the top and cannot be covered by
        // that deck's top surface. Both depth positions deliberately differ by <rise.
        float sy=depth-h*.2f;
        float[] front=clip(c,x,y,depth-h*.2f),rear=clip(c,x,y,depth+h*.2f);
        if(visible){check(ShoreOcclusion.contains(front,sx,sy),"Foreground vertical wall covers lower vessel pixels");check(!ShoreOcclusion.contains(rear,sx,sy),"Background vertical wall never cuts a nearer vessel");}
        // A region containing only this edge cannot include a long wall far beyond its bounds.
        for(float[] triangles:new float[][]{front,rear})for(float v:triangles)check(Float.isFinite(v),"Raised clip coordinates remain finite");
    }
    private static void receivers(){
        float[] pier=MontroseHarbor.DOCKS[MontroseHarbor.DOCKS.length-2];
        float[] northPier=MontroseHarbor.DOCKS[MontroseHarbor.DOCKS.length-1];
        for(float scale:new float[]{.45f,1.3f,3.2f})for(float pan:new float[]{0,937}){
            RiverCamera c=new RiverCamera(31+pan,57-pan*.3f,scale,148+pan,460-pan,1);
            // Western pier's long lower edge is front-facing. The eastern pier has a
            // differently oriented foreground face: test the normal instead of a fixed index.
            for(float[] q:new float[][]{pier,northPier})for(int edge=0;edge<4;edge++){
                int i=edge*2,j=(i+2)%8;float dx=q[j]-q[i],dy=q[j+1]-q[i+1],sign=PolygonWater.area(q)>0?1:-1;
                if((-dx-dy)*sign<=0 || Physics.length(dx,dy)<80)continue;
                face(c,q[i],q[i+1],q[j],q[j+1],MontroseHarbor.DOCK_RISE,true);
            }
            // Outer southern peninsula seawall, well clear of dock geometry.
            face(c,MontroseHarbor.mapX(763),MontroseHarbor.mapY(1180),MontroseHarbor.mapX(910),MontroseHarbor.mapY(1208),MontroseHarbor.LAND_RISE,true);
            // Top receiver: a broad inland patch avoids edge-face ambiguity. Depth eligibility
            // is the unraised pivot even though the pixels are lifted onto the lawn surface.
            float x=MontroseHarbor.mapX(210),y=MontroseHarbor.mapY(1400),sx=c.screenX(x,y),sy=c.screenY(x,y)-MontroseHarbor.LAND_RISE*c.scale;
            check(!MontroseHarbor.openWater(x,y),"Land receiver sample is dry");
            check(ShoreOcclusion.contains(clip(c,x,y,c.screenY(x,y)-2*c.scale),sx,sy),"Foreground raised land surface masks hulls");
            check(!ShoreOcclusion.contains(clip(c,x,y,c.screenY(x,y)+2*c.scale),sx,sy),"Background raised land surface preserves hulls");
            check(ShoreOcclusion.contains(clip(c,x,y,Float.NEGATIVE_INFINITY),sx,sy),"Water effects exclude both front and rear land");
            float[] sea=clip(c,MontroseHarbor.mapX(440),MontroseHarbor.mapY(1460),Float.NEGATIVE_INFINITY);
            check(sea.length==0,"Open lake receives no phantom shore masks");
        }
        Game g=MontroseTest.game();g.tug.x=MontroseHarbor.mapX(300);g.tug.y=MontroseHarbor.mapY(1275);
        g.layout=Layout.focused(1333,600,g.tug.x,g.tug.y,2.2f,650,340);float[] first=ShoreOcclusion.vessel(g,g.layout.camera,g.tug,false);
        for(int heading=0;heading<32;heading++){g.tug.angle=heading*(float)StrictMath.PI/16;g.tug.vx=heading%2==0?80:-80;g.tug.heave=heading%3-1;
            check(java.util.Arrays.equals(first,ShoreOcclusion.vessel(g,g.layout.camera,g.tug,false)),"Heading, reverse movement and heave do not move the shoreline mask");}
    }
    private static void nativeMasks(){
        for(float scale:new float[]{.6f,2.3f})for(float[] spot:new float[][]{{823,255},{995,320}}){
            RiverCamera c=new RiverCamera(71,93,scale);float x=MontroseHarbor.mapX(spot[0]),y=MontroseHarbor.mapY(spot[1]);
            float[] mask=clip(c,x,y,Float.NEGATIVE_INFINITY);
            // iOS excludes an even-odd compound path. Overlapping triangle holes would reopen
            // pixels; prove the actual returned mask is a disjoint union, not just correct on JVM.
            float[][] triangles=new float[mask.length/6][],bounds=new float[triangles.length][];
            for(int i=0;i<triangles.length;i++){triangles[i]=java.util.Arrays.copyOfRange(mask,i*6,i*6+6);bounds[i]=PolygonWater.bounds(triangles[i]);}
            for(int i=0;i<triangles.length;i++)for(int j=i+1;j<triangles.length;j++)if(PolygonWater.overlaps(bounds[i],bounds[j])){
                float[] overlap=PolygonWater.clip(triangles[i],triangles[j]);
                check(Math.abs(PolygonWater.area(overlap))<.01f,"Native even-odd shore triangles have no visible positive-area overlap");
            }
            for(float[] top:new float[][]{
                MontroseHarbor.projected(MontroseHarbor.LAND_TRIANGLES,c,x-36,y-36,x+36,y+36,Float.NEGATIVE_INFINITY,MontroseHarbor.LAND_RISE),
                MontroseHarbor.projected(MontroseHarbor.DOCKS,c,x-36,y-36,x+36,y+36,Float.NEGATIVE_INFINITY,MontroseHarbor.DOCK_RISE)}){
                for(int i=0;i<top.length;i+=6){
                    float sx=(top[i]+top[i+2]+top[i+4])/3,sy=(top[i+1]+top[i+3]+top[i+5])/3;
                    if(Math.abs(PolygonWater.cross(top[i],top[i+1],top[i+2],top[i+3],top[i+4],top[i+5]))>.001f)
                        check(ShoreOcclusion.contains(mask,sx,sy),"Disjoint union preserves every solid top receiver");
                }
            }
            float sx=c.screenX(x,y),sy=c.screenY(x,y);
            for(int yy=-35;yy<35;yy+=3)for(int xx=-35;xx<35;xx+=3){
                float px=sx+(xx+.371f)*scale,py=sy+(yy+.529f)*scale;int covers=0;
                for(float[] triangle:triangles)if(PolygonWater.contains(triangle,px,py))covers++;
                check(covers<=1,"Sampled native even-odd mask matches union coverage at dock junctions");
            }
        }
    }
    private static void exactCache(){
        RiverCamera c=new RiverCamera(35,61,1.4f);float x=MontroseHarbor.mapX(1015),y=MontroseHarbor.mapY(422),depth=c.screenY(x,y);
        float[] hull=clip(c,x,y,depth),surface=clip(c,x,y,Float.NEGATIVE_INFINITY);
        check(hull==clip(c,x,y,depth) && surface==clip(c,x,y,Float.NEGATIVE_INFINITY),"Repeated hull and surface masks reuse exact immutable arrays");
        float[] moved=clip(c,x+.25f,y,depth),differentDepth=clip(c,x,y,depth+.25f);
        check(hull!=moved && hull!=differentDepth,"Region or physical depth change cannot reuse a stale mask");
        RiverCamera shifted=new RiverCamera(36,61,1.4f);
        check(hull!=clip(shifted,x,y,depth),"Camera changes invalidate projected masks");
        float[] rebuilt=clip(c,x,y,depth);check(java.util.Arrays.equals(hull,rebuilt),"Camera invalidation preserves exact rebuilt coordinates");
        // Empty lake requests still count toward the fixed entry cap, without a giant fixture.
        float[][] masks=new float[33][];float lx=MontroseHarbor.mapX(600),ly=MontroseHarbor.mapY(1450);
        for(int i=0;i<masks.length;i++)masks[i]=MontroseHarbor.shore(c,lx+i,ly,lx+i+1,ly+1,Float.NEGATIVE_INFINITY);
        check(masks[32]==MontroseHarbor.shore(c,lx+32,ly,lx+33,ly+1,Float.NEGATIVE_INFINITY),"Newest exact request remains cached");
        check(masks[0]!=MontroseHarbor.shore(c,lx,ly,lx+1,ly+1,Float.NEGATIVE_INFINITY),"Cache cannot retain more than 32 request entries");
    }
    public static void run(){collision();receivers();nativeMasks();exactCache();System.out.println(checks+" Montrose edge collision, raised shore and dock depth assertions passed");}
    public static void main(String[] args){run();}
}
