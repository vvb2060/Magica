package android.os;

public class SystemProperties {
    public static native String get(String key);

    public static native String get(String key, String def);

    public static native int getInt(String key, int def);

    public static native long getLong(String key, long def);

    public static native boolean getBoolean(String key, boolean def);

    public static native void set(String key, String val);
}
