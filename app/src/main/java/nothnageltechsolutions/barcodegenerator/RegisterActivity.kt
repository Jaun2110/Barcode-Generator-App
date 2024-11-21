package nothnageltechsolutions.barcodegenerator

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class RegisterActivity : AppCompatActivity() {

    private lateinit var supabaseClient: SupabaseClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Supabase Client
        supabaseClient = createSupabaseClient(
            supabaseUrl = "https://axniqnfvsmhrudqaulxd.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImF4bmlxbmZ2c21ocnVkcWF1bHhkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzIwMTEzODIsImV4cCI6MjA0NzU4NzM4Mn0.OFcWduCWX2PbVZ5h2Hd08faLuLLfe7gPRZVm3Sk8Vuk" // Replace with your Supabase public key
        ) {
            install(Auth) // Install the Auth module
            install(Postgrest) // Install the Postgrest module
        }

        // UI References
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val firstNameInput = findViewById<EditText>(R.id.firstNameInput)
        val lastNameInput = findViewById<EditText>(R.id.lastNameInput)
        val mobileInput = findViewById<EditText>(R.id.mobileInput)
        val addressInput = findViewById<EditText>(R.id.addressInput)
        val cityInput = findViewById<EditText>(R.id.cityInput)
        val zipCodeInput = findViewById<EditText>(R.id.zipCodeInput)
        val provinceInput = findViewById<EditText>(R.id.provinceInput)
        val countryInput = findViewById<EditText>(R.id.countryInput)
        val registerButton = findViewById<Button>(R.id.registerButton)

        // Register Button Click Listener
        registerButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val firstName = firstNameInput.text.toString()
            val lastName = lastNameInput.text.toString()
            val mobile = mobileInput.text.toString()
            val address = addressInput.text.toString()
            val city = cityInput.text.toString()
            val zipCode = zipCodeInput.text.toString()
            val province = provinceInput.text.toString()
            val country = countryInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && firstName.isNotEmpty() && lastName.isNotEmpty()) {
                registerUser(
                    email, password, firstName, lastName, mobile, address,
                    city, zipCode, province, country
                )
            } else {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Login link click listener
        val loginLink = findViewById<TextView>(R.id.loginLink)
        loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun registerUser(
        email: String, password: String, firstName: String, lastName: String,
        mobile: String, address: String, city: String, zipCode: String, province: String, country: String
    ) {
        lifecycleScope.launch {
            try {
//                // Sign up the user with Supabase Auth
                supabaseClient.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                // Create a user object
                val user = User(
                    email = email,
                    firstname = firstName,
                    lastname = lastName,
                    mobile = mobile.toLongOrNull(),
                    address = address,
                    city = city,
                    zipcode = zipCode.toLongOrNull(),
                    province = province,
                    country = country
                )

                // Insert the user into the Users table
                supabaseClient.from("Users").insert(user)

                Toast.makeText(this@RegisterActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
                // Redirect to the Barcode Scan Activity
                startActivity(Intent(this@RegisterActivity, BarcodeScanActivity::class.java))
            } catch (error: Exception) {
                Toast.makeText(this@RegisterActivity, "Registration failed: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("TAG", "Error occurred: ${error.message}", error) // Logs the error with a tag
            }
        }
    }
    @Serializable
    data class User(
        val email: String,
        val firstname: String,
        val lastname: String,
        val mobile: Long?,
        val address: String,
        val city: String,
        val zipcode: Long?,
        val province: String,
        val country: String
    )
}
