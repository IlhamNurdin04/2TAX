package com.pmob.projectakhirpemrogramanmobile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.pmob.projectakhirpemrogramanmobile.databinding.ActivityBuyBinding
import java.text.NumberFormat
import java.util.Locale

class BuyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBuyBinding

    private var bookTitle: String = ""
    private var bookPrice: Double = 0.0
    private var bookCover: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBuyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ===== Ambil data dari Intent =====
        bookTitle = intent.getStringExtra("BOOK_TITLE") ?: ""
        bookPrice = intent.getDoubleExtra("BOOK_PRICE", 0.0)
        bookCover = intent.getStringExtra("BOOK_COVER")

        // ===== Set UI =====
        binding.tvTitle.text = bookTitle
        binding.tvPrice.text = formatRupiah(bookPrice)

        Glide.with(this)
            .load(bookCover)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .into(binding.ivCover)

        // ===== Actions =====
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnBuyNow.setOnClickListener {
            val intent = Intent(this, PaymentActivity::class.java)
            intent.putExtra("BOOK_TITLE", bookTitle)
            intent.putExtra("BOOK_PRICE", bookPrice)
            intent.putExtra("BOOK_COVER", bookCover)
            startActivity(intent)
        }

    }

    // ================== PAYMENT DUMMY ==================
    private fun processDummyPayment() {
        savePurchaseToFirebase(bookTitle, bookPrice)

        Toast.makeText(this, "Pembayaran berhasil (Simulasi)", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, SuccessActivity::class.java)
        intent.putExtra("BOOK_TITLE", bookTitle)
        intent.putExtra("BOOK_PRICE", formatRupiah(bookPrice))
        startActivity(intent)
        finish()
    }

    // ================== SAVE TO FIREBASE ==================
    private fun savePurchaseToFirebase(title: String, price: Double) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val db = FirebaseDatabase.getInstance()
            .getReference("purchases")
            .child(uid)

        val purchaseId = db.push().key ?: return

        val purchase = Purchase(
            title = title,
            price = price,
            timestamp = System.currentTimeMillis(),
            status = "SUCCESS"
        )

        db.child(purchaseId).setValue(purchase)
    }

    // ================== FORMAT RUPIAH ==================
    private fun formatRupiah(value: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        formatter.maximumFractionDigits = 0
        formatter.minimumFractionDigits = 0
        return formatter.format(value)
    }

}
