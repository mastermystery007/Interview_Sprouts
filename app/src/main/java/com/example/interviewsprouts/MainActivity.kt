package com.example.interviewsprouts

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnResumeReview = findViewById<Button>(R.id.btnResumeReview)
        val btnSavedReports = findViewById<Button>(R.id.btnSavedReports)
        val textMotivation = findViewById<TextView>(R.id.textMotivation)

        val motivationalLines = listOf(
            "Your next interview starts with a sharper resume.",
            "Turn your resume into interview confidence.",
            "Small resume fixes can unlock better opportunities.",
            "Match the role. Show the evidence. Prepare smarter.",
            "Make every bullet earn its place.",
            "Let your resume speak clearly before the interview.",
            "Build a stronger story for your next role."
        )
        textMotivation.text = motivationalLines.random()

        btnResumeReview.setOnClickListener {
            startActivity(Intent(this, ResumeInputActivity::class.java))
        }

        btnSavedReports.setOnClickListener {
            startActivity(Intent(this, SavedReportsActivity::class.java))
        }
    }
}
