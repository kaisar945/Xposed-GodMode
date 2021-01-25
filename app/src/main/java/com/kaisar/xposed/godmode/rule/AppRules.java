package com.kaisar.xposed.godmode.rule;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Keep;

import java.util.HashMap;
import java.util.List;

/**
 * Created by jrsen on 17-10-14.
 */
@Keep
public final class AppRules extends HashMap<String, ActRules> implements Parcelable {

    public AppRules() {
    }


    protected AppRules(Parcel in) {
        HashMap<?, ?> hashMap = in.readHashMap(getClass().getClassLoader());
        for (Entry<?, ?> entry : hashMap.entrySet()) {
            String key = (String) entry.getKey();
            @SuppressWarnings("unchecked") HashMap<String, List<ViewRule>> value = (HashMap<String, List<ViewRule>>) entry.getValue();
            ActRules actRules = new ActRules(value.size());
            actRules.putAll(value);
            put(key, actRules);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeMap(this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AppRules> CREATOR = new Creator<AppRules>() {
        @Override
        public AppRules createFromParcel(Parcel in) {
            return new AppRules(in);
        }

        @Override
        public AppRules[] newArray(int size) {
            return new AppRules[size];
        }
    };
}
