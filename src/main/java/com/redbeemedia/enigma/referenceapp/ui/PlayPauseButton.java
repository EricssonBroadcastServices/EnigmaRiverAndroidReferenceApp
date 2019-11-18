package com.redbeemedia.enigma.referenceapp.ui;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageButton;

import com.redbeemedia.enigma.core.virtualui.BaseVirtualButtonListener;
import com.redbeemedia.enigma.core.virtualui.IVirtualButton;
import com.redbeemedia.enigma.core.virtualui.IVirtualButtonListener;
import com.redbeemedia.enigma.core.virtualui.IVirtualControls;

public class PlayPauseButton extends AppCompatImageButton {
    private Handler handler;
    private boolean usingPauseButton = false;
    private IVirtualButton playButton;
    private IVirtualButton pauseButton;

    public PlayPauseButton(Context context) {
        super(context);
        init();
    }

    public PlayPauseButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PlayPauseButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    protected void init() {
        this.handler = new Handler();
        updatePlayPauseButtonImage();
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(playButton != null && pauseButton != null) {
                    if(playButton.isEnabled()) {
                        playButton.click();
                    } else if(pauseButton.isEnabled()) {
                        pauseButton.click();
                    }
                }
            }
        });
    }

    public void connectTo(IVirtualControls virtualControls){
        this.playButton = virtualControls.getPlay();
        this.pauseButton = virtualControls.getPause();

        IVirtualButtonListener virtualButtonChangedListener = new BaseVirtualButtonListener() {
            @Override
            public void onStateChanged() {
                updatePlayPauseButtonImage();
                boolean visible = playButton.isRelevant() || pauseButton.isRelevant();
                setVisibility(visible ? VISIBLE : GONE);
            }
        };
        addOnAttachStateChangeListener(new VirtualButtonViewAttacher(playButton, virtualButtonChangedListener, handler));
        addOnAttachStateChangeListener(new VirtualButtonViewAttacher(pauseButton, virtualButtonChangedListener, handler));
    }

    private void updatePlayPauseButtonImage() {
        if(playButton != null && pauseButton != null) {
            usingPauseButton = !playButton.isEnabled();
        }
        int icon = usingPauseButton ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        this.setImageResource(icon);
    }
}