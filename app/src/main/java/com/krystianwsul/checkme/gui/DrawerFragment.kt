package com.krystianwsul.checkme.gui

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
import com.krystianwsul.checkme.gui.MainActivity.Tab
import com.krystianwsul.checkme.gui.MainActivity.TabSearchState
import com.krystianwsul.checkme.utils.loadPhoto
import com.krystianwsul.checkme.viewmodels.DrawerViewModel
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.main_navigation.*
import kotlinx.android.synthetic.main.nav_header_main.view.*

class DrawerFragment : NoCollapseBottomSheetDialogFragment() {

    companion object {

        fun newInstance() = DrawerFragment()
    }

    override val alwaysExpand = false

    override val backgroundView get() = drawerRoot!!
    override val contentView get() = drawerContentWrapper!!

    private val mainActivity get() = activity as MainActivity

    private val drawerViewModel by lazy { DrawerViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        drawerViewModel.start()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.main_navigation, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        drawerBackgroundLayout.apply {
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

        mainActivityNavigation.apply {
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

                    drawerViewModel.data
                            .subscribe {
                                navHeaderPhoto.loadPhoto(it.photoUrl)
                                navHeaderName.text = it.name
                                navHeaderEmail.text = it.email
                            }
                            .addTo(viewCreatedDisposable)
                }
            }
        }
    }
}