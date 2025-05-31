package com.billbuddy.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.billbuddy.shared.Greeting // Import from shared module

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Simple TextView to display greeting
        val tv = TextView(this)
        tv.text = Greeting().greet() // Use the Greeting class from shared module
        setContentView(tv)
    }
}
