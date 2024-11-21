package nothnageltechsolutions.barcodegenerator

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class RegisterActivity : AppCompatActivity() {

    private lateinit var supabaseClient: SupabaseClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Supabase Client
        supabaseClient = createSupabaseClient(
            supabaseUrl = "https://axniqnfvsmhrudqaulxd.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImF4bmlxbmZ2c21ocnVkcWF1bHhkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzIwMTEzODIsImV4cCI6MjA0NzU4NzM4Mn0.OFcWduCWX2PbVZ5h2Hd08faLuLLfe7gPRZVm3Sk8Vuk"
        ) {
            install(Auth) // Install the Auth module
        }

        // UI References
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val registerButton = findViewById<Button>(R.id.registerButton)

        // Register Button Click Listener
        registerButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                registerUser(email, password)
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
        //        Login link click listener
        val loginLink = findViewById<TextView>(R.id.loginLink)
        loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }


    private fun registerUser(email: String, password: String) {
        lifecycleScope.launch {
            try {
                supabaseClient.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                Toast.makeText(this@RegisterActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
                // Redirect to login screen or home screen
                startActivity(Intent(this@RegisterActivity, BarcodeScanActivity::class.java))
            } catch (error: Exception) {
                Toast.makeText(this@RegisterActivity, "Registration failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
