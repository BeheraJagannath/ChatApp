package com.example.mychatapp.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mychatapp.databinding.ActivityVerificationBinding
import com.google.firebase.auth.FirebaseAuth

class VerificationActivity : AppCompatActivity() {
    lateinit var firebaseAuth: FirebaseAuth
    lateinit var binding: ActivityVerificationBinding
    lateinit var countrycode: String
    lateinit var phonenumber: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth.currentUser!=null){
            val intent= Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        countrycode = binding.countryCodepicker.selectedCountryCodeWithPlus
        binding.countryCodepicker.setOnCountryChangeListener {
            countrycode = binding.countryCodepicker.selectedCountryCodeWithPlus
        }

        binding.sentOtp.setOnClickListener{
            val number = binding.getphonenumber.text.toString()
            if (number.isEmpty()) {
                Toast.makeText(this, "Please Enter Your Number", Toast.LENGTH_SHORT).show()
            }else{
                phonenumber = countrycode + number
                val intent= Intent(this, OtpActivity::class.java)
                intent.putExtra("phoneNumber",phonenumber)
                startActivity(intent)

            }

        }
    }
}