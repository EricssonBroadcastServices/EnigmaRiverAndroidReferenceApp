package com.redbeemedia.enigma.referenceapp;

import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.core.session.ISession;
import com.redbeemedia.enigma.exposureutils.EnigmaExposure;
import com.redbeemedia.enigma.exposureutils.FieldSet;
import com.redbeemedia.enigma.exposureutils.GetAllAssetsRequest;
import com.redbeemedia.enigma.exposureutils.IExposureResultHandler;
import com.redbeemedia.enigma.exposureutils.models.asset.ApiAsset;
import com.redbeemedia.enigma.exposureutils.models.asset.ApiAssetList;
import com.redbeemedia.enigma.referenceapp.assets.Asset;
import com.redbeemedia.enigma.referenceapp.assets.IAsset;

import java.util.ArrayList;
import java.util.List;

public class ExposureUtil {

    public static void getReferenceAppAssets(ISession session, IExposureResultHandler<List<IAsset>> resultHandler, int maxResults) {
        if(maxResults > 10000) {
            throw new IllegalArgumentException();
        }
        EnigmaExposure enigmaExposure = new EnigmaExposure(session);
        GetAllAssetsRequest assetsRequest = new GetAllAssetsRequest(new IExposureResultHandler<ApiAssetList>() {
            @Override
            public void onSuccess(ApiAssetList result) {
                List<IAsset> assetList = new ArrayList<>();
                for(ApiAsset apiAsset : result.getItems()) {
                    assetList.add(new Asset(apiAsset));
                }
                resultHandler.onSuccess(assetList);
            }

            @Override
            public void onError(EnigmaError error) {
                resultHandler.onError(error);
            }
        });
        assetsRequest.setFieldSet(FieldSet.ALL);
        assetsRequest.setQuery("tags.other:enigma-android-ref-app_5417D6");
        assetsRequest.setPageSize(maxResults);

        enigmaExposure.doRequest(assetsRequest);
    }
}
