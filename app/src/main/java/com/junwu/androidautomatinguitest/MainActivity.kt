package com.junwu.androidautomatinguitest

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.junwu.androidautomatinguitest.network.NetworkUtils
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var continueButton: Button
    private lateinit var phoneNumberEditText: EditText
    private lateinit var loginSuccessTextView: TextView
    private val networkUtils = NetworkUtils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        continueButton = findViewById(R.id.continueButton)
        continueButton.setOnClickListener(this)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        loginSuccessTextView = findViewById(R.id.login_success)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.continueButton -> onContinueButtonClicked()
        }
    }

    private fun onContinueButtonClicked() {
        lifecycleScope.launch {
            val requestOtp = networkUtils.requestOtp(phoneNumberEditText.text.toString())
            if (requestOtp.retryAfterSeconds > 0) {
                loginSuccessTextView.isVisible = true
            }
        }
    }
}