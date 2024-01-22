package com.redbeemedia.enigma.referenceapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.util.AndroidThreadUtil;
import com.redbeemedia.enigma.exoplayerintegration.ExoPlayerTech;

import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerService extends Service {

    public static final int ID = 111;
    public static final String TAG = "PLAYER_SERVICE";
    public static final String CHANNEL_ID = "RedBee_player_service";
    private static IEnigmaPlayer enigmaPlayer;
    private static ExoPlayerTech exoPlayerTech;
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    private static final AtomicBoolean isRunning = new AtomicBoolean(false);

    public static void setEnigmaPlayer(IEnigmaPlayer enigmaPlayer) {
        PlayerService.enigmaPlayer = enigmaPlayer;
    }

    public static void setExoPlayerTech(ExoPlayerTech exoPlayerTech) {
        PlayerService.exoPlayerTech = exoPlayerTech;
    }

    public static IEnigmaPlayer getEnigmaPlayer() {
        return enigmaPlayer;
    }

    public static boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void onCreate() {
        String CHANNEL_ID = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CHANNEL_ID = createNotificationChannel(PlayerService.CHANNEL_ID, "Player");
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Player notification", NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Player notification")
                    .setContentText("Player notification").build();
            startForeground(ID, notification);
        } else {
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Player notification")
                    .setContentText("Player notification").build();
            startForeground(ID, notification);
        }

        PlayerNotificationManager.MediaDescriptionAdapter s = new PlayerNotificationManager.MediaDescriptionAdapter() {
            @Override
            public CharSequence getCurrentContentTitle(Player player) {
                return "RedBee Player";
            }

            @Nullable
            @Override
            public PendingIntent createCurrentContentIntent(Player player) {
                return null;
            }

            @Override
            public CharSequence getCurrentContentText(Player player) {
                return "RedBee player";
            }

            @Nullable
            @Override
            public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                return null;
            }
        };

        PlayerNotificationManager.Builder build = new PlayerNotificationManager.Builder(this, ID, CHANNEL_ID);
        build.setMediaDescriptionAdapter(s);
        PlayerNotificationManager manager = build.build();
        exoPlayerTech.setupPlayerNotificationManager(manager);
        //Always write your long running tasks in a separate thread, to avoid ANR
        AndroidThreadUtil.runOnUiThread(() -> {
            enigmaPlayer.getControls().start();
            isRunning.set(true);
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP_FOREGROUND_SERVICE.equals(action)) {
                stopForegroundService();
            }
        }
        return Service.START_STICKY;
    }

    private void stopForegroundService() {
        Log.d(TAG, "******* Stopping foreground service ********");
        // Stop foreground service and remove the notification.
        stopForeground(true);
        // Stop the foreground service.
        stopSelf();
        isRunning.set(false);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        enigmaPlayer.getControls().start();
        return null;
    }

    @Override
    public void onDestroy() {
        //enigmaPlayer.getControls().stop();
        //enigmaPlayer.release();
        //isRunning.set(false);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelId, String channelName) {
        NotificationChannel chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return channelId;
    }
}