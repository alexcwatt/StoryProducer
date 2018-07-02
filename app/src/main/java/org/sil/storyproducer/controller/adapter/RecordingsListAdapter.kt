package org.sil.storyproducer.controller.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.media.MediaPlayer
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.Modal
import org.sil.storyproducer.controller.draft.DraftFrag
import org.sil.storyproducer.controller.draft.DraftListRecordingsModal
import org.sil.storyproducer.controller.dramatization.DramaListRecordingsModal
import org.sil.storyproducer.controller.remote.BackTranslationListRecordingsModal
import org.sil.storyproducer.model.StoryState
import org.sil.storyproducer.tools.StorySharedPreferences
import org.sil.storyproducer.tools.file.AudioFiles
import org.sil.storyproducer.tools.media.AudioPlayer

/**
 * This class handles the layout inflation for an audio recording list
 */

class RecordingsListAdapter(context: Context, private val values: Array<String>, private val listeners: ClickListeners) : ArrayAdapter<String>(context, -1, values) {
    private var deleteTitle: String? = null
    private var deleteMessage: String? = null

    interface ClickListeners {
        fun onRowClick(name: String)
        fun onPlayClick(name: String, buttonClickedNow: ImageButton)
        fun onDeleteClick(name: String)
        fun onRenameClick(name: String, newName: String): AudioFiles.RenameCode
        fun onRenameSuccess()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = inflater.inflate(R.layout.audio_comment_list_item, parent, false)
        val titleView = rowView.findViewById<TextView>(R.id.audio_comment_title)
        val playButton = rowView.findViewById<ImageButton>(R.id.audio_comment_play_button)
        val deleteButton = rowView.findViewById<ImageButton>(R.id.audio_comment_delete_button)

        titleView.text = values[position]

        //things specifically for the modals
        if (listeners is DramaListRecordingsModal || listeners is DraftListRecordingsModal
                || listeners is BackTranslationListRecordingsModal) {
            rowView.setOnClickListener { listeners.onRowClick(values[position]) }
            titleView.setOnClickListener { listeners.onRowClick(values[position]) }
            if (listeners is DraftListRecordingsModal && StorySharedPreferences.getDraftForSlideAndStory(slidePosition, StoryState.getStoryName()) == values[position]) {
                setUiForSelectedView(rowView, deleteButton, playButton)
            }
            if (listeners is DramaListRecordingsModal && StorySharedPreferences.getDramatizationForSlideAndStory(slidePosition, StoryState.getStoryName()) == values[position]) {
                setUiForSelectedView(rowView, deleteButton, playButton)
            }
            if (listeners is BackTranslationListRecordingsModal && StorySharedPreferences.getBackTranslationForSlideAndStory(slidePosition, StoryState.getStoryName()) == values[position]) {
                setUiForSelectedView(rowView, deleteButton, playButton)
            }
        }

        playButton.setOnClickListener { listeners.onPlayClick(values[position], playButton) }

        deleteButton.setOnClickListener { showDeleteItemDialog(position) }

        titleView.setOnLongClickListener {
            showItemRenameDialog(position)
            true
        }
        return rowView
    }

    private fun setUiForSelectedView(rowView: View, deleteButton: ImageButton, playButton: ImageButton) {
        rowView.setBackgroundColor(context.resources.getColor(android.R.color.holo_blue_light))
        deleteButton.setBackgroundColor(context.resources.getColor(android.R.color.holo_blue_light))      //have to set the background here as well so the corners are the right color
        playButton.setBackgroundColor(context.resources.getColor(android.R.color.holo_blue_light))
    }

    /**
     * Shows a dialog to the user asking if they really want to delete the recording
     *
     * @param position the integer position of the recording where the button was pressed
     */
    private fun showDeleteItemDialog(position: Int) {
        val dialog = AlertDialog.Builder(context)
                .setTitle(deleteTitle)
                .setMessage(deleteMessage)
                .setNegativeButton(context.getString(R.string.no), null)
                .setPositiveButton(context.getString(R.string.yes)) { dialog, id -> listeners.onDeleteClick(values[position]) }.create()

        dialog.show()
    }

    fun setDeleteTitle(title: String) {
        deleteTitle = title
    }

    fun setDeleteMessage(message: String) {
        deleteMessage = message
    }

    /**
     * Show to the user a dialog to rename the audio comment
     *
     * @param position the integer position of the comment the user "long-clicked"
     */
    private fun showItemRenameDialog(position: Int) {
        val newName = EditText(context)

        // Programmatically set layout properties for edit text field
        val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
        // Apply layout properties
        newName.layoutParams = params

        val dialog = AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.rename_title))
                .setView(newName)
                .setNegativeButton(context.getString(R.string.cancel), null)
                .setPositiveButton(context.getString(R.string.save)) { dialog, id ->
                    val newNameText = newName.text.toString()
                    val returnCode = listeners.onRenameClick(values[position], newName.text.toString())
                    when (returnCode) {

                        AudioFiles.RenameCode.SUCCESS -> {
                            listeners.onRenameSuccess()
                            Toast.makeText(getContext(), context.resources.getString(R.string.renamed_success), Toast.LENGTH_SHORT).show()
                        }
                        AudioFiles.RenameCode.ERROR_LENGTH -> Toast.makeText(getContext(), context.resources.getString(R.string.rename_must_be_20), Toast.LENGTH_SHORT).show()
                        AudioFiles.RenameCode.ERROR_SPECIAL_CHARS -> Toast.makeText(getContext(), context.resources.getString(R.string.rename_no_special), Toast.LENGTH_SHORT).show()
                        AudioFiles.RenameCode.ERROR_UNDEFINED -> Toast.makeText(getContext(), context.resources.getString(R.string.rename_failed), Toast.LENGTH_SHORT).show()
                    }
                }.create()

        dialog.show()
        // show keyboard for renaming
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)

    }
}

class RecordingsListModal(private val context: Context, private val parentFragment: DraftFrag) : RecordingsListAdapter.ClickListeners, Modal {
    private var rootView: LinearLayout? = null
    private var dialog: AlertDialog? = null

    private var draftTitles: Array<String>? = null
    private var lastNewName: String? = null
    private var lastOldName: String? = null

    private val audioPlayer: AudioPlayer
    private var currentPlayingButton: ImageButton? = null

    init {
        audioPlayer = AudioPlayer()
    }

    override fun show() {
        val inflater = parentFragment.activity.layoutInflater
        rootView = inflater.inflate(R.layout.recordings_list, null) as LinearLayout

        createRecordingList()


        val tb = rootView!!.findViewById<Toolbar>(R.id.toolbar2)
        //Note that user-facing slide number is 1-based while it is 0-based in code.
        tb.setTitle(R.string.draft_recordings_title)
        val exit = rootView!!.findViewById<ImageButton>(R.id.exitButton)

        val alertDialog = AlertDialog.Builder(context)
        alertDialog.setView(rootView)
        dialog = alertDialog.create()
        exit.setOnClickListener { dialog!!.dismiss() }
        dialog!!.show()

    }

    /**
     * Updates the list of draft recordings at beginning of fragment creation and after any list change
     */
    private fun createRecordingList() {
        val listView = rootView!!.findViewById<ListView>(R.id.recordings_list)
        listView.isScrollbarFadingEnabled = false
        val adapter = RecordingsListAdapter(context, draftTitles, slidePosition, this)
        adapter.setDeleteTitle(context.resources.getString(R.string.delete_draft_title))
        adapter.setDeleteMessage(context.resources.getString(R.string.delete_draft_message))
        listView.adapter = adapter
    }

    override fun onRowClick(recordingTitle: String) {
        StorySharedPreferences.setDraftForSlideAndStory(recordingTitle, slidePosition, StoryState.getStoryName())
        parentFragment.updatePlayBackPath()
        dialog!!.dismiss()
    }

    override fun onPlayClick(recordingTitle: String, buttonClickedNow: ImageButton) {
        parentFragment.stopPlayBackAndRecording()
        if (audioPlayer.isAudioPlaying && currentPlayingButton == buttonClickedNow) {
            currentPlayingButton!!.setImageResource(R.drawable.ic_green_play)
            audioPlayer.stopAudio()
        } else {
            if (audioPlayer.isAudioPlaying) {
                currentPlayingButton!!.setImageResource(R.drawable.ic_green_play)
                audioPlayer.stopAudio()
            }
            currentPlayingButton = buttonClickedNow
            currentPlayingButton!!.setImageResource(R.drawable.ic_stop_red)
            audioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener { currentPlayingButton!!.setImageResource(R.drawable.ic_green_play) })
            val draftFile = AudioFiles.getDraft(StoryState.getStoryName(), slidePosition, recordingTitle)
            if (draftFile.exists()) {
                //FIXME
                //audioPlayer.setSource(draftFile.getPath());
                audioPlayer.playAudio()
                Toast.makeText(parentFragment.context, context.getString(R.string.draft_playing_draft), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(parentFragment.context, context.getString(R.string.draft_no_draft_found), Toast.LENGTH_SHORT).show()
            }

        }
    }

    override fun onDeleteClick(recordingTitle: String) {
        AudioFiles.deleteDraft(StoryState.getStoryName(), slidePosition, recordingTitle)
        createRecordingList()
        if (StorySharedPreferences.getDraftForSlideAndStory(slidePosition, StoryState.getStoryName()) == recordingTitle) {        //deleted the selected file
            if (draftTitles!!.size > 0) {
                StorySharedPreferences.setDraftForSlideAndStory(draftTitles!![draftTitles!!.size - 1], slidePosition, StoryState.getStoryName())
            } else {
                StorySharedPreferences.setDraftForSlideAndStory("", slidePosition, StoryState.getStoryName())       //no stories to set it to
                parentFragment.hideButtonsToolbar()
            }

        }
        parentFragment.updatePlayBackPath()
    }

    override fun onRenameClick(name: String, newName: String): AudioFiles.RenameCode {
        lastOldName = name
        lastNewName = newName
        return AudioFiles.renameDraft(StoryState.getStoryName(), slidePosition, name, newName)
    }

    override fun onRenameSuccess() {
        createRecordingList()
        if (StorySharedPreferences.getDraftForSlideAndStory(slidePosition, StoryState.getStoryName()) == lastOldName) {
            StorySharedPreferences.setDraftForSlideAndStory(lastNewName, slidePosition, StoryState.getStoryName())
        }
        parentFragment.updatePlayBackPath()
    }
}
