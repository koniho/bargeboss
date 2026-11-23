package com.bargeboss.game;

/** Projection of the common articulated bridge; shared bridge rules remain authoritative. */
public final class BridgeView {
    private BridgeView() {}
    static void lights(Game g,SceneryLighting out){
        ArticulatedModel.Pose pose=BridgeModel.MODEL.pose(g.bridge.lift());
        for(int i=0;i<BridgeModel.LIGHTS.length;i++){BridgeModel.Light lamp=BridgeModel.LIGHTS[i];float[] a=pose.point(lamp.node,lamp.x,lamp.y,lamp.z);
            out.add(a[0],a[1],a[2],140,.65f*SceneryEffects.intensity(g.visualTicks,i*1.27f),lamp.kind==BridgeModel.SIGNAL?(g.bridge.passable()?0xff8fe4a2:0xffff5945):lamp.color);
        }
    }
    public static void draw(Game g,Painter canvas) {
        RiverCamera c=g.layout.camera;
        float centerX=c.screenX(300,Bridge.Y),centerY=c.screenY(300,Bridge.Y);
        if(centerX+440*c.scale<0 || centerX-440*c.scale>g.layout.width || centerY+230*c.scale<0 || centerY-350*c.scale>g.layout.height)return;
        Painter world=new RiverPainter(canvas,c);
        ArticulatedModel.Pose pose=BridgeModel.MODEL.pose(g.bridge.lift());
        BridgeModel.MODEL.draw(g,canvas,pose);
        for(int i=0;i<BridgeModel.LIGHTS.length;i++) {
            BridgeModel.Light lamp=BridgeModel.LIGHTS[i];float[] xyz=pose.point(lamp.node,lamp.x,lamp.y,lamp.z);
            float[] normal=pose.normal(lamp.node,lamp.nx,lamp.ny,lamp.nz);
            if(-normal[0]+normal[1]+normal[2]<=.02f || pose.occluded(xyz))continue;
            float x=c.screenX(xyz[0],xyz[1]),y=c.screenY(xyz[0],xyz[1])-xyz[2]*ArticulatedModel.Z_SCALE*c.scale;
            float radius=lamp.radius*c.scale;if(x < -radius*8 || x>g.layout.width+radius*8 || y < -radius*8 || y>g.layout.height+radius*8)continue;
            int color=lamp.kind==BridgeModel.SIGNAL?(g.bridge.passable()?0xff8fe4a2:0xffff5945):lamp.color;
            float alpha=pose.opacity(g,canvas,lamp.node);
            SceneryEffects.glow(canvas,x,y,radius,color,alpha,g.visualTicks,i*1.27f);
            canvas.circle(x,y,Math.max(.7f,1.25f*c.scale),((int)(alpha*255)<<24)|(color&0xffffff));
        }
        int signal=g.bridge.passable()?0xff9fe4a6:VisualStyle.RED;
        RiverPainter.center(world,g.bridge.signal(),475,Bridge.Y+13,10,signal);
        if(!g.bridge.passable())for(int side=-1;side<=1;side+=2) {
            float y=Bridge.Y+side*90;
            for(int x=250;x<350;x+=14)world.line(x,y,x+7,y,2,0x99ffce65);
            RiverPainter.center(world,"WAIT HERE",300,y+side*15,8,VisualStyle.GOLD);
        }
    }
}
