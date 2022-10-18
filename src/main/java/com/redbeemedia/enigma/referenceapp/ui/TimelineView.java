package com.redbeemedia.enigma.referenceapp.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.emsg.EventMessage;
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist;
import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.controls.IControlResultHandler;
import com.redbeemedia.enigma.core.player.controls.IEnigmaPlayerControls;
import com.redbeemedia.enigma.core.player.timeline.BaseTimelineListener;
import com.redbeemedia.enigma.core.player.timeline.ITimelinePosition;
import com.redbeemedia.enigma.core.player.timeline.TimelinePositionFormat;
import com.redbeemedia.enigma.core.time.Duration;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class TimelineView extends View {
    private static final TimelinePositionFormat timelinePositionFormat = TimelinePositionFormat.newFormat("${minutes}m${sec}s", new SimpleDateFormat("hh:mm:ss a"));

    private Paint paint;
    private int timelinePad = 15;

    private ITimelinePosition start;
    private ITimelinePosition end;
    private ITimelinePosition pos;

    private boolean seeking = false;
    private float seekPos;

    private boolean scrubbing = false;
    private float currentScrubPos;

    private float currentPos;
    private IEnigmaPlayerControls controls;
    private Handler handler;

    public TimelineView(Context context) {
        super(context);
        init();
    }

    public TimelineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TimelineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TimelineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    protected void init() {
        this.handler = new Handler();
        setWillNotDraw(false);
        this.paint = new Paint();
        paint.setColor(Color.RED);
        if(isInEditMode()) {
            currentPos = (float) Math.random();
        }
        setClickable(true);
        setVisibility(INVISIBLE);
    }

    private void updateScrubPosition(MotionEvent event) {
        currentScrubPos = (event.getX()-timelinePad)/(getMeasuredWidth()-timelinePad*2);
        currentScrubPos = clamp(currentScrubPos, 0, 1);
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            scrubbing = true;
            updateScrubPosition(event);
        } else if(event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if(scrubbing) {
                updateScrubPosition(event);
            }
        } else if(event.getActionMasked() == MotionEvent.ACTION_UP) {
            scrubbing = false;
            float relativeClick = (event.getX()-timelinePad)/(getMeasuredWidth()-timelinePad*2);
            relativeClick = clamp(relativeClick, 0, 1);
            if(start != null && end != null) {
                seekPos = relativeClick;
                Duration durationAfterStart = end.subtract(start).multiply(relativeClick);
                ITimelinePosition seekPosition = start.add(durationAfterStart);
                seeking = true;
                controls.seekTo(seekPosition, new OnCommandOverHandler() {
                    @Override
                    protected void onFinished() {
                        seeking = false;
                    }
                });
            }
        }
        return super.onTouchEvent(event);
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }

    public void connectTo(IEnigmaPlayer enigmaPlayer) {
        this.controls = enigmaPlayer.getControls();
        enigmaPlayer.getTimeline().addListener(new BaseTimelineListener() {
            @Override
            public void onVisibilityChanged(boolean visible) {
                TimelineView.this.setVisibility(visible ? VISIBLE: INVISIBLE);
            }

            private void recalculatePos() {
                if(start != null && end != null && pos != null) {
                    TimelineView.this.currentPos = pos.subtract(start).inUnits(Duration.Unit.MILLISECONDS)/end.subtract(start).inUnits(Duration.Unit.MILLISECONDS);
                } else {
                    TimelineView.this.currentPos = 0f;
                }
                TimelineView.this.postInvalidate();
            }

            @Override
            public void onCurrentPositionChanged(ITimelinePosition timelinePosition) {
                TimelineView.this.pos = timelinePosition;
                recalculatePos();
            }

            @Override
            public void onBoundsChanged(ITimelinePosition start, ITimelinePosition end) {
                TimelineView.this.start = start;
                TimelineView.this.end = end;
                recalculatePos();
            }

            @Override
            public void onDashMetadata(Metadata metadata) {
                // handle meta data event
                for (int i = 0; i < metadata.length(); i++) {
                    Metadata.Entry entry = metadata.get(i);
                    byte[] messageData = ((EventMessage) (entry)).messageData;
                    Log.d("Metadata-Stream", new String(messageData));
                }
            }

            @Override
            public void onHlsMetadata(HlsMediaPlaylist metadata) {
                for (String tag : metadata.tags) {
                    if (tag.toLowerCase(Locale.ROOT).contains("X-COM-DAICONNECT-TRACK".toLowerCase(Locale.ROOT))) {
                        // We only interested in DAICONNECT-TRACK
                        Log.d("Metadata-Stream", tag);
                    }
                }
            }
        }, handler);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //draw the whole bar
        paint.setColor(Color.BLACK);
        canvas.drawRect(canvas.getClipBounds(),paint);

        //draw the progress
        float progress;
        if(seeking) {
            progress = seekPos;
        } else if(scrubbing) {
            progress = currentScrubPos;
        } else {
            progress = currentPos;
        }
        onDrawProgress(canvas, progress);

        //draw time bounds
        paint.setColor(Color.WHITE);
        paint.setTextSize(1.5f*(getMeasuredHeight()-timelinePad*2)/2);
        float fontPad = (getMeasuredHeight()-paint.getFontMetrics(null)*0.8f)/2;
        if(pos != null && end != null) {
            String text = pos.toString(timelinePositionFormat)+" / "+end.toString(timelinePositionFormat);
            canvas.drawText(text, timelinePad+(getMeasuredWidth()-timelinePad*2-paint.measureText(text))/2, getMeasuredHeight()-fontPad, paint);
        }
    }

    private void onDrawProgress(Canvas canvas, float progress) {
        RectF rect = new RectF();
        rect.set(timelinePad ,timelinePad, ((int) ((getMeasuredWidth()-2*timelinePad)*progress)+timelinePad), getMeasuredHeight()-timelinePad);
        paint.setColor(Color.GREEN);
        float cornerRadius = timelinePad;
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);
    }

    private static abstract class OnCommandOverHandler implements IControlResultHandler {
        @Override
        public void onRejected(IRejectReason reason) {
            onFinished();
        }

        @Override
        public void onCancelled() {
            onFinished();
        }

        @Override
        public void onError(EnigmaError error) {
            onFinished();
        }

        @Override
        public void onDone() {
            onFinished();
        }

        protected abstract void onFinished();
    }
}