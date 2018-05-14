package com.lansosdk.videoeditor;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;


public class LanSoEditor {


    private static boolean isLoaded = false;

    public static void initSDK(Context context, String str) {
        loadLibraries(); // 拿出来单独加载库文件.
        LanSoEditor.initSo(context, str);
    }


    private static synchronized void loadLibraries() {
        if (isLoaded)
            return;

        Log.d("lansoeditor", "load libraries.....LanSongffmpeg.");

        System.loadLibrary("LanSongffmpeg");
        System.loadLibrary("LanSongdisplay");
        System.loadLibrary("LanSongplayer");

        isLoaded = true;
    }

    public static void initSo(Context context, String argv) {
        nativeInit(context, context.getAssets(), argv);
    }

    public static void unInitSo() {
        nativeUninit();
    }

    public static native void nativeInit(Context ctx, AssetManager ass, String filename);

    public static native void nativeUninit();

}
