package com.redbeemedia.enigma.referenceapp;

import android.app.Application;

import com.redbeemedia.enigma.core.businessunit.IBusinessUnit;
import com.redbeemedia.enigma.core.context.EnigmaRiverContext;

public class EnigmaRiverReferenceApp extends Application {
    public static String BASE_URL = null; // will be filled from Input box "https://redbee.enigmatv.io";
    public static IBusinessUnit BUSINESS_UNIT = null; // will be filled from Input box  new BusinessUnit("RedBee", "RedBeeTV");
    public boolean isStickyPlayer;

    public static String getLocale() {
        return "en";
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static class ReferenceAppInitialization extends EnigmaRiverContext.EnigmaRiverContextInitialization {
        public ReferenceAppInitialization() {
            super(BASE_URL);
        }
    }

    public boolean isStickyPlayer() {
        return isStickyPlayer;
    }

    public void setStickyPlayer(boolean stickyPlayer) {
        isStickyPlayer = stickyPlayer;
    }
}
