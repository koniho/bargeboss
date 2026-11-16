package com.bargeboss.game;

/** Pixel-reviewed painted lanterns; unresolved roof lamps are explicitly hidden.
 * Source: docs/art-studies/tug-isometric-v5-runtime-lights.json.
 * No guessed XYZ or unreviewed guide anchors are enabled by this data. */
public final class TugLightArt {
    public static final int COUNT=3;
    private static final float STEP=(float)(StrictMath.PI*2/32);
    private TugLightArt() {}
    private static final float[][] U={
        {0.57657658f,0.57207207f,0.57466063f,0.58108108f,0.57207207f,0.57657658f,0.58371041f,0.58108108f,0.50450450f,0.51351351f,0.52941176f,0.51801802f,0.51351351f,0.50900901f,0.52036199f,0.50900901f,0.49549550f,0.49099099f,0.49321267f,0.48648649f,0.49099099f,0.49549550f,0.50678733f,0.52702703f,0.52702703f,0.54954955f,0.55656109f,0.55855856f,0.56306306f,0.56756757f,0.57466063f,0.57657658f},
        {0.71171171f,0.69819820f,0.66515837f,0.63513514f,0.60810811f,0.59909910f,0.58371041f,0.55405405f,0.44144144f,0.41891892f,0.41176471f,0.40090090f,0.36936937f,0.35585586f,0.36651584f,0.35585586f,0.33333333f,0.33783784f,0.34389140f,0.35585586f,0.37837838f,0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.00000000f},
        {0.00000000f,0.00000000f,0.70135747f,0.70720721f,0.69819820f,0.71171171f,0.71493213f,0.68918919f,0.57657658f,0.54504505f,0.51583710f,0.49099099f,0.45045045f,0.41441441f,0.40723982f,0.37387387f,0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.51801802f,0.53153153f,0.55203620f,0.58558559f,0.59909910f,0.63963964f,0.66063348f,0.67117117f,0.68468468f,0.69819820f,0.71493213f,0.71621622f}
    };
    private static final float[][] V={
        {0.13063063f,0.13513514f,0.13513514f,0.13513514f,0.13513514f,0.13513514f,0.13063063f,0.13063063f,0.11711712f,0.11711712f,0.11261261f,0.11261261f,0.11711712f,0.11261261f,0.10810811f,0.11261261f,0.10407240f,0.10407240f,0.10407240f,0.09954751f,0.09502262f,0.09502262f,0.09502262f,0.09502262f,0.09909910f,0.09909910f,0.09909910f,0.09909910f,0.10360360f,0.10810811f,0.10360360f,0.10360360f},
        {0.31981982f,0.31981982f,0.36036036f,0.36486486f,0.37837838f,0.39189189f,0.39639640f,0.40090090f,0.38288288f,0.37837838f,0.35585586f,0.33783784f,0.33333333f,0.30180180f,0.29279279f,0.28378378f,0.31674208f,0.28959276f,0.28054299f,0.26696833f,0.24886878f,0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.00000000f},
        {0.00000000f,0.00000000f,0.32882883f,0.33333333f,0.34234234f,0.35135135f,0.36486486f,0.37837838f,0.37837838f,0.38288288f,0.37837838f,0.36936937f,0.36936937f,0.35585586f,0.35135135f,0.32882883f,0.00000000f,0.00000000f,0.00000000f,0.00000000f,0.22171946f,0.22171946f,0.21266968f,0.21719457f,0.24774775f,0.24774775f,0.23873874f,0.24324324f,0.25225225f,0.26576577f,0.28828829f,0.29279279f}
    };
    public static String id(int light) { return light==0?"mast-lantern":light==1?"roof-worklight-a":"roof-worklight-b"; }
    public static float u(int light,int frame) { return U[light][frame]; }
    public static float v(int light,int frame) { return V[light][frame]; }
    public static boolean visible(int light,int frame) { return U[light][frame]>0; }
    private static int frame(float angle,boolean river) {
        int a=(int)StrictMath.floor(angle/STEP)+(river?RiverCamera.DIRECTION_OFFSET:0);
        return (a%32+32)%32;
    }
    private static float blend(float angle) {
        float heading=angle/STEP;
        float mix=Physics.clamp((heading-(float)StrictMath.floor(heading)-.4f)*5,0,1);
        return mix*mix*(3-2*mix);
    }
    public static float opacity(int light,float angle,boolean river) {
        int a=frame(angle,river),b=(a+1)%32;float t=blend(angle);
        return (visible(light,a)?1-t:0)+(visible(light,b)?t:0);
    }
    public static float offset(int light,float angle,boolean river,boolean vertical) {
        int a=frame(angle,river),b=(a+1)%32;float t=SpriteAtlas.attachmentBlend(angle);
        float wa=visible(light,a)?1-t:0,wb=visible(light,b)?t:0,total=wa+wb;
        if(total==0)return 0;
        float pa=vertical?V[light][a]-TugArt.pivotY(a):U[light][a]-TugArt.pivotX(a);
        float pb=vertical?V[light][b]-TugArt.pivotY(b):U[light][b]-TugArt.pivotX(b);
        return (pa*wa+pb*wb)/total*TugArt.SCALE;
    }
}

