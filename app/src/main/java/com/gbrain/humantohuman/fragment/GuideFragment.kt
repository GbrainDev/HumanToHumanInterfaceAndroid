package com.gbrain.humantohuman.fragment

import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.gbrain.humantohuman.R
import kotlinx.android.synthetic.main.fragment_guide.*
import kotlinx.android.synthetic.main.fragment_guide.view.*
import kotlinx.android.synthetic.main.fragment_main.*
import kr.co.prnd.YouTubePlayerView

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

/**
 * A simple [Fragment] subclass.
 * Use the [GuideFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class GuideFragment : Fragment() {

    private var lang = 0 // 0:kor 1:eng

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_guide, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val youTubePlayerView: YouTubePlayerView = view.you_tube_player_view
        youTubePlayerView.play("fGMDq3ay7Ro")
        btn_lang.setOnClickListener {
            if (lang == 0) {
                txt_guide.text = getString(R.string.guide_description_kor)
                btn_lang.text= "English"
                lang = 1
            }
            else {
                txt_guide.text = getString(R.string.guide_description_eng)
                btn_lang.text= "한국어"
                lang = 0
            }
        }
    }


}