package com.redbeemedia.enigma.referenceapp;

import android.app.Application;

import com.redbeemedia.enigma.core.businessunit.BusinessUnit;
import com.redbeemedia.enigma.core.businessunit.IBusinessUnit;
import com.redbeemedia.enigma.core.context.EnigmaRiverContext;

public class EnigmaRiverReferenceApp extends Application {
    public static final String BASE_URL = "https://redbee.enigmatv.io";
    public static final IBusinessUnit BUSINESS_UNIT = new BusinessUnit("RedBee", "RedBeeTV");

    public static String getLocale() {
        return "en";
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ReferenceAppInitialization initialization = new ReferenceAppInitialization();

        EnigmaRiverContext.initialize(this, initialization);
    }

    private static class ReferenceAppInitialization extends EnigmaRiverContext.EnigmaRiverContextInitialization {
        public ReferenceAppInitialization() {
            super(BASE_URL);
        }
    }
}
