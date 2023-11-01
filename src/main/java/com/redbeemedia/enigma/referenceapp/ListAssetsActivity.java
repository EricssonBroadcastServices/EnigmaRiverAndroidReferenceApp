package com.redbeemedia.enigma.referenceapp;

import static com.redbeemedia.enigma.core.player.EnigmaPlayerState.IDLE;
import static com.redbeemedia.enigma.core.player.EnigmaPlayerState.PAUSED;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.redbeemedia.enigma.cast.manager.EnigmaCastManager;
import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.core.player.timeline.ITimelinePosition;
import com.redbeemedia.enigma.core.session.ISession;
import com.redbeemedia.enigma.core.time.Duration;
import com.redbeemedia.enigma.exposureutils.BaseExposureResultHandler;
import com.redbeemedia.enigma.referenceapp.activityutil.ActivityConnector;
import com.redbeemedia.enigma.referenceapp.activityutil.IActivityConnector;
import com.redbeemedia.enigma.referenceapp.assets.IAsset;
import com.redbeemedia.enigma.referenceapp.session.SessionContainer;

import java.util.ArrayList;
import java.util.List;

public class ListAssetsActivity extends AppCompatActivity {
    private MutableLiveData<List<IAsset>> assets = new MutableLiveData<>();
    private Duration CONTROL_INCREMENT = Duration.seconds(10);
    private CastContext mCastContext;


    // for cast
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.top_menu, menu);
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_assets);
        // for cast
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        LiveData<ISession> sessionLiveData = SessionContainer.getSession();
        updateAssets(sessionLiveData.getValue());
        sessionLiveData.observe(this, new Observer<ISession>() {
            @Override
            public void onChanged(ISession session) {
                updateAssets(session);
            }
        });
        RecyclerView assetList = findViewById(R.id.asset_list);
        assetList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        assetList.setAdapter(new AssetListAdapter(this, assets, new ActivityConnector<>(this)));
        EnigmaCastManager.getSharedInstance(getApplicationContext());
        setupStickyPlayerButtons();
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            if (((EnigmaRiverReferenceApp) getApplication()).isStickyPlayer()) {
                findViewById(R.id.player_scroll_view).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.player_scroll_view).setVisibility(View.INVISIBLE);
            }
        }
    }

    private void setupStickyPlayerButtons() {


        Button playButton = findViewById(R.id.stickyPlayButton);
        playButton.setOnClickListener(v -> {
            if (PlayerService.getEnigmaPlayer().getState() == PAUSED
                    || PlayerService.getEnigmaPlayer().getState() == IDLE) {
                PlayerService.getEnigmaPlayer().getControls().start();
                Button p1_button = findViewById(R.id.stickyPlayButton);
                p1_button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.exo_styled_controls_pause, 0, 0, 0);

            } else {
                PlayerService.getEnigmaPlayer().getControls().pause();
                Button p1_button = findViewById(R.id.stickyPlayButton);
                p1_button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.exo_styled_controls_play, 0, 0, 0);
            }
        });

        Button fwdButton = findViewById(R.id.stickyFwdButton);
        fwdButton.setOnClickListener(v -> {
            PlayerService.getEnigmaPlayer().getControls().pause();
            ITimelinePosition currentPosition = PlayerService.getEnigmaPlayer().getTimeline().getCurrentPosition();
            if (currentPosition != null) {
                ITimelinePosition newPosition = currentPosition.add(CONTROL_INCREMENT);
                ITimelinePosition endPosition = PlayerService.getEnigmaPlayer().getTimeline().getCurrentEndBound();
                if (newPosition.after(endPosition)) {
                    newPosition = endPosition;
                }
                PlayerService.getEnigmaPlayer().getControls().seekTo(newPosition);
                PlayerService.getEnigmaPlayer().getControls().start();
            }
        });

        Button rwdButton = findViewById(R.id.stickyRwdButton);
        rwdButton.setOnClickListener(v -> {
            PlayerService.getEnigmaPlayer().getControls().pause();
            ITimelinePosition currentPosition = PlayerService.getEnigmaPlayer().getTimeline().getCurrentPosition();
            if (currentPosition != null) {
                ITimelinePosition newPosition = currentPosition.subtract(CONTROL_INCREMENT);
                ITimelinePosition startPosition = PlayerService.getEnigmaPlayer().getTimeline().getCurrentStartBound();
                if (newPosition.before(startPosition)) {
                    newPosition = startPosition;
                }
                PlayerService.getEnigmaPlayer().getControls().seekTo(newPosition);
                PlayerService.getEnigmaPlayer().getControls().start();
            }
        });
    }

    private void updateAssets(ISession session) {
        if (session != null) {
            ExposureUtil.getReferenceAppAssets(session, new BaseExposureResultHandler<List<IAsset>>() {
                @Override
                public void onSuccess(List<IAsset> result) {
                   assets.postValue(result);
                }

                @Override
                public void onError(EnigmaError error) {
                    error.printStackTrace();
                }
            }, 100);
        }
    }

    private static class AssetListAdapter extends RecyclerView.Adapter<AssetListAdapter.AssetViewHolder> {
        private final IActivityConnector<? extends Activity> activityConnector;
        private List<IAsset> currentAssetList = new ArrayList<>();

        public AssetListAdapter(LifecycleOwner lifecycleOwner, LiveData<List<IAsset>> assets, IActivityConnector<? extends Activity> activityConnector) {
            this.activityConnector = activityConnector;
            this.currentAssetList = assets.getValue();
            assets.observe(lifecycleOwner, newAssets -> {
                currentAssetList = new ArrayList<>(newAssets);
                AssetListAdapter.this.notifyDataSetChanged();
            });
        }

        private class AssetViewHolder extends RecyclerView.ViewHolder {
            private ImageButton button;

            public AssetViewHolder(@NonNull ImageButton button) {
                super(button);
                this.button = button;
            }

            public void bind(IAsset asset) {
                button.setOnClickListener(v -> {
                    activityConnector.perform(activity -> {
                        activity.startActivity(PlayerActivity.getStartIntent(activity, asset));
                    });
                });
                asset.loadImage(button);
            }
        }

        @NonNull
        @Override
        public AssetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageButton button = new ImageButton(parent.getContext());
            Resources resources = parent.getResources();
            int width = resources.getDimensionPixelSize(R.dimen.thumbnail_width);
            int height = resources.getDimensionPixelSize(R.dimen.thumbnail_height);
            button.setLayoutParams(new ViewGroup.MarginLayoutParams(width, height));
            return new AssetViewHolder(button);
        }

        @Override
        public void onBindViewHolder(@NonNull AssetViewHolder holder, int position) {
            if (currentAssetList != null) {
                IAsset asset = currentAssetList.get(position);
                holder.bind(asset);
            }
        }

        @Override
        public int getItemCount() {
            return currentAssetList != null ? currentAssetList.size() : 0;
        }
    }
}
