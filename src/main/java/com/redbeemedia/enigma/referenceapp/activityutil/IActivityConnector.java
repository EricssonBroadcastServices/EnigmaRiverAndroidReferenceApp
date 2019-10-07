package com.redbeemedia.enigma.referenceapp.activityutil;

import android.app.Activity;

public interface IActivityConnector<T extends Activity> {
    void perform(IActivityAction<? super T> action);
}
