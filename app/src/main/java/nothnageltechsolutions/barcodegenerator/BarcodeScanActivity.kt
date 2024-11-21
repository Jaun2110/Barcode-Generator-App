package nothnageltechsolutions.barcodegenerator

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.SyncStateContract
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.OutputStream
import java.util.*

class BarcodeScanActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val printerName = "BlueTooth Printer"
    private val handler = Handler(Looper.getMainLooper())
    private var debounceRunnable: Runnable? = null
    private lateinit var supabaseClient: SupabaseClient
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scan)

        // Initialize Supabase Client
        supabaseClient = createSupabaseClient(
            supabaseUrl = "https://axniqnfvsmhrudqaulxd.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImF4bmlxbmZ2c21ocnVkcWF1bHhkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzIwMTEzODIsImV4cCI6MjA0NzU4NzM4Mn0.OFcWduCWX2PbVZ5h2Hd08faLuLLfe7gPRZVm3Sk8Vuk"
        ) {
            install(Auth)
            install(Postgrest)
        }

        // Retrieve the authenticated user's email
        fetchAuthenticatedUserEmail()

        val scannerInput = findViewById<EditText>(R.id.scannerInput)
        val scannedDataTextView = findViewById<TextView>(R.id.scannedData)

        // Get the current year
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // Set the text for the footer TextView
        val footerTextView = findViewById<TextView>(R.id.FooterText)
        footerTextView.text = "Nothnagel Tech Solutions @ $currentYear"

        scannerInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No action needed here
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val data = s.toString().trim()
                scannedDataTextView.text = data

                // Cancel any previous debounce actions
                debounceRunnable?.let { handler.removeCallbacks(it) }

                // Set up a new debounce action
                debounceRunnable = Runnable {
                    // Only send data to the printer if it’s exactly 5 digits
                    if (data.length >= 5) {
                        connectToPrinterAndPrint(data)

                        // Fetch the user ID and write the scanned data to the database
                        userEmail?.let { email ->
                            fetchUserIdAndInsertScanData(email, data)
                        } ?: Toast.makeText(this@BarcodeScanActivity, "User email not found!", Toast.LENGTH_SHORT).show()

                        // Clear the EditText after processing
                        scannerInput.text.clear()
                    }
                }

                // Post the action with a 4-second delay
                handler.postDelayed(debounceRunnable!!, 2000)
            }

            override fun afterTextChanged(s: Editable?) {
                // No action needed here
            }
        })
    }

    private fun fetchAuthenticatedUserEmail() {
        val session = supabaseClient.auth.currentSessionOrNull()
        userEmail = session?.user?.email
    }

    private fun connectToPrinterAndPrint(data: String) {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        val device = findPrinterByName(printerName)
        if (device == null) {
            Toast.makeText(this, "Printer not found", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream

            printBarcode(data)

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Could not connect to printer", Toast.LENGTH_SHORT).show()
            closeConnection()
        }
    }

    private fun findPrinterByName(name: String): BluetoothDevice? {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            if (device.name == name) {
                return device
            }
        }
        return null
    }

    private fun printBarcode(data: String) {
        try {
            // Print logic remains the same...
            Toast.makeText(this, "Barcode sent to printer", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error printing barcode", Toast.LENGTH_SHORT).show()
        } finally {
            closeConnection()
        }
    }

    private fun closeConnection() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun fetchUserIdAndInsertScanData(email: String, dataScanned: String) {
        lifecycleScope.launch {
            try {
                // Query Supabase for user ID
                println("Querying Supabase for email: $email")
                val response = supabaseClient.from("Users").select(
                    columns = Columns.list("id")
                ) {
                    filter {
                        eq("email", email) // Filter by email
                    }
                }.decodeList<Map<String, Any>>() // Decode the result as a list

                // Check if the response has the user
                if (response.isNotEmpty()) {
                    val user = response.firstOrNull()
                    val userId = user?.get("id")?.toString()?.toLongOrNull()

                    if (userId != null) {
                        println("User ID fetched: $userId")
                        // Insert scanned data into scanData table
                        insertScanData(userId, dataScanned)
                    } else {
                        println("User ID is null for email: $email")
                        Toast.makeText(this@BarcodeScanActivity, "User not found!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    println("No user found for email: $email")
                    Toast.makeText(this@BarcodeScanActivity, "No user found for email: $email", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                println("Error fetching user ID: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@BarcodeScanActivity, "Error fetching user ID: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun insertScanData(userId: Long, dataScanned: String) {
        lifecycleScope.launch {
            try {
                val scanData = ScanData(
                    userID = userId,
                    dataScanned = dataScanned
                )
                supabaseClient.from("scanData").insert(scanData)
                Toast.makeText(this@BarcodeScanActivity, "Scan data saved successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@BarcodeScanActivity, "Error saving scan data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @kotlinx.serialization.Serializable
    data class ScanData(
        val userID: Long,
        val dataScanned: String
    )
}
