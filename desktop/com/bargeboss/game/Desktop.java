package com.bargeboss.game;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Desktop events and display timing only; all behavior stays in core/. */
public final class Desktop extends JPanel {
    private final Game game=new Game();
    private long last=System.nanoTime();
    private Viewport viewport=new Viewport(600,960);
    private Desktop() {
        setPreferredSize(new Dimension(600,960)); setBackground(new Color(0x101e25));
        MouseAdapter mouse=new MouseAdapter() {
            public void mousePressed(MouseEvent e) { if(SwingUtilities.isLeftMouseButton(e)) game.down(viewport.worldX(e.getX()),viewport.worldY(e.getY())); }
            public void mouseDragged(MouseEvent e) { game.move(viewport.worldX(e.getX()),viewport.worldY(e.getY())); }
            public void mouseReleased(MouseEvent e) { if(SwingUtilities.isLeftMouseButton(e)) game.up(viewport.worldX(e.getX()),viewport.worldY(e.getY())); }
        };
        addMouseListener(mouse); addMouseMotionListener(mouse);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"),"pause");
        getActionMap().put("pause",new AbstractAction() { public void actionPerformed(ActionEvent e) { game.pause(); } });
        new Timer(8,e->{ long now=System.nanoTime(); game.advance((now-last)/1e9); last=now; repaint(); }).start();
    }
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics); Graphics2D g=(Graphics2D)graphics.create();
        viewport=new Viewport(getWidth(),getHeight());
        g.translate(viewport.x,viewport.y); g.scale(viewport.scale,viewport.scale);
        g.clipRect(0,0,600,960); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.draw(game,new DesktopPainter(g)); g.dispose();
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(()->{
            Desktop panel=new Desktop(); JFrame frame=new JFrame("Barge Boss / Calumet Reach");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); frame.setContentPane(panel); frame.pack();
            Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();
            if(frame.getHeight()>screen.height-60) frame.setSize((int)((screen.height-80)*.625),screen.height-60);
            frame.setLocationRelativeTo(null);
            frame.addWindowFocusListener(new WindowAdapter() { public void windowLostFocus(WindowEvent e) { panel.game.background(); panel.last=System.nanoTime(); } });
            frame.setVisible(true);
        });
    }
}
