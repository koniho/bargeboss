package com.bargeboss.game;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Canvas;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.HapticFeedbackConstants;
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
        fullscreen(); root.requestApplyInsets();
        if(android.os.Build.VERSION.SDK_INT>=33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->gameView.game.pause());
        }
    }
    protected void onResume() { super.onResume(); gameView.start(); }
    protected void onPause() { gameView.stop(); super.onPause(); }
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) fullscreen(); else if(gameView!=null) gameView.game.background();
    }
    private void fullscreen() {
        if(android.os.Build.VERSION.SDK_INT>=30) {
            getWindow().setDecorFitsSystemWindows(false);
            android.view.WindowInsetsController controller=getWindow().getInsetsController();
            if(controller!=null) {
                controller.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(WindowInsets.Type.systemBars());
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    |View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    |View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
    @Override public void onBackPressed() { gameView.game.pause(); }

    private final class GameView extends View implements Choreographer.FrameCallback {
        private final Game game=new Game();
        private final CanvasPainter painter=new CanvasPainter(MainActivity.this.getAssets());
        private Viewport viewport=new Viewport(Layout.WIDTH,Layout.HEIGHT);
        private long last;
        private boolean running;
        GameView() { super(MainActivity.this); setContentDescription("Barge Boss river game. Drag the tug to drive, hold cargo to push, and lasso a bollard to tie up."); }
        void start() { if(running) return; running=true; last=0; Choreographer.getInstance().postFrameCallback(this); }
        void stop() { running=false; last=0; game.background(); Choreographer.getInstance().removeFrameCallback(this); }
        public void doFrame(long now) {
            if(!running) return; if(last!=0) game.advance((now-last)/1e9); last=now;
            int event=game.pollHaptic();
            if(event!=0) {
                int feedback=event==Game.HAPTIC_IMPACT?HapticFeedbackConstants.LONG_PRESS:
                    event==Game.HAPTIC_CONTACT?HapticFeedbackConstants.VIRTUAL_KEY:HapticFeedbackConstants.CLOCK_TICK;
                if(event==Game.HAPTIC_SUCCESS && android.os.Build.VERSION.SDK_INT>=30) feedback=HapticFeedbackConstants.CONFIRM;
                performHapticFeedback(feedback);
            }
            invalidate(); Choreographer.getInstance().postFrameCallback(this);
        }
        protected void onSizeChanged(int w,int h,int oldW,int oldH) {
            viewport=new Viewport(w,h);
            game.configureCanvas(viewport.logicalWidth(),viewport.logicalHeight()); game.cancelInputs();
        }
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(0xff101e25); canvas.save(); canvas.translate(viewport.x,viewport.y); canvas.scale(viewport.scale,viewport.scale);
            canvas.clipRect(0,0,viewport.logicalWidth(),viewport.logicalHeight()); painter.bind(canvas); game.render(painter); canvas.restore();
        }
        public boolean onTouchEvent(MotionEvent event) {
            int action=event.getActionMasked(), index=event.getActionIndex();
            if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN) {
                game.pointerDown(event.getPointerId(index),viewport.worldX(event.getX(index)),viewport.worldY(event.getY(index)));
            } else if(action==MotionEvent.ACTION_MOVE) {
                for(int i=0;i<event.getPointerCount();i++)
                    game.pointerMove(event.getPointerId(i),viewport.worldX(event.getX(i)),viewport.worldY(event.getY(i)));
            } else if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_POINTER_UP) {
                game.pointerUp(event.getPointerId(index),viewport.worldX(event.getX(index)),viewport.worldY(event.getY(index))); performClick();
            } else if(action==MotionEvent.ACTION_CANCEL) game.cancelInputs();
            return true;
        }
        public boolean performClick() { super.performClick(); return true; }
    }
}
