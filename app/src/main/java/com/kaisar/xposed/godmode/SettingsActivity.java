package com.kaisar.xposed.godmode;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.kaisar.xposed.godmode.model.SharedViewModel;
import com.kaisar.xposed.godmode.widget.Snackbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_container_activity);
        SharedViewModel sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);
        sharedViewModel.title.observe(this, this::setTitle);
        startNotificationService();
    }

    private void startNotificationService() {
        Intent notificationService = new Intent(this, NotificationService.class);
        startService(notificationService);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && resultCode == Activity.RESULT_OK) {
            if (requestCode == 114514) {
                try {
                    OutputStream outputStream = this.getContentResolver().openOutputStream(data.getData());
                    File file = new File(GodModeApplication.thisBackupFilePath);
                    if (file.canRead()) {
                        FileInputStream fileInputStream = new FileInputStream(file);
                        byte[] bytes = new byte[fileInputStream.available()];
                        fileInputStream.read(bytes);
                        outputStream.write(bytes);
                        fileInputStream.close();
                        outputStream.close();
                        file.delete();
                    }
                    GodModeApplication.thisBackupFilePath = "";
                } catch (Throwable e) {
                    e.printStackTrace();
                    Snackbar.make(this, getString(R.string.export_failed), Snackbar.LENGTH_LONG).show();
                } finally {
                    Snackbar.make(this, getString(R.string.export_successful, data.getData().getPath()), Snackbar.LENGTH_LONG).show();
                }

            }
        }
        if (GodModeApplication.thisBackupFilePath.equals("")) {
            File file = new File(GodModeApplication.thisBackupFilePath);
            file.delete();
            GodModeApplication.thisBackupFilePath = "";
        }
    }
}
