/*
 * Copyright 2021 John "topjohnwu" Wu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.topjohnwu.superuser.internal;

import static io.github.vvb2060.puellamagi.App.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.RestrictTo;

import com.topjohnwu.superuser.Shell;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class Utils {

    @SuppressLint("StaticFieldLeak")
    public static Context context;
    public static final Charset UTF_8 = StandardCharsets.UTF_8;
    private static Class<?> synchronizedCollectionClass;

    public static void log(String tag, Object log) {
        if (vLog())
            Log.d(TAG, tag + ": " + log.toString());
    }

    public static void ex(Throwable t) {
        if (vLog())
            Log.w(TAG, "LIBSU: ", t);
    }

    public static void err(Throwable t) {
        Log.e(TAG, "LIBSU: ", t);
    }

    public static boolean vLog() {
        return Shell.enableVerboseLogging;
    }

    @SuppressLint("PrivateApi")
    public static synchronized Context getContext() {
        if (context == null) {
            UiThreadHandler.runAndWait(() -> {
                try {
                    Method currentApplication = Class.forName("android.app.ActivityThread")
                            .getMethod("currentApplication");
                    context = (Context) currentApplication.invoke(null);
                } catch (Exception e) {
                    // Shall never happen
                    Utils.err(e);
                }
            });
        }
        return context;
    }

    public static boolean isSynchronized(Collection<?> collection) {
        if (synchronizedCollectionClass == null) {
            synchronizedCollectionClass =
                    Collections.synchronizedCollection(NOPList.getInstance()).getClass();
        }
        return synchronizedCollectionClass.isInstance(collection);
    }

    public static long pump(InputStream in, OutputStream out) throws IOException {
        int read;
        long total = 0;
        byte[] buf = new byte[64 * 1024];  /* 64K buffer */
        while ((read = in.read(buf)) > 0) {
            out.write(buf, 0, read);
            total += read;
        }
        return total;
    }
}
