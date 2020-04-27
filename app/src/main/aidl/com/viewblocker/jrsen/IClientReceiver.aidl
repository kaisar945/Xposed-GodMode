// IClientReceiver.aidl
package com.viewblocker.jrsen;

import com.viewblocker.jrsen.rule.ActRules;

interface IClientReceiver {
    void editModeStateChanged(boolean enable);
    void viewRulesHasChanged(in ActRules actRules);
}
