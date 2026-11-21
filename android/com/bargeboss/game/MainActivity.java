package com.bargeboss.game;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Canvas;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
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
        gameView=new GameView();
        if(!"canvas".equals(getIntent().getStringExtra("bb_renderer"))) {
            SurfaceView surface=new SurfaceView(this);root.addView(surface,new FrameLayout.LayoutParams(-1,-1));gameView.gpuSurface=surface;
            surface.getHolder().addCallback(new SurfaceHolder.Callback(){
                public void surfaceCreated(SurfaceHolder holder){gameView.openGpu(holder);}
                public void surfaceChanged(SurfaceHolder holder,int format,int w,int h){gameView.requestRedraw();}
                public void surfaceDestroyed(SurfaceHolder holder){gameView.closeGpu();}
            });
        }
        root.addView(gameView,new FrameLayout.LayoutParams(-1,-1)); setContentView(root);
        fullscreen(); root.requestApplyInsets();
        if(android.os.Build.VERSION.SDK_INT>=33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->gameView.togglePause());
        }
    }
    protected void onResume() { super.onResume(); gameView.start(); }
    protected void onDestroy(){if(gameView!=null){gameView.audio.close();gameView.closeGpu();gameView.painter.close();}super.onDestroy();}
    protected void onPause() { gameView.stop(); super.onPause(); }
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) fullscreen(); else if(gameView!=null) {if(gameView.developer==null)gameView.game.background();gameView.audio.stop();}
        if(gameView!=null) gameView.requestRedraw();
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
    @Override public void onBackPressed() { gameView.togglePause(); }

    private final class GameView extends View implements Choreographer.FrameCallback {
        private SurfaceView gpuSurface;
        private VulkanPainter gpu;
        private boolean gpuFailed;
        void openGpu(SurfaceHolder holder){
            if(gpuFailed)return;
            try {gpu=new VulkanPainter(holder.getSurface(),getAssets(),painter,developer==null?0:(getIntent().getBooleanExtra("bb_vk_reuse_commands",false)?1:0)|(getIntent().getBooleanExtra("bb_vk_minimal",false)?2:0));android.util.Log.i("BargeBoss","Vulkan renderer active");}
            catch(RuntimeException|UnsatisfiedLinkError e){fallbackGpu(e);}
            requestRedraw();
        }
        void closeGpu(){if(gpu!=null){gpu.close();gpu=null;}}
        void fallbackGpu(Throwable error){android.util.Log.e("BargeBoss","Vulkan unavailable; using Canvas",error);gpuFailed=true;closeGpu();if(gpuSurface!=null)gpuSurface.setVisibility(View.GONE);requestRedraw();}
        private final FrameGate frames=new FrameGate();
        private final FramePacer gpuPacer=new FramePacer();
        private final EngineSound audio=new EngineSound(MainActivity.this);
        private final CanvasPainter painter=new CanvasPainter(MainActivity.this.getAssets());
        private final DeveloperHooks.Session developer=DeveloperHooks.create(MainActivity.this,painter);
        private final Game game=developer==null?new Game():developer.game();
        private Viewport viewport=new Viewport(Layout.WIDTH,Layout.HEIGHT);
        private long last;
        private boolean running;
        private android.content.SharedPreferences progress;
        private int savedVersion,attemptedVersion;
        private LevelAssets.Plan preparedPlan;
        private LightingProfile preparedLighting;
        GameView() {
            super(MainActivity.this);
            game.settings.developerWaves=(getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0;
            setContentDescription("Barge Boss river game. Drag the tug to drive, hold cargo to push, and lasso a bollard to tie up.");
            if(developer==null)try {
                progress=getSharedPreferences("captain-progress",MODE_PRIVATE);
                game.restoreProgress(progress.getString("snapshot",null));
            } catch(RuntimeException e) { android.util.Log.w("BargeBoss","Could not read captain progress",e); }
            savedVersion=attemptedVersion=game.progressVersion();
            if(developer==null)prepareLevelAssets();
        }
        private void prepareLevelAssets() {
            LevelAssets.Plan plan=LevelAssets.upcoming(game);
            LightingProfile lighting=game.activeGame().lighting();
            if(plan!=preparedPlan || lighting!=preparedLighting){
                boolean changed=lighting!=painter.lighting();preparedPlan=plan;preparedLighting=lighting;
                painter.lighting(lighting);painter.prepareRotations(plan.preparationAtlases());
                if(changed)try{painter.profileAwaitRotations(0);last=0;}catch(InterruptedException e){Thread.currentThread().interrupt();}
            }
            if(game.activeGame().mode==Game.PLAY && !painter.profileRotationsReady())try{painter.profileAwaitRotations(0);last=0;}catch(InterruptedException e){Thread.currentThread().interrupt();}
        }
        void saveProgress(boolean flush) {
            if(developer!=null)return;
            int version=game.progressVersion();
            if(progress==null || version==savedVersion || (!flush && version==attemptedVersion)) return;
            attemptedVersion=version;
            try {
                if(progress.edit().putString("snapshot",game.saveProgress()).commit()) savedVersion=version;
                else android.util.Log.w("BargeBoss","Could not save captain progress");
            } catch(RuntimeException e) { android.util.Log.w("BargeBoss","Could not save captain progress",e); }
        }
        void requestRedraw() { frames.markDirty(); invalidate(); }
        void togglePause() { if(developer!=null){MainActivity.this.finish();return;} game.back(); requestRedraw(); }
        void start() { requestRedraw(); if(running) return; running=true; last=0; gpuPacer.reset(); Choreographer.getInstance().postFrameCallback(this); }
        void stop() { audio.stop(); running=false; last=0; game.background(); if(developer!=null)developer.stop(); saveProgress(true); Choreographer.getInstance().removeFrameCallback(this); requestRedraw(); }
        public void doFrame(long now) {
            if(!running) return;
            prepareLevelAssets();
            if(last==0)now=System.nanoTime();
            if(last!=0) {if(developer==null)game.advance((now-last)/1e9);else developer.advance((now-last)/1e9);} last=now;
            audio.update(game);
            int event=game.pollHaptic();
            int enginePulse=game.pollEngineHaptic();
            if(event!=0) {
                int feedback=event==Game.HAPTIC_IMPACT?HapticFeedbackConstants.LONG_PRESS:
                    event==Game.HAPTIC_CONTACT?HapticFeedbackConstants.VIRTUAL_KEY:HapticFeedbackConstants.CLOCK_TICK;
                if(event==Game.HAPTIC_SUCCESS && android.os.Build.VERSION.SDK_INT>=30) feedback=HapticFeedbackConstants.CONFIRM;
                performHapticFeedback(feedback);
            }else if(enginePulse!=0){
                performHapticFeedback(enginePulse==2?HapticFeedbackConstants.VIRTUAL_KEY:HapticFeedbackConstants.CLOCK_TICK);
            }
            saveProgress(false); if(developer!=null && developer.running() || frames.needsFrame(game)) invalidate(); Choreographer.getInstance().postFrameCallback(this);
        }
        protected void onSizeChanged(int w,int h,int oldW,int oldH) {
            viewport=new Viewport(w,h);
            game.configureCanvas(viewport.logicalWidth(),viewport.logicalHeight()); game.cancelInputs();
            if(developer!=null)developer.configure(viewport.logicalWidth(),viewport.logicalHeight());
            requestRedraw();
        }
        private boolean retryingPreparedFrame;
        protected void onDraw(Canvas canvas) {
            if(gpu!=null && !gpuPacer.allow(System.nanoTime()))return;
            prepareLevelAssets();
            if(developer!=null && !retryingPreparedFrame)developer.beforeDraw();
            if(gpu!=null){
                try {gpu.start(viewport.logicalWidth(),viewport.logicalHeight());game.render(gpu);gpu.present(viewport);retryingPreparedFrame=false;painter.profileGpuFrame(true);frames.rendered(game);if(developer!=null)developer.afterDraw();return;}
                catch(VulkanPainter.PreparingLayer waiting){retryingPreparedFrame=true;requestRedraw();return;}
                catch(RuntimeException error){fallbackGpu(error);}
            }
            retryingPreparedFrame=false;
            canvas.drawColor(0xff101e25); canvas.save(); canvas.translate(viewport.x,viewport.y); canvas.scale(viewport.scale,viewport.scale);
            canvas.clipRect(0,0,viewport.logicalWidth(),viewport.logicalHeight()); painter.bind(canvas); game.render(painter); canvas.restore();
            frames.rendered(game);
            if(developer!=null)developer.afterDraw();
        }
        public boolean onTouchEvent(MotionEvent event) {
            if(developer!=null)return true; // Reproducible captures ignore user gestures; relaunch normally to play.
            int action=event.getActionMasked(), index=event.getActionIndex();
            if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN) {
                game.pointerDown(event.getPointerId(index),viewport.worldX(event.getX(index)),viewport.worldY(event.getY(index)));
            } else if(action==MotionEvent.ACTION_MOVE) {
                for(int i=0;i<event.getPointerCount();i++)
                    game.pointerMove(event.getPointerId(i),viewport.worldX(event.getX(i)),viewport.worldY(event.getY(i)));
            } else if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_POINTER_UP) {
                game.pointerUp(event.getPointerId(index),viewport.worldX(event.getX(index)),viewport.worldY(event.getY(index))); performClick();
            } else if(action==MotionEvent.ACTION_CANCEL) game.cancelInputs();
            requestRedraw();
            return true;
        }
        public boolean performClick() { super.performClick(); return true; }
    }
}
