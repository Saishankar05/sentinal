package com.example.sentinal;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.telephony.SmsManager;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.github.tbouron.shakedetector.library.ShakeDetector;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class ServiceMine extends Service {

    boolean isRunning = false;
    private MediaPlayer mediaPlayer;
    FusedLocationProviderClient fusedLocationClient;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    SmsManager manager = SmsManager.getDefault();
    String myLocation;

    @Override
    public void onCreate() {
        super.onCreate();


        mediaPlayer = MediaPlayer.create(this, R.raw.siren);
        mediaPlayer.setLooping(true);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            myLocation = "http://maps.google.com/maps?q=loc:" + location.getLatitude() + "," + location.getLongitude();
                        } else {
                            myLocation = "Unable to Find Location :(";
                        }
                    }
                });

        ShakeDetector.create(this, () -> {

            try {
                if (!mediaPlayer.isPlaying()) {
                    mediaPlayer = MediaPlayer.create(this, R.raw.siren);
                    mediaPlayer.setLooping(true);
                    mediaPlayer.setVolume(1.0f, 1.0f);
                    mediaPlayer.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
            String ENUM = sharedPreferences.getString("ENUM", "NONE");
            if (!ENUM.equalsIgnoreCase("NONE")) {
                manager.sendTextMessage(ENUM, null, "Im in Trouble!\nSending My Location :\n" + myLocation, null, null);
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null && intent.getAction().equalsIgnoreCase("STOP")) {
            if (isRunning) {
                stopSiren();
                this.stopForeground(true);
                this.stopSelf();
            }
        } else {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("MYID", "CHANNELFOREGROUND", NotificationManager.IMPORTANCE_DEFAULT);

                NotificationManager m = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                m.createNotificationChannel(channel);

                Notification notification = new Notification.Builder(this, "MYID")
                        .setContentTitle("Women Safety")
                        .setContentText("Shake Device to Send SOS")
                        .setSmallIcon(R.drawable.girl_vector)
                        .setContentIntent(pendingIntent)
                        .build();
                this.startForeground(115, notification);
                isRunning = true;
                return START_NOT_STICKY;
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void stopSiren() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onDestroy() {
        stopSiren();
        super.onDestroy();
    }
}