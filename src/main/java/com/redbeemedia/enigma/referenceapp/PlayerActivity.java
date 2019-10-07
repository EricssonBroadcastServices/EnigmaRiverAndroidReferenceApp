package com.redbeemedia.enigma.referenceapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.redbeemedia.enigma.core.error.AssetFormatError;
import com.redbeemedia.enigma.core.error.AssetGeoBlockedError;
import com.redbeemedia.enigma.core.error.AssetNotAvailableError;
import com.redbeemedia.enigma.core.error.AssetNotAvailableForDeviceError;
import com.redbeemedia.enigma.core.error.AssetRestrictedError;
import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.core.error.InvalidAssetError;
import com.redbeemedia.enigma.core.error.NotEntitledToAssetError;
import com.redbeemedia.enigma.core.playbacksession.IPlaybackSession;
import com.redbeemedia.enigma.core.player.EnigmaPlayer;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.controls.IControlResultHandler;
import com.redbeemedia.enigma.core.playrequest.BasePlayResultHandler;
import com.redbeemedia.enigma.core.playrequest.IPlaybackProperties;
import com.redbeemedia.enigma.core.playrequest.PlayRequest;
import com.redbeemedia.enigma.core.playrequest.PlaybackProperties;
import com.redbeemedia.enigma.core.session.ISession;
import com.redbeemedia.enigma.exoplayerintegration.ExoPlayerTech;
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

    private Handler handler;
    private IEnigmaPlayer enigmaPlayer;
    private IAsset asset;
    private boolean startedAtLeastOnce = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.handler = new Handler();

        setContentView(R.layout.activity_player);

        ExoPlayerTech exoPlayerTech = new ExoPlayerTech(this, getString(R.string.app_name));
        exoPlayerTech.attachView(findViewById(R.id.player_view));
        exoPlayerTech.hideController();

        final ISession session = SessionContainer.getSession().getValue();
        if(session == null) {
            Toast.makeText(this, getString(R.string.session_not_found), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        SessionContainer.getSession().observe(this, newSessionValue -> {
            if(session != newSessionValue) {
                finish();
            }
        });

        IAsset asset = AssetMarshaller.get(getIntent().getExtras(), EXTRA_ASSET);
        if(asset == null) {
            Toast.makeText(this, getString(R.string.app_error_no_asset_selected), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        this.asset = asset;

        this.enigmaPlayer = new EnigmaPlayer(session, exoPlayerTech).setActivity(this);
        enigmaPlayer.setCallbackHandler(handler);

        AssetCoverView assetCoverView = findViewById(R.id.assetCover);
        assetCoverView.connectTo(enigmaPlayer);

        CustomControlsView customControlsView = findViewById(R.id.player_controls);
        customControlsView.connectTo(enigmaPlayer);
    }

    private void startStream() {
        PlaybackProperties playbackProperties = new PlaybackProperties();
        playbackProperties.setPlayFrom(startedAtLeastOnce ? IPlaybackProperties.PlayFrom.BOOKMARK : IPlaybackProperties.PlayFrom.PLAYER_DEFAULT);
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
        enigmaPlayer.getControls().start(new ContinueStreamResultHandler(new ActivityConnector<>(this)));
    }

    private void onPlaybackStarted(IPlaybackSession playbackSession) {
        startedAtLeastOnce = true;
    }

    private void onPlayRequestError(EnigmaError error) {
        String errorMessage = getErrorForUser(error);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        error.printStackTrace();
    }

    private String getErrorForUser(EnigmaError error) {
        if(error instanceof InvalidAssetError) {
            return getString(R.string.asset_error_invalid);
        } else if(error instanceof AssetFormatError) {
            return getString(R.string.asset_error_format);
        } else if(error instanceof AssetNotAvailableForDeviceError) {
            return getString(R.string.asset_error_not_available_for_device);
        } else if(error instanceof NotEntitledToAssetError) {
            return getString(R.string.asset_error_not_entitled);
        } else if(error instanceof AssetGeoBlockedError) {
            return getString(R.string.asset_error_geo_block);
        } else if(error instanceof AssetRestrictedError) {
            return getString(R.string.asset_error_restricted);
        } else if(error instanceof AssetNotAvailableError) {
            return getString(R.string.asset_error_not_available);
        } else {
            return getString(R.string.error_unknown);
        }
    }

    public static Intent getStartIntent(Context context, IAsset asset) {
        Intent intent = new Intent(context, PlayerActivity.class);
        AssetMarshaller.put(intent,EXTRA_ASSET, asset);
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
