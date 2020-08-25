package com.gbrain.humantohuman.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.gbrain.humantohuman.R
import kotlinx.android.synthetic.main.fragment_chart.*
import kotlinx.android.synthetic.main.fragment_info.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class InfoFragment : Fragment() {
    var container: ViewGroup? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        this.container = container
        return inflater.inflate(R.layout.fragment_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGUI()
    }

    private fun setupGUI() {
        web_image.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW);
            val uri = Uri.parse("http://gbrainlife.com/");
            intent.setData(uri);
            startActivity(intent);
        }

        license_button.setOnClickListener {
            container?.also {
                val builder = AlertDialog.Builder(requireContext(), android.R.style.ThemeOverlay_Material_Dialog_Alert)
                builder.setTitle("Open Source Licenses.txt")
                    .setNeutralButton("Close", null)
                    .setView(loadLicenseView())
                builder.create().show()
            }
        }
    }

    private fun loadLicenseView(): View {
        val inflater = LayoutInflater.from(requireActivity())
        val licenseView = inflater.inflate(R.layout.layout_license_viewer, container!!, false)
        val licenseTextView = licenseView.findViewById<TextView>(R.id.license_text)
        licenseTextView.setText(loadLicenseContents())
        return licenseView
    }

    private fun loadLicenseContents(): String {
        val fileInputStream = resources.openRawResource(R.raw.license_enumeration)
        val contents = fileInputStream.readBytes()
        fileInputStream.close()
        return String(contents)
    }
}