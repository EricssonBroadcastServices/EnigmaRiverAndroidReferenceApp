package com.redbeemedia.enigma.referenceapp.ui;

import android.content.Context;
import android.util.AttributeSet;

import com.redbeemedia.enigma.core.playbacksession.BasePlaybackSessionListener;
import com.redbeemedia.enigma.core.playbacksession.IPlaybackSession;
import com.redbeemedia.enigma.core.playbacksession.IPlaybackSessionListener;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.subtitle.ISubtitleTrack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SubtitleTracksSpinner extends AbstractTracksSpinner<ISubtitleTrack> {
    public SubtitleTracksSpinner(Context context) {
        super(context);
    }

    public SubtitleTracksSpinner(Context context, int mode) {
        super(context, mode);
    }

    public SubtitleTracksSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected ItemAdapter<ISubtitleTrack> wrapItem(ISubtitleTrack item) {
        return new ItemAdapter<ISubtitleTrack>(item) {
            @Override
            protected String getLabel(ISubtitleTrack obj) {
                return obj != null ? obj.getLanguageCode() : "No subtitles";
            }
        };
    }

    @Override
    protected IPlaybackSessionListener newPlaybackSessionListener() {
        return new BasePlaybackSessionListener() {
            @Override
            public void onSubtitleTracks(List<ISubtitleTrack> tracks) {
                onTracks(tracks);
            }

            @Override
            public void onSelectedSubtitleTrackChanged(ISubtitleTrack oldSelectedTrack, ISubtitleTrack newSelectedTrack) {
                onSelectionChanged(newSelectedTrack);
            }
        };
    }

    @Override
    protected void onTracks(List<ISubtitleTrack> newTracks) {
        List<ISubtitleTrack> options = new ArrayList<>();
        options.add(null); //Add no subtitles option
        options.addAll(newTracks);
        super.onTracks(options);
    }

    @Override
    protected void onNewPlaybackSession(IPlaybackSession playbackSession) {
        List<ISubtitleTrack> tracks = playbackSession.getSubtitleTracks();
        if(tracks != null) {
            onTracks(tracks);
        } else {
            onTracks(Collections.emptyList());
        }
    }

    @Override
    protected void onSelected(IEnigmaPlayer enigmaPlayer, ISubtitleTrack object) {
        enigmaPlayer.getControls().setSubtitleTrack(object);
    }
}
