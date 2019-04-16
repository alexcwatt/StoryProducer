package org.sil.storyproducer.controller.dramatization

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MultiRecordFrag
import org.sil.storyproducer.controller.SlidePhaseFrag
import org.sil.storyproducer.controller.SlidePhaseFrag.Companion.SLIDE_NUM
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.toolbar.RecordingToolbar

class DramatizationFrag : Fragment(), RecordingToolbar.RecordingListener, MultiRecordFrag.PlaybackListener {
    private var slideText: EditText? = null
    private var slideNum: Int = 0
    private val recordingToolbar = RecordingToolbar()
    private val multiRecordFrag = MultiRecordFrag()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_dramatization, container, false)

        slideNum = arguments?.getInt(SLIDE_NUM)!!

        setSlide()

        slideText = rootView.findViewById(R.id.fragment_dramatization_edit_text)
        slideText?.setText(Workspace.activeStory.slides[slideNum].translatedContent, TextView.BufferType.EDITABLE)

        if (Workspace.activeStory.isApproved) {
            if(Workspace.activeStory.slides[slideNum].slideType != SlideType.LOCALCREDITS) {
                setToolbar()
            }
            //closeKeyboardOnTouch(rootView)
            rootView.findViewById<View>(R.id.lock_overlay)?.visibility = View.INVISIBLE
        } else {
            PhaseBaseActivity.disableViewAndChildren(rootView)
        }

        //Make the text bigger if it is the front Page.
        if(Workspace.activeStory.slides[slideNum].slideType == SlideType.FRONTCOVER){
            slideText?.setTextSize(COMPLEX_UNIT_DIP,24f)
            slideText?.hint = context!!.getString(R.string.dramatization_edit_title_text_hint)
        }

        return rootView
    }

    /**
     * This function serves to handle draft page changes and stops the audio streams from
     * continuing.
     *
     * @param isVisibleToUser whether fragment is visible to user
     */
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        // Make sure that we are currently visible
        if (this.isVisible) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                //closeKeyboard(rootView)
            }
        }
    }

    override fun onStoppedRecordingOrPlayback(isRecording: Boolean) {}
    override fun onStartedRecordingOrPlayback(isRecording: Boolean) {
        multiRecordFrag.stopPlayback()
    }

    override fun onStoppedPlayback() {}
    override fun onStartedPlayback() {
        recordingToolbar.stopToolbarMedia()
    }

    /**
     * Initializes the toolbar and toolbar buttons.
     */
    private fun setToolbar() {
        val bundle = Bundle()
        bundle.putBooleanArray("buttonEnabled", booleanArrayOf(true,true,true,false))
        bundle.putInt(SlidePhaseFrag.SLIDE_NUM, slideNum)
        recordingToolbar.arguments = bundle
        childFragmentManager.beginTransaction().replace(R.id.toolbar_for_recording_toolbar, recordingToolbar).commit()

        recordingToolbar.keepToolbarVisible()
    }

    private fun setSlide(){
        val bundle = Bundle()
        bundle.putInt(SlidePhaseFrag.SLIDE_NUM, slideNum)

        multiRecordFrag.arguments = bundle
        childFragmentManager.beginTransaction().add(R.id.slide_phase, multiRecordFrag).commit()
    }

//    /**
//     * This function will set a listener to the passed in view so that when the passed in view
//     * is touched the keyboard close function will be called see: [.closeKeyboard].
//     *
//     * @param touchedView The view that will have an on touch listener assigned so that a touch of
//     * the view will close the softkeyboard.
//     */
//    private fun closeKeyboardOnTouch(touchedView: View?) {
//        touchedView?.setOnClickListener { closeKeyboard(touchedView) }
//    }
//
//    /**
//     * This function closes the keyboard. The passed in view will gain focus after the keyboard is
//     * hidden. The reestablished focus allows the removal of a cursor or any other focus indicator
//     * from the previously focused view.
//     *
//     * @param viewToFocus The view that will gain focus after the keyboard is hidden.
//     */
//    private fun closeKeyboard(viewToFocus: View?) {
//        if (viewToFocus != null) {
//            val imm = context!!.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.hideSoftInputFromWindow(viewToFocus.windowToken, 0)
//            viewToFocus.requestFocus()
//        }
//        if(slideText!!.visibility == android.view.View.VISIBLE) {
//            //Don't update with a press when in title and local credits slides.
//            val newText = slideText!!.text.toString()
//            if (newText != Workspace.activeStory.slides[slideNum].translatedContent) {
//                Workspace.activeStory.slides[slideNum].translatedContent = newText
//                setPic(rootView!!.findViewById(R.id.fragment_image_view))
//            }
//        }
//    }

}
