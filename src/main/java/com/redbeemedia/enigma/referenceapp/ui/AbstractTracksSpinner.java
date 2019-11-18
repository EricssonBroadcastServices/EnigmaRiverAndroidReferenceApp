package com.redbeemedia.enigma.referenceapp.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.widget.AppCompatSpinner;

import com.redbeemedia.enigma.core.playbacksession.IPlaybackSession;
import com.redbeemedia.enigma.core.playbacksession.IPlaybackSessionListener;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.listener.BaseEnigmaPlayerListener;
import com.redbeemedia.enigma.core.util.AndroidThreadUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractTracksSpinner<T> extends AppCompatSpinner {
    private static final String TAG = "AbstractTracksSpinner";

    private final List<ItemAdapter<T>> tracks = new ArrayList<>();
    private ArrayAdapter adapter;
    private IEnigmaPlayer enigmaPlayer;

    public AbstractTracksSpinner(Context context) {
        super(context);
        init();
    }

    public AbstractTracksSpinner(Context context, int mode) {
        super(context, mode);
        init();
    }

    public AbstractTracksSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    protected void init() {
        setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(enigmaPlayer != null) {
                    try {
                        onSelected(enigmaPlayer, tracks.get(position).object);
                    } catch (IndexOutOfBoundsException e) {
                        e.printStackTrace(); //Log and ignore.
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //TODO
            }
        });
        this.adapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_dropdown_item, tracks);
        setAdapter(adapter);
    }

    protected abstract ItemAdapter<T> wrapItem(T item);

    protected void onTracks(List<T> newTracks) {
        tracks.clear();
        for(T track : newTracks) {
            tracks.add(wrapItem(track));
        }
        AndroidThreadUtil.runOnUiThread(() -> adapter.notifyDataSetChanged());
    }

    private static <U> boolean equalsTypeSafe(U a, U b) {
        return Objects.equals(a, b);
    }

    protected void onSelectionChanged(T selected) {
        int selectedIndex = -1;
        for(int i = 0; i < tracks.size(); ++i) {
            if(AbstractTracksSpinner.<T>equalsTypeSafe(tracks.get(i).object, selected)) {
                selectedIndex = i;
                break;
            }
        }
        if(selectedIndex != -1) {
            setSelection(selectedIndex);
        } else {
            Log.d(TAG, "Could not find index of "+selected);
        }
    }

    public void connectTo(IEnigmaPlayer enigmaPlayer) {
        this.enigmaPlayer = enigmaPlayer;
        enigmaPlayer.addListener(new BaseEnigmaPlayerListener() {
            private IPlaybackSessionListener playbackSessionListener = newPlaybackSessionListener();

            @Override
            public void onPlaybackSessionChanged(IPlaybackSession from, IPlaybackSession to) {
                if(from != null) {
                    from.removeListener(playbackSessionListener);
                }
                if(to != null) {
                    onNewPlaybackSession(to);
                    to.addListener(playbackSessionListener);
                }
            }
        });
    }

    protected abstract IPlaybackSessionListener newPlaybackSessionListener();

    protected abstract void onNewPlaybackSession(IPlaybackSession playbackSession);

    protected abstract void onSelected(IEnigmaPlayer enigmaPlayer, T object);

    public abstract static class ItemAdapter<T> {
        public final T object;

        public ItemAdapter(T object) {
            this.object = object;
        }

        @Override
        public String toString() {
            return getLabel(object);
        }

        protected abstract String getLabel(T obj);
    }
}
