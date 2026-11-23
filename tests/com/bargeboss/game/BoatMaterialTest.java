package com.bargeboss.game;
public final class BoatMaterialTest {
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    public static void run(){
        MooredBoatLight.Lights lights=new MooredBoatLight.Lights();int albedo=0xffabb9c4;
        int dark=BoatMaterial.shade(LightingProfile.DUSK,lights,BoatMaterial.PAINT,albedo,0,0,5,1,0,0,1);
        lights.add(20,0,8,90,1,0xffff8844);
        int lit=BoatMaterial.shade(LightingProfile.DUSK,lights,BoatMaterial.PAINT,albedo,0,0,5,1,0,0,1);
        check((lit>>16&255)>(dark>>16&255)+20,"paint receives nearby warm lamps");
        int away=BoatMaterial.shade(LightingProfile.DUSK,lights,BoatMaterial.PAINT,albedo,0,0,5,-1,0,0,1);
        check((lit>>16&255)>(away>>16&255),"real geometry orientation controls receiver light");
        lights.count=0;
        int bottom=BoatMaterial.shade(LightingProfile.DAYLIGHT,lights,BoatMaterial.HULL,albedo,0,0,0,0,1,0,1);
        int top=BoatMaterial.shade(LightingProfile.DAYLIGHT,lights,BoatMaterial.HULL,albedo,0,0,3.5f,0,1,0,1);
        check((top&255)>(bottom&255),"waterline stays softly shaded");
        check(TriangleShade.mix(0x00ff0000,0xff0000ff,.5f)==0x800000ff,"transparent vertex interpolation is premultiplied");
        System.out.println("Boat material lamp reception, rotated normals, waterline shading and premultiplied color checks passed");
    }
}
