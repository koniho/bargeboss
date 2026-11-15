package com.bargeboss.game;

import android.app.Activity;
import android.content.pm.ApplicationInfo;

/** Optional debug adapter. Release builds contain no scenario runner or profiling entry point. */
final class DeveloperHooks {
    interface Session {
        Game game();
        void configure(float width,float height);
        void advance(double seconds);
        void beforeDraw();
        void afterDraw();
        void stop();
        boolean running();
    }
    static Session create(Activity activity,CanvasPainter painter) {
        if((activity.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)==0
                || !activity.getIntent().hasExtra("bb_scene"))return null;
        try {
            return (Session)Class.forName("com.bargeboss.game.AndroidProfileSession")
                    .getConstructor(Activity.class,CanvasPainter.class).newInstance(activity,painter);
        } catch(ReflectiveOperationException e) {
            throw new IllegalArgumentException("Invalid or unavailable developer profiling session",e);
        }
    }
}
