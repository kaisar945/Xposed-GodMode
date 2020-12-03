package com.viewblocker.jrsen.injection.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.viewblocker.jrsen.BuildConfig;

import java.lang.reflect.InvocationTargetException;

public class GmLayoutInflater implements LayoutInflater.Factory2 {

    public static LayoutInflater from(Context context) throws PackageManager.NameNotFoundException {
        Context gmContext = context.createPackageContext(BuildConfig.APPLICATION_ID, 0);
        LayoutInflater layoutInflater = LayoutInflater.from(gmContext);
        GmLayoutInflater factory = new GmLayoutInflater();
        layoutInflater.setFactory2(factory);
        return layoutInflater;
    }

    @Nullable
    @Override
    public View onCreateView(@Nullable View parent, @NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        try {
            return (View) Class.forName(name).getConstructor(Context.class, AttributeSet.class).newInstance(context, attrs);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        try {
            return (View) Class.forName(name).getConstructor(Context.class, AttributeSet.class).newInstance(context, attrs);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
