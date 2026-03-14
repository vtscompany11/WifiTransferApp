package com.example.wifitransfer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Formatter;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    
    private Switch autoStartSwitch;
    private TextView statusText;
    private TextView ipText;
    private TextView urlText;
    private SharedPreferences prefs;
    private static final int PORT = 8080;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        autoStartSwitch = findViewById(R.id.auto_start_switch);
        statusText = findViewById(R.id.status_text);
        ipText = findViewById(R.id.ip_text);
        urlText = findViewById(R.id.url_text);
        
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        
        // Load saved preference
        boolean autoStart = prefs.getBoolean("auto_start", true);
        autoStartSwitch.setChecked(autoStart);
        
        // Show current IP
        updateIpDisplay();
        
        // Start service
        findViewById(R.id.start_button).setOnClickListener(v -> {
            startFileServer();
        });
        
        // Stop service
        findViewById(R.id.stop_button).setOnClickListener(v -> {
            stopFileServer();
        });
        
        // Auto-start toggle listener
        autoStartSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("auto_start", isChecked).apply();
            Toast.makeText(MainActivity.this, 
                "Auto-start " + (isChecked ? "enabled" : "disabled"), 
                Toast.LENGTH_SHORT).show();
        });
        
        // Check if service is already running
        checkServerStatus();
    }
    
    private void startFileServer() {
        Intent serviceIntent = new Intent(this, FileServerService.class);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        statusText.setText("Server Status: RUNNING");
        statusText.setTextColor(0xFF00FF00); // Green
        
        Toast.makeText(this, "Server started", Toast.LENGTH_SHORT).show();
    }
    
    private void stopFileServer() {
        Intent serviceIntent = new Intent(this, FileServerService.class);
        stopService(serviceIntent);
        
        statusText.setText("Server Status: STOPPED");
        statusText.setTextColor(0xFFFF0000); // Red
        
        Toast.makeText(this, "Server stopped", Toast.LENGTH_SHORT).show();
    }
    
    private void updateIpDisplay() {
        WifiManager wifiManager = (WifiManager) getApplicationContext()
            .getSystemService(WIFI_SERVICE);
        
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            String ip = Formatter.formatIpAddress(ipAddress);
            ipText.setText("Device IP: " + ip);
            urlText.setText("Access URL: http://" + ip + ":" + PORT);
            urlText.setTextColor(0xFF0000FF); // Blue
        } else {
            ipText.setText("WiFi not connected");
            urlText.setText("Connect to WiFi to access server");
        }
    }
    
    private void checkServerStatus() {
        // In a real app, you'd check if service is running
        // For now, just show ready
        statusText.setText("Server Status: READY");
        statusText.setTextColor(0xFF000000); // Black
    }
}