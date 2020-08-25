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
            val sharedPref = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)

            with (sharedPref.edit()){
                putString(SHARED_PREF_WIFI_NAME, wifi_name_input.text.toString())
                putString(SHARED_PREF_WIFI_PASSWD, wifi_pw_input.text.toString())
                commit()
            }

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

}