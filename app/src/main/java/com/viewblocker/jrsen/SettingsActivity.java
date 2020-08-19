package com.viewblocker.jrsen;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.viewblocker.jrsen.fragment.GeneralPreferenceFragment;
import com.viewblocker.jrsen.util.Clipboard;
import com.viewblocker.jrsen.util.PermissionHelper;

public class SettingsActivity extends AppCompatActivity {

    private static final String BACK_STACK_PREFS = ":android:prefs";
    private PermissionHelper permissionHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String crashLog = CrashHandler.getCrashLog();
        if (!TextUtils.isEmpty(crashLog)) {
            showErrorDialog(crashLog);
            return;
        }

        // Display the fragment as the main content.
        startPreferenceFragment(new GeneralPreferenceFragment(), false);

        permissionHelper = new PermissionHelper(this);
        if (!permissionHelper.isAllRequestedPermissionGranted()) {
            permissionHelper.applyPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())
                && intent.getType() != null
                && (intent.getType().startsWith("*/*")
                || intent.getType().startsWith("image/")
                || intent.getType().startsWith("application/octet-stream"))) {
            for (int i = getFragmentManager().getBackStackEntryCount(); i > 0; i--) {
                getFragmentManager().popBackStackImmediate();
            }
            GeneralPreferenceFragment fragment =
                    (GeneralPreferenceFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
            fragment.dispatchOnNewIntent(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        permissionHelper.onActivityResult(requestCode, resultCode, data);
    }

    private void showErrorDialog(final String stackTrace) {
        SpannableString text = new SpannableString("    我想 你遇到些麻烦 说实话我真不想看到这样情况但是它的确出现了 噢 我亲爱的上帝也不能保佑它可以完美的运行 是的 我保证 不过你应该告诉我也许可以试试修复\n\n");
        SpannableString st = new SpannableString(stackTrace);
        st.setSpan(new RelativeSizeSpan(0.3f), 0, st.length(), 0);
        CharSequence message = TextUtils.concat(text, st);
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle("嘿 伙计")
                .setMessage(message)
                .setPositiveButton("复制给我的老伙计", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Clipboard.putContent(getApplicationContext(), stackTrace);
                        CrashHandler.handledCrashLog();
                        CrashHandler.restart(SettingsActivity.this);
                    }
                })
                .show();
    }

    public void startPreferenceFragment(Fragment fragment, boolean push) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(android.R.id.content, fragment);
        if (push) {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.addToBackStack(BACK_STACK_PREFS);
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        }
        transaction.commitAllowingStateLoss();
    }

}
