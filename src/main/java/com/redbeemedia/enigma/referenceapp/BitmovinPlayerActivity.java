package com.redbeemedia.enigma.referenceapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bitmovin.analytics.api.AnalyticsConfig;
import com.bitmovin.analytics.api.SourceMetadata;
import com.bitmovin.player.PlayerView;
import com.bitmovin.player.api.PlaybackConfig;
import com.bitmovin.player.api.Player;
import com.bitmovin.player.api.PlayerBuilder;
import com.bitmovin.player.api.PlayerConfig;
import com.bitmovin.player.api.advertising.AdItem;
import com.bitmovin.player.api.advertising.AdSource;
import com.bitmovin.player.api.advertising.AdSourceType;
import com.bitmovin.player.api.decoder.DecoderConfig;
import com.bitmovin.player.api.drm.WidevineConfig;
import com.bitmovin.player.api.source.Source;
import com.bitmovin.player.api.source.SourceBuilder;
import com.bitmovin.player.api.source.SourceConfig;
import com.bitmovin.player.api.source.SourceType;
import com.redbeemedia.enigma.bitmovinanalytics.BitmovinAnalyticsListener;
import com.redbeemedia.enigma.bitmovinanalytics.BitmovinDecoderPriorityProvider;
import com.redbeemedia.enigma.core.adapter.EnigmaExternalAdMarker;
import com.redbeemedia.enigma.core.adapter.EnigmaExternalAdTagType;
import com.redbeemedia.enigma.core.adapter.EnigmaExternalPlayerAdapterBuilder;
import com.redbeemedia.enigma.core.adapter.IEnigmaExternalPlayerAdapter;
import com.redbeemedia.enigma.core.adapter.IEnigmaExternalPlayerDrmSession;
import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.referenceapp.assets.AssetMarshaller;
import com.redbeemedia.enigma.referenceapp.assets.IAsset;
import com.redbeemedia.enigma.referenceapp.session.SessionContainer;

import java.util.List;

public class BitmovinPlayerActivity extends AppCompatActivity {
    private static final String TAG = "BitmovinPlayerActivity";
    private static final String EXTRA_ASSET = "asset";
    private static final String ANALYTICS_LICENSE_KEY = "6f00ce5d-96d2-48fc-8db9-de1d4c904612";
    private static final String DEFAULT_ASSET_ID = "fff6dae0_2D25B4";
    private static final String DEFAULT_STREAM_TITLE = "Cosmos Laundromat";

    private PlayerView playerView;
    private Player player;
    private IEnigmaExternalPlayerAdapter analyticsAdapter;

    private String assetId;
    private String streamTitle;

    public static Intent getStartIntent(Context context, IAsset asset) {
        Intent intent = new Intent(context, BitmovinPlayerActivity.class);
        AssetMarshaller.put(intent, EXTRA_ASSET, asset);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitmovin_player);

        IAsset asset = getIntent().getExtras() != null
                ? AssetMarshaller.get(getIntent().getExtras(), EXTRA_ASSET)
                : null;
        assetId = asset != null ? asset.getAssetId() : DEFAULT_ASSET_ID;
        streamTitle = asset != null && asset.getTitle() != null
                ? asset.getTitle()
                : DEFAULT_STREAM_TITLE;

        AnalyticsConfig analyticsConfig = new AnalyticsConfig(ANALYTICS_LICENSE_KEY);
        player = createPlayer(analyticsConfig);

        playerView = findViewById(R.id.playerView);
        playerView.setPlayer(player);

        analyticsAdapter = createAnalyticsAdapter();
        analyticsAdapter.prepare(assetId, new IEnigmaExternalPlayerAdapter.PrepareCallback() {
            @Override
            public void onPrepared(@Nullable String manifestUrl, @Nullable IEnigmaExternalPlayerDrmSession drmSession) {
                runOnUiThread(() -> handlePrepared(manifestUrl, drmSession));
            }

            @Override
            public void onError(EnigmaError error) {
                Log.e(TAG, "Failed to prepare external playback: " + error.getTrace());
            }
        });
    }

    private IEnigmaExternalPlayerAdapter createAnalyticsAdapter() {
        return new EnigmaExternalPlayerAdapterBuilder(
                SessionContainer.getSession().getValue(),
                new BitmovinAnalyticsListener(player)
        ).build();
    }

    private void handlePrepared(@Nullable String manifestUrl, @Nullable IEnigmaExternalPlayerDrmSession drmSession) {
        if (TextUtils.isEmpty(manifestUrl)) {
            Log.e(TAG, "External player adapter prepared without a manifest URL");
            return;
        }

        Source source = createSource(manifestUrl, drmSession);
        player.load(source);
        scheduleAds(analyticsAdapter.getAdMarkers());
    }

    private Source createSource(String manifestUrl, @Nullable IEnigmaExternalPlayerDrmSession drmSession) {
        SourceConfig sourceConfig = new SourceConfig(manifestUrl, getSourceType(manifestUrl));
        sourceConfig.setTitle(streamTitle);
        configureDrmIfAvailable(sourceConfig, drmSession);
        return buildSourceWithAnalytics(sourceConfig, assetId, streamTitle);
    }

    private void scheduleAds(@Nullable List<EnigmaExternalAdMarker> markers) {
        if (markers == null || markers.isEmpty()) {
            return;
        }
        for (EnigmaExternalAdMarker marker : markers) {
            scheduleAd(marker);
        }
    }

    private void scheduleAd(EnigmaExternalAdMarker marker) {
        AdSourceType type = mapAdSourceType(marker.getResolvedTagType());
        String position = resolveAdPosition(marker.getType());

        Log.d(TAG, "Scheduling ad: position=" + position + ", type=" + marker.getTagType());

        AdSource adSource = new AdSource(type, marker.getAdTag());
        AdItem adItem = new AdItem(position, adSource);
        player.getAds().schedule(adItem);
    }

    private AdSourceType mapAdSourceType(EnigmaExternalAdTagType tagType) {
        switch (tagType) {
            case IMA:
                return AdSourceType.Ima;
            case PROGRESSIVE:
                return AdSourceType.Progressive;
            case VAST:
            case VMAP:
            case BITMOVIN:
                return AdSourceType.Bitmovin;
            case UNKNOWN:
            default:
                Log.w(TAG, "Unknown ad tagType, falling back to default AdSourceType.Bitmovin");
                return AdSourceType.Bitmovin;
        }
    }

    private String resolveAdPosition(@Nullable String type) {
        if (type == null || type.trim().isEmpty()) {
            Log.w(TAG, "Missing ad position (type) in adMarker, defaulting to pre-roll");
            return "pre";
        }
        return type;
    }

    @Override
    protected void onStart() {
        super.onStart();
        playerView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        playerView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        playerView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        playerView.onStop();
    }

    @Override
    protected void onDestroy() {
        playerView.onDestroy();
        if (player != null) {
            player.destroy();
        }
        if (analyticsAdapter != null) {
            analyticsAdapter.release();
        }
        super.onDestroy();
    }

    private Player createPlayer(AnalyticsConfig analyticsConfig) {
        DecoderConfig decoderConfig = new DecoderConfig.Builder()
                .setDecoderPriorityProvider(new BitmovinDecoderPriorityProvider())
                .build();

        PlaybackConfig playbackConfig = new PlaybackConfig.Builder()
                .setDecoderConfig(decoderConfig)
                .build();

        PlayerConfig playerConfig = new PlayerConfig.Builder()
                .setPlaybackConfig(playbackConfig)
                .build();

        return new PlayerBuilder(this)
                .setPlayerConfig(playerConfig)
                .configureAnalytics(analyticsConfig)
                .build();
    }

    private void configureDrmIfAvailable(SourceConfig sourceConfig, @Nullable IEnigmaExternalPlayerDrmSession drmSession) {
        if (drmSession == null || TextUtils.isEmpty(drmSession.getLicenseUrl())) {
            return;
        }

        WidevineConfig widevineConfig = new WidevineConfig(
                drmSession.getLicenseUrl(),
                drmSession.getLicenseRequestHeaders(),
                /* isLicenseRenewable */ true,
                /* shouldKeepDrmSessionsAlive */ false,
                /* preferredSecurityLevel */ null,
                /* prepareMessageCallback */ null,
                /* prepareLicenseCallback */ null
        );

        sourceConfig.setDrmConfig(widevineConfig);
    }

    private Source buildSourceWithAnalytics(SourceConfig sourceConfig, String assetId, String streamTitle) {
        SourceMetadata sourceMetadata = new SourceMetadata.Builder()
                .setVideoId(assetId)
                .setTitle(streamTitle)
                .build();

        return new SourceBuilder(sourceConfig)
                .configureAnalytics(sourceMetadata)
                .build();
    }

    private SourceType getSourceType(String manifestUrl) {
        if (manifestUrl.toLowerCase().contains(".mpd")) {
            return SourceType.Dash;
        }
        return SourceType.Hls;
    }
}