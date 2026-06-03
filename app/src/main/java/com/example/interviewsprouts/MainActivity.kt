package com.example.interviewsprouts

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnResumeReview = findViewById<Button>(R.id.btnResumeReview)
        val btnSavedReports = findViewById<Button>(R.id.btnSavedReports)

        btnResumeReview.setOnClickListener {
            startActivity(Intent(this, ResumeInputActivity::class.java))
        }

        btnSavedReports.setOnClickListener {
            startActivity(Intent(this, SavedReportsActivity::class.java))
        }
    }
}
