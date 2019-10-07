package com.redbeemedia.enigma.referenceapp.localization;

import com.redbeemedia.enigma.exposureutils.models.localized.ApiLocalizedData;
import com.redbeemedia.enigma.referenceapp.EnigmaRiverReferenceApp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalizationUtil {
    public static ApiLocalizedData getBestFit(List<ApiLocalizedData> localizedDataList) {
        Map<String,ApiLocalizedData> localizedMap = new HashMap<>();
        for(ApiLocalizedData localizedData : localizedDataList) {
            localizedMap.put(localizedData.getLocale(), localizedData);
        }
        ApiLocalizedData localizedData = localizedMap.get(EnigmaRiverReferenceApp.getLocale());
        if(localizedData != null) {
            return localizedData;
        }
        localizedData = localizedMap.get("en"); //default language
        if(localizedData != null) {
            return localizedData;
        }
        if(!localizedDataList.isEmpty()) {
            return localizedDataList.get(0);
        }
        return null;
    }
}
