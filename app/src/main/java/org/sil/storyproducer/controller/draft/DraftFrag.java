package org.sil.storyproducer.controller.draft;

import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.SlideText;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.BitmapScaler;
import org.sil.storyproducer.tools.StorySharedPreferences;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.file.ImageFiles;
import org.sil.storyproducer.tools.file.TextFiles;
import org.sil.storyproducer.tools.media.AudioPlayer;
import org.sil.storyproducer.tools.media.wavaudio.WavAudioRecorder;
import org.sil.storyproducer.tools.toolbar.RecordingToolbar;
import org.sil.storyproducer.tools.toolbar.RecordingToolbar.RecordingListener;


import java.io.File;

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
public class DraftFrag extends Fragment {
    private View rootView;
    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";
    private int slideNumber;
    private SlideText slideText;
    private AudioPlayer narrationAudioPlayer;
    private String narrationFilePath;
    private String recordFilePath;
    private ImageButton narrationPlayButton;
    private TextView slideNumberText;
    private RecordingToolbar recordingToolbar;

    private WavAudioRecorder wavAudioRecorder;

    public DraftFrag() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle passedArgs = this.getArguments();

        slideNumber = passedArgs.getInt(SLIDE_NUM);
        slideText = TextFiles.getSlideText(StoryState.getStoryName(), slideNumber);
        setRecordFilePath();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        rootView = inflater.inflate(R.layout.fragment_draft, container, false);
        View rootViewToolbar = inflater.inflate(R.layout.toolbar_for_recording, container, false);

        setUiColors();
        setPic(rootView.findViewById(R.id.fragment_draft_image_view), slideNumber);
        setScriptureText(rootView.findViewById(R.id.fragment_draft_scripture_text));
        setReferenceText(rootView.findViewById(R.id.fragment_draft_reference_text));
        setNarrationButton(rootView.findViewById(R.id.fragment_draft_narration_button));
        setToolbar(rootViewToolbar);
        slideNumberText = (TextView) rootView.findViewById(R.id.slide_number_text);
        slideNumberText.setText(slideNumber + 1 + "");

        return rootView;
    }

    /**
     * This function serves to handle draft page changes and stops the audio streams from
     * continuing.
     *
     * @param isVisibleToUser
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // Make sure that we are currently visible
        if (this.isVisible()) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                if (recordingToolbar != null) {
                    recordingToolbar.onClose();
                }
            }
        }
    }

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on pause.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (recordingToolbar != null) {
            recordingToolbar.onClose();
        }
    }

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on stop.
     */
    @Override
    public void onStop() {
        super.onStop();
        if (recordingToolbar != null) {
            recordingToolbar.onClose();
        }
    }

    /**
     * Used to hide the play and multiple recordings button.
     */
    public void hideButtonsToolbar(){
        recordingToolbar.hideButtons();
    }

    /**
     * sets the playback path
     */
    public void setPlayBackPath() {
        String playBackFilePath = AudioFiles.getDraft(StoryState.getStoryName(), slideNumber).getPath();
        recordingToolbar.setPlaybackRecordFilePath(playBackFilePath);
    }

    /**
     * Stops the toolbar from recording or playing back media
     */
    public void stopPlayBackAndRecording() {
        recordingToolbar.stopToolbarMedia();
    }

    /**
     * This function sets the first slide of each story to the blue color in order to prevent
     * clashing of the grey starting picture.
     */
    private void setUiColors() {
        if (slideNumber == 0) {
            RelativeLayout rl = (RelativeLayout) rootView.findViewById(R.id.fragment_draft_root_relayout_layout);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            rl = (RelativeLayout) rootView.findViewById(R.id.fragment_draft_envelope);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            rl = (RelativeLayout) rootView.findViewById(R.id.fragment_draft_text_envelope);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));


            TextView tv = (TextView) rootView.findViewById(R.id.fragment_draft_scripture_text);
            tv.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            tv = (TextView) rootView.findViewById(R.id.fragment_draft_reference_text);
            tv.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
        }
    }

    /**
     * This function allows the picture to scale with the phone's screen size.
     *
     * @param aView    The ImageView that will contain the picture.
     * @param slideNum The slide number to grab the picture from the files.
     */
    private void setPic(View aView, int slideNum) {
        if (aView == null || !(aView instanceof ImageView)) {
            return;
        }

        ImageView slideImage = (ImageView) aView;
        Bitmap slidePicture = ImageFiles.getBitmap(StoryState.getStoryName(), slideNum);

        if (slidePicture == null) {
            Snackbar.make(rootView, R.string.dramatization_draft_no_picture, Snackbar.LENGTH_SHORT).show();
        }

        //Get the height of the phone.
        DisplayMetrics phoneProperties = getContext().getResources().getDisplayMetrics();
        int height = phoneProperties.heightPixels;
        double scalingFactor = 0.4;
        height = (int) (height * scalingFactor);

        //scale bitmap
        slidePicture = BitmapScaler.scaleToFitHeight(slidePicture, height);

        //Set the height of the image view
        slideImage.getLayoutParams().height = height;
        slideImage.requestLayout();

        slideImage.setImageBitmap(slidePicture);
    }

    /**
     * Sets the main text of the layout.
     *
     * @param aView The text view that will be filled with the verse's text.
     */
    private void setScriptureText(View aView) {
        if (aView == null || !(aView instanceof TextView)) {
            return;
        }
        TextView textView = (TextView) aView;
        textView.setText(slideText.getContent());
    }

    /**
     * This function sets the reference text.
     *
     * @param aView The view that will be populated with the reference text.
     */
    private void setReferenceText(View aView) {
        if (aView == null || !(aView instanceof TextView)) {
            return;
        }
        TextView textView = (TextView) aView;

        String[] titleNamePriority = new String[]{slideText.getReference(),
                slideText.getSubtitle(), slideText.getTitle()};

        for (String title : titleNamePriority) {
            if (title != null && !title.equals("")) {
                textView.setText(title);
                return;
            }
        }

        textView.setText(R.string.draft_bible_story);
    }

    /**
     * This function sets the narration playback to the correct audio file. Also, the narration
     * button will have a listener added to it in order to detect playback when pressed.
     *
     * @param aView
     */
    private void setNarrationButton(View aView) {
        if (aView == null || !(aView instanceof ImageButton)) {
            return;
        }
        narrationFilePath = AudioFiles.getLWC(StoryState.getStoryName(), slideNumber).getPath();
        narrationPlayButton = (ImageButton) aView;
        narrationPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (narrationFilePath == null) {
                    Snackbar.make(rootView, R.string.draft_playback_no_narration_audio, Snackbar.LENGTH_SHORT).show();
                } else {
                    if (narrationAudioPlayer != null && narrationAudioPlayer.isAudioPlaying()) {
                        narrationAudioPlayer.stopAudio();
                        narrationAudioPlayer.releaseAudio();
                        narrationPlayButton.setBackgroundResource(R.drawable.ic_menu_play);
                    } else {
                        //stop other playback streams.
                        recordingToolbar.stopToolbarMedia();
                        narrationAudioPlayer = new AudioPlayer();
                        narrationAudioPlayer.audioCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                narrationAudioPlayer.releaseAudio();
                                narrationPlayButton.setBackgroundResource(R.drawable.ic_menu_play);
                            }
                        });
                        narrationAudioPlayer.playWithPath(narrationFilePath);
                        if (recordingToolbar != null) {
                            recordingToolbar.onToolbarTouchStopAudio(narrationPlayButton, R.drawable.ic_menu_play, narrationAudioPlayer);
                        }
                        narrationPlayButton.setBackgroundResource(R.drawable.ic_stop_white_36dp);
                        Toast.makeText(getContext(), R.string.draft_playback_narration_audio, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void setRecordFilePath() {
        int nextDraftIndex = AudioFiles.getDraftTitles(StoryState.getStoryName(), slideNumber).length + 1;
        File recordFile = AudioFiles.getDraft(StoryState.getStoryName(), slideNumber,
                "Draft " + nextDraftIndex);
        while (recordFile.exists()) {
            nextDraftIndex++;
            recordFile = AudioFiles.getDraft(StoryState.getStoryName(), slideNumber,
                    "Draft " + nextDraftIndex);
        }
        recordFilePath = recordFile.getPath();
    }

    /**
     * Initializes the toolbar and toolbar buttons.
     */
    private void setToolbar(View toolbar) {
        if (rootView instanceof RelativeLayout) {
            String playBackFilePath = AudioFiles.getDraft(StoryState.getStoryName(), slideNumber).getPath();
            RecordingListener recordingListener = new RecordingListener() {
                @Override
                public void stoppedRecording() {
                    String[] splitPath = recordFilePath.split("translation" + "\\d+" + "_");    //get just the title from the path
                    String title = splitPath[1].replace(".mp3", "");
                    StorySharedPreferences.setDraftForSlideAndStory(title, slideNumber, StoryState.getStoryName());
                    setRecordFilePath();
                    recordingToolbar.setRecordFilePath(recordFilePath);
                    setPlayBackPath();
                }

                @Override
                public void startedRecordingOrPlayback() {
                    //not used here
                }
            };
            DraftListRecordingsModal modal = new DraftListRecordingsModal(getContext(), slideNumber, this);

            recordingToolbar = new RecordingToolbar(getActivity(), toolbar, (RelativeLayout) rootView, true, false, true, playBackFilePath, recordFilePath, modal, recordingListener);
            recordingToolbar.keepToolbarVisible();
            recordingToolbar.stopToolbarMedia();
        }
    }

    //used in the DraftListRecordingsModal
    //TODO add to the area where the other public functions in this class.


}
