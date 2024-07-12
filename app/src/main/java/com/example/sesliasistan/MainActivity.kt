package com.example.sesliasistan

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CAMERA = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val goCameraBtn = findViewById<Button>(R.id.goCameraBtn)

        goCameraBtn.setOnClickListener {
            getPermission()
        }
    }

    private fun getPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Kamera izni yok, izin iste
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), PERMISSION_REQUEST_CAMERA)
        } else {
            // Kamera izni var, kamera aktivitesini başlat
            startCameraActivity()
        }
    }

    private fun startCameraActivity() {
        val intent = Intent(this, Camera::class.java)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Kullanıcı kamera iznini verdi, kamera aktivitesini başlat
                startCameraActivity()
            }
        }
    }
}


