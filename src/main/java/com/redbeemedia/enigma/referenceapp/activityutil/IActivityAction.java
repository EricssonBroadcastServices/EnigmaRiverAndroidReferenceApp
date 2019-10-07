package com.redbeemedia.enigma.referenceapp.activityutil;

import android.app.Activity;

public interface IActivityAction<T extends Activity> {
    void execute(T activity);
}
