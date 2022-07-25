package com.redbeemedia.enigma.referenceapp;

import static com.google.android.exoplayer2.ui.CaptionStyleCompat.DEFAULT;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.exoplayer2.ui.CaptionStyleCompat;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.redbeemedia.enigma.cast.listeners.BaseEnigmaCastManagerListener;
import com.redbeemedia.enigma.cast.manager.EnigmaCastManager;
import com.redbeemedia.enigma.cast.manager.IEnigmaCastManager;
import com.redbeemedia.enigma.cast.request.EnigmaCastPlaybackProperties;
import com.redbeemedia.enigma.cast.request.EnigmaCastRequest;
import com.redbeemedia.enigma.cast.request.IEnigmaCastRequest;
import com.redbeemedia.enigma.cast.resulthandler.BaseEnigmaCastResultHandler;
import com.redbeemedia.enigma.cast.session.IEnigmaCastSession;
import com.redbeemedia.enigma.core.error.AssetFormatError;
import com.redbeemedia.enigma.core.error.AssetGeoBlockedError;
import com.redbeemedia.enigma.core.error.AssetNotAvailableError;
import com.redbeemedia.enigma.core.error.AssetNotAvailableForDeviceError;
import com.redbeemedia.enigma.core.error.AssetRestrictedError;
import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.core.error.InvalidAssetError;
import com.redbeemedia.enigma.core.error.NoInternetConnectionError;
import com.redbeemedia.enigma.core.error.NotEntitledToAssetError;
import com.redbeemedia.enigma.core.playable.AssetPlayable;
import com.redbeemedia.enigma.core.playbacksession.IPlaybackSession;
import com.redbeemedia.enigma.core.player.EnigmaPlayer;
import com.redbeemedia.enigma.core.player.EnigmaPlayerState;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.controls.IControlResultHandler;
import com.redbeemedia.enigma.core.player.listener.BaseEnigmaPlayerListener;
import com.redbeemedia.enigma.core.playrequest.BasePlayResultHandler;
import com.redbeemedia.enigma.core.playrequest.IPlaybackProperties;
import com.redbeemedia.enigma.core.playrequest.PlayRequest;
import com.redbeemedia.enigma.core.playrequest.PlaybackProperties;
import com.redbeemedia.enigma.core.session.ISession;
import com.redbeemedia.enigma.core.util.AndroidThreadUtil;
import com.redbeemedia.enigma.exoplayerintegration.ExoPlayerTech;
import com.redbeemedia.enigma.exoplayerintegration.drift.DriftCorrector;
import com.redbeemedia.enigma.referenceapp.activityutil.ActivityConnector;
import com.redbeemedia.enigma.referenceapp.activityutil.IActivityAction;
import com.redbeemedia.enigma.referenceapp.activityutil.IActivityConnector;
import com.redbeemedia.enigma.referenceapp.assets.AssetMarshaller;
import com.redbeemedia.enigma.referenceapp.assets.IAsset;
import com.redbeemedia.enigma.referenceapp.session.SessionContainer;
import com.redbeemedia.enigma.referenceapp.ui.AssetCoverView;
import com.redbeemedia.enigma.referenceapp.ui.CustomControlsView;

public class PlayerActivity extends AppCompatActivity {
    private static final String EXTRA_ASSET = "asset";

    private IEnigmaPlayer enigmaPlayer;
    private ExoPlayerTech exoPlayerTech;
    private IAsset asset;
    private boolean startedAtLeastOnce = false;
    private IEnigmaCastManager enigmaCastManager;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.top_menu, menu);
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        // cast
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        exoPlayerTech = new ExoPlayerTech(this, getString(R.string.app_name));
        exoPlayerTech.addDriftListener(new DriftCorrector()); // For automatic drift correction
        exoPlayerTech.attachView(findViewById(R.id.player_view));
        exoPlayerTech.hideController();

        final ISession session = SessionContainer.getSession().getValue();
        if (session == null) {
            Toast.makeText(this, getString(R.string.session_not_found), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        SessionContainer.getSession().observe(this, newSessionValue -> {
            if (session != newSessionValue) {
                finish();
            }
        });

        IAsset asset = AssetMarshaller.get(getIntent().getExtras(), EXTRA_ASSET);
        if (asset == null) {
            Toast.makeText(this, getString(R.string.app_error_no_asset_selected), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        this.asset = asset;

        this.enigmaPlayer = new EnigmaPlayer(session, exoPlayerTech).setActivity(this);
        enigmaPlayer.addListener(new BaseEnigmaPlayerListener() {
            @Override
            public void onStateChanged(EnigmaPlayerState from, EnigmaPlayerState to) {
                if (to == EnigmaPlayerState.PLAYING) {
                    ((EnigmaRiverReferenceApp) getApplication()).setStickyPlayer(enigmaPlayer.isCurrentStreamTypeAudioOnly());
                }
            }
        });

        AssetCoverView assetCoverView = findViewById(R.id.assetCover);
        assetCoverView.connectTo(enigmaPlayer);

        CustomControlsView customControlsView = findViewById(R.id.player_controls);
        customControlsView.connectTo(enigmaPlayer);
        enigmaCastManager = EnigmaCastManager.getSharedInstance(getApplicationContext());
        enigmaCastManager.addListener(new BaseEnigmaCastManagerListener() {
            @Override
            public void onCastSessionChanged(@Nullable IEnigmaCastSession oldSession, @Nullable IEnigmaCastSession newSession) {
                playToChromecast(session);
            }
        }, new Handler(Looper.getMainLooper()));
    }


    private void playToChromecast(ISession session) {
        EnigmaCastPlaybackProperties playbackProperties = new EnigmaCastPlaybackProperties.Builder().build();
        AssetPlayable assetPlayable = (AssetPlayable)asset.getPlayable();
        IEnigmaCastRequest castRequest =
                new EnigmaCastRequest.Builder(assetPlayable.getAssetId(), session)
                        .setPlaybackProperties(playbackProperties)
                        .build();
        AndroidThreadUtil.runOnUiThread(() -> {
            enigmaCastManager.play(
                    castRequest, new BaseEnigmaCastResultHandler() {
                        @Override
                        public void onSuccess() {
                            Log.d("Cast","Asset cast success");
                        }
                        @Override
                        public void onException(Exception e) {
                            e.printStackTrace();
                            Log.e("Cast","Asset cast error",e);
                        }
                    },
                    new Handler(Looper.getMainLooper()));
        });
    }


    private void startStream() {
        PlaybackProperties playbackProperties = new PlaybackProperties();
        playbackProperties.setPlayFrom(startedAtLeastOnce ? IPlaybackProperties.PlayFrom.BOOKMARK : IPlaybackProperties.PlayFrom.PLAYER_DEFAULT);

        // Stop the sticky player if it is already running
        if (PlayerService.isRunning()) {
            Intent stopIntent = new Intent(this, PlayerService.class);
            stopIntent.setAction(PlayerService.ACTION_STOP_FOREGROUND_SERVICE);
            startService(stopIntent);
        }

        enigmaPlayer.play(new PlayRequest(asset.getPlayable(), playbackProperties, new PlayRequestResultHandler(new ActivityConnector<>(this))));
    }

    @Override
    protected void onPause() {
        super.onPause();
        enigmaPlayer.getControls().pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CaptionStyleCompat captionStyleCompat = new CaptionStyleCompat(DEFAULT.foregroundColor,
                Color.BLUE,
                DEFAULT.windowColor,
                DEFAULT.edgeType,
                DEFAULT.edgeColor,
                DEFAULT.typeface);
        //enigmaPlayer.getPlayerSubtitleView().setStyle(captionStyleCompat);
        //enigmaPlayer.getPlayerSubtitleView().setFixedTextSize(Dimension.PX,30);
        enigmaPlayer.getControls().start(new ContinueStreamResultHandler(new ActivityConnector<>(this)));
    }

    private void onPlaybackStarted(IPlaybackSession playbackSession) {
        startedAtLeastOnce = true;
    }

    private void onPlayRequestError(EnigmaError error) {
        String errorMessage = getErrorForUser(error);
        error.printStackTrace();
        PlayerActivity object = this;
        this.runOnUiThread(() -> Toast.makeText(object, errorMessage, Toast.LENGTH_LONG).show());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onDestroy() {
        // create sticky player only if audio media type
        if (enigmaPlayer.isCurrentStreamTypeAudioOnly()) {
            // Important to setStickyPlayer flag true if want sticky functionality
            enigmaPlayer.setStickyPlayer(true);

            // Pass parameters to Foreground service
            PlayerService.setEnigmaPlayer(enigmaPlayer);
            PlayerService.setExoPlayerTech(exoPlayerTech);
            Intent dialogIntent = new Intent(this, PlayerService.class);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startForegroundService(dialogIntent);
        }
        super.onDestroy();
    }

    private String getErrorForUser(EnigmaError error) {
        if (error instanceof InvalidAssetError) {
            return getString(R.string.asset_error_invalid);
        } else if (error instanceof AssetFormatError) {
            return getString(R.string.asset_error_format);
        } else if (error instanceof AssetNotAvailableForDeviceError) {
            return getString(R.string.asset_error_not_available_for_device);
        } else if (error instanceof NotEntitledToAssetError) {
            return getString(R.string.asset_error_not_entitled);
        } else if (error instanceof AssetGeoBlockedError) {
            return getString(R.string.asset_error_geo_block);
        } else if (error instanceof AssetRestrictedError) {
            return getString(R.string.asset_error_restricted);
        } else if (error instanceof AssetNotAvailableError) {
            return getString(R.string.asset_error_not_available);
        }  else if (error instanceof NoInternetConnectionError) {
            return "Network fault, check your internet";
        } else {
            return getString(R.string.error_unknown);
        }
    }

    public static Intent getStartIntent(Context context, IAsset asset) {
        Intent intent = new Intent(context, PlayerActivity.class);
        AssetMarshaller.put(intent, EXTRA_ASSET, asset);
        return intent;
    }

    private static class PlayRequestResultHandler extends BasePlayResultHandler {
        private IActivityConnector<PlayerActivity> activity;

        public PlayRequestResultHandler(IActivityConnector<PlayerActivity> activity) {
            this.activity = activity;
        }

        @Override
        public void onStarted(IPlaybackSession playbackSession) {
            activity.perform(activity -> activity.onPlaybackStarted(playbackSession));
        }

        @Override
        public void onError(EnigmaError error) {
            activity.perform(activity -> activity.onPlayRequestError(error));
        }
    }

    private static class ContinueStreamResultHandler implements IControlResultHandler {
        private IActivityConnector<PlayerActivity> activityConnector;
        private final IActivityAction<PlayerActivity> makeNewPlayRequest = new IActivityAction<PlayerActivity>() {
            @Override
            public void execute(PlayerActivity activity) {
                activity.startStream();
            }
        };

        public ContinueStreamResultHandler(IActivityConnector<PlayerActivity> activityConnector) {
            this.activityConnector = activityConnector;
        }

        @Override
        public void onRejected(IRejectReason reason) {
            activityConnector.perform(makeNewPlayRequest);
        }

        @Override
        public void onCancelled() {
        }

        @Override
        public void onError(EnigmaError error) {
            error.printStackTrace();
            activityConnector.perform(makeNewPlayRequest);
        }

        @Override
        public void onDone() {
        }
    }
}
