package com.bargeboss.game;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Canvas;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;

public final class MainActivity extends Activity {
    private GameView gameView;
    public void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout root=new FrameLayout(this); root.setBackgroundColor(0xff101e25);
        root.setOnApplyWindowInsetsListener((view,insets)->{
            int left=insets.getSystemWindowInsetLeft(), top=insets.getSystemWindowInsetTop();
            int right=insets.getSystemWindowInsetRight(), bottom=insets.getSystemWindowInsetBottom();
            if(android.os.Build.VERSION.SDK_INT>=28 && insets.getDisplayCutout()!=null) {
                left=Math.max(left,insets.getDisplayCutout().getSafeInsetLeft());
                top=Math.max(top,insets.getDisplayCutout().getSafeInsetTop());
                right=Math.max(right,insets.getDisplayCutout().getSafeInsetRight());
                bottom=Math.max(bottom,insets.getDisplayCutout().getSafeInsetBottom());
            }
            view.setPadding(left,top,right,bottom); return insets;
        });
        gameView=new GameView(); root.addView(gameView,new FrameLayout.LayoutParams(-1,-1)); setContentView(root);
        root.requestApplyInsets();
        if(android.os.Build.VERSION.SDK_INT>=33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->gameView.game.pause());
        }
    }
    protected void onResume() { super.onResume(); gameView.start(); }
    protected void onPause() { gameView.stop(); super.onPause(); }
    public void onWindowFocusChanged(boolean hasFocus) { super.onWindowFocusChanged(hasFocus); if(!hasFocus && gameView!=null) gameView.game.background(); }
    @Override public void onBackPressed() { gameView.game.pause(); }

    private final class GameView extends View implements Choreographer.FrameCallback {
        private final Game game=new Game();
        private final CanvasPainter painter=new CanvasPainter();
        private Viewport viewport=new Viewport(600,960);
        private int activePointer=-1;
        private long last;
        private boolean running;
        GameView() { super(MainActivity.this); setContentDescription("Barge Boss river game. Tap water to move. Hold cargo to push."); }
        void start() { if(running) return; running=true; last=0; Choreographer.getInstance().postFrameCallback(this); }
        void stop() { running=false; last=0; activePointer=-1; game.background(); Choreographer.getInstance().removeFrameCallback(this); }
        public void doFrame(long now) {
            if(!running) return; if(last!=0) game.advance((now-last)/1e9); last=now;
            invalidate(); Choreographer.getInstance().postFrameCallback(this);
        }
        protected void onSizeChanged(int w,int h,int oldW,int oldH) { viewport=new Viewport(w,h); game.cancel(); activePointer=-1; }
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(0xff101e25); canvas.save(); canvas.translate(viewport.x,viewport.y); canvas.scale(viewport.scale,viewport.scale);
            canvas.clipRect(0,0,600,960); painter.bind(canvas); game.render(painter); canvas.restore();
        }
        public boolean onTouchEvent(MotionEvent event) {
            int action=event.getActionMasked(), index=event.getActionIndex();
            if(action==MotionEvent.ACTION_DOWN) {
                activePointer=event.getPointerId(index);
                game.down(viewport.worldX(event.getX(index)),viewport.worldY(event.getY(index)));
            } else if(action==MotionEvent.ACTION_MOVE && activePointer!=-1) {
                int i=event.findPointerIndex(activePointer);
                if(i>=0) game.move(viewport.worldX(event.getX(i)),viewport.worldY(event.getY(i)));
            } else if((action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_POINTER_UP) && event.getPointerId(index)==activePointer) {
                game.up(viewport.worldX(event.getX(index)),viewport.worldY(event.getY(index))); activePointer=-1; performClick();
            } else if(action==MotionEvent.ACTION_CANCEL) { activePointer=-1; game.cancel(); }
            return true;
        }
        public boolean performClick() { super.performClick(); return true; }
    }
}
