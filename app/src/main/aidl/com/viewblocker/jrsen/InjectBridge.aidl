// InjectBridge.aidl
package com.viewblocker.jrsen;

import com.viewblocker.jrsen.IClientReceiver;
import com.viewblocker.jrsen.rule.ActRules;
import com.viewblocker.jrsen.rule.ViewRule;
import com.viewblocker.jrsen.analytics.CrashRecord;
import android.graphics.Bitmap;

interface InjectBridge {

    boolean isInEditMode();

    void registerReceiver(String packageName, in IClientReceiver receiver);

    ActRules getRules(String packageName);

    void writeRule(String packageName, in ViewRule viewRule, in Bitmap bitmap);

}
