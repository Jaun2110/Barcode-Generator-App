package nothnageltechsolutions.barcodegenerator

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var supabaseClient: SupabaseClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Supabase Client
        supabaseClient = createSupabaseClient(
            supabaseUrl = "https://axniqnfvsmhrudqaulxd.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImF4bmlxbmZ2c21ocnVkcWF1bHhkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzIwMTEzODIsImV4cCI6MjA0NzU4NzM4Mn0.OFcWduCWX2PbVZ5h2Hd08faLuLLfe7gPRZVm3Sk8Vuk"
        ) {
            install(Auth)
        }

        // UI References
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)

        // Login Button Click Listener
        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
//        Register link click listener
        val registerLink = findViewById<TextView>(R.id.registerLink)
        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

    }

    private fun loginUser(email: String, password: String) {
        lifecycleScope.launch {
            try {
                supabaseClient.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                // Redirect to home screen or another activity
                startActivity(Intent(this@LoginActivity, BarcodeScanActivity::class.java))
            } catch (error: Exception) {
                Toast.makeText(this@LoginActivity, "Login failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
