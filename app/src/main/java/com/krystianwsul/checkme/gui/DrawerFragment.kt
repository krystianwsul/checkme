package com.krystianwsul.checkme.gui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
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
        setInsetViews(drawerRoot, drawerBackgroundLayout)

        mainActivityNavigation.apply {
            mainActivity.apply {
                setCheckedItem(when (visibleTab.value!!) {
                    MainActivity.Tab.INSTANCES -> R.id.main_drawer_instances
                    MainActivity.Tab.TASKS -> R.id.main_drawer_tasks
                    MainActivity.Tab.PROJECTS -> R.id.main_drawer_projects
                    MainActivity.Tab.CUSTOM_TIMES -> R.id.main_drawer_custom_times
                    MainActivity.Tab.FRIENDS -> R.id.main_drawer_friends
                    MainActivity.Tab.DEBUG -> R.id.main_drawer_debug
                    MainActivity.Tab.ABOUT -> R.id.main_drawer_about
                })

                setNavigationItemSelectedListener {
                    when (it.itemId) {
                        R.id.main_drawer_instances -> showTab(MainActivity.Tab.INSTANCES)
                        R.id.main_drawer_tasks -> showTab(MainActivity.Tab.TASKS)
                        R.id.main_drawer_projects -> showTab(MainActivity.Tab.PROJECTS)
                        R.id.main_drawer_custom_times -> showTab(MainActivity.Tab.CUSTOM_TIMES)
                        R.id.main_drawer_friends -> showTab(MainActivity.Tab.FRIENDS)
                        R.id.main_drawer_about -> showTab(MainActivity.Tab.ABOUT)
                        R.id.main_drawer_debug -> showTab(MainActivity.Tab.DEBUG)
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

    override fun onCreateDialog(savedInstanceState: Bundle?) = TransparentNavigationDialog().apply {
        setCancelable(true)
    }
}