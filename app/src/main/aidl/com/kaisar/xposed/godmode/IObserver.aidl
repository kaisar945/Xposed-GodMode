package com.kaisar.xposed.godmode;

import com.kaisar.xposed.godmode.rule.ActRules;

interface IObserver {
    void onEditModeChanged(boolean enable);
    void onViewRuleChanged(String packageName, in ActRules actRules);
}
