package com.viewblocker.jrsen;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.viewblocker.jrsen.service.InjectBridgeService;

public final class FixHostDirPermActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InjectBridgeService.getBridge(this).fixHostDirPermission();
        finish();
    }
}
