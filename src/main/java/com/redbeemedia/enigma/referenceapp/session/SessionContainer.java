package com.redbeemedia.enigma.referenceapp.session;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.redbeemedia.enigma.core.session.ISession;

public class SessionContainer {
    private static final MutableLiveData<ISession> session = new MutableLiveData<>();

    public static LiveData<ISession> getSession() {
        return session;
    }

    public static void setSession(ISession session) {
        SessionContainer.session.setValue(session);
    }
}
