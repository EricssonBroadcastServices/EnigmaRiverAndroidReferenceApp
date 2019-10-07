package com.redbeemedia.enigma.referenceapp;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.core.session.ISession;
import com.redbeemedia.enigma.exposureutils.IExposureResultHandler;
import com.redbeemedia.enigma.referenceapp.activityutil.ActivityConnector;
import com.redbeemedia.enigma.referenceapp.activityutil.IActivityConnector;
import com.redbeemedia.enigma.referenceapp.assets.IAsset;
import com.redbeemedia.enigma.referenceapp.session.SessionContainer;

import java.util.ArrayList;
import java.util.List;

public class ListAssetsActivity extends AppCompatActivity {
    private MutableLiveData<List<IAsset>> assets = new MutableLiveData<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_assets);

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
    }

    private void updateAssets(ISession session) {
        if(session != null) {
            ExposureUtil.getReferenceAppAssets(session, new IExposureResultHandler<List<IAsset>>() {
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
            if(currentAssetList != null) {
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
