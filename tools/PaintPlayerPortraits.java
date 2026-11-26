package com.bargeboss.game;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Paints the three selectable player-captain portrait sources.
 *
 * <p>Same contract as the other radio portraits: a right-facing three-quarter bust, bold ink
 * contours, warm cream/gold key light, cool navy/burgundy form shadows and a warm rim on the back
 * edges, six expressions with an identical head/shoulder silhouette, transparent backdrop. Output
 * is a 1536x1024 alpha source (3x2, 512 px cells) that {@link PackGavinPortrait} packs into the
 * 576x384 runtime sheet with one uniform scale and shoulder-base registration.
 *
 * <p>Frames: 0 neutral, 1 small speech, 2 open speech, 3 blink, 4 mad, 5 wince.
 * Usage: PaintPlayerPortraits [outDir]  (writes brittney/weslie/dot -source-v1.png)
 */
public final class PaintPlayerPortraits {
    static final int NEUTRAL = 0, TALK = 1, OPEN = 2, BLINK = 3, MAD = 4, WINCE = 5;
    static final float CELL = 512f, S = CELL / 192f;
    static final Color INK = new Color(0x1b1311);
    static final Color RIM = new Color(0xe8893c);
    static final Color TEETH = new Color(0xf7f1e4);
    static final Color MOUTH = new Color(0x4a1719);
    static final Color TONGUE = new Color(0xb4545a);
    static final Color SCLERA = new Color(0xf6efe3);

    /** Per-character face proportions in the shared 192 px portrait space. */
    static final class Face {
        float jawAX = 94, jawAY = 101, backX = 60, topY = 22, foreX = 138, browY = 58;
        float noseX = 153, noseY = 79, lipX = 147, mouthY = 93, chinX = 143, chinY = 111;
        float earX = 94, earY = 74, eyeX = 127, eyeY = 67;
        Color skin, skinShade, skinLight, lip, iris, brow;
        boolean lashes, crowsFeet, age;
    }

    private final Graphics2D g;
    private final Face f;
    private final int frame;

    private PaintPlayerPortraits(Graphics2D g, Face f, int frame) {
        this.g = g;
        this.f = f;
        this.frame = frame;
    }

    public static void main(String[] args) throws Exception {
        String dir = args.length > 0 ? args[0] : "docs/art-studies/captains";
        new File(dir).mkdirs();
        String[] names = {"brittney", "weslie", "dot"};
        for (int c = 0; c < 3; c++) {
            BufferedImage sheet = new BufferedImage(1536, 1024, BufferedImage.TYPE_INT_ARGB);
            for (int fr = 0; fr < 6; fr++) {
                Graphics2D g = sheet.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.translate(fr % 3 * CELL, fr / 3 * CELL);
                g.scale(S, S);
                g.clip(new Rectangle2D.Float(0, 0, 192, 192));
                Face face = c == 0 ? brittneyFace() : c == 1 ? weslieFace() : dotFace();
                PaintPlayerPortraits painter = new PaintPlayerPortraits(g, face, fr);
                if (c == 0) painter.brittney();
                else if (c == 1) painter.weslie();
                else painter.dot();
                g.dispose();
            }
            ImageIO.write(sheet, "png", new File(dir, names[c] + "-source-v1.png"));
        }
    }

    // ---------------------------------------------------------------- shared drawing helpers

    static Color rgb(int value) { return new Color(value); }
    static Color alpha(Color c, float a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(255 * a)); }

    static Area shifted(Shape s, float dx, float dy) {
        return new Area(AffineTransform.getTranslateInstance(dx, dy).createTransformedShape(s));
    }

    /** Edge crescent: the part of a shape not covered by itself moved by (dx, dy). */
    static Area crescent(Shape s, float dx, float dy) {
        Area a = new Area(s);
        a.subtract(shifted(s, dx, dy));
        return a;
    }

    void ink(Shape s, float width) {
        g.setColor(INK);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
    }

    void line(float x0, float y0, float x1, float y1, float width, Color c) {
        g.setColor(c);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(x0, y0, x1, y1));
    }

    void curve(float width, Color c, float... p) {
        Path2D path = new Path2D.Float();
        path.moveTo(p[0], p[1]);
        for (int i = 2; i + 5 < p.length; i += 6) {
            path.curveTo(p[i], p[i + 1], p[i + 2], p[i + 3], p[i + 4], p[i + 5]);
        }
        g.setColor(c);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(path);
    }

    void fill(Shape s, Color c) {
        g.setColor(c);
        g.fill(s);
    }

    /** Fills inside a clip, leaving the caller's clip untouched afterwards. */
    void within(Shape clip, Shape s, Color c) {
        Shape old = g.getClip();
        g.clip(clip);
        fill(s, c);
        g.setClip(old);
    }

    /** Diagonal comic hatching restricted to an area. */
    void hatch(Shape area, float spacing, float width, Color c) {
        Shape old = g.getClip();
        g.clip(area);
        Rectangle2D b = area.getBounds2D();
        g.setColor(c);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (float x = (float) b.getMinX() - (float) b.getHeight(); x < b.getMaxX(); x += spacing) {
            g.draw(new Line2D.Float(x, (float) b.getMaxY(), x + (float) b.getHeight() * .8f, (float) b.getMinY()));
        }
        g.setClip(old);
    }

    /**
     * One painted form: flat base, cool form shadow on the lower/back edge, warm key light on the
     * upper/front edge, warm back rim and a bold ink contour.
     */
    void form(Shape s, Color base, Color shade, Color light, float shadeShift, float lightShift, float outline) {
        fill(s, base);
        if (shadeShift > 0) {
            // Stacked crescents read as a painted gradient rather than a flat cel band.
            for (int i = 3; i >= 1; i--) fill(crescent(s, shadeShift * i / 3f, -shadeShift * .55f * i / 3f), alpha(shade, i == 1 ? .95f : .45f));
            hatch(crescent(s, shadeShift * .8f, -shadeShift * .44f), 2.6f, .35f, alpha(INK, .26f));
        }
        if (lightShift > 0) {
            fill(crescent(s, -lightShift * 1.8f, lightShift * 1.8f), alpha(light, .45f));
            fill(crescent(s, -lightShift, lightShift), light);
        }
        if (outline >= 1.4f) fill(crescent(s, 1.6f, 0), alpha(RIM, .7f));
        if (outline > 0) ink(s, outline);
    }

    static Path2D poly(float... p) {
        Path2D path = new Path2D.Float();
        path.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) path.lineTo(p[i], p[i + 1]);
        path.closePath();
        return path;
    }

    // ---------------------------------------------------------------- head construction

    float drop() { return frame == OPEN ? 3.5f : frame == TALK ? 1.2f : 0; }

    Path2D headPath() {
        float d = drop();
        Path2D p = new Path2D.Float();
        p.moveTo(f.jawAX, f.jawAY);
        p.curveTo(f.jawAX - 16, f.jawAY - 10, f.backX - 3, f.topY + 44, f.backX + 7, f.topY + 22);
        p.curveTo(f.backX + 20, f.topY - 1, f.foreX - 26, f.topY - 4, f.foreX - 7, f.topY + 13);
        p.curveTo(f.foreX - 1, f.topY + 21, f.foreX + 2, f.browY - 9, f.foreX + 3, f.browY);
        p.curveTo(f.foreX + 3.5f, f.browY + 3, f.foreX + 1.5f, f.browY + 5, f.foreX + 2.5f, f.browY + 7.5f);
        p.curveTo(f.noseX - 5, f.noseY - 7, f.noseX + 1.5f, f.noseY - 2.5f, f.noseX, f.noseY + 1);
        p.curveTo(f.noseX - 1, f.noseY + 4, f.noseX - 5, f.noseY + 5, f.noseX - 7.5f, f.noseY + 5);
        p.curveTo(f.lipX - 1.5f, f.noseY + 7, f.lipX, f.mouthY - 2.5f, f.lipX, f.mouthY - .5f);
        p.curveTo(f.lipX + .3f, f.mouthY + 2 + d * .4f, f.lipX - .5f, f.mouthY + 4.5f + d, f.lipX - 2.5f, f.mouthY + 6.5f + d);
        p.curveTo(f.chinX - 3, f.chinY - 6 + d, f.chinX + 1, f.chinY - 4 + d, f.chinX, f.chinY + d);
        p.curveTo(f.chinX - 2.5f, f.chinY + 5 + d, f.chinX - 12, f.chinY + 6 + d, f.chinX - 20, f.chinY + 4 + d);
        p.curveTo(f.chinX - 32, f.chinY + 1 + d * .5f, f.jawAX + 8, f.jawAY + 6, f.jawAX, f.jawAY);
        p.closePath();
        return p;
    }

    Path2D neckPath(float width) {
        float x0 = f.jawAX - 2 - width, x1 = f.chinX - 12 + width;
        return poly(f.jawAX + 2, f.jawAY - 4, f.chinX - 15, f.chinY - 2, x1, 142, x0, 142);
    }

    void neck(float width) {
        Path2D n = neckPath(width);
        form(n, f.skin, f.skinShade, f.skinLight, 7, 0, 1.4f);
        // Cast shadow from the jaw.
        Path2D cast = new Path2D.Float();
        cast.moveTo(f.jawAX - 4, f.jawAY - 6);
        cast.curveTo(f.jawAX + 10, f.jawAY + 12, f.chinX - 24, f.chinY + 10, f.chinX - 10, f.chinY + 2);
        cast.lineTo(f.chinX - 10, f.chinY + 14);
        cast.curveTo(f.chinX - 24, f.chinY + 20, f.jawAX + 4, f.jawAY + 22, f.jawAX - 8, f.jawAY + 10);
        cast.closePath();
        within(n, cast, alpha(f.skinShade, .9f));
        line(f.chinX - 14, f.chinY + 13, f.chinX - 17, f.chinY + 22, .6f, alpha(INK, .5f));
    }

    void head() {
        Path2D h = headPath();
        fill(h, f.skin);
        for (int i = 4; i >= 1; i--) fill(crescent(h, 4 + 3.4f * i, -2 - 1.5f * i), alpha(f.skinShade, i == 1 ? .9f : .35f));
        hatch(crescent(h, 11, -5), 2.4f, .3f, alpha(INK, .2f));
        fill(crescent(h, -4.5f, 4.5f), alpha(f.skinLight, .45f));
        fill(crescent(h, -2.4f, 2.6f), f.skinLight);
        fill(crescent(h, 1.5f, 0), alpha(RIM, .7f));
        // Cheekbone plane and a soft blush.
        within(h, new Ellipse2D.Float(f.eyeX - 6, f.mouthY - 19, 16, 9), alpha(f.skinLight, .55f));
        within(h, new Ellipse2D.Float(f.eyeX - 4, f.mouthY - 14, 14, 7), new Color(0xe0, 0x6a, 0x5a, frame == MAD ? 120 : 46));
        // Under-jaw shadow keeps the head readable over the collar.
        Path2D jaw = new Path2D.Float();
        jaw.moveTo(f.jawAX - 6, f.jawAY - 6);
        jaw.curveTo(f.jawAX + 8, f.jawAY + 2, f.chinX - 26, f.chinY + 2 + drop(), f.chinX - 8, f.chinY + drop());
        jaw.lineTo(f.chinX - 8, f.chinY + 12);
        jaw.lineTo(f.jawAX - 6, f.jawAY + 12);
        jaw.closePath();
        within(h, jaw, alpha(f.skinShade, .55f));
        ink(h, 1.6f);
        nose();
        mouth();
        eye();
        brow();
        if (f.crowsFeet) {
            for (int i = 0; i < 3; i++) {
                float y = f.eyeY - 2 + i * 2.6f;
                line(f.eyeX - 10, y, f.eyeX - 14.5f, y - 1 + i * 1.2f, .55f, alpha(INK, .6f));
            }
        }
        if (f.age) {
            curve(.55f, alpha(INK, .5f), f.foreX - 14, f.browY - 12, f.foreX - 10, f.browY - 13.5f, f.foreX - 6, f.browY - 13.5f, f.foreX - 3, f.browY - 12);
        }
        // Smile line from nose wing toward the mouth corner.
        curve(.7f, alpha(INK, .7f), f.noseX - 10, f.noseY + 1, f.noseX - 15, f.noseY + 6, f.noseX - 17, f.mouthY - 2, f.noseX - 16 - (frame == WINCE ? 3 : 0), f.mouthY + 3);
        if (frame == MAD) angerMark();
        if (frame == WINCE) sweat();
    }

    void ear() {
        Shape e = AffineTransform.getRotateInstance(-.18, f.earX, f.earY)
                .createTransformedShape(new Ellipse2D.Float(f.earX - 5.5f, f.earY - 9, 11, 18));
        form(e, f.skin, f.skinShade, f.skinLight, 3, 1.4f, 1.4f);
        curve(.9f, INK, f.earX + 1.5f, f.earY - 5, f.earX - 3, f.earY - 4, f.earX - 3, f.earY + 3, f.earX + .5f, f.earY + 5);
        curve(.6f, alpha(INK, .7f), f.earX + 2, f.earY - 1, f.earX, f.earY, f.earX, f.earY + 2, f.earX + 2, f.earY + 3);
    }

    void nose() {
        curve(.9f, INK, f.noseX - 9, f.noseY + 1.5f, f.noseX - 7.5f, f.noseY + 3.5f, f.noseX - 5, f.noseY + 3.8f, f.noseX - 3, f.noseY + 2.2f);
        line(f.foreX + 4, f.browY + 11, f.noseX - 2.5f, f.noseY - 3, 1.1f, alpha(f.skinLight, .9f));
    }

    void mouth() {
        float cx = f.lipX - 15, my = f.mouthY + .5f, lx = f.lipX - .8f, d = drop();
        Path2D m = new Path2D.Float();
        switch (frame) {
            case TALK:
            case OPEN: {
                float open = frame == OPEN ? 8.5f : 4.5f;
                m.moveTo(cx, my - 1);
                m.curveTo(cx + 4, my - 2.2f, lx - 4, my - 2, lx, my - 1.2f);
                m.curveTo(lx + .2f, my + open * .5f, lx - 2, my + open, lx - 5, my + open);
                m.curveTo(cx + 5, my + open + .3f, cx + 1, my + open * .6f, cx, my - 1);
                m.closePath();
                fill(m, MOUTH);
                within(m, new Rectangle2D.Float(cx, my - 3, lx - cx + 1, 2.6f), TEETH);
                within(m, new Ellipse2D.Float(cx + 3, my + open - 3, lx - cx - 5, 5), TONGUE);
                ink(m, .95f);
                break;
            }
            case MAD: {
                m.moveTo(cx - 1, my + 1.5f);
                m.curveTo(cx + 4, my - 1, lx - 4, my - 1.5f, lx, my - .6f);
                m.lineTo(lx - .6f, my + 3.6f);
                m.curveTo(lx - 4, my + 3.2f, cx + 4, my + 3.4f, cx - 1, my + 1.5f);
                m.closePath();
                fill(m, TEETH);
                line(cx + 1, my + 1.3f, lx - .5f, my + 1.4f, .5f, alpha(INK, .8f));
                for (float x = cx + 4; x < lx - 1; x += 3) line(x, my - .5f, x, my + 3, .45f, alpha(INK, .7f));
                ink(m, .95f);
                line(cx - 1, my + 1.5f, cx - 3.5f, my + 4.5f, .9f, INK);
                break;
            }
            case WINCE: {
                m.moveTo(cx - 4, my - 3);
                m.curveTo(cx + 2, my - 2.5f, lx - 5, my - 1.6f, lx, my - .8f);
                m.lineTo(lx - .5f, my + 3.2f);
                m.curveTo(lx - 5, my + 3.6f, cx + 1, my + 2.8f, cx - 4, my - 3);
                m.closePath();
                fill(m, TEETH);
                line(cx - 2, my - 1.2f, lx - .5f, my + 1.2f, .5f, alpha(INK, .8f));
                for (float x = cx + 1; x < lx - 1; x += 2.8f) line(x, my - 1.5f, x + .3f, my + 2.5f, .45f, alpha(INK, .65f));
                ink(m, .95f);
                line(cx - 4, my - 3, cx - 6, my - 4.6f, .8f, INK);
                break;
            }
            default: {
                if (smileTeeth()) {
                    m.moveTo(cx - 1.5f, my - 2.5f);
                    m.curveTo(cx + 4, my - 1.4f, lx - 4, my - 1.4f, lx, my - 1);
                    m.curveTo(lx - .5f, my + 2.5f, lx - 3.5f, my + 4, lx - 6, my + 4);
                    m.curveTo(cx + 3, my + 3.6f, cx, my + 1, cx - 1.5f, my - 2.5f);
                    m.closePath();
                    fill(m, TEETH);
                    line(cx + 1, my + 1.2f, lx - 1, my + .8f, .45f, alpha(INK, .55f));
                    ink(m, .95f);
                } else {
                    curve(1.1f, INK, cx - 1.5f, my - 2.4f, cx + 3, my + 1.4f, lx - 4, my + .8f, lx, my - .6f);
                }
                curve(.7f, INK, cx - 2.5f, my - 4, cx - 2.2f, my - 2.8f, cx - 1.8f, my - 2.2f, cx - .8f, my - 1.6f);
                break;
            }
        }
        // Lower lip tone.
        if (frame != MAD && frame != WINCE) {
            float y = frame == OPEN ? my + 9.6f : frame == TALK ? my + 5.6f : my + 4.4f;
            curve(.8f, alpha(f.lip, .9f), cx + 4, y, cx + 7, y + 1.1f + d * .1f, lx - 5, y + 1.1f, lx - 2.5f, y - .4f);
        }
    }

    boolean smileTeeth = false;
    boolean smileTeeth() { return smileTeeth; }

    void eye() {
        float x = f.eyeX, y = f.eyeY;
        if (frame == BLINK) {
            curve(1.3f, INK, x - 7, y - .2f, x - 3, y + 2.4f, x + 3, y + 2.4f, x + 7, y + .2f);
            if (f.lashes) line(x - 7, y - .2f, x - 9.5f, y - 1.6f, .9f, INK);
            return;
        }
        if (frame == WINCE) {
            line(x - 7, y - 3, x + 6, y + .2f, 1.3f, INK);
            line(x - 7, y + 3, x + 6, y + .2f, 1.3f, INK);
            line(x - 9, y - 1, x - 12, y - 2.4f, .6f, alpha(INK, .7f));
            line(x - 9, y + 1.4f, x - 12, y + 2.4f, .6f, alpha(INK, .7f));
            return;
        }
        Path2D almond = new Path2D.Float();
        almond.moveTo(x - 7, y);
        almond.curveTo(x - 4, y - 4, x + 3, y - 4.2f, x + 7, y - .5f);
        almond.curveTo(x + 4, y + 3.2f, x - 3, y + 3.4f, x - 7, y);
        almond.closePath();
        fill(almond, SCLERA);
        float ix = x + 2.2f;
        within(almond, new Ellipse2D.Float(ix - 3.4f, y - 3.6f, 6.8f, 7f), f.iris);
        within(almond, new Ellipse2D.Float(ix - 1.6f, y - 1.8f, 3.2f, 3.4f), new Color(0x14100e));
        within(almond, new Ellipse2D.Float(ix, y - 2.6f, 1.6f, 1.6f), Color.WHITE);
        if (frame == MAD) {
            // Lowered, flattened upper lid.
            Path2D lid = poly(x - 8, y - 6, x + 8, y - 4, x + 8, y - .4f, x - 8, y - 1.2f);
            within(almond, lid, f.skinShade);
            line(x - 7.5f, y - 1, x + 7, y - .4f, 1.5f, INK);
        } else {
            curve(1.5f, INK, x - 7, y, x - 4, y - 4, x + 3, y - 4.2f, x + 7, y - .5f);
        }
        curve(.6f, alpha(INK, .65f), x - 6, y + 1, x - 3, y + 3.4f, x + 3, y + 3.2f, x + 6, y + .3f);
        if (f.lashes && frame != MAD) {
            line(x - 7, y, x - 9.8f, y - 1.8f, 1f, INK);
            line(x - 5.5f, y - 2.4f, x - 7.6f, y - 4.6f, .8f, INK);
        }
    }

    void brow() {
        float x = f.eyeX, y = f.browY + 1;
        float inner = frame == MAD ? 3.8f : frame == WINCE ? -2.8f : 0;
        float outer = frame == MAD ? -1.5f : frame == WINCE ? 1.2f : 0;
        Path2D b = new Path2D.Float();
        b.moveTo(x - 10, y + 2 + outer);
        b.curveTo(x - 5, y - 2 + outer, x + 2, y - 2.4f + inner * .6f, x + 9, y + inner);
        b.lineTo(x + 8.5f, y + 2.2f + inner);
        b.curveTo(x + 2, y + .4f + inner * .6f, x - 5, y + .8f + outer, x - 10, y + 2 + outer);
        b.closePath();
        fill(b, f.brow);
        ink(b, .6f);
        if (frame == MAD) {
            line(x + 9.5f, y + 3.5f, x + 11.5f, y - 1, .6f, alpha(INK, .7f));
            line(x + 7.5f, y + 4.5f, x + 9, y - .5f, .5f, alpha(INK, .6f));
        }
        if (frame == WINCE) curve(.5f, alpha(INK, .6f), x - 2, y - 5, x + 1, y - 6.5f, x + 4, y - 6.5f, x + 7, y - 5);
    }

    void angerMark() {
        float x = f.foreX - 12, y = f.topY + 22;
        Color red = new Color(0xd8342c);
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 4 + i * Math.PI / 2;
            float cx = x + (float) Math.cos(a) * 2.6f, cy = y + (float) Math.sin(a) * 2.6f;
            float ox = (float) Math.cos(a + Math.PI / 4) * 2.2f, oy = (float) Math.sin(a + Math.PI / 4) * 2.2f;
            Path2D arc = new Path2D.Float();
            arc.moveTo(cx - ox, cy - oy);
            arc.quadTo(x + (float) Math.cos(a) * .6f, y + (float) Math.sin(a) * .6f, cx + ox, cy + oy);
            g.setColor(INK);
            g.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(arc);
            g.setColor(red);
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(arc);
        }
    }

    void sweat() {
        float x = f.foreX - 4, y = f.topY + 20;
        Path2D drop = new Path2D.Float();
        drop.moveTo(x, y);
        drop.curveTo(x + 2.5f, y + 4, x + 3.6f, y + 6.5f, x + 2.6f, y + 8.4f);
        drop.curveTo(x + 1.4f, y + 10.2f, x - 1.6f, y + 10.2f, x - 2.6f, y + 8.4f);
        drop.curveTo(x - 3.4f, y + 6.4f, x - 2.4f, y + 4, x, y);
        drop.closePath();
        fill(drop, new Color(0x9fdcf2));
        fill(new Ellipse2D.Float(x - 1.6f, y + 5.4f, 1.6f, 2.2f), Color.WHITE);
        ink(drop, .9f);
    }

    /** Natural three-quarter shoulders: back arm, trapezius slope, neck, chest and front shoulder. */
    static Path2D torso(float left, float right, float neckBack, float neckFront, float neckY, float bulk) {
        Path2D p = new Path2D.Float();
        p.moveTo(left, 184);
        p.curveTo(left + 1, 170, left + 3 - bulk, 152, left + 15, 143 - bulk);
        p.curveTo(left + 25, 135 - bulk, 44, 129 - bulk, 62, 126 - bulk * .5f);
        p.curveTo(74, 123, 84, neckY, neckBack, neckY);
        p.curveTo(neckBack + 8, neckY + 11, neckFront - 9, neckY + 13, neckFront, neckY + 2);
        p.curveTo(neckFront + 14, neckY + 4, right - 24, neckY + 9 - bulk, right - 11, neckY + 19 - bulk * .5f);
        p.curveTo(right - 3, neckY + 28, right, neckY + 44, right, 184);
        p.closePath();
        return p;
    }

    // ---------------------------------------------------------------- characters

    static Face brittneyFace() {
        Face f = new Face();
        f.skin = rgb(0xf1b78f);
        f.skinShade = rgb(0xc27b5b);
        f.skinLight = rgb(0xffd9b8);
        f.lip = rgb(0xc65d55);
        f.iris = rgb(0x5a3a24);
        f.brow = rgb(0x221713);
        f.lashes = true;
        f.jawAX = 95; f.jawAY = 100; f.chinX = 143; f.chinY = 110; f.noseX = 152; f.noseY = 79;
        return f;
    }

    void brittney() {
        smileTeeth = true;
        // Torso: olive utility jacket, open over a black tee.
        Path2D body = torso(8, 185, 94, 134, 121, 0);
        neck(1);
        form(body, rgb(0x6f7349), rgb(0x444729), rgb(0x9d9d6a), 16, 2.6f, 1.6f);
        Path2D tee = new Path2D.Float();
        tee.moveTo(97, 126);
        tee.curveTo(104, 136, 126, 138, 135, 127);
        tee.lineTo(152, 132);
        tee.curveTo(150, 150, 152, 168, 158, 184);
        tee.lineTo(114, 184);
        tee.curveTo(110, 160, 104, 140, 92, 128);
        tee.closePath();
        form(tee, rgb(0x1d1d23), rgb(0x0f0f13), rgb(0x3b3c48), 6, 1.6f, 1.3f);
        curve(1.1f, INK, 98, 127, 105, 136, 125, 138, 135, 128);
        curve(.7f, alpha(rgb(0x3b3c48), .9f), 101, 130, 107, 137, 124, 139, 132, 132);
        // Stand collar of the jacket around the back of the neck.
        Path2D hood = poly(78, 124, 94, 114, 99, 122, 96, 130, 84, 131);
        form(hood, rgb(0x777b4f), rgb(0x444729), rgb(0xa6a672), 4, 1.6f, 1.4f);
        // Jacket front edges with zipper teeth.
        Path2D edgeL = poly(92, 128, 99, 133, 106, 150, 113, 184, 104, 184, 98, 150);
        form(edgeL, rgb(0x787c50), rgb(0x4c4f2e), rgb(0xa8a874), 3, 1.4f, 1.2f);
        Path2D edgeR = poly(135, 127, 152, 132, 150, 156, 158, 184, 150, 184, 145, 156, 142, 136);
        form(edgeR, rgb(0x81855a), rgb(0x4c4f2e), rgb(0xb5b47e), 3, 1.6f, 1.2f);
        for (float t = 0; t < 1; t += .085f) {
            fill(new Rectangle2D.Float(105 + t * 7.5f, 140 + t * 42, 1.3f, 1.1f), rgb(0xc9c9c0));
            fill(new Rectangle2D.Float(144.5f + t * 6, 140 + t * 42, 1.3f, 1.1f), rgb(0xd9d9cf));
        }
        // Chest pocket on the back panel and seam lines.
        Path2D pocket = poly(40, 150, 68, 146, 69, 156, 41, 160);
        form(pocket, rgb(0x777b4f), rgb(0x4c4f2e), rgb(0xa2a26e), 2, 1, 1.1f);
        fill(new Ellipse2D.Float(53, 152, 2.6f, 2.6f), rgb(0x2d2d20));
        curve(.7f, alpha(INK, .7f), 28, 142, 36, 150, 38, 166, 36, 184);
        curve(.7f, alpha(INK, .6f), 160, 146, 166, 154, 170, 166, 172, 184);
        // Crossbody strap from the back shoulder across the chest.
        Path2D strap = poly(52, 130, 60, 127, 168, 184, 158, 184);
        form(strap, rgb(0xb07a4d), rgb(0x7a4c2c), rgb(0xd9a674), 2, 1, 1.2f);
        line(57, 130, 162, 183, .4f, alpha(rgb(0xf0d2a8), .85f));
        fill(new RoundRectangle2D.Float(82, 143, 7, 6, 2, 2), rgb(0xc9a050));
        ink(new RoundRectangle2D.Float(82, 143, 7, 6, 2, 2), .8f);

        head();
        ear();
        // Top-knot bun and its base.
        Path2D knotBase = poly(90, 30, 108, 26, 112, 34, 92, 38);
        fill(knotBase, rgb(0x241a16));
        Shape bun = new Ellipse2D.Float(85, 3, 30, 26);
        form(bun, rgb(0x2a1e19), rgb(0x100b0a), rgb(0x6a4634), 5, 2.2f, 1.6f);
        curve(.7f, alpha(rgb(0x7d5540), .9f), 92, 10, 98, 6, 106, 7, 110, 13);
        curve(.7f, alpha(INK, .8f), 90, 18, 96, 12, 104, 12, 112, 18);
        curve(.6f, alpha(INK, .7f), 89, 23, 97, 18, 106, 20, 112, 24);
        // Hair cap swept back from the hairline.
        Path2D cap = new Path2D.Float();
        cap.moveTo(139, 44);
        cap.curveTo(134, 30, 112, 20, 92, 24);
        cap.curveTo(70, 28, 58, 42, 58, 62);
        cap.curveTo(58, 78, 62, 90, 70, 98);
        cap.lineTo(84, 96);
        cap.curveTo(86, 86, 86, 70, 90, 62);
        cap.curveTo(98, 52, 112, 44, 128, 42);
        cap.curveTo(132, 42, 136, 43, 139, 44);
        cap.closePath();
        form(cap, rgb(0x2a1e19), rgb(0x120c0b), rgb(0x6d4936), 7, 2.2f, 1.6f);
        Color strand = alpha(rgb(0x8a5c44), .85f);
        curve(.7f, strand, 132, 38, 118, 30, 100, 30, 84, 34);
        curve(.6f, strand, 126, 44, 110, 38, 92, 40, 76, 50);
        curve(.6f, alpha(INK, .8f), 120, 44, 104, 40, 90, 46, 80, 62);
        curve(.6f, alpha(INK, .8f), 136, 41, 120, 33, 100, 33, 86, 38);
        curve(.6f, alpha(INK, .8f), 102, 48, 92, 54, 86, 66, 84, 80);

        sunglasses();
    }

    void sunglasses() {
        float x = f.eyeX, y = f.eyeY;
        // Temple arm back to the ear.
        line(x - 9, y - 4, f.earX + 3, f.earY - 6, 2.2f, INK);
        line(x - 9, y - 4.6f, f.earX + 4, f.earY - 6.6f, .7f, rgb(0xc89a52));
        Path2D near = poly(x - 10, y - 8, x + 12, y - 8.6f, x + 13.5f, y + 8, x - 7.5f, y + 9.5f);
        Path2D far = poly(f.noseX - 6, y - 8.2f, f.noseX + 4, y - 8.4f, f.noseX + 3.5f, y + 6.5f, f.noseX - 4.5f, y + 7.5f);
        Color top = rgb(0x26181a), bottom = rgb(0x6b4134);
        for (Path2D lens : new Path2D[]{far, near}) {
            Rectangle2D b = lens.getBounds2D();
            g.setPaint(new GradientPaint(0, (float) b.getMinY(), top, 0, (float) b.getMaxY(), bottom));
            g.fill(lens);
            Shape old = g.getClip();
            g.clip(lens);
            line((float) b.getMinX() + 3, (float) b.getMaxY() - 2, (float) b.getMinX() + 9, (float) b.getMinY() + 1, 1.4f, alpha(Color.WHITE, .38f));
            line((float) b.getMinX() + 7, (float) b.getMaxY() - 1, (float) b.getMinX() + 11, (float) b.getMinY() + 5, .7f, alpha(Color.WHITE, .25f));
            if (frame == BLINK) {
                // Eyes hide behind the lenses: the idle beat becomes a travelling glint.
                line((float) b.getMinX() + 12, (float) b.getMaxY(), (float) b.getMaxX(), (float) b.getMinY() + 2, 2.4f, alpha(Color.WHITE, .7f));
            }
            g.setClip(old);
            ink(lens, 1.7f);
        }
        line(x - 9, y - 8, x + 11, y - 8.6f, .7f, rgb(0xd9ad5e));
        line(x + 13, y - 6, f.noseX - 6, y - 6.5f, 1.8f, INK);
    }

    static Face weslieFace() {
        Face f = new Face();
        f.skin = rgb(0xd28e5e);
        f.skinShade = rgb(0x985737);
        f.skinLight = rgb(0xf3ba88);
        f.lip = rgb(0xa9563f);
        f.iris = rgb(0x4f7590);
        f.brow = rgb(0x6c5644);
        f.crowsFeet = true;
        f.age = true;
        f.jawAX = 92; f.jawAY = 103; f.chinX = 144; f.chinY = 114; f.noseX = 154; f.noseY = 80;
        f.mouthY = 95; f.foreX = 139; f.browY = 59; f.eyeY = 68; f.topY = 21;
        return f;
    }

    void weslie() {
        smileTeeth = true;
        Path2D body = torso(5, 187, 93, 136, 121, 1);
        neck(2);
        form(body, rgb(0x2b2f3b), rgb(0x161821), rgb(0x59617a), 16, 2.6f, 1.6f);
        hatch(crescent(body, 26, -10), 3.2f, .35f, alpha(INK, .3f));
        // Shirt panel and open collar.
        Path2D shirt = poly(96, 124, 136, 126, 146, 136, 128, 184, 114, 184, 98, 140);
        form(shirt, rgb(0xe8ebf1), rgb(0xa6afc0), rgb(0xffffff), 5, 1.4f, 1.3f);
        Path2D vee = poly(104, 126, 132, 127, 120, 150);
        form(vee, f.skin, f.skinShade, f.skinLight, 4, 0, 1.1f);
        curve(.6f, alpha(INK, .6f), 113, 131, 116, 134, 120, 134, 124, 131);
        Path2D collarL = poly(96, 118, 106, 126, 118, 150, 100, 140, 90, 128);
        form(collarL, rgb(0xeef0f5), rgb(0xa6afc0), rgb(0xffffff), 3, 1.2f, 1.3f);
        Path2D collarR = poly(132, 120, 140, 126, 146, 142, 132, 138, 122, 150);
        form(collarR, rgb(0xf3f5f9), rgb(0xb3bbca), rgb(0xffffff), 3, 1.2f, 1.3f);
        fill(new Ellipse2D.Float(119.5f, 160, 2.4f, 2.4f), rgb(0xc9ccd6));
        ink(new Ellipse2D.Float(119.5f, 160, 2.4f, 2.4f), .5f);
        // Notched lapels.
        Path2D lapelL = poly(90, 126, 98, 140, 112, 160, 118, 184, 106, 184, 96, 158, 84, 142, 88, 134);
        form(lapelL, rgb(0x343948), rgb(0x1a1d26), rgb(0x6d7690), 3, 1.6f, 1.4f);
        Path2D lapelR = poly(140, 128, 152, 134, 148, 144, 154, 148, 140, 170, 130, 184, 124, 184, 136, 156, 142, 142);
        form(lapelR, rgb(0x3a4050), rgb(0x1a1d26), rgb(0x7a84a0), 3, 1.8f, 1.4f);
        curve(.6f, alpha(rgb(0x8d97b3), .8f), 92, 136, 98, 148, 106, 162, 112, 180);
        // Gold pocket square on the far breast.
        Path2D pocket = poly(158, 156, 172, 154, 172, 158, 158, 160);
        form(pocket, rgb(0x2f3442), rgb(0x1a1d26), rgb(0x59617a), 1, 0, 1);
        Path2D square = poly(160, 156, 163, 150, 166, 153, 169, 149, 171, 155);
        form(square, rgb(0xe3a93b), rgb(0xa86e1c), rgb(0xffd98a), 1.4f, .8f, .9f);
        curve(.7f, alpha(INK, .65f), 30, 140, 34, 152, 36, 168, 34, 184);
        head();
        ear();
        // Short swept hair: lighter brown with greying highlights.
        Path2D hair = new Path2D.Float();
        hair.moveTo(140, 40);
        hair.curveTo(144, 34, 145, 28, 140, 24);
        hair.curveTo(132, 15, 106, 13, 88, 17);
        hair.curveTo(70, 21, 59, 36, 58, 56);
        hair.curveTo(58, 66, 60, 74, 63, 80);
        hair.lineTo(67, 78);
        hair.lineTo(68, 85);
        hair.lineTo(72, 81);
        hair.lineTo(75, 88);
        hair.lineTo(79, 84);
        hair.curveTo(82, 76, 86, 66, 92, 62);
        hair.curveTo(96, 66, 98, 74, 101, 80);
        hair.lineTo(104, 78);
        hair.curveTo(102, 66, 104, 56, 112, 49);
        hair.curveTo(120, 43, 132, 42, 140, 40);
        hair.closePath();
        form(hair, rgb(0xa78868), rgb(0x6b5442), rgb(0xe2d6c2), 6, 2.8f, 1.6f);
        Color grey = alpha(rgb(0xece6da), .9f), brown = alpha(rgb(0x5a4434), .85f);
        curve(.8f, grey, 140, 27, 128, 18, 108, 16, 90, 20);
        curve(.7f, grey, 136, 35, 122, 26, 104, 24, 86, 30);
        curve(.6f, brown, 132, 41, 118, 32, 98, 32, 80, 42);
        curve(.6f, brown, 112, 48, 100, 44, 86, 46, 72, 58);
        curve(.6f, grey, 96, 50, 84, 50, 72, 58, 64, 72);
        curve(.6f, brown, 90, 60, 80, 62, 72, 70, 68, 82);
        // Swept front lock.
        curve(.9f, INK, 141, 31, 136, 24, 124, 20, 112, 21);
        // Sideburn shading.
        line(100, 66, 102, 77, .7f, alpha(INK, .6f));
    }

    static Face dotFace() {
        Face f = new Face();
        f.skin = rgb(0x8c5636);
        f.skinShade = rgb(0x5b3420);
        f.skinLight = rgb(0xbb8357);
        f.lip = rgb(0x6e2f2a);
        f.iris = rgb(0x3a2116);
        f.brow = rgb(0xd9d4cc);
        f.lashes = true;
        f.crowsFeet = true;
        f.age = true;
        // A rounder, fuller face with a soft chin and a small nose.
        f.jawAX = 90; f.jawAY = 104; f.chinX = 141; f.chinY = 113; f.noseX = 150; f.noseY = 80;
        f.lipX = 145; f.mouthY = 95; f.foreX = 137; f.browY = 60; f.eyeX = 125; f.eyeY = 69; f.topY = 24;
        f.earX = 92; f.earY = 77;
        return f;
    }

    void dot() {
        smileTeeth = false;
        // Stocky build: broad, sloping shoulders.
        Path2D body = torso(3, 189, 91, 138, 119, 3);
        neck(3);
        form(body, rgb(0xf0c02f), rgb(0xb07d14), rgb(0xffe484), 16, 2.6f, 1.6f);
        // Slicker front placket with snaps, and fold lines.
        Path2D placket = poly(134, 132, 146, 134, 156, 184, 144, 184);
        form(placket, rgb(0xf6c83a), rgb(0xb98616), rgb(0xffea98), 2.5f, 1.2f, 1.2f);
        for (int i = 0; i < 3; i++) {
            Shape snap = new Ellipse2D.Float(142 + i * 3.4f, 146 + i * 13, 4, 4);
            fill(snap, rgb(0xe9e4da));
            ink(snap, .7f);
        }
        curve(.8f, alpha(INK, .7f), 34, 140, 40, 152, 42, 168, 40, 184);
        curve(.7f, alpha(INK, .6f), 164, 146, 170, 156, 172, 170, 174, 184);
        curve(.7f, alpha(rgb(0x9a6a0c), .9f), 60, 150, 70, 160, 74, 172, 76, 184);
        // High slicker collar behind the scarf.
        Path2D collar = new Path2D.Float();
        collar.moveTo(70, 126);
        collar.curveTo(72, 110, 86, 104, 100, 108);
        collar.lineTo(140, 112);
        collar.curveTo(148, 116, 150, 126, 146, 132);
        collar.lineTo(76, 132);
        collar.closePath();
        form(collar, rgb(0xf3c535), rgb(0xb07d14), rgb(0xffe88f), 6, 2, 1.5f);
        // Teal knit scarf wrapped at the neck, knot and hanging tail.
        Path2D scarf = new Path2D.Float();
        scarf.moveTo(78, 120);
        scarf.curveTo(84, 108, 112, 106, 140, 114);
        scarf.curveTo(146, 118, 146, 128, 140, 132);
        scarf.curveTo(116, 136, 90, 134, 78, 130);
        scarf.closePath();
        form(scarf, rgb(0x2f9289), rgb(0x1a5a55), rgb(0x67c4b6), 6, 2, 1.5f);
        for (float x = 84; x < 140; x += 5) curve(.5f, alpha(rgb(0x14403c), .75f), x, 112 + (x - 84) * .05f, x - 1, 118, x - 1, 126, x, 132);
        Path2D tail = poly(122, 128, 136, 127, 140, 168, 126, 170);
        form(tail, rgb(0x34998f), rgb(0x1a5a55), rgb(0x6ccabb), 4, 1.6f, 1.4f);
        for (float y = 136; y < 166; y += 6) line(124, y, 138, y - .6f, .5f, alpha(rgb(0x14403c), .75f));
        for (float x = 127; x < 140; x += 2.6f) line(x, 169.5f, x - .6f, 175, .9f, rgb(0x2f9289));
        Shape knot = new Ellipse2D.Float(120, 120, 18, 14);
        form(knot, rgb(0x34998f), rgb(0x1a5a55), rgb(0x76d2c2), 3, 1.6f, 1.4f);

        // White curls behind and under the hat.
        float[][] curls = {{64, 64, 9}, {60, 76, 9}, {66, 88, 8}, {76, 94, 7}, {72, 56, 8}, {86, 60, 6}};
        for (float[] c : curls) {
            Shape curl = new Ellipse2D.Float(c[0] - c[2], c[1] - c[2], c[2] * 2, c[2] * 2);
            form(curl, rgb(0xeeebe4), rgb(0xa9a39a), rgb(0xffffff), 3, 1.2f, 1.3f);
            curve(.6f, alpha(INK, .6f), c[0] - c[2] * .4f, c[1] + c[2] * .2f, c[0] - c[2] * .2f, c[1] - c[2] * .5f, c[0] + c[2] * .4f, c[1] - c[2] * .4f, c[0] + c[2] * .3f, c[1] + c[2] * .3f);
        }
        head();
        ear();
        // Gold hoop earring.
        Shape hoop = new Ellipse2D.Float(f.earX - 4, f.earY + 7, 7, 8);
        g.setColor(INK);
        g.setStroke(new BasicStroke(2.4f));
        g.draw(hoop);
        g.setColor(rgb(0xf0b93a));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(hoop);
        // Temple curls in front of the ear.
        for (float[] c : new float[][]{{84, 64, 4.5f}, {77, 66, 4}}) {
            Shape curl = new Ellipse2D.Float(c[0] - c[2], c[1] - c[2], c[2] * 2, c[2] * 2);
            form(curl, rgb(0xeeebe4), rgb(0xa9a39a), rgb(0xffffff), 2, 1, 1.1f);
        }
        // Red knit beanie with a folded rib cuff and a pompom.
        Path2D dome = new Path2D.Float();
        dome.moveTo(138, 48);
        dome.curveTo(140, 26, 118, 12, 96, 13);
        dome.curveTo(74, 14, 58, 30, 56, 58);
        dome.lineTo(138, 48);
        dome.closePath();
        form(dome, rgb(0xc63c30), rgb(0x7e1f1a), rgb(0xee7258), 7, 2.4f, 1.6f);
        Shape oldClip = g.getClip();
        g.clip(dome);
        for (float x = 66; x < 136; x += 6) curve(.5f, alpha(rgb(0x5a1410), .7f), x, 50 - (x - 66) * .08f, x + 2, 36, x + 6, 24, x + 12, 18);
        g.setClip(oldClip);
        Path2D cuff = poly(54, 52, 140, 42, 142, 54, 57, 66);
        form(cuff, rgb(0xd24638), rgb(0x84221c), rgb(0xf58466), 4, 1.6f, 1.6f);
        for (float t = .04f; t < 1; t += .055f) {
            float x0 = 54 + t * 86, y0 = 52 - t * 10, x1 = 57 + t * 85, y1 = 66 - t * 12;
            line(x0, y0 + 1, x1, y1 - 1, .5f, alpha(rgb(0x5a1410), .8f));
        }
        Shape pom = new Ellipse2D.Float(82, 1, 22, 18);
        form(pom, rgb(0xd9483a), rgb(0x84221c), rgb(0xff8f74), 4, 2, 1.5f);
        for (int i = 0; i < 7; i++) {
            double a = i * Math.PI * 2 / 7;
            line(93 + (float) Math.cos(a) * 4, 10 + (float) Math.sin(a) * 3.5f, 93 + (float) Math.cos(a) * 8, 10 + (float) Math.sin(a) * 6.5f, .5f, alpha(rgb(0x5a1410), .8f));
        }
    }
}
