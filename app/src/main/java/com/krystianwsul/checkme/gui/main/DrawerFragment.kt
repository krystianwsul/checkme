package com.krystianwsul.checkme.gui.main

import android.graphics.Outline
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import com.google.firebase.auth.FirebaseAuth
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.MainNavigationBinding
import com.krystianwsul.checkme.databinding.NavHeaderMainBinding
import com.krystianwsul.checkme.gui.base.NoCollapseBottomSheetDialogFragment
import com.krystianwsul.checkme.gui.main.MainActivity.Tab
import com.krystianwsul.checkme.gui.main.MainActivity.TabSearchState
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import com.krystianwsul.checkme.utils.loadPhoto
import com.krystianwsul.checkme.viewmodels.DrawerViewModel
import io.reactivex.rxjava3.kotlin.addTo

class DrawerFragment : NoCollapseBottomSheetDialogFragment() {

    companion object {

        fun newInstance() = DrawerFragment()
    }

    override val alwaysExpand = false

    override val backgroundView get() = binding.drawerRoot
    override val contentView get() = binding.drawerContentWrapper

    private val mainActivity get() = activity as MainActivity

    private val drawerViewModel by lazy { DrawerViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        drawerViewModel.start()
    }

    private val bindingProperty = ResettableProperty<MainNavigationBinding>()
    private var binding by bindingProperty

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = MainNavigationBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.drawerBackgroundLayout.apply {
            outlineProvider = object : ViewOutlineProvider() {

                private val radius = resources.getDimension(R.dimen.bottom_sheet_corner_radius)

                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(
                            0,
                            0,
                            view.width,
                            view.height + radius.toInt(),
                            radius
                    )
                }
            }

            clipToOutline = true
        }

        binding.mainActivityNavigation.apply {
            mainActivity.apply {
                setCheckedItem(when (tabSearchStateRelay.value!!.tab) {
                    Tab.INSTANCES -> R.id.main_drawer_instances
                    Tab.TASKS -> R.id.main_drawer_tasks
                    Tab.PROJECTS -> R.id.main_drawer_projects
                    Tab.CUSTOM_TIMES -> R.id.main_drawer_custom_times
                    Tab.FRIENDS -> R.id.main_drawer_friends
                    Tab.DEBUG -> R.id.main_drawer_debug
                    Tab.ABOUT -> R.id.main_drawer_about
                })

                setNavigationItemSelectedListener {
                    when (it.itemId) {
                        R.id.main_drawer_instances -> setTabSearchState(TabSearchState.Instances(false))
                        R.id.main_drawer_tasks -> setTabSearchState(TabSearchState.Tasks(false))
                        R.id.main_drawer_projects -> setTabSearchState(TabSearchState.Projects)
                        R.id.main_drawer_custom_times -> setTabSearchState(TabSearchState.CustomTimes)
                        R.id.main_drawer_friends -> setTabSearchState(TabSearchState.Friends)
                        R.id.main_drawer_about -> setTabSearchState(TabSearchState.About)
                        R.id.main_drawer_debug -> setTabSearchState(TabSearchState.Debug)
                        R.id.main_drawer_settings -> startActivity(SettingsActivity.newIntent())
                        R.id.main_drawer_tutorial -> startActivity(TutorialActivity.newHelpIntent())
                        R.id.main_drawer_sign_out -> {
                            Preferences.token = null

                            MyApplication.instance.googleSignInClient.signOut()
                            FirebaseAuth.getInstance().signOut()

                            finish()

                            startActivity(TutorialActivity.newLoginIntent())
                        }
                        else -> throw IndexOutOfBoundsException()
                    }

                    updateBottomMenu()

                    dismiss()

                    true
                }

                fun updateDebug() {
                    menu.findItem(R.id.main_drawer_debug).isVisible = debug
                }

                updateDebug()

                getHeaderView(0)!!.run {
                    setOnLongClickListener {
                        debug = true

                        updateDebug()
                        true
                    }

                    val headerBinding = NavHeaderMainBinding.bind(this)

                    drawerViewModel.data
                            .subscribe {
                                headerBinding.navHeaderPhoto.loadPhoto(it.photoUrl)
                                headerBinding.navHeaderName.text = it.name
                                headerBinding.navHeaderEmail.text = it.email
                            }
                            .addTo(viewCreatedDisposable)
                }
            }
        }
    }

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }
}