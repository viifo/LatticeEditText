package com.viifo.latticeedittext.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.viifo.latticeedittext.LatticeEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DemoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)

        val latticeEditText = findViewById<LatticeEditText>(R.id.et_input)
        latticeEditText.textChangeListener = {
            println("--> OnTextChangeListener： text = $it")
        }
        latticeEditText.setOnTextChangeListener {
            println("--> OnTextChangeListener： text = $it")
        }
        findViewById<Button>(R.id.btn_get).setOnClickListener {
            findViewById<TextView>(R.id.tv_text).text = latticeEditText.content
        }

//        GlobalScope.launch {
//            delay(6100)
//            latticeEditText.isCursorVisible(false)
//        }
    }
}