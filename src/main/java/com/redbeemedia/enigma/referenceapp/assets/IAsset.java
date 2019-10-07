package com.redbeemedia.enigma.referenceapp.assets;

import android.content.Intent;
import android.widget.ImageButton;

import com.redbeemedia.enigma.core.playable.IPlayable;

public interface IAsset {
    IPlayable getPlayable();
    String getTitle();
    void loadImage(ImageButton button);
    void save(Intent intent, String name);
}
