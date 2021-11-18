package com.redbeemedia.enigma.referenceapp.ui;

import android.content.Context;
import android.util.AttributeSet;

import com.redbeemedia.enigma.core.audio.IAudioTrack;
import com.redbeemedia.enigma.core.playbacksession.BasePlaybackSessionListener;
import com.redbeemedia.enigma.core.playbacksession.IPlaybackSession;
import com.redbeemedia.enigma.core.playbacksession.IPlaybackSessionListener;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;

import java.util.Collections;
import java.util.List;

public class AudioTracksSpinner extends AbstractTracksSpinner<IAudioTrack> {
    public AudioTracksSpinner(Context context) {
        super(context);
    }

    public AudioTracksSpinner(Context context, int mode) {
        super(context, mode);
    }

    public AudioTracksSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected ItemAdapter<IAudioTrack> wrapItem(IAudioTrack item) {
        return new ItemAdapter<IAudioTrack>(item) {
            @Override
            protected String getLabel(IAudioTrack obj) {
                return obj != null ? obj.getLabel() : "None";
            }
        };
    }

    @Override
    protected IPlaybackSessionListener newPlaybackSessionListener() {
        return new BasePlaybackSessionListener() {
            @Override
            public void onAudioTracks(List<IAudioTrack> tracks) {
                onTracks(tracks);
            }

            @Override
            public void onSelectedAudioTrackChanged(IAudioTrack oldSelectedTrack, IAudioTrack newSelectedTrack) {
                onSelectionChanged(newSelectedTrack);
            }
        };
    }

    @Override
    protected void onNewPlaybackSession(IPlaybackSession playbackSession) {
        List<IAudioTrack> tracks = playbackSession.getAudioTracks();
        if(tracks != null) {
            onTracks(tracks);
        } else {
            onTracks(Collections.emptyList());
        }
    }

    @Override
    protected void onSelected(IEnigmaPlayer enigmaPlayer, IAudioTrack object) {
        enigmaPlayer.getControls().setAudioTrack(object);
    }
}
