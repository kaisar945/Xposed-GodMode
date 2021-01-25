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
public final class ActRules extends HashMap<String, List<ViewRule>> implements Parcelable {

    public ActRules() {
    }

    public ActRules(int initialCapacity) {
        super(initialCapacity);
    }

    protected ActRules(Parcel in) {
        in.readMap(this, getClass().getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeMap(this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ActRules> CREATOR = new Creator<ActRules>() {
        @Override
        public ActRules createFromParcel(Parcel in) {
            return new ActRules(in);
        }

        @Override
        public ActRules[] newArray(int size) {
            return new ActRules[size];
        }
    };

}
