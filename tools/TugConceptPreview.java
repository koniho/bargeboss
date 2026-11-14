package com.bargeboss.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.imageio.ImageIO;

/** Review generated art without replacing the approved gameplay atlas. No source pixels are edited. */
public final class TugConceptPreview {
    private static final String SOURCE="docs/art-studies/tug-isometric-v5-32.png";
    private static final int COLS=8,ROWS=4,COUNT=COLS*ROWS;
    private TugConceptPreview() {}

    public static void main(String[] args) throws Exception {
        String source=args.length>0?args[0]:SOURCE;
        File output=new File(args.length>1?args[1]:"out/isometric-tug");
        String registration=args.length>2?args[2]:"docs/art-studies/tug-isometric-v5-registration.csv";
        String guidePath=args.length>3?args[3]:"docs/art-studies/tug-isometric-v5-pose-guide.png";
        String effects=args.length>4?args[4]:source.equals(SOURCE)?"docs/art-studies/tug-isometric-v5-anchors.json":"-";
        double[][] pivots=new double[32][3];
        for(int i=0;i<32;i++) pivots[i]=new double[]{.5,.72,1};
        if(!registration.equals("-")) {
            java.util.List<String> lines=Files.readAllLines(new File(registration).toPath(),StandardCharsets.UTF_8);
            if(lines.size()!=33) throw new IllegalArgumentException("Expected 32 registration rows");
            for(int i=0;i<32;i++) {
                String[] values=lines.get(i+1).split(",");
                if(Integer.parseInt(values[0])!=i) throw new IllegalArgumentException("Registration frame order");
                for(int j=0;j<3;j++) {
                    pivots[i][j]=Double.parseDouble(values[j+1]);
                    if(!Double.isFinite(pivots[i][j]) || pivots[i][j]<=0 || pivots[i][j]>2)
                        throw new IllegalArgumentException("Invalid registration value");
                }
            }
        }
        BufferedImage atlas=ImageIO.read(new File(source));
        if(atlas==null || !atlas.getColorModel().hasAlpha())
            throw new IllegalArgumentException("Expected transparent PNG atlas");
        output.mkdirs();
        File frames=new File(output,"frames"); frames.mkdirs();
        BufferedImage contact=new BufferedImage(2048,1024,BufferedImage.TYPE_INT_RGB);
        Graphics2D sheet=contact.createGraphics();
        StringBuilder metrics=new StringBuilder("frame,source_x,source_y,width,height,opaque_pixels,transparent_pixels,edge_pixels\n");
        for(int frame=0;frame<COUNT;frame++) {
            int col=frame%COLS,row=frame/COLS;
            int x=Math.round(atlas.getWidth()*col/(float)COLS),y=Math.round(atlas.getHeight()*row/(float)ROWS);
            int w=Math.round(atlas.getWidth()*(col+1)/(float)COLS)-x;
            int h=Math.round(atlas.getHeight()*(row+1)/(float)ROWS)-y;
            BufferedImage cell=atlas.getSubimage(x,y,w,h);
            int opaque=0,transparent=0,edge=0;
            for(int py=0;py<h;py++) for(int px=0;px<w;px++) {
                int a=cell.getRGB(px,py)>>>24;
                if(a==0) transparent++;
                if(a>=128) {
                    opaque++;
                    if(px==0 || py==0 || px==w-1 || py==h-1) edge++;
                }
            }
            if(opaque==0 || transparent==0) throw new IllegalStateException("Empty or nontransparent cell "+frame);
            metrics.append(frame).append(',').append(x).append(',').append(y).append(',').append(w).append(',').append(h)
                .append(',').append(opaque).append(',').append(transparent).append(',').append(edge).append('\n');
            BufferedImage review=render(cell,frame,768,512,true,pivots[frame]);
            ImageIO.write(review,"png",new File(frames,String.format("%02d.png",frame)));
            sheet.drawImage(render(cell,frame,256,256,false,pivots[frame]),col*256,row*256,null);
        }
        sheet.dispose();
        ImageIO.write(contact,"png",new File(output,"all-32-frames.png"));
        Files.write(new File(output,"measurements.csv").toPath(),metrics.toString().getBytes(StandardCharsets.UTF_8));
        // Self-contained: can be copied to a phone or opened directly without a server.
        String html=interactive(source,guidePath,pivots,effects);
        Files.write(new File(output,"review.html").toPath(),html.getBytes(StandardCharsets.UTF_8));
        System.out.println("32 nonempty transparent cells reviewed; source PNG unchanged.");
        System.out.println("Open "+output+"/review.html; contact sheet and measurements in the same directory.");
    }

    private static BufferedImage render(BufferedImage cell,int frame,int width,int height,boolean detailed,double[] pivot) {
        BufferedImage result=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setColor(new Color(0x173d49)); g.fillRect(0,0,width,height);
        int size=detailed?360:220,cx=width/2,cy=detailed?355:191;
        g.setColor(new Color(0x285664));
        for(int i=-8;i<=8;i++) {
            int dx=i*35;
            g.drawLine(cx+dx-250,cy-144,cx+dx+250,cy+144);
            g.drawLine(cx+dx-250,cy+144,cx+dx+250,cy-144);
        }
        g.setColor(new Color(0xbb7496));
        g.drawLine(cx-size/2-8,cy,cx+size/2+8,cy); g.drawLine(cx,cy-size/2,cx,cy+70);
        // Guide-derived registration uses one scale for every frame; never deform the hull.
        int drawSize=(int)Math.round(size*pivot[2]);
        g.drawImage(cell,cx-(int)Math.round(drawSize*pivot[0]),cy-(int)Math.round(drawSize*pivot[1]),drawSize,drawSize,null);
        g.setColor(new Color(0xff8fb7)); g.drawLine(cx-7,cy,cx+7,cy); g.drawLine(cx,cy-7,cx,cy+7);
        g.setColor(new Color(0xf5e4bd)); g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,detailed?23:15));
        g.drawString(detailed?"BARGE BOSS / ISOMETRIC TUG STUDY":"FRAME "+frame,16,detailed?35:23);
        if(detailed) {
            g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,16));
            g.drawString("Frame "+frame+" / 31   •   registered to the projected hull guide",16,62);
            g.drawString("CANDIDATE — inspect model details and residual shape changes",16,height-24);
        }
        g.dispose(); return result;
    }

    private static String interactive(String source,String guide,double[][] pivots,String effects) throws Exception {
        StringBuilder p=new StringBuilder("[");
        for(int i=0;i<32;i++) { if(i>0)p.append(',');p.append(java.util.Arrays.toString(pivots[i])); }
        p.append(']');
        String atlas=java.util.Base64.getEncoder().encodeToString(Files.readAllBytes(new File(source).toPath()));
        String blockout=java.util.Base64.getEncoder().encodeToString(Files.readAllBytes(new File(guide).toPath()));
        String anchors=effects.equals("-")?"{\"frames\":[]}":new String(Files.readAllBytes(new File(effects).toPath()),StandardCharsets.UTF_8);
        return "<!doctype html><html lang='en'><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>Barge Boss — watercraft review</title>"
            +"<style>body{margin:0;background:#102c36;color:#f5e4bd;font:16px system-ui;text-align:center;padding:16px}h1{font-size:24px}canvas{display:block;width:min(100%,768px);margin:auto}button,input,select{margin:8px;min-height:44px}input{width:min(80%,600px)}p{max-width:760px;margin:12px auto;line-height:1.5}</style>"
            +"<h1>Watercraft / 32-heading review</h1><canvas id='view' width='768' height='512' aria-label='Watercraft sprite rotation with a fixed waterline anchor'></canvas><button id='play'>Pause</button><button id='previous'>Previous</button><button id='next'>Next</button>"
            +"<br><label>View <select id='mode'><option value='registered'>Registered</option><option value='raw'>Raw anchor</option><option value='overlay'>Registered + guide</option><option value='guide'>Guide only</option></select></label>"
            +"<label>Background <select id='background'><option value='#173d49'>Water</option><option value='#eeeeee'>Light</option><option value='#101010'>Dark</option></select></label>"
            +"<label>Cell size <select id='size'><option>360</option><option>160</option><option>80</option></select></label><br><label>Frame <output id='number'>0</output><br><input id='frame' type='range' min='0' max='31' value='0'></label>"
            +"<br><label>Effect anchors <select id='effects'><option value='none'>Off</option><option value='lights'>Lights</option><option value='smokestacks'>Exhaust outlets</option><option value='tires'>Tire fenders</option><option value='all'>All</option></select></label><p id='anchor-note'>Anchor points are guide estimates awaiting checks against finished pixels. Dashed circles indicate predicted occlusion.</p>"
            +"<p>Pink crosshair: fixed waterline anchor. Registration uses one uniform scale and small per-frame translations estimated from the geometry guide. No image rotation, per-frame stretching or blending. Overlap does not prove equipment or hull consistency.</p>"
            +"<script>const pivots="+p+",effectData="+anchors+",atlas=new Image(),guide=new Image();let ready=0,index=0,running=true;const el=id=>document.getElementById(id),ctx=el('view').getContext('2d');"
            +"function layer(img,px,py,scale,alpha){const col=index%8,row=Math.floor(index/8),x=Math.round(img.width*col/8),y=Math.round(img.height*row/4),w=Math.round(img.width*(col+1)/8)-x,h=Math.round(img.height*(row+1)/4)-y,s=+el('size').value*scale;ctx.globalAlpha=alpha;ctx.drawImage(img,x,y,w,h,384-s*px,355-s*py,s,s);ctx.globalAlpha=1}"
            +"function draw(){if(ready<2)return;ctx.fillStyle=el('background').value;ctx.fillRect(0,0,768,512);ctx.strokeStyle='#487381';ctx.lineWidth=.5;for(let i=-8;i<=8;i++){ctx.beginPath();ctx.moveTo(384+i*35-250,211);ctx.lineTo(384+i*35+250,499);ctx.moveTo(384+i*35-250,499);ctx.lineTo(384+i*35+250,211);ctx.stroke()}const mode=el('mode').value,p=pivots[index];if(mode==='guide')layer(guide,.5,.72,1,1);else{layer(atlas,mode==='raw'?.5:p[0],mode==='raw'?.72:p[1],mode==='raw'?1:p[2],1);if(mode==='overlay')layer(guide,.5,.72,1,.35)}ctx.strokeStyle='#ff8fb7';ctx.lineWidth=2;ctx.beginPath();ctx.moveTo(374,355);ctx.lineTo(394,355);ctx.moveTo(384,345);ctx.lineTo(384,365);ctx.stroke();ctx.fillStyle=el('background').value==='#eeeeee'?'#102c36':'#f5e4bd';ctx.font='22px sans-serif';ctx.fillText('Frame '+index+' / 31 — intended yaw '+(index*11.25)+'°',20,36);ctx.font='16px sans-serif';ctx.fillText(mode+' / source '+atlas.width+' × '+atlas.height,20,64);el('number').textContent=index;el('frame').value=index}"
            +"const baseDraw=draw;draw=function(){baseDraw();if(ready<2)return;const f=effectData.frames[index],choice=el('effects').value;if(!f||choice==='none')return;const mode=el('mode').value,isGuide=mode==='guide',p=pivots[index],raw=mode==='raw',s=+el('size').value*(isGuide||raw?1:p[2]),px=isGuide||raw?.5:p[0],py=isGuide||raw?.72:p[1];for(const kind of ['lights','smokestacks','tires']){if(choice!=='all'&&choice!==kind)continue;for(const a of f[kind]){const q=isGuide?a.guidePosition:a.position,r=isGuide?a.guideRadius:a.radius,x=384+(q[0]-px)*s,y=355+(q[1]-py)*s;ctx.strokeStyle=kind==='lights'?'#ffe475':kind==='smokestacks'?'#7effe0':'#ff87cf';ctx.fillStyle=ctx.strokeStyle;ctx.lineWidth=1.5;ctx.setLineDash(a.visibility==='occluded'?[3,3]:[]);ctx.beginPath();ctx.arc(x,y,Math.max(3,r*s),0,Math.PI*2);ctx.stroke();ctx.setLineDash([]);ctx.fillRect(x-1,y-1,2,2);ctx.font='10px sans-serif';ctx.fillText(a.id,x+5,y-4)}}};"
            +"function show(i){index=(i+32)%32;draw()}function pause(){running=false;el('play').textContent='Play'}el('play').onclick=()=>{running=!running;el('play').textContent=running?'Pause':'Play'};el('previous').onclick=()=>{pause();show(index-1)};el('next').onclick=()=>{pause();show(index+1)};el('frame').oninput=()=>{pause();show(+el('frame').value)};for(const id of ['mode','background','size','effects'])el(id).onchange=draw;if(!effectData.frames.length){el('effects').disabled=true;el('anchor-note').textContent='No anchor metadata supplied for this candidate.'}atlas.onload=guide.onload=()=>{ready++;draw()};atlas.onerror=guide.onerror=()=>{el('number').textContent='Image failed to load';pause()};atlas.src='data:image/png;base64,"+atlas+"';guide.src='data:image/png;base64,"+blockout+"';setInterval(()=>{if(running&&ready===2)show(index+1)},180);</script></html>";
    }
}
