package com.viewblocker.jrsen.rule;

import android.app.Activity;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Keep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by jrsen on 17-10-14.
 */
@Keep
public final class ActRules extends HashMap<String, List<ViewRule>> implements Parcelable {

    public ActRules() {
    }

    @SuppressWarnings("unchecked")
    protected ActRules(Parcel in) {
        putAll(in.readHashMap(ViewRule.class.getClassLoader()));
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

    public List<ViewRule> getRuleList(Activity activity) {
        String key = activity.getComponentName().getClassName();
        return containsKey(key) ? get(key) : new ArrayList<ViewRule>();
    }

}
