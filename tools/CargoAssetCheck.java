package com.bargeboss.game;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;

/** Read-only production cargo packing/registration checks; never certifies 3-D pose fidelity. */
public final class CargoAssetCheck {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static boolean near(float a,float b){return Math.abs(a-b)<.000002f;}
    private static void finite(float value,String why){check(Float.isFinite(value),why);}
    private static String fingerprint(BufferedImage image,int l,int t,int r,int b)throws Exception{
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        // Normalize rounded source-cell sizes; hidden RGB does not make duplicate art unique.
        for(int y=0;y<128;y++)for(int x=0;x<128;x++){
            int pixel=image.getRGB(l+Math.min(r-l-1,(int)((x+.5f)*(r-l)/128)),t+Math.min(b-t-1,(int)((y+.5f)*(b-t)/128)));
            if((pixel>>>24)==0)pixel=0;
            for(int shift=0;shift<32;shift+=8)digest.update((byte)(pixel>>>shift));
        }
        StringBuilder out=new StringBuilder();for(byte value:digest.digest())out.append(String.format("%02x",value&255));return out.toString();
    }
    private static void registration(int atlas,String filename)throws Exception{
        String stem=filename.substring(0,filename.length()-7); // remove -32.png
        File csv=new File("docs/art-studies/"+stem+"-registration.csv");
        List<String> rows=Files.readAllLines(csv.toPath(),StandardCharsets.UTF_8);
        check(rows.size()==33,"Expected 32 registration rows: "+csv);
        check(rows.get(0).startsWith("frame,pivot_x,pivot_y,uniform_scale,"),"Unsupported registration schema: "+csv);
        float scale=CargoArt.scale(atlas);finite(scale,"Finite uniform scale: "+filename);check(scale>0,"Positive scale: "+filename);
        for(int frame=0;frame<32;frame++){
            String[] columns=rows.get(frame+1).split(",",-1);check(columns.length>=4,"Registration row width: "+csv);
            check(Integer.parseInt(columns[0])==frame,"Ordered 32 frame registration: "+csv);
            float x=Float.parseFloat(columns[1]),y=Float.parseFloat(columns[2]),s=Float.parseFloat(columns[3]);
            finite(x,"Finite pivot X");finite(y,"Finite pivot Y");finite(s,"Finite scale");
            check(x>0 && x<1 && y>0 && y<1,"Waterline pivot inside source cell: "+filename+" "+frame);
            check(near(s,scale),"One reviewed scale across all headings: "+filename+" "+frame);
            check(near(x,CargoArt.pivotX(atlas,frame)) && near(y,CargoArt.pivotY(atlas,frame)),"Runtime pivots match reviewed registration: "+filename+" "+frame);
            check(near(x,SpriteAtlas.pivotX(atlas,frame)) && near(y,SpriteAtlas.pivotY(atlas,frame)),"Shared SpriteAtlas uses cargo registration: "+filename+" "+frame);
            check(near(SpriteAtlas.widthScale(atlas,frame),scale) && near(SpriteAtlas.heightScale(atlas,frame),scale),"No per-heading/nonuniform stretching: "+filename+" "+frame);
        }
    }
    private static void atlas(int atlas)throws Exception{
        String filename=SpriteAtlas.file(atlas,0);File file=new File("assets/sprites/"+filename);
        BufferedImage image=ImageIO.read(file);check(image!=null,"Readable cargo PNG: "+file);
        check(image.getColorModel().hasAlpha(),"True alpha channel: "+file);
        check(SpriteAtlas.count(atlas)==32 && SpriteAtlas.columns(atlas)==8 && SpriteAtlas.rows(atlas)==4,"Cargo uses 32 cells in one 8x4 sheet: "+filename);
        Set<String> fingerprints=new HashSet<String>();int pixels=0,residue=0,maxEdgeAlpha=0;
        for(int frame=0;frame<32;frame++){
            check(filename.equals(SpriteAtlas.file(atlas,frame)),"No interleaved legacy sheet: "+filename);
            check(SpriteAtlas.sourceFrame(atlas,frame)==frame,"One real source cell per heading: "+filename);
            check(near(SpriteAtlas.sourceLeft(atlas,frame),(frame%8)/8f) && near(SpriteAtlas.sourceTop(atlas,frame),(frame/8)/4f),"Row-major source mapping: "+filename+" "+frame);
            int l=Math.round(image.getWidth()*SpriteAtlas.sourceLeft(atlas,frame)),t=Math.round(image.getHeight()*SpriteAtlas.sourceTop(atlas,frame));
            int r=Math.round(image.getWidth()*SpriteAtlas.sourceRight(atlas,frame)),b=Math.round(image.getHeight()*SpriteAtlas.sourceBottom(atlas,frame));
            check(r>l+2 && b>t+2,"Nonzero crop/gutter: "+filename+" "+frame);
            int opaque=0;
            for(int y=t;y<b;y++)for(int x=l;x<r;x++){
                int alpha=image.getRGB(x,y)>>>24;
                if(x==l || x==r-1 || y==t || y==b-1){
                    if(alpha>0)residue++;maxEdgeAlpha=Math.max(maxEdgeAlpha,alpha);
                    check(alpha<=2,"Crop-edge alpha exceeds documented 2/255 source tolerance: "+filename+" frame="+frame+" pixel="+x+","+y+" alpha="+alpha);
                }
                if(alpha>=128)opaque++;
            }
            check(opaque>16,"Nonempty visible cargo: "+filename+" "+frame);pixels+=opaque;
            check(fingerprints.add(fingerprint(image,l,t,r,b)),"Duplicate normalized frame content: "+filename+" "+frame);
        }
        registration(atlas,filename);
        int surface=SurfaceLightArt.forSource(atlas);
        metadata(atlas,filename);
        check(new File("assets/sprites/"+SpriteAtlas.file(surface)).isFile(),"Surface-light data exists: "+filename);
        System.out.println(filename+": "+image.getWidth()+"x"+image.getHeight()+", 32 nonempty/distinct cells, "+residue+" source-edge residue pixels (max "+maxEdgeAlpha+"/255; allowed <=2), "+pixels+" opaque pixels");
    }
    private static Map<?,?> entry(Object value,int atlas){
        if(value instanceof Map){Map<?,?> object=(Map<?,?>)value;Object id=object.get("atlas");
            if(id instanceof Number && ((Number)id).intValue()==atlas)return object;
            for(Object child:object.values()){Map<?,?> result=entry(child,atlas);if(result!=null)return result;}
        }else if(value instanceof List)for(Object child:(List<?>)value){Map<?,?> result=entry(child,atlas);if(result!=null)return result;}
        return null;
    }
    private static void metadata(int atlas,String source)throws Exception{
        String text=new String(Files.readAllBytes(new File("docs/art-studies/cargo-effects-v1.json").toPath()),StandardCharsets.UTF_8);
        Object parsed=new Json(text).parse();check(parsed instanceof Map,"Object metadata root");Map<?,?> root=(Map<?,?>)parsed;
        check(root.get("schemaVersion") instanceof Number && ((Number)root.get("schemaVersion")).intValue()==1,"Known metadata schema");
        check(root.get("frames") instanceof Number && ((Number)root.get("frames")).intValue()==32,"Metadata declares all headings");
        check(root.get("geometry") instanceof String && new File((String)root.get("geometry")).isFile(),"Named geometry provenance exists");
        Map<?,?> object=entry(parsed,atlas);check(object!=null,"Explicit effects metadata for atlas "+atlas);
        check(source.equals(object.get("source")),"Metadata source matches production sheet: "+source);
        check("body-local-xyz".equals(object.get("coordinateSpace")),"Declared body-local coordinate space: "+source);
        Object origin=object.get("cargoOrigin");check(origin instanceof List && ((List<?>)origin).size()==3,"Three-component cargo origin: "+source);
        List<?> xyz=(List<?>)origin;
        check(xyz.get(0) instanceof Number && xyz.get(1) instanceof Number && xyz.get(2) instanceof Number,"Numeric cargo origin: "+source);
        check(near(((Number)xyz.get(0)).floatValue(),0) && near(((Number)xyz.get(1)).floatValue(),0) && near(((Number)xyz.get(2)).floatValue(),CargoArt.FLOOR),"Cargo-floor anchor matches runtime hold: "+source);
        for(String name:new String[]{"lights","smokestacks","tires"})check(object.get(name) instanceof List && ((List<?>)object.get(name)).isEmpty(),"Explicit empty "+name+" for cargo: "+source);
        check(object.get("shadows") instanceof Map,"Shadow provenance: "+source);Map<?,?> shadows=(Map<?,?>)object.get("shadows");
        check(source.equals(shadows.get("source")),"Shadow source identity: "+source);
        check("hold-floor".equals(shadows.get("receivingPlane")),"Cargo shadow receiving plane: "+source);
        for(String key:new String[]{"cell","inner","pad","contactSigma","castSigma"})check(shadows.get(key) instanceof Number,"Numeric shadow setting "+key);
        check(((Number)shadows.get("cell")).intValue()==128 && ((Number)shadows.get("inner")).intValue()==96 && ((Number)shadows.get("pad")).intValue()==16,"Shadow cell/padding transform");
        check(near(((Number)shadows.get("contactSigma")).floatValue(),1) && near(((Number)shadows.get("castSigma")).floatValue(),3.2f),"Separate contact/cast softness");
        check(shadows.get("file") instanceof String,"Named shadow delivery: "+source);
        BufferedImage masks=ImageIO.read(new File((String)shadows.get("file")));
        check(masks!=null && masks.getWidth()==1024 && masks.getHeight()==1024 && masks.getColorModel().hasAlpha(),"64 independent shadow cells: "+source);
        for(int frame=0;frame<64;frame++){
            int l=frame%8*128,t=frame/8*128,visible=0;
            for(int y=0;y<128;y++)for(int x=0;x<128;x++){
                int alpha=masks.getRGB(l+x,t+y)>>>24;if(alpha>0)visible++;
                if(x==0 || x==127 || y==0 || y==127)check(alpha==0,"Transparent shadow gutter: "+source+" "+frame);
            }
            check(visible>0,"Nonempty shadow variant: "+source+" "+frame);
        }
    }
    /** Small strict JSON reader keeps the asset checker dependency-free on desktop Java. */
    private static final class Json{
        private final String text;private int at;
        Json(String value){text=value;}
        private void space(){while(at<text.length() && Character.isWhitespace(text.charAt(at)))at++;}
        private char take(){if(at>=text.length())throw new IllegalArgumentException("Unexpected end of JSON");return text.charAt(at++);}
        private void expect(char c){space();if(take()!=c)throw new IllegalArgumentException("Expected "+c+" at "+at);}
        Object parse(){Object value=value();space();if(at!=text.length())throw new IllegalArgumentException("Trailing JSON at "+at);return value;}
        private Object value(){
            space();if(at>=text.length())throw new IllegalArgumentException("Missing JSON value");char c=text.charAt(at);
            if(c=='{'){at++;Map<String,Object> map=new LinkedHashMap<String,Object>();space();if(text.charAt(at)=='}'){at++;return map;}
                while(true){space();String key=string();expect(':');if(map.containsKey(key))throw new IllegalArgumentException("Duplicate JSON key "+key);map.put(key,value());space();char end=take();if(end=='}')return map;if(end!=',')throw new IllegalArgumentException("Object separator");}}
            if(c=='['){at++;List<Object> list=new ArrayList<Object>();space();if(text.charAt(at)==']'){at++;return list;}
                while(true){list.add(value());space();char end=take();if(end==']')return list;if(end!=',')throw new IllegalArgumentException("Array separator");}}
            if(c=='"')return string();
            for(String word:new String[]{"true","false","null"})if(text.startsWith(word,at)){at+=word.length();return word.equals("null")?null:Boolean.valueOf(word);}
            int start=at;while(at<text.length() && "-+0123456789.eE".indexOf(text.charAt(at))>=0)at++;
            if(start==at)throw new IllegalArgumentException("Bad JSON value at "+at);
            String number=text.substring(start,at);if(!number.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?"))throw new IllegalArgumentException("Bad JSON number "+number);
            double n=Double.parseDouble(number);if(!Double.isFinite(n))throw new IllegalArgumentException("Nonfinite JSON number");return n;
        }
        private String string(){
            if(take()!='"')throw new IllegalArgumentException("JSON string expected");StringBuilder value=new StringBuilder();
            while(true){char c=take();if(c=='"')return value.toString();if(c<32)throw new IllegalArgumentException("JSON control character");
                if(c=='\\'){char escape=take();switch(escape){case '"':case '\\':case '/':c=escape;break;case 'b':c='\b';break;case 'f':c='\f';break;case 'n':c='\n';break;case 'r':c='\r';break;case 't':c='\t';break;
                    case 'u':if(at+4>text.length())throw new IllegalArgumentException("Short unicode escape");c=(char)Integer.parseInt(text.substring(at,at+4),16);at+=4;break;default:throw new IllegalArgumentException("JSON escape");}}
                value.append(c);
            }
        }
    }
    public static void main(String[]args)throws Exception{
        int count=0;for(int atlas=0;atlas<SpriteAtlas.ATLAS_COUNT;atlas++)if(CargoArt.atlas(atlas)){atlas(atlas);count++;}
        check(count>=3,"All existing cargo families remain represented");
        System.out.println(checks+" cargo packing, source mapping, registration and derivative-presence checks passed.");
        System.out.println("Distinct pixels and within-tolerance gutters do not prove correct pose angles, material style or hold/coaming fit. Review the actual 32-heading composites, unload cycle and 31-to-0 turn.");
    }
}
