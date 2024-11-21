package nothnageltechsolutions.barcodegenerator

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.io.OutputStream
import java.util.*

class BarcodeScanActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val printerName = "BlueTooth Printer"
    private val handler = Handler(Looper.getMainLooper())
    private var pendingBarcode: String? = null
    private var debounceRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


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
                    if (data.length >= 5 ) {
                        connectToPrinterAndPrint(data)

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
            // Center align the content
            outputStream?.write(byteArrayOf(0x1B, 0x61, 0x01)) // ESC a 1: Center alignment

            // Print the company name above the barcode
            outputStream?.write(byteArrayOf(0x0A)) // New line before the company name
            val companyName = "Pro Sheep Solutions"
            outputStream?.write(companyName.toByteArray(Charsets.US_ASCII))
            outputStream?.write(byteArrayOf(0x0A)) // New line for spacing
            outputStream?.write(byteArrayOf(0x0A)) // New line after the company name

            // Set the barcode height
            outputStream?.write(byteArrayOf(0x1D, 0x68, 100.toByte())) // GS h 100: Set barcode height to 100 dots

            // Set the barcode width
            outputStream?.write(byteArrayOf(0x1D, 0x77, 3.toByte())) // GS w 2: Set barcode width to 2

            // Print the CODE39 barcode without asterisks
            outputStream?.write(byteArrayOf(0x1D, 0x6B, 4)) // GS k 4: Select CODE39
            outputStream?.write(data.toByteArray(Charsets.US_ASCII)) // Print data in CODE39 format
            outputStream?.write(byteArrayOf(0x00)) // Null terminator for barcode data

            // Print the text below the barcode
            outputStream?.write(data.toByteArray(Charsets.US_ASCII)) // Print the text "24848"
            outputStream?.write(byteArrayOf(0x0A)) // New line for spacing

            // Add extra spacing
            outputStream?.write(byteArrayOf(0x0A, 0x0A, 0x0A)) // Three extra newlines for additional space

            outputStream?.flush()
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
}
