package com.example.wifitransfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Boot completed received!");
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Check if user enabled auto-start in settings
            SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
            boolean autoStartEnabled = prefs.getBoolean("auto_start", true);
            
            if (autoStartEnabled) {
                Log.d(TAG, "Auto-start enabled, scheduling server start...");
                
                // Add a delay to ensure WiFi is connected
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startFileServer(context);
                    }
                }, 30000); // 30 second delay
            }
        }
    }
    
    private void startFileServer(Context context) {
        Log.d(TAG, "Starting file server service...");
        Intent serviceIntent = new Intent(context, FileServerService.class);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}