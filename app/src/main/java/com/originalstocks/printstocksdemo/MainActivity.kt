package com.originalstocks.printstocksdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.originalstocks.printstocksdemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}