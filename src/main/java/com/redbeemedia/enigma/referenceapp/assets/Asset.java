package com.redbeemedia.enigma.referenceapp.assets;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import com.bumptech.glide.Glide;
import com.redbeemedia.enigma.core.playable.AssetPlayable;
import com.redbeemedia.enigma.core.playable.IPlayable;
import com.redbeemedia.enigma.exposureutils.models.asset.ApiAsset;
import com.redbeemedia.enigma.exposureutils.models.image.ApiImage;
import com.redbeemedia.enigma.exposureutils.models.localized.ApiLocalizedData;
import com.redbeemedia.enigma.referenceapp.image.ImageUtil;
import com.redbeemedia.enigma.referenceapp.localization.LocalizationUtil;

public class Asset implements IAsset {
    private static final String LANDSCAPE = "LANDSCAPE";

    private final ApiAsset apiAsset;
    private final IPlayable playable;
    private final String title;
    private final ApiLocalizedData localizedData;

    public Asset(ApiAsset apiAsset) {
        this.apiAsset = apiAsset;
        this.playable = new AssetPlayable(apiAsset.getAssetId());
        this.localizedData = LocalizationUtil.getBestFit(apiAsset.getLocalized());
        if(localizedData != null) {
            title = localizedData.getTitle();
        } else {
            title = apiAsset.getOriginalTitle();
        }
    }

    public static IAsset load(Bundle bundle, String name) {
        ApiAsset apiAsset = bundle.getParcelable(name);
        return apiAsset != null ? new Asset(apiAsset) : null;
    }

    @Override
    public void save(Intent intent, String name) {
        intent.putExtra(name, apiAsset);
    }

    @Override
    public IPlayable getPlayable() {
        return playable;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void loadImage(ImageButton target) {
        boolean foundImage = false;
        if(localizedData != null) {
            ApiImage image = ImageUtil.getBestFit(localizedData.getImages(), LANDSCAPE, target.getWidth(), target.getHeight());
            if(image != null) {
                String imageUrl = image.getUrl();
                if(imageUrl != null) {
                    Glide.with(target).load(imageUrl).into(target);
                    foundImage = true;
                }
            }
        }
        if(!foundImage) {
            target.setImageDrawable(null);
        }
    }
}
