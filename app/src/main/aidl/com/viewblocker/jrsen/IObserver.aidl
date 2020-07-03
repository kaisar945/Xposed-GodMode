package com.viewblocker.jrsen;

import com.viewblocker.jrsen.rule.ActRules;

interface IObserver {
    void onEditModeChanged(boolean enable);
    void onViewRuleChanged(in ActRules actRules);
}
