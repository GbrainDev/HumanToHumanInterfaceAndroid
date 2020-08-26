package com.gbrain.humantohuman

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import kotlinx.android.synthetic.main.activity_setting.*


class SettingActivity : AppCompatActivity() {

    lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        sharedPref = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        setupGUI()
        loadStoredWifiCredentials()
    }

    private fun loadStoredWifiCredentials() {
        val wifiName = sharedPref.getString(SHARED_PREF_WIFI_NAME, "")
        wifi_name_input.setText(wifiName)
        val wifiPasswd = sharedPref.getString(SHARED_PREF_WIFI_PASSWD, "")
        wifi_pw_input.setText(wifiPasswd)
    }

    private fun setupGUI() {
        wifi_button.setOnClickListener{
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