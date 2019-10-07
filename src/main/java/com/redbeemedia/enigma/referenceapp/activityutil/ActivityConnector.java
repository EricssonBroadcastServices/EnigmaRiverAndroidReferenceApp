package com.redbeemedia.enigma.referenceapp.activityutil;

import android.app.Activity;

import java.lang.ref.WeakReference;

public class ActivityConnector<T extends Activity> implements IActivityConnector<T> {
    private WeakReference<T> activityReference;

    public ActivityConnector(T activity) {
        this.activityReference = new WeakReference<>(activity);
    }

    @Override
    public void perform(IActivityAction<? super T> action) {
        T activity = activityReference.get();
        if(activity != null) {
            action.execute(activity);
        }
    }
}
