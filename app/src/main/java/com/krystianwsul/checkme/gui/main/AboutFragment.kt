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
import com.krystianwsul.checkme.databinding.FragmentAboutBinding
import com.krystianwsul.checkme.gui.base.AbstractFragment
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.addTo
import mehdi.sakout.aboutpage.AboutPage
import mehdi.sakout.aboutpage.Element
import java.util.concurrent.TimeUnit

class AboutFragment : AbstractFragment() {

    companion object {

        fun newInstance() = AboutFragment()
    }

    private val bindingProperty = ResettableProperty<FragmentAboutBinding>()
    private var binding by bindingProperty

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = FragmentAboutBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val element = Element().setTitle(getString(R.string.designBy)).setIconDrawable(R.drawable.ic_brush_black_24dp)

        RemoteConfig.observable
                .subscribe { element.intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.beniaUrl)) }
                .addTo(viewCreatedDisposable)

        binding.aboutRoot
                .addView(AboutPage(requireContext()).setImage(R.drawable.ic_launcher_round_try_sign2_unscaled)
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
                .subscribe { binding.aboutRoot.apply { smoothScrollTo(0, bottom) } }
                .addTo(viewCreatedDisposable)
    }

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }
}
