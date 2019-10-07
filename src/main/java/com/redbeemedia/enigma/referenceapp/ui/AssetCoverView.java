package com.redbeemedia.enigma.referenceapp.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.redbeemedia.enigma.core.playbacksession.BasePlaybackSessionListener;
import com.redbeemedia.enigma.core.playbacksession.IPlaybackSession;
import com.redbeemedia.enigma.core.playbacksession.IPlaybackSessionListener;
import com.redbeemedia.enigma.core.player.EnigmaPlayerState;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.listener.BaseEnigmaPlayerListener;
import com.redbeemedia.enigma.core.util.AndroidThreadUtil;
import com.redbeemedia.enigma.referenceapp.R;

public class AssetCoverView extends View {
    private static final String LOADING = "Loading...";
    private static final String STREAM_ENDED = "Stream ended";
    private static final String ENJOY = "Enjoy!";

    private Paint paint;
    private float coverAmount = 1;
    private Handler handler;
    private String text = ".";
    private final IPlaybackSessionListener streamEndListener = new BasePlaybackSessionListener() {
        @Override
        public void onEndReached() {
            text = STREAM_ENDED;
            invalidate();
        }
    };

    public AssetCoverView(Context context) {
        super(context);
        init();
    }

    public AssetCoverView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AssetCoverView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public AssetCoverView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    protected void init() {
        setWillNotDraw(false);
        this.paint = new Paint();
        this.handler = new Handler();
    }

    public void connectTo(IEnigmaPlayer enigmaPlayer) {
        enigmaPlayer.addListener(new BaseEnigmaPlayerListener() {
            @Override
            public void onStateChanged(EnigmaPlayerState from, EnigmaPlayerState to) {
                if(to == EnigmaPlayerState.LOADED) {
                    animateCoverAmountTo(0f, 1000);
                } else if(to == EnigmaPlayerState.LOADING || to == EnigmaPlayerState.IDLE) {
                    animateCoverAmountTo(1f, 200);
                    if(to == EnigmaPlayerState.LOADING) {
                        text = LOADING;
                    }
                    invalidate();
                }
            }

            @Override
            public void onPlaybackSessionChanged(IPlaybackSession from, IPlaybackSession to) {
                if(from != null) {
                    from.removeListener(streamEndListener);
                }
                if(to != null) {
                    runWhenLoaded(enigmaPlayer, () -> {
                        text = ENJOY;
                        invalidate();
                    });
                    to.addListener(streamEndListener, handler);
                }
            }
        }, handler);


    }

    private void runWhenLoaded(IEnigmaPlayer enigmaPlayer, Runnable runnable) {
        EnigmaPlayerState state = enigmaPlayer.getState();
        enigmaPlayer.addListener(new BaseEnigmaPlayerListener() {
            @Override
            public void onStateChanged(EnigmaPlayerState from, EnigmaPlayerState to) {
                if(to == EnigmaPlayerState.LOADED) {
                    runnable.run();
                    enigmaPlayer.removeListener(this);
                }
            }
        }, handler);
    }

    private void animateCoverAmountTo(float newCoverAmount, long duration) {
        ValueAnimator valueAnimator = new ValueAnimator().setDuration(duration);
        valueAnimator.setInterpolator(new AccelerateInterpolator());
        valueAnimator.setFloatValues(coverAmount,newCoverAmount);
        valueAnimator.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            setCoverAmount(t);
        });
        valueAnimator.start();
    }

    public void setCoverAmount(float newCoverAmount) {
        if(newCoverAmount != coverAmount) {
            coverAmount = newCoverAmount;
            setTranslationY(-(1f-coverAmount)*getMeasuredHeight());
            if(AndroidThreadUtil.isOnUiThread()) {
                invalidate();
            } else {
                postInvalidate();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        paint.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        paint.setStyle(Paint.Style.FILL);
        Rect viewBounds = canvas.getClipBounds();
        canvas.drawRect(viewBounds, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(viewBounds.height()/10);
        paint.setSubpixelText(true);
        paint.setAntiAlias(true);
        float textWidth = paint.measureText(text);
        canvas.drawText(text, viewBounds.left+(viewBounds.width()-textWidth)/2,viewBounds.bottom-(viewBounds.height()-paint.getTextSize())/2, paint);
    }
}
