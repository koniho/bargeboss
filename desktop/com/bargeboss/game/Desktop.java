package com.bargeboss.game;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Desktop events and display timing only; all behavior stays in core/. */
public final class Desktop extends JPanel {
    private final Game game=new Game();
    private final FrameGate frames=new FrameGate();
    private long last=System.nanoTime();
    private Viewport viewport=new Viewport(Layout.WIDTH,Layout.HEIGHT);
    private int canvasWidth=-1,canvasHeight=-1;
    private DesktopProgress progress;
    private int savedVersion,attemptedVersion;
    private Desktop() {
        try {
            progress=new DesktopProgress(java.nio.file.Paths.get(System.getProperty("user.home"),".bargeboss","progress.dat"));
            game.restoreProgress(progress.read());
        } catch(RuntimeException e) { System.err.println("Could not read captain progress: "+e); }
        savedVersion=attemptedVersion=game.progressVersion();
        setPreferredSize(new Dimension(Layout.WIDTH,Layout.HEIGHT)); setBackground(new Color(0x101e25));
        MouseAdapter mouse=new MouseAdapter() {
            public void mousePressed(MouseEvent e) { if(SwingUtilities.isLeftMouseButton(e)) {game.pointerDown(0,viewport.worldX(e.getX()),viewport.worldY(e.getY()));requestRedraw();} }
            public void mouseDragged(MouseEvent e) { game.pointerMove(0,viewport.worldX(e.getX()),viewport.worldY(e.getY()));requestRedraw(); }
            public void mouseReleased(MouseEvent e) { if(SwingUtilities.isLeftMouseButton(e)) {game.pointerUp(0,viewport.worldX(e.getX()),viewport.worldY(e.getY()));requestRedraw();} }
        };
        addMouseListener(mouse); addMouseMotionListener(mouse);
        addComponentListener(new ComponentAdapter() { public void componentResized(ComponentEvent e) { updateViewport();requestRedraw(); } });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"),"pause");
        getActionMap().put("pause",new AbstractAction() { public void actionPerformed(ActionEvent e) { game.back();requestRedraw(); } });
        new Timer(8,e->{ long now=System.nanoTime(); game.advance((now-last)/1e9); game.pollHaptic(); saveProgress(false); last=now; if(frames.needsFrame(game)) repaint(); }).start();
    }
    private void requestRedraw() { frames.markDirty();repaint(); }
    private void saveProgress(boolean flush) {
        int version=game.progressVersion();
        if(progress==null || version==savedVersion || (!flush && version==attemptedVersion)) return;
        attemptedVersion=version;
        if(progress.write(game.saveProgress())) savedVersion=version;
    }
    private void updateViewport() {
        int width=getWidth(),height=getHeight();
        if(width==canvasWidth && height==canvasHeight) return;
        canvasWidth=width; canvasHeight=height; viewport=new Viewport(width,height);
        game.configureCanvas(viewport.logicalWidth(),viewport.logicalHeight()); game.cancelInputs();
        frames.markDirty();
    }
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics); Graphics2D g=(Graphics2D)graphics.create();
        updateViewport();
        g.translate(viewport.x,viewport.y); g.scale(viewport.scale,viewport.scale);
        g.clip(new java.awt.geom.Rectangle2D.Float(0,0,viewport.logicalWidth(),viewport.logicalHeight())); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.draw(game,new DesktopPainter(g)); g.dispose();
        frames.rendered(game);
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(()->{
            Desktop panel=new Desktop(); JFrame frame=new JFrame("Barge Boss / Calumet Reach");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); frame.setContentPane(panel); frame.pack();
            Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();
            double scale=Math.min(1,Math.min((screen.width-60.0)/frame.getWidth(),(screen.height-60.0)/frame.getHeight()));
            if(scale<1) frame.setSize((int)(frame.getWidth()*scale),(int)(frame.getHeight()*scale));
            frame.setLocationRelativeTo(null);
            frame.addWindowFocusListener(new WindowAdapter() {
                public void windowLostFocus(WindowEvent e) { panel.game.background(); panel.saveProgress(true); panel.last=System.nanoTime();panel.requestRedraw(); }
                public void windowGainedFocus(WindowEvent e) {panel.last=System.nanoTime();panel.requestRedraw();}
            });
            frame.addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { panel.saveProgress(true); } });
            frame.setVisible(true);
        });
    }
}
