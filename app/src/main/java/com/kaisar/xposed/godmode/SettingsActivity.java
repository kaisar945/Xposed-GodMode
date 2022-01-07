package com.kaisar.xposed.godmode;


import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.kaisar.xposed.godmode.model.SharedViewModel;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_container_activity);
        SharedViewModel sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);
        sharedViewModel.mTitle.observe(this, this::setTitle);
        startNotificationService();
    }

    public void startPreferenceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_view, fragment, null)
                .setReorderingAllowed(true)
                .addToBackStack(fragment.getClass().getSimpleName())
                .commit();
    }


    private void startNotificationService() {
        Intent notificationService = new Intent(this, NotificationService.class);
        startService(notificationService);
    }

}
