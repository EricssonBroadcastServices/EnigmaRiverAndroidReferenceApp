package com.redbeemedia.enigma.referenceapp.ui;

import android.os.Handler;
import android.view.View;

import com.redbeemedia.enigma.core.virtualui.IVirtualButton;
import com.redbeemedia.enigma.core.virtualui.IVirtualButtonListener;

import java.util.Objects;

public class VirtualButtonViewAttacher implements View.OnAttachStateChangeListener {
    private final IVirtualButton virtualButton;
    private final IVirtualButtonListener virtualButtonListener;
    private final Handler handler;

    public VirtualButtonViewAttacher(IVirtualButton virtualButton, IVirtualButtonListener virtualButtonListener, Handler handler) {
        Objects.requireNonNull(virtualButton, "virtualButton was null");
        Objects.requireNonNull(virtualButtonListener, "virtualButtonListener was null");
        Objects.requireNonNull(handler, "handler was null");

        this.virtualButton = virtualButton;
        this.virtualButtonListener = virtualButtonListener;
        this.handler = handler;
    }

    @Override
    public void onViewAttachedToWindow(View v) {
        virtualButton.addListener(virtualButtonListener, handler);
        virtualButtonListener.onStateChanged(); //refresh
    }

    @Override
    public void onViewDetachedFromWindow(View v) {
        virtualButton.removeListener(virtualButtonListener);
    }
}
