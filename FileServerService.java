package com.example.wifitransfer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.text.format.Formatter;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

public class FileServerService extends Service {
    
    private static final String TAG = "FileServerService";
    private static final int PORT = 8080;
    private ServerThread serverThread;
    private boolean isRunning = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        
        createNotificationChannel();
        startForeground(1, createNotification());
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "file_server_channel",
                "File Server Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows that file server is running");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, 
            notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        
        String ipAddress = getIpAddress();
        String content = "Server running at http://" + ipAddress + ":" + PORT;
        
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, "file_server_channel");
        } else {
            builder = new Notification.Builder(this);
        }
        
        return builder
            .setContentTitle("WiFi File Transfer")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service start command received");
        
        if (!isRunning) {
            serverThread = new ServerThread();
            serverThread.start();
            isRunning = true;
            
            // Update notification with IP
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.notify(1, createNotification());
        }
        
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        if (serverThread != null) {
            serverThread.stopServer();
        }
    }
    
    private String getIpAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext()
            .getSystemService(WIFI_SERVICE);
        
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            return Formatter.formatIpAddress(ipAddress);
        }
        return "unknown";
    }
    
    private class ServerThread extends Thread {
        private ServerSocket serverSocket;
        private boolean running = true;
        
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(PORT);
                Log.d(TAG, "Server started on port " + PORT);
                
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        Log.d(TAG, "Client connected: " + clientSocket.getInetAddress());
                        new ClientHandler(clientSocket).start();
                    } catch (IOException e) {
                        if (running) {
                            Log.e(TAG, "Error accepting connection", e);
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not start server", e);
            }
        }
        
        public void stopServer() {
            running = false;
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing server", e);
            }
        }
    }
    
    private class ClientHandler extends Thread {
        private Socket clientSocket;
        
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }
        
        @Override
        public void run() {
            try {
                String htmlResponse = "<html><body style='font-family:Arial;text-align:center;padding:50px;'>" +
                    "<h1>✅ WiFi File Transfer</h1>" +
                    "<p>Server is running successfully!</p>" +
                    "<p>Your IP: " + getIpAddress() + "</p>" +
                    "<p>Port: " + PORT + "</p>" +
                    "<hr>" +
                    "<p>To transfer files, you would need a more advanced server.</p>" +
                    "<p>This is a basic working example.</p>" +
                    "</body></html>";
                
                String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length: " + htmlResponse.length() + "\r\n" +
                    "Connection: close\r\n\r\n" +
                    htmlResponse;
                
                clientSocket.getOutputStream().write(response.getBytes());
                clientSocket.getOutputStream().flush();
                
            } catch (IOException e) {
                Log.e(TAG, "Error handling client", e);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing socket", e);
                }
            }
        }
    }
}