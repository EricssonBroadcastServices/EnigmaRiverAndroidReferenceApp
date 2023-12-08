package com.redbeemedia.enigma.referenceapp.ui;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.timeline.ITimeline;
import com.redbeemedia.enigma.core.player.timeline.ITimelinePosition;
import com.redbeemedia.enigma.core.time.Duration;
import com.redbeemedia.enigma.core.virtualui.BaseVirtualButtonListener;
import com.redbeemedia.enigma.core.virtualui.IVirtualButton;
import com.redbeemedia.enigma.core.virtualui.IVirtualButtonListener;
import com.redbeemedia.enigma.core.virtualui.IVirtualControls;
import com.redbeemedia.enigma.core.virtualui.VirtualControlsSettings;
import com.redbeemedia.enigma.core.virtualui.impl.VirtualControls;
import com.redbeemedia.enigma.referenceapp.R;

public class CustomControlsView extends ConstraintLayout {
    private static final Duration SMALL_SEEK_STEP = Duration.seconds(10);
    private static final Duration BIG_SEEK_STEP = Duration.seconds(30);

    private boolean isHidden = true;
    private Handler handler;

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
        this.handler = new Handler();
        LayoutInflater.from(getContext()).inflate(R.layout.player_controls_layout, this);
        setClickable(true);
        CustomControlsView.this.setAlpha(isHidden ? 0f : 1f);
        setOnClickListener(v -> {
            isHidden = !isHidden;
            CustomControlsView.this.setAlpha(isHidden ? 0f : 1f);
        });
    }

    public View getPipButton() {
        return findViewById(R.id.pip);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev) || isHidden;
    }

    public void connectTo(final IEnigmaPlayer enigmaPlayer) {
        TimelineView timelineView = findViewById(R.id.timelineView);
        timelineView.connectTo(enigmaPlayer);

        IVirtualControls virtualControls = VirtualControls.create(enigmaPlayer, new CustomVirtualControlsSettings(enigmaPlayer));

        PlayPauseButton playPauseButton = findViewById(R.id.play_pause_button);
        playPauseButton.connectTo(virtualControls);
        connect(findViewById(R.id.seek_back_button), virtualControls.getRewind());
        connect(findViewById(R.id.seek_forward_button), virtualControls.getFastForward());
        connect(findViewById(R.id.previous_program), virtualControls.getPreviousProgram());
        connect(findViewById(R.id.next_program), virtualControls.getNextProgram());

        ((AbstractTracksSpinner) findViewById(R.id.audio_spinner)).connectTo(enigmaPlayer);
        ((AbstractTracksSpinner) findViewById(R.id.subtitles_spinner)).connectTo(enigmaPlayer);
        ((AbstractTracksSpinner) findViewById(R.id.video_spinner)).connectTo(enigmaPlayer);
    }

    private void connect(View view, IVirtualButton virtualButton) {
        view.setOnClickListener(v -> virtualButton.click());

        final IVirtualButtonListener buttonListener = new BaseVirtualButtonListener() {
            @Override
            public void onStateChanged() {
                view.setEnabled(virtualButton.isEnabled());
                view.setVisibility(virtualButton.isRelevant() ? VISIBLE : GONE);
            }
        };

        view.addOnAttachStateChangeListener(new VirtualButtonViewAttacher(virtualButton, buttonListener, handler));
    }

    private static class CustomVirtualControlsSettings extends VirtualControlsSettings {
        private final ITimeline timeline;

        public CustomVirtualControlsSettings(IEnigmaPlayer enigmaPlayer) {
            this.timeline = enigmaPlayer.getTimeline();
        }

        @Override
        public Duration getSeekBackwardStep() {
            return getSeekStep();
        }

        @Override
        public Duration getSeekForwardStep() {
            return getSeekStep();
        }

        private Duration getSeekStep() {
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
}
