package com.redbeemedia.enigma.referenceapp.ui;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageButton;

import com.redbeemedia.enigma.core.player.EnigmaPlayerState;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.listener.BaseEnigmaPlayerListener;

public class PlayPauseButton extends AppCompatImageButton {
    private IEnigmaPlayer enigmaPlayer;
    private Handler handler;
    private boolean usingPauseButton = false;

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
    }

    public void connectTo(IEnigmaPlayer enigmaPlayer){
        this.enigmaPlayer = enigmaPlayer;
        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (usingPauseButton){
                    enigmaPlayer.getControls().pause();
                }else {
                    enigmaPlayer.getControls().start();
                }
            }
        });

        this.enigmaPlayer.addListener(new BaseEnigmaPlayerListener(){
            @Override
            public void onStateChanged(EnigmaPlayerState from, EnigmaPlayerState to) {
                if (to == EnigmaPlayerState.PLAYING){
                    usingPauseButton = true;
                    updatePlayPauseButtonImage();
                } else if(from == EnigmaPlayerState.PLAYING){
                    usingPauseButton = false;
                    updatePlayPauseButtonImage();
                }
            }
        }, handler);
    }

    private void updatePlayPauseButtonImage() {
        int icon = usingPauseButton ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        this.setImageResource(icon);
    }
}