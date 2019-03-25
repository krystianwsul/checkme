package com.krystianwsul.checkme.gui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.krystianwsul.checkme.R
import kotlinx.android.synthetic.main.fragment_about.*
import mehdi.sakout.aboutpage.AboutPage
import mehdi.sakout.aboutpage.Element

class AboutFragment : AbstractFragment() {

    companion object {

        fun newInstance() = AboutFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.fragment_about, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        aboutRoot.addView(AboutPage(requireContext()).setImage(R.drawable.ic_launcher_round_try_sign2_unscaled)
                .setDescription(getString(R.string.aboutDescription))
                .addItem(Element().setTitle(getString(R.string.designBy)).setIconDrawable(R.drawable.ic_brush_black_24dp))
                .addEmail("krystianwsul@gmail.com")
                .addPlayStore("com.krystianwsul.checkme")
                .create().apply {
                    findViewById<ImageView>(mehdi.sakout.aboutpage.R.id.image).apply {
                        layoutParams = layoutParams.apply {
                            width = resources.getDimension(R.dimen.splashWidth).toInt()
                            height = resources.getDimension(R.dimen.splashHeight).toInt()
                        }
                    }
                })
    }
}
