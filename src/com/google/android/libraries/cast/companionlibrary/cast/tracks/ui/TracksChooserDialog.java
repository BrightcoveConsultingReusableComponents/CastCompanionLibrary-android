/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.libraries.cast.companionlibrary.cast.tracks.ui;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaTrack;
import com.google.android.libraries.cast.companionlibrary.R;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.utils.Utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * A dialog to show the available tracks (Text and Audio) for user to select.
 */
public class TracksChooserDialog extends DialogFragment {

    // TODO: make this a MaterialDialog like the one used in PlayerActivity
    // this would be a met-specific enhancement. discard audiotracks.

    private VideoCastManager mCastManager;
    private long[] mActiveTracks = null;
    private MediaInfo mMediaInfo;
    private TracksListAdapter mTextAdapter;
    private TracksListAdapter mAudioVideoAdapter;
    private List<MediaTrack> mTextTracks = new ArrayList<>();
    private List<MediaTrack> mAudioTracks = new ArrayList<>();
    private static final long TEXT_TRACK_NONE_ID = -1;
    private int mSelectedTextPosition = 0;
    private int mSelectedAudioPosition = -1;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.custom_tracks_dialog_layout, null);
        setUpView(view);

        builder.title(R.string.ccl_caption_subtitles)
                .customView(view, false)
                .positiveText(getString(R.string.ccl_ok))
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        List<MediaTrack> selectedTracks = new ArrayList<>();
                        MediaTrack textTrack = mTextAdapter.getSelectedTrack();
                        if (textTrack.getId() != TEXT_TRACK_NONE_ID) {
                            selectedTracks.add(textTrack);
                        }
                        mCastManager.notifyTracksSelectedListeners(selectedTracks);
                        dismiss();
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                        dismiss();
                    }
                })
                .negativeText(R.string.ccl_cancel);

        return builder.build();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Bundle mediaWrapper = getArguments().getBundle(VideoCastManager.EXTRA_MEDIA);
        mMediaInfo = Utils.bundleToMediaInfo(mediaWrapper);
        mCastManager = VideoCastManager.getInstance();
        mActiveTracks = mCastManager.getActiveTrackIds();
        List<MediaTrack> allTracks = mMediaInfo.getMediaTracks();
        if (allTracks == null || allTracks.isEmpty()) {
            Utils.showToast(getActivity(), R.string.ccl_caption_no_tracks_available);
            dismiss();
        }
    }

    /**
     * This is to get around the following bug:
     * https://code.google.com/p/android/issues/detail?id=17423
     */
    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    private void setUpView(View view) {
        ListView listView = (ListView) view.findViewById(R.id.listview);
        partitionTracks();

        mTextAdapter = new TracksListAdapter(getActivity(), R.layout.tracks_row_layout,
                mTextTracks, mSelectedTextPosition);

        listView.setAdapter(mTextAdapter);
    }

    private MediaTrack buildNoneTrack() {
        return new MediaTrack.Builder(TEXT_TRACK_NONE_ID, MediaTrack.TYPE_TEXT)
                .setName(getString(R.string.ccl_none))
                .setSubtype(MediaTrack.SUBTYPE_CAPTIONS)
                .setContentId("").build();
    }

    /**
     * This method loops through the tracks and partitions them into a group of Text tracks and a
     * group of Audio tracks, and skips over the Video tracks.
     */
    private void partitionTracks() {
        List<MediaTrack> allTracks = mMediaInfo.getMediaTracks();
        mAudioTracks.clear();
        mTextTracks.clear();
        mTextTracks.add(buildNoneTrack());
        mSelectedTextPosition = 0;
        mSelectedAudioPosition = -1;
        if (allTracks != null) {
            int textPosition = 1; /* start from 1 since we have a NONE selection at the beginning */
            int audioPosition = 0;
            for (MediaTrack track : allTracks) {
                switch (track.getType()) {
                    case MediaTrack.TYPE_TEXT:
                        mTextTracks.add(track);
                        if (mActiveTracks != null) {
                            for (long mActiveTrack : mActiveTracks) {
                                if (mActiveTrack == track.getId()) {
                                    mSelectedTextPosition = textPosition;
                                }
                            }
                        }
                        textPosition++;
                        break;
                    case MediaTrack.TYPE_AUDIO:
                        mAudioTracks.add(track);
                        if (mActiveTracks != null) {
                            for (long mActiveTrack : mActiveTracks) {
                                if (mActiveTrack == track.getId()) {
                                    mSelectedAudioPosition = audioPosition;
                                }
                            }
                        }
                        audioPosition++;
                        break;
                }
            }
        }
    }

    /**
     * Call this static method to create a new instance of the dialog.
     */
    public static TracksChooserDialog newInstance(MediaInfo mediaInfo) {
        TracksChooserDialog fragment = new TracksChooserDialog();
        Bundle bundle = new Bundle();
        bundle.putBundle(VideoCastManager.EXTRA_MEDIA, Utils.mediaInfoToBundle(mediaInfo));
        fragment.setArguments(bundle);
        return fragment;
    }
}
