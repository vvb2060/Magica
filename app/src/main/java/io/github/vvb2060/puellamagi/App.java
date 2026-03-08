package io.github.vvb2060.puellamagi;

import android.app.Application;
import android.content.Context;

public class App extends Application {
    public static final String TAG = "Magica";
    public static Context application;
    public static Context deviceProtectedContext;
    public static IRemoteService server;

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        deviceProtectedContext = createDeviceProtectedStorageContext();
    }
}
