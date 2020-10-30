package com.krystianwsul.checkme.gui.main


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.krystianwsul.checkme.BuildConfig
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.RemoteConfig
import com.krystianwsul.checkme.gui.base.AbstractFragment
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_about.*
import mehdi.sakout.aboutpage.AboutPage
import mehdi.sakout.aboutpage.Element
import java.util.concurrent.TimeUnit

class AboutFragment : AbstractFragment() {

    companion object {

        fun newInstance() = AboutFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.fragment_about, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val element = Element().setTitle(getString(R.string.designBy)).setIconDrawable(R.drawable.ic_brush_black_24dp)

        RemoteConfig.observable
                .subscribe { element.intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.beniaUrl)) }
                .addTo(viewCreatedDisposable)

        aboutRoot.addView(AboutPage(requireContext()).setImage(R.drawable.ic_launcher_round_try_sign2_unscaled)
                .setDescription(getString(R.string.aboutDescription))
                .addItem(element)
                .addEmail("krystianwsul@gmail.com")
                .addPlayStore("com.krystianwsul.checkme")
                .addItem(Element().setTitle(getString(R.string.version) + " " + BuildConfig.VERSION_NAME))
                .create().apply {
                    findViewById<ImageView>(mehdi.sakout.aboutpage.R.id.image).apply {
                        layoutParams = layoutParams.apply {
                            width = resources.getDimension(R.dimen.splashWidth).toInt()
                            height = resources.getDimension(R.dimen.splashHeight).toInt()
                        }
                    }
                })
    }

    fun onShown() {
        Observable.just(Unit)
                .delay(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { aboutRoot.apply { smoothScrollTo(0, bottom) } }
                .addTo(viewCreatedDisposable)
    }
}
