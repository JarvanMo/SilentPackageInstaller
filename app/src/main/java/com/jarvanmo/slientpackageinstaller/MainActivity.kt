package com.jarvanmo.slientpackageinstaller

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val serviceIntent = Intent(this@MainActivity, PmService::class.java)
        serviceIntent.action = intent?.action
        serviceIntent.data = intent?.data
        startService(serviceIntent)
    }

    override fun onResume() {
        super.onResume()
        //we try to finish this activity because this activity can interrupt back key while pressing back
        finish()
    }
}
