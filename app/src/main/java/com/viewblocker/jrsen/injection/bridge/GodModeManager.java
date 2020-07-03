package com.viewblocker.jrsen.injection.bridge;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.RemoteException;

import com.viewblocker.jrsen.IGodModeManager;
import com.viewblocker.jrsen.IObserver;
import com.viewblocker.jrsen.rule.ActRules;
import com.viewblocker.jrsen.rule.ViewRule;

import java.lang.reflect.Method;

public final class GodModeManager {

    private static GodModeManager godModeManager;
    private final IGodModeManager mGMM;

    private GodModeManager(IGodModeManager gmm) {
        mGMM = gmm;
    }

    public static GodModeManager getDefault() {
        synchronized (GodModeManager.class) {
            if (godModeManager == null) {
                try {
                    @SuppressLint("PrivateApi") Class<?> ServiceManagerClass = Class.forName("android.os.ServiceManager");
                    Method checkServiceMethod = ServiceManagerClass.getMethod("checkService", String.class);
                    IBinder binder = (IBinder) checkServiceMethod.invoke(null, "godmode");
                    IGodModeManager gmm = IGodModeManager.Stub.asInterface(binder);
                    godModeManager = new GodModeManager(gmm);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return godModeManager;
        }
    }

    public void setEditMode(boolean enable) {
//        godModeManager.
    }

    public boolean isInEditMode() {
        try {
            return mGMM.isInEditMode();
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void addObserver(String packageName, IObserver observer) {
        try {
            mGMM.addObserver(packageName, observer);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public ActRules getRules(String packageName) {
        try {
            return mGMM.getRules(packageName);
        } catch (RemoteException e) {
            e.printStackTrace();
            return new ActRules();
        }
    }

    public void writeRules(String packageName, ViewRule viewRule, Bitmap bitmap) {
        try {
            mGMM.writeRule(packageName, viewRule, bitmap);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
