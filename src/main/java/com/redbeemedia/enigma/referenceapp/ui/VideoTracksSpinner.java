package com.redbeemedia.enigma.referenceapp.ui;

import android.content.Context;
import android.util.AttributeSet;

import com.redbeemedia.enigma.core.playbacksession.BasePlaybackSessionListener;
import com.redbeemedia.enigma.core.playbacksession.IPlaybackSession;
import com.redbeemedia.enigma.core.playbacksession.IPlaybackSessionListener;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.video.IVideoTrack;

import java.util.Collections;
import java.util.List;

public class VideoTracksSpinner extends AbstractTracksSpinner<IVideoTrack> {
    private boolean toggle = true;
    public VideoTracksSpinner(Context context) {
        super(context);
    }

    public VideoTracksSpinner(Context context, int mode) {
        super(context, mode);
    }

    public VideoTracksSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected ItemAdapter<IVideoTrack> wrapItem(IVideoTrack item) {
        return new ItemAdapter<IVideoTrack>(item) {
            @Override
            protected String getLabel(IVideoTrack obj) {
                if (obj != null) {
                    float i = (float)obj.getBitrate() / 1000000.0f;
                    return (i + " mbps");
                } else {
                    return "None";
                }
            }
        };
    }

    @Override
    protected IPlaybackSessionListener newPlaybackSessionListener() {
        return new BasePlaybackSessionListener() {
            @Override
            public void onVideoTracks(List<IVideoTrack> tracks) {
                onTracks(tracks);
            }

            @Override
            public void onSelectedVideoTrackChanged(IVideoTrack oldSelectedTrack, IVideoTrack newSelectedTrack) {
                onSelectionChanged(newSelectedTrack);
            }
        };
    }

    @Override
    protected void onNewPlaybackSession(IPlaybackSession playbackSession) {
        List<IVideoTrack> tracks = playbackSession.getVideoTracks();
        if(tracks != null) {
            onTracks(tracks);
        } else {
            onTracks(Collections.emptyList());
        }
    }

    @Override
    protected void onSelected(IEnigmaPlayer enigmaPlayer, IVideoTrack object) {
        enigmaPlayer.getControls().setMaxVideoTrackDimensions(object);
    }
}
