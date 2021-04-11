package com.bluesolution.driver.ui.Menu

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bluesolution.driver.R
import com.bluesolution.driver.databinding.ActivityMenuBinding
import com.bluesolution.driver.ui.directions.MainActivity
import com.bluesolution.driver.ui.user.User
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Menu : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val actionBar = supportActionBar
        actionBar!!.title = "Menu"
        binding.driver.setOnClickListener {
            startActivity(Intent(this@Menu, MainActivity::class.java))
        }
        binding.user.setOnClickListener {
            startActivity(Intent(this@Menu, User::class.java))
        }
    }

}