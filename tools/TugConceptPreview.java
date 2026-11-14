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
    private static final String SOURCE="docs/art-studies/tug-isometric-v2-32.png";
    private static final File OUTPUT=new File("out/isometric-tug");
    private static final int COLS=8,ROWS=4,COUNT=COLS*ROWS;
    private TugConceptPreview() {}

    public static void main(String[] args) throws Exception {
        BufferedImage atlas=ImageIO.read(new File(SOURCE));
        if(atlas==null || !atlas.getColorModel().hasAlpha())
            throw new IllegalArgumentException("Expected transparent PNG atlas");
        OUTPUT.mkdirs();
        File frames=new File(OUTPUT,"frames"); frames.mkdirs();
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
            BufferedImage review=render(cell,frame,768,512,true);
            ImageIO.write(review,"png",new File(frames,String.format("%02d.png",frame)));
            sheet.drawImage(render(cell,frame,256,256,false),col*256,row*256,null);
        }
        sheet.dispose();
        ImageIO.write(contact,"png",new File(OUTPUT,"all-32-frames.png"));
        Files.write(new File(OUTPUT,"measurements.csv").toPath(),metrics.toString().getBytes(StandardCharsets.UTF_8));
        // Self-contained: can be copied to a phone or opened directly without a server.
        StringBuilder html=new StringBuilder("<!doctype html><html lang='en'><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>Barge Boss — tug study</title><style>body{margin:0;background:#102c36;color:#f5e4bd;font:16px system-ui;text-align:center;padding:16px}h1{font-size:24px}img{display:block;width:min(100%,768px);margin:auto}button,input{margin:12px;min-height:44px}input{width:min(80%,600px)}p{max-width:760px;margin:12px auto;line-height:1.5}</style><h1>Isometric tug — 32-frame art study</h1><p>Review candidate: heading spacing, model consistency, alpha edges and registration still need correction.</p><img id='view' alt='Tug rotation against a stationary footprint crosshair'><button id='play'>Pause</button><button id='previous'>Previous</button><button id='next'>Next</button><br><label>Frame <output id='number'>0</output><br><input id='frame' type='range' min='0' max='31' value='0'></label><p>The pink crosshair marks the requested fixed anchor. Source artwork is shown uncorrected so drift remains visible. No blending or flat image rotation conceals changes between frames. Smoke and wakes remain separate.</p><script>const images=[");
        for(int i=0;i<COUNT;i++) {
            if(i>0) html.append(',');
            html.append("'data:image/png;base64,").append(java.util.Base64.getEncoder().encodeToString(
                Files.readAllBytes(new File(frames,String.format("%02d.png",i)).toPath()))).append('\'');
        }
        html.append("];let index=0,running=true;const view=document.getElementById('view'),slider=document.getElementById('frame'),number=document.getElementById('number'),play=document.getElementById('play');function show(i){index=(i+32)%32;view.src=images[index];slider.value=index;number.textContent=index}function pause(){running=false;play.textContent='Play'}play.onclick=()=>{running=!running;play.textContent=running?'Pause':'Play'};slider.oninput=()=>{pause();show(+slider.value)};document.getElementById('previous').onclick=()=>{pause();show(index-1)};document.getElementById('next').onclick=()=>{pause();show(index+1)};setInterval(()=>{if(running)show(index+1)},180);show(0);</script></html>");
        Files.write(new File(OUTPUT,"review.html").toPath(),html.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("32 nonempty transparent cells reviewed; source PNG unchanged.");
        System.out.println("Open out/isometric-tug/review.html; contact sheet and measurements in the same directory.");
    }

    private static BufferedImage render(BufferedImage cell,int frame,int width,int height,boolean detailed) {
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
        // Deliberately use the prompt's fixed anchor; don't auto-center changing silhouettes.
        g.drawImage(cell,cx-size/2,cy-Math.round(size*.72f),size,size,null);
        g.setColor(new Color(0xff8fb7)); g.drawLine(cx-7,cy,cx+7,cy); g.drawLine(cx,cy-7,cx,cy+7);
        g.setColor(new Color(0xf5e4bd)); g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,detailed?23:15));
        g.drawString(detailed?"BARGE BOSS / ISOMETRIC TUG STUDY":"FRAME "+frame,16,detailed?35:23);
        if(detailed) {
            g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,16));
            g.drawString("Frame "+frame+" / 31   •   fixed source anchor (0.50, 0.72)",16,62);
            g.drawString("DRAFT — inspect heading spacing and footprint drift",16,height-24);
        }
        g.dispose(); return result;
    }
}
