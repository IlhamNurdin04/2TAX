package com.pmob.projectakhirpemrogramanmobile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class StoreMapActivity : AppCompatActivity(), OnMapReadyCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store_map)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {

        val stores = listOf(
            Pair("Gramedia Senayan", LatLng(-6.224, 106.802)),
            Pair("Gramedia Grand Indonesia", LatLng(-6.195, 106.821)),
            Pair("Gramedia Pasaraya", LatLng(-6.244, 106.793))
        )

        stores.forEach {
            googleMap.addMarker(
                MarkerOptions()
                    .position(it.second)
                    .title(it.first)
            )
        }

        // Fokus ke Jakarta
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(-6.2088, 106.8456), 12f
            )
        )
    }
}