package com.pmob.projectakhirpemrogramanmobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.pmob.projectakhirpemrogramanmobile.databinding.ActivityPaymentBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding

    private var selectedMethod: String? = null
    private var bookTitle = ""
    private var bookPrice = 0.0
    private var bookCover: String? = null
    private var currentOrderId: String? = null

    // ===== GANTI DENGAN URL NGROK ANDA =====
    private val API_URL = "https://traducianistic-unexaggeratory-tessa.ngrok-free.dev/create_transaction.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ===== Ambil data dari Intent =====
        bookTitle = intent.getStringExtra("BOOK_TITLE") ?: ""
        bookPrice = intent.getDoubleExtra("BOOK_PRICE", 0.0)
        bookCover = intent.getStringExtra("BOOK_COVER")

        // ===== Set UI =====
        binding.tvTitle.text = bookTitle
        binding.tvPrice.text = formatRupiah(bookPrice)
        binding.tvTotal.text = formatRupiah(bookPrice)

        Glide.with(this)
            .load(bookCover)
            .placeholder(R.drawable.ic_book_placeholder)
            .error(R.drawable.ic_book_placeholder)
            .into(binding.ivCover)

        // ===== Klik metode pembayaran =====
        binding.layoutTransfer.setOnClickListener {
            binding.rbTransfer.isChecked = true
            selectedMethod = "Transfer Bank"
            Log.d("PaymentActivity", "Transfer selected: $selectedMethod")
        }

        binding.layoutEwallet.setOnClickListener {
            binding.rbEwallet.isChecked = true
            selectedMethod = "E-Wallet"
            Log.d("PaymentActivity", "E-Wallet selected: $selectedMethod")
        }

        binding.rgPaymentMethod.setOnCheckedChangeListener { _, checkedId ->
            resetHighlight()
            when (checkedId) {
                R.id.rbTransfer -> {
                    selectedMethod = "Transfer Bank"
                    binding.layoutTransfer.setBackgroundResource(R.drawable.bg_card_selected)
                    Log.d("PaymentActivity", "RadioGroup: Transfer selected")
                }
                R.id.rbEwallet -> {
                    selectedMethod = "E-Wallet"
                    binding.layoutEwallet.setBackgroundResource(R.drawable.bg_card_selected)
                    Log.d("PaymentActivity", "RadioGroup: E-Wallet selected")
                }
            }
        }

        // ===== Tombol Bayar =====
        binding.btnPayNow.setOnClickListener {
            Log.d("PaymentActivity", "Button clicked, selectedMethod: $selectedMethod")

            if (selectedMethod == null) {
                Toast.makeText(
                    this,
                    "Pilih metode pembayaran terlebih dahulu",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                processPayment()
            }
        }
    }

    // ================== RESET CARD ==================
    private fun resetHighlight() {
        binding.layoutTransfer.setBackgroundResource(R.drawable.bg_card)
        binding.layoutEwallet.setBackgroundResource(R.drawable.bg_card)
    }

    // ================== STEP 1: PROSES PAYMENT ==================
    private fun processPayment() {
        // Disable button
        binding.btnPayNow.isEnabled = false
        binding.btnPayNow.text = "Memproses..."

        // Buat order ID unik
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        currentOrderId = "ORDER-${System.currentTimeMillis()}"

        // Hit API PHP untuk generate token
        generateMidtransToken()
    }

    // ================== STEP 2: HIT API PHP ==================
    private fun generateMidtransToken() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                // Prepare JSON payload
                val jsonPayload = JSONObject().apply {
                    put("order_id", currentOrderId)
                    put("gross_amount", bookPrice.toInt())
                    put("title", bookTitle)
                    put("customer_name", FirebaseAuth.getInstance().currentUser?.displayName ?: "Customer")
                    put("customer_email", FirebaseAuth.getInstance().currentUser?.email ?: "customer@example.com")
                }

                // Send request
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonPayload.toString())
                writer.flush()
                writer.close()

                // Read response
                val responseCode = connection.responseCode
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                Log.d("PaymentActivity", "Response Code: $responseCode")
                Log.d("PaymentActivity", "Response: $response")

                // Parse response
                val jsonResponse = JSONObject(response)

                withContext(Dispatchers.Main) {
                    if (jsonResponse.getBoolean("success")) {
                        val snapToken = jsonResponse.getString("snap_token")
                        val redirectUrl = jsonResponse.getString("redirect_url")

                        // Simpan ke Firebase
                        saveOrderToFirebase(snapToken, redirectUrl)

                        // Buka payment page
                        openMidtransPayment(redirectUrl)
                    } else {
                        val errorMsg = jsonResponse.optString("message", "Unknown error")
                        Toast.makeText(
                            this@PaymentActivity,
                            "Error: $errorMsg",
                            Toast.LENGTH_LONG
                        ).show()
                        resetButton()
                    }
                }

            } catch (e: Exception) {
                Log.e("PaymentActivity", "Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@PaymentActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    resetButton()
                }
            }
        }
    }

    // ================== STEP 3: SIMPAN ORDER KE FIREBASE ==================
    private fun saveOrderToFirebase(snapToken: String, redirectUrl: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val db = FirebaseDatabase.getInstance()
            .getReference("orders")
            .child(uid)
            .child(currentOrderId ?: return)

        val orderData = HashMap<String, Any>()
        orderData["order_id"] = currentOrderId ?: ""
        orderData["title"] = bookTitle
        orderData["price"] = bookPrice
        orderData["timestamp"] = System.currentTimeMillis()
        orderData["status_pembayaran"] = "pending"
        orderData["method"] = selectedMethod ?: "-"
        orderData["snap_token"] = snapToken
        orderData["redirect_url"] = redirectUrl

        db.setValue(orderData)
            .addOnSuccessListener {
                Log.d("PaymentActivity", "Order saved to Firebase")
            }
            .addOnFailureListener { e ->
                Log.e("PaymentActivity", "Failed to save order", e)
            }
    }

    // ================== STEP 4: BUKA MIDTRANS PAYMENT ==================
    private fun openMidtransPayment(redirectUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl))
            startActivity(intent)

            Toast.makeText(
                this,
                "Silakan selesaikan pembayaran",
                Toast.LENGTH_LONG
            ).show()

            resetButton()

        } catch (e: Exception) {
            Log.e("PaymentActivity", "Error opening browser", e)
            Toast.makeText(
                this,
                "Gagal membuka browser: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            resetButton()
        }
    }

    // ================== RESET BUTTON ==================
    private fun resetButton() {
        binding.btnPayNow.isEnabled = true
        binding.btnPayNow.text = "Bayar Sekarang"
    }

    // ================== FORMAT RUPIAH ==================
    private fun formatRupiah(value: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return formatter.format(value)
    }
}