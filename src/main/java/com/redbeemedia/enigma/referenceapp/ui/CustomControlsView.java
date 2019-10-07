package com.redbeemedia.enigma.referenceapp.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.timeline.ITimeline;
import com.redbeemedia.enigma.core.player.timeline.ITimelinePosition;
import com.redbeemedia.enigma.core.time.Duration;
import com.redbeemedia.enigma.referenceapp.R;

public class CustomControlsView extends ConstraintLayout {
    private static final Duration SMALL_SEEK_STEP = Duration.seconds(10);
    private static final Duration BIG_SEEK_STEP = Duration.seconds(30);

    private boolean isHidden = true;

    public CustomControlsView(Context context) {
        super(context);
        init();
    }

    public CustomControlsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomControlsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    protected void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.player_controls_layout, this);
        setClickable(true);
        CustomControlsView.this.setAlpha(isHidden ? 0f : 1f);
        setOnClickListener(v -> {
            isHidden = !isHidden;
            CustomControlsView.this.setAlpha(isHidden ? 0f : 1f);
        });
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev) || isHidden;
    }

    public void connectTo(final IEnigmaPlayer enigmaPlayer) {
        TimelineView timelineView = findViewById(R.id.timelineView);
        timelineView.connectTo(enigmaPlayer);

        PlayPauseButton playPauseButton = findViewById(R.id.play_pause_button);
        playPauseButton.connectTo(enigmaPlayer);

        findViewById(R.id.seek_back_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ITimelinePosition currentPosition = enigmaPlayer.getTimeline().getCurrentPosition();
                if(currentPosition != null) {
                    enigmaPlayer.getControls().seekTo(currentPosition.subtract(getSeekStep(enigmaPlayer.getTimeline())));
                }
            }
        });

        findViewById(R.id.seek_forward_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ITimelinePosition currentPosition = enigmaPlayer.getTimeline().getCurrentPosition();
                if(currentPosition != null) {
                    enigmaPlayer.getControls().seekTo(currentPosition.add(getSeekStep(enigmaPlayer.getTimeline())));
                }
            }
        });

        ((AbstractTracksSpinner) findViewById(R.id.audio_spinner)).connectTo(enigmaPlayer);
        ((AbstractTracksSpinner) findViewById(R.id.subtitles_spinner)).connectTo(enigmaPlayer);
    }

    private static Duration getSeekStep(ITimeline timeline) {
        ITimelinePosition startBound = timeline.getCurrentStartBound();
        ITimelinePosition endBound = timeline.getCurrentEndBound();
        if(startBound != null && endBound != null) {
            if(endBound.subtract(startBound).inUnits(Duration.Unit.MINUTES) < 10) {
                return SMALL_SEEK_STEP;
            } else {
                return BIG_SEEK_STEP;
            }
        } else {
            return BIG_SEEK_STEP;
        }
    }
}
