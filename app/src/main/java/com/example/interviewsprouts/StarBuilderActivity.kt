package com.example.interviewsprouts

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class StarBuilderActivity : AppCompatActivity() {

    private val answerTypes = listOf(
        "Leadership", "Conflict", "Failure", "Teamwork", "Problem Solving", "Achievement",
        "Tight Deadline", "Learning Quickly", "Handling Feedback", "Taking Initiative"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_star_builder)

        val storyInput = findViewById<EditText>(R.id.editRoughStory)
        val answerTypeSpinner = findViewById<Spinner>(R.id.spinnerAnswerType)
        val buildButton = findViewById<Button>(R.id.btnBuildStarAnswer)
        val outputText = findViewById<TextView>(R.id.textStarOutput)

        answerTypeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, answerTypes).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        buildButton.setOnClickListener {
            val story = storyInput.text.toString().trim()
            if (story.length < 20) {
                storyInput.error = "Enter at least 20 characters."
                outputText.text = "Please add a little more detail so the STAR answer can be structured."
                return@setOnClickListener
            }

            val answerType = answerTypeSpinner.selectedItem.toString()
            outputText.text = buildStarAnswer(story, answerType)
        }
    }

    private fun buildStarAnswer(story: String, answerType: String): String {
        val hasMetric = Regex("(\\d+%?|\\$|₹|€|£|hours?|days?|weeks?|months?|users?|customers?|clients?|revenue|cost|saved|reduced|increased)", RegexOption.IGNORE_CASE)
            .containsMatchIn(story)
        val metricAdvice = if (hasMetric) {
            "You included measurable detail. Keep it accurate and be ready to explain it."
        } else {
            "No clear measurable detail was detected. Add a real metric if you have one, such as [X%], [number], [hours], users supported, cost reduced, or time saved."
        }

        return """
            STAR Answer Builder
            Answer Type: $answerType

            Situation:
            In a real $answerType-related situation, the context was: $story

            Task:
            My responsibility was to understand the goal, clarify expectations, and contribute a practical solution without overstating my role.

            Action:
            I broke the situation into smaller steps, communicated with the people involved, took ownership of the work I could control, and followed through until the issue moved forward.

            Result:
            The outcome was a clearer path forward and a completed or improved piece of work. $metricAdvice

            Improved Final Answer:
            "In one $answerType situation, $story My task was to contribute responsibly and make progress. I clarified what needed to be done, took specific action, communicated updates, and learned from the result. The outcome was improved work quality and a stronger understanding of how to handle similar situations. If I had a verified metric, I would include it here rather than inventing one."

            Follow-up Questions:
            • What was your exact personal contribution?
            • What would you do differently next time?
            • Which metric or outcome can you prove honestly?
            • Who else was involved, and how did you communicate with them?

            Truth Warning:
            Do not invent fake metrics, titles, responsibilities, or outcomes. Replace placeholders only with true details.
        """.trimIndent()
    }
}
