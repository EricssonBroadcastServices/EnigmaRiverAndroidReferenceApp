package com.redbeemedia.enigma.referenceapp.assets;

import android.content.Intent;
import android.os.Bundle;

public class AssetMarshaller {
    public static void put(Intent intent, String name, IAsset asset) {
        intent.putExtra(name+".class", asset == null ? "null" : asset.getClass().getName());
        asset.save(intent, name);
    }

    public static IAsset get(Bundle bundle, String name) {
        String className = bundle.getString(name+".class");
        if("null".equals(className) || className == null) {
            return null;
        } else if(Asset.class.getName().equals(className)) {
            return Asset.load(bundle, name);
        } else {
            throw new RuntimeException("Unhandled asset type "+className);
        }
    }
}
