package com.krystianwsul.checkme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.MainActivity
import com.krystianwsul.checkme.gui.TutorialActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import kotlinx.android.synthetic.main.main_navigation.*
import kotlinx.android.synthetic.main.nav_header_main.view.*

class DrawerFragment : BottomSheetDialogFragment() {

    companion object {

        fun newInstance() = DrawerFragment()
    }

    private val mainActivity get() = activity as MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.main_navigation, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivityNavigation.run {
            mainActivity.apply {
                setCheckedItem(when (visibleTab.value!!) {
                    MainActivity.Tab.INSTANCES -> R.id.main_drawer_instances
                    MainActivity.Tab.TASKS -> R.id.main_drawer_tasks
                    MainActivity.Tab.PROJECTS -> R.id.main_drawer_projects
                    MainActivity.Tab.CUSTOM_TIMES -> R.id.main_drawer_custom_times
                    MainActivity.Tab.FRIENDS -> R.id.main_drawer_friends
                    MainActivity.Tab.DEBUG -> R.id.main_drawer_debug
                })

                setNavigationItemSelectedListener {

                    when (it.itemId) {
                        R.id.main_drawer_instances -> showTab(MainActivity.Tab.INSTANCES)
                        R.id.main_drawer_tasks -> showTab(MainActivity.Tab.TASKS)
                        R.id.main_drawer_projects -> showTab(MainActivity.Tab.PROJECTS)
                        R.id.main_drawer_custom_times -> showTab(MainActivity.Tab.CUSTOM_TIMES)
                        R.id.main_drawer_friends -> showTab(MainActivity.Tab.FRIENDS)
                        R.id.main_drawer_sign_out -> {
                            val domainFactory = DomainFactory.instance
                            val userInfo = MyApplication.instance.userInfo

                            domainFactory.updateUserInfo(SaveService.Source.GUI, userInfo.copy(token = null))

                            MyApplication.instance.googleSigninClient.signOut()

                            FirebaseAuth.getInstance().signOut()

                            finish()

                            startActivity(TutorialActivity.newLoginIntent())
                        }
                        R.id.main_drawer_tutorial -> startActivity(TutorialActivity.newHelpIntent())
                        R.id.main_drawer_debug -> showTab(MainActivity.Tab.DEBUG)
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

                    FirebaseAuth.getInstance()
                            .currentUser!!
                            .let {
                                navHeaderName.text = it.displayName
                                navHeaderEmail.text = it.email
                            }
                }
            }
        }
    }
}