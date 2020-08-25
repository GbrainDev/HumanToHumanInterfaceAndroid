package com.gbrain.humantohuman

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.activity_setting.*


class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        wifi_button.setOnClickListener{
            val sharedPref = getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE)

            with (sharedPref.edit()){
                putString("Wifi_ID", wifi_name_input.toString())
                putString("Wifi_PW", wifi_pw_input.toString())
                commit()
            }

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

}