package apptentive.com.android.feedback.survey.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isVisible
import apptentive.com.android.feedback.survey.R
import apptentive.com.android.ui.getThemeColor
import com.google.android.material.textview.MaterialTextView

internal class SurveyQuestionContainerView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr) {
    private val titleInstructionsLayout: LinearLayout
    private val titleTextView: MaterialTextView
    private val instructionsTextView: MaterialTextView
    private val answerContainerView: LinearLayout
    private val errorMessageView: MaterialTextView

    var title: CharSequence?
        get() = titleTextView.text
        set(value) {
            titleTextView.text = value
        }

    var instructions: CharSequence?
        get() = instructionsTextView.text
        set(value) {
            // hide instructions if there's no value
            instructionsTextView.isVisible = !value.isNullOrEmpty()
            instructionsTextView.text = value
        }

    var accessibilityDescription: String = ""

    // hold view initial text colors to restore later
    private val titleTextViewDefaultColor: ColorStateList
    private val instructionsTextViewDefaultColor: ColorStateList

    // error "override" color
    private val errorColor: Int

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    init {
        val contentView = LayoutInflater.from(context)
            .inflate(R.layout.apptentive_survey_question_container, this, true)

        titleInstructionsLayout = contentView.findViewById(R.id.apptentive_question_title_instructions_layout)
        titleTextView = contentView.findViewById(R.id.apptentive_question_title)
        instructionsTextView = contentView.findViewById(R.id.apptentive_question_instructions)
        answerContainerView = contentView.findViewById(R.id.apptentive_answer_container)
        errorMessageView = contentView.findViewById(R.id.apptentive_question_error_message)

        titleTextViewDefaultColor = titleTextView.textColors
        instructionsTextViewDefaultColor = instructionsTextView.textColors

        errorColor = context.getThemeColor(R.attr.colorError)
    }

    fun setAnswerView(layoutId: Int) {
        answerContainerView.removeAllViews()
        val answerView = LayoutInflater.from(context).inflate(layoutId, answerContainerView, false)
        answerContainerView.addView(answerView)
    }

    fun setQuestionContentDescription(cd: String) {
        titleInstructionsLayout.contentDescription = cd
    }

    fun setErrorMessage(errorMessage: String?) {
        if (errorMessage != null) {
            titleTextView.setTextColor(errorColor)
            instructionsTextView.setTextColor(errorColor)
            errorMessageView.visibility = View.VISIBLE
            errorMessageView.text = errorMessage
            setQuestionContentDescription("$errorMessage. $accessibilityDescription")
        } else {
            titleTextView.setTextColor(titleTextViewDefaultColor)
            instructionsTextView.setTextColor(instructionsTextViewDefaultColor)
            errorMessageView.visibility = View.INVISIBLE
            setQuestionContentDescription(accessibilityDescription)
        }
    }
}
