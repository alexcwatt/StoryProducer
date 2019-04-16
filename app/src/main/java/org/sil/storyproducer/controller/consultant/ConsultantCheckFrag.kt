package org.sil.storyproducer.controller.consultant

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.ScriptureFrag
import org.sil.storyproducer.controller.SlidePhaseFrag
import org.sil.storyproducer.model.Workspace

/**
 * The fragment for the Consultant check view. The consultant can check that the draft is ok
 */
class ConsultantCheckFrag : ConsultantBaseFrag(), SlidePhaseFrag.PlaybackListener {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        // The last two arguments ensure LayoutParams are inflated
        // properly.
        val rootView = inflater.inflate(R.layout.fragment_consultant_check, container, false)

        setSlide()
        setScripture()
        setLogsButton(rootView.findViewById(R.id.concheck_logs_button))

        val checkButton = rootView.findViewById<com.getbase.floatingactionbutton.FloatingActionButton>(R.id.concheck_checkmark_button)
        setCheckmarkButton(checkButton)
        checkButton.setOnClickListener(View.OnClickListener {
            if (Workspace.activeStory.isApproved) {
                Toast.makeText(context, "Story already approved", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            if (Workspace.activeStory.slides[slideNum].isChecked) {
                checkButton.background = grayCheckmark
                Workspace.activeStory.slides[slideNum].isChecked = false
            } else {
                checkButton.background = greenCheckmark
                Workspace.activeStory.slides[slideNum].isChecked = true
                if (checkAllMarked()) {
                    showConsultantPasswordDialog()
                }
            }
        })
        return rootView
    }

    /**
     * Launches a dialog for the consultant to enter a password once all slides approved
     */
    private fun showConsultantPasswordDialog() {
        val password = EditText(context)
        password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        password.id = org.sil.storyproducer.R.id.password_text_field

        // Programmatically set layout properties for edit text field
        val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
        // Apply layout properties
        password.layoutParams = params
        val passwordDialog = AlertDialog.Builder(context!!)
                .setTitle(getString(R.string.consultant_password_title))
                .setMessage(getString(R.string.consultant_password_message))
                .setView(password)
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.submit), null)
                .create()
        // This is set to dismiss the keyboard manually on dialog dismiss
        passwordDialog.setOnDismissListener { toggleKeyboard(false, view) }

        // This manually sets the submit button listener so that the dialog doesn't always submit
        // If the password is incorrect, we want to stay on the dialog and give an error message
        passwordDialog.setOnShowListener { dialog ->
            val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val passwordText = password.text.toString()
                if (passwordText.contentEquals(PASSWORD)) {
                    saveConsultantApproval()
                    dialog.dismiss()
                    launchDramatizationPhase()
                } else {
                    password.error = getString(R.string.consultant_incorrect_password_message)
                }
            }
        }

        passwordDialog.show()
        toggleKeyboard(true, password)
    }

    override fun onStoppedPlayback() {}
    override fun onStartedPlayback() {}

    /**
     * This function toggles the soft input keyboard. Allowing the user to have the keyboard
     * to open or close seamlessly alongside the rest UI.
     * @param showKeyBoard The boolean to be passed in to determine if the keyboard show be shown.
     * @param aView The view associated with the soft input keyboard.
     */
    private fun toggleKeyboard(showKeyBoard: Boolean, aView: View?) {
        val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (showKeyBoard) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        } else {
            imm.hideSoftInputFromWindow(aView!!.windowToken, 0)
        }
    }

    private fun setSlide(){
        val bundle = Bundle()
        bundle.putInt(SlidePhaseFrag.SLIDE_NUM, slideNum)
        val slidePhaseFrag = SlidePhaseFrag()
        slidePhaseFrag.arguments = bundle
        childFragmentManager.beginTransaction().add(R.id.slide_phase, slidePhaseFrag).commit()
    }

    private fun setScripture(){
        val bundle = Bundle()
        bundle.putInt(SlidePhaseFrag.SLIDE_NUM, slideNum)
        val scriptureFrag = ScriptureFrag()
        scriptureFrag.arguments = bundle
        childFragmentManager.beginTransaction().add(R.id.scripture_text, scriptureFrag).commit()
    }

    companion object {
        val CONSULTANT_PREFS = "Consultant_Checks"
        val IS_CONSULTANT_APPROVED = "isApproved"
        private val PASSWORD = "appr00ved"
    }
}
