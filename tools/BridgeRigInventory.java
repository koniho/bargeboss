package com.bargeboss.game;

/** Exports the exact shared rig's named light anchors for review and future machinery. */
public final class BridgeRigInventory {
    public static void main(String[] args) {
        ArticulatedModel m=BridgeModel.MODEL;
        System.out.println("{\n  \"geometry\": \"core/com/bargeboss/game/BridgeModel.java\",\n  \"representation\": \"rigid code-native XYZ mesh\",\n  \"sourceUV\": null,\n  \"reviewStatus\": \"geometry-authored; shared-render phase review\",\n  \"physicalZProjection\": "+ArticulatedModel.Z_SCALE+",\n  \"smoke\": [],\n  \"steam\": [],\n  \"parts\": [");
        for(int i=0;i<m.nodeCount();i++) {
            int parent=m.parent(i);
            System.out.print("    {\"id\": \""+m.nodeName(i)+"\", \"parent\": "+(parent<0?"null":"\""+m.nodeName(parent)+"\"")+", \"joint\": \""+(i==BridgeModel.LEFT_LEAF || i==BridgeModel.RIGHT_LEAF?"local upward pitch = Bridge.lift * PI/2":"fixed")+"\"}");
            System.out.println(i+1==m.nodeCount()?"":",");
        }
        System.out.println("  ],\n  \"lights\": [");
        for(int i=0;i<BridgeModel.LIGHTS.length;i++) {
            BridgeModel.Light a=BridgeModel.LIGHTS[i];
            System.out.print("    {\"id\": \""+a.id+"\", \"parent\": \""+m.nodeName(a.node)+"\", \"kind\": \""+(a.kind==BridgeModel.WARM?"warm lantern":a.kind==BridgeModel.SIGNAL?"navigation signal":"red warning marker")+"\", \"localXYZ\": ["+a.x+", "+a.y+", "+a.z+"], \"outwardNormal\": ["+a.nx+", "+a.ny+", "+a.nz+"], \"colorARGB\": \""+Integer.toHexString(a.color)+"\", \"radius\": "+a.radius+", \"visibility\": \"normal facing camera and convex-face ray clear; parent overlap fade\", \"emission\": \""+(a.kind==BridgeModel.SIGNAL?"green 0xff8fe4a2 only when Bridge.passable; red otherwise":"SceneryEffects.intensity from shared visualTicks")+"\"}");
            System.out.println(i+1==BridgeModel.LIGHTS.length?"":",");
        }
        System.out.println("  ]\n}");
    }
}
