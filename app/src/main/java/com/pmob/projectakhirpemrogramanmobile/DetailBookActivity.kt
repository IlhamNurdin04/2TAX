package com.pmob.projectakhirpemrogramanmobile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.pmob.projectakhirpemrogramanmobile.databinding.ActivityDetailBookBinding
import java.text.NumberFormat
import java.util.Locale


class DetailBookActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBookBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDetailBookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvSeeAllStores.setOnClickListener {
            startActivity(
                Intent(this@DetailBookActivity, StoreMapActivity::class.java)
            )
        }


        // Ambil data dari Intent
        val title = intent.getStringExtra("BOOK_TITLE") ?: "Unknown"
        val author = intent.getStringExtra("BOOK_AUTHOR") ?: "Unknown"
        val coverUrl = intent.getStringExtra("BOOK_COVER") ?: ""
        val rating = intent.getDoubleExtra("BOOK_RATING", 0.0)
        val year = intent.getIntExtra("BOOK_YEAR", 0)
        val pages = intent.getIntExtra("BOOK_PAGES", 0)
        val synopsis = intent.getStringExtra("BOOK_SYNOPSIS") ?: "No description"
        val price = intent.getDoubleExtra("BOOK_PRICE", 0.0)
        val genres = intent.getStringArrayListExtra("BOOK_GENRES") ?: arrayListOf()

        setupViews(
            title, author, coverUrl,
            rating, year, pages,
            synopsis, genres
        )

        setupListeners(
            title, author, coverUrl,
            rating, pages, synopsis, price
        )
    }

    private fun setupViews(
        title: String,
        author: String,
        coverUrl: String,
        rating: Double,
        year: Int,
        pages: Int,
        synopsis: String,
        genres: ArrayList<String>
    ) = with(binding) {

        ivBack.setOnClickListener { finish() }

        tvBookTitle.text = title
        tvAuthor.text = author
        tvRating.text = String.format("%.1f", rating)
        tvPublished.text = year.toString()
        tvPages.text = pages.toString()
        tvSynopsis.text = synopsis

        Glide.with(this@DetailBookActivity)
            .load(coverUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .into(ivBookCover)

        genreChipGroup.removeAllViews()
        genres.forEach { genre ->
            val chip = Chip(this@DetailBookActivity).apply {
                text = genre
                isClickable = false
                isCheckable = false
            }
            genreChipGroup.addView(chip)
        }

        // Read More / Less
        tvSynopsis.maxLines = 4
        tvReadMore.setOnClickListener {
            val expanded = tvSynopsis.maxLines != 4
            tvSynopsis.maxLines = if (expanded) 4 else Int.MAX_VALUE
            tvReadMore.text = if (expanded) "Read More ▼" else "Read Less ▲"
        }
    }

    private fun setupListeners(
        title: String,
        author: String,
        coverUrl: String,
        rating: Double,
        pages: Int,
        synopsis: String,
        price: Double
    ) = with(binding) {

        ivShare.setOnClickListener {
            Toast.makeText(
                this@DetailBookActivity,
                "Share coming soon",
                Toast.LENGTH_SHORT
            ).show()
        }

        ivBookmark.setOnClickListener {
            Toast.makeText(
                this@DetailBookActivity,
                "Added to bookmark",
                Toast.LENGTH_SHORT
            ).show()
        }
        val rateUsdToIdr = 16000
        val priceIdr = price * rateUsdToIdr

        val rupiah = NumberFormat
            .getCurrencyInstance(Locale("in", "ID"))
            .format(priceIdr)

        btnBuy.text = "Beli $rupiah"

        btnBuy.setOnClickListener {
            Intent(this@DetailBookActivity, BuyActivity::class.java).apply {
                putExtra("BOOK_TITLE", title)
                putExtra("BOOK_PRICE", priceIdr) // ✅ INI WAJIB
                putExtra("BOOK_COVER", coverUrl)
                startActivity(this)
            }
        }



        btnReadNow.setOnClickListener {
            Intent(this@DetailBookActivity, PreviewActivity::class.java).apply {
                putExtra("BOOK_TITLE", title)
                putExtra("BOOK_AUTHOR", author)
                putExtra("BOOK_COVER", coverUrl)
                putExtra("BOOK_RATING", rating)
                putExtra("BOOK_PAGES", pages)
                putExtra("BOOK_SYNOPSIS", synopsis)
                startActivity(this)
            }
        }
    }
}
