package com.viewblocker.jrsen.injection.bridge;

import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import com.kaisar.xservicemanager.XServiceManager;
import com.viewblocker.jrsen.IGodModeManager;
import com.viewblocker.jrsen.IObserver;
import com.viewblocker.jrsen.rule.ActRules;
import com.viewblocker.jrsen.rule.ViewRule;

import java.util.Collections;
import java.util.Map;

public final class GodModeManager {

    private static GodModeManager instance;
    private final IGodModeManager mGMM;

    private GodModeManager(IGodModeManager gmm) {
        this.mGMM = gmm;
    }

    public static GodModeManager getDefault() {
        synchronized (GodModeManager.class) {
            if (instance == null) {
                IBinder service = XServiceManager.getService("godmode");
                if (service != null) {
                    instance = new GodModeManager(IGodModeManager.Stub.asInterface(service));
                } else {
                    instance = new GodModeManager(new IGodModeManager.Default());
                }
            }
            return instance;
        }
    }

    public void setEditMode(boolean enable) {
        try {
            mGMM.setEditMode(enable);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
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

    @SuppressWarnings("unchecked")
    public Map<String, ActRules> getAllRules() {
        try {
            return mGMM.getAllRules();
        } catch (RemoteException e) {
            e.printStackTrace();
            return Collections.emptyMap();
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

    public boolean writeRule(String packageName, ViewRule viewRule, Bitmap bitmap) {
        try {
            return mGMM.writeRule(packageName, viewRule, bitmap);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateRule(String packageName, ViewRule viewRule) {
        try {
            return mGMM.updateRule(packageName, viewRule);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteRule(String packageName, ViewRule viewRule) {
        try {
            return mGMM.deleteRule(packageName, viewRule);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteRules(String packageName) {
        try {
            return mGMM.deleteRules(packageName);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public ParcelFileDescriptor openFile(String filePath , int mode) {
        try {
            return mGMM.openFile(filePath, mode);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }
}
