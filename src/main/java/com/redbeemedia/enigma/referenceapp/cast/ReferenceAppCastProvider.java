package com.redbeemedia.enigma.referenceapp.cast;

import static com.redbeemedia.enigma.referenceapp.cast.ExpandedControlActivity.CAST_ALBUM_ART_THUMBNAIL_URL;

import androidx.annotation.NonNull;

import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.media.ImageHints;
import com.google.android.gms.cast.framework.media.ImagePicker;
import com.google.android.gms.common.images.WebImage;
import com.redbeemedia.enigma.cast.optionsprovider.EnigmaCastOptionsProvider;

import org.json.JSONException;
import org.json.JSONObject;

public class ReferenceAppCastProvider extends EnigmaCastOptionsProvider {

    @Override
    protected void buildCastOptions(CastOptions.Builder builder) {
        super.buildCastOptions(builder);
        CastMediaOptions castMediaOptions = new CastMediaOptions.Builder()
                .setMediaSessionEnabled(true)
                .setImagePicker(new ImagePickerImpl())
                .setExpandedControllerActivityClassName(ExpandedControlActivity.class.getName()).build();
        builder.setCastMediaOptions(castMediaOptions);
    }
}

class ImagePickerImpl extends ImagePicker {

    public static final int WIDTH = 300;
    public static final int HEIGHT = 500;

    @Override
    public WebImage onPickImage(@NonNull MediaMetadata mediaMetadata, ImageHints hints) {
        JSONObject var1 = new JSONObject();
        try {
            var1.put("url", CAST_ALBUM_ART_THUMBNAIL_URL);
            var1.put("width", WIDTH);
            var1.put("height", HEIGHT);
        } catch (JSONException ignored) {
        }
        return new WebImage(var1);
    }
}