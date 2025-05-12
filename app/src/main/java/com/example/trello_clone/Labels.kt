package com.example.trello_clone


import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity


class Labels : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_labels)

        var closeBtn = findViewById<ImageView>(R.id.close)

        closeBtn.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    val intent = Intent(this, EditAsset::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }
}

