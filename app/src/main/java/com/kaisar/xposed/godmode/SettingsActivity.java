package com.kaisar.xposed.godmode;


import android.content.DialogInterface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.kaisar.xposed.godmode.util.Clipboard;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String crashLog = CrashHandler.loadCrashLog();
        if (!TextUtils.isEmpty(crashLog)) {
            CrashHandler.clearCrashLog();
            showCrashDialog(crashLog);
        }
        setContentView(R.layout.fragment_container_activity);
    }

    private void showCrashDialog(final String stackTrace) {
        SpannableString text = new SpannableString(getString(R.string.crash_tip));
        SpannableString st = new SpannableString(stackTrace);
        st.setSpan(new RelativeSizeSpan(0.7f), 0, st.length(), 0);
        CharSequence message = TextUtils.concat(text, st);
        new AlertDialog.Builder(this)
                .setTitle(R.string.hey_guy)
                .setMessage(message)
                .setPositiveButton(R.string.share_crash_info, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Clipboard.putContent(getApplicationContext(), stackTrace);
                    }
                })
                .show();
    }

    public void startPreferenceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_view, fragment, null)
                .setReorderingAllowed(true)
                .addToBackStack(fragment.getClass().getSimpleName())
                .commit();
    }

}
