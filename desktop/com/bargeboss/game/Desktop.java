package com.bargeboss.game;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Desktop events and display timing only; all behavior stays in core/. */
public final class Desktop extends JPanel {
    private final Game game=new Game();
    private long last=System.nanoTime();
    private Viewport viewport=new Viewport(Layout.WIDTH,Layout.HEIGHT);
    private Desktop() {
        setPreferredSize(new Dimension(Layout.WIDTH,Layout.HEIGHT)); setBackground(new Color(0x101e25));
        MouseAdapter mouse=new MouseAdapter() {
            public void mousePressed(MouseEvent e) { if(SwingUtilities.isLeftMouseButton(e)) game.pointerDown(0,viewport.worldX(e.getX()),viewport.worldY(e.getY())); }
            public void mouseDragged(MouseEvent e) { game.pointerMove(0,viewport.worldX(e.getX()),viewport.worldY(e.getY())); }
            public void mouseReleased(MouseEvent e) { if(SwingUtilities.isLeftMouseButton(e)) game.pointerUp(0,viewport.worldX(e.getX()),viewport.worldY(e.getY())); }
        };
        addMouseListener(mouse); addMouseMotionListener(mouse);
        addComponentListener(new ComponentAdapter() { public void componentResized(ComponentEvent e) { game.cancelInputs(); } });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"),"pause");
        getActionMap().put("pause",new AbstractAction() { public void actionPerformed(ActionEvent e) { game.pause(); } });
        new Timer(8,e->{ long now=System.nanoTime(); game.advance((now-last)/1e9); game.pollHaptic(); last=now; repaint(); }).start();
    }
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics); Graphics2D g=(Graphics2D)graphics.create();
        viewport=new Viewport(getWidth(),getHeight());
        g.translate(viewport.x,viewport.y); g.scale(viewport.scale,viewport.scale);
        g.clipRect(0,0,Layout.WIDTH,Layout.HEIGHT); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.draw(game,new DesktopPainter(g)); g.dispose();
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(()->{
            Desktop panel=new Desktop(); JFrame frame=new JFrame("Barge Boss / Calumet Reach");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); frame.setContentPane(panel); frame.pack();
            Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();
            double scale=Math.min(1,Math.min((screen.width-60.0)/frame.getWidth(),(screen.height-60.0)/frame.getHeight()));
            if(scale<1) frame.setSize((int)(frame.getWidth()*scale),(int)(frame.getHeight()*scale));
            frame.setLocationRelativeTo(null);
            frame.addWindowFocusListener(new WindowAdapter() { public void windowLostFocus(WindowEvent e) { panel.game.background(); panel.last=System.nanoTime(); } });
            frame.setVisible(true);
        });
    }
}
