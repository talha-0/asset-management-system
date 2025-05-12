package com.example.trello_clone

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Find views
        val logoutBtn = findViewById<ImageView>(R.id.exit_icon)
        val goToBoards = findViewById<LinearLayout>(R.id.boards_section)
        val goToCards = findViewById<LinearLayout>(R.id.cards_section)
        val tvName = findViewById<TextView>(R.id.tv_name)
        val tvEmail = findViewById<TextView>(R.id.tv_email)

        // Set user data
        val currentUser = auth.currentUser
        if (currentUser != null) {
            tvName.text = currentUser.displayName ?: "User Name"
            tvEmail.text = currentUser.email ?: "User Email"
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

        // Logout button
        logoutBtn.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Navigate to Boards section
        goToBoards.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        // Navigate to Cards section
        goToCards.setOnClickListener {
            val intent = Intent(this, AssetInventoryActivity::class.java)
            startActivity(intent)
        }
    }
}
