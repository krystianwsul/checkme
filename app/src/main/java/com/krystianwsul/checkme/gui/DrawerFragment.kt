package com.krystianwsul.checkme.gui

import android.net.Uri
import android.os.Bundle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.persistencemodel.SaveService
import kotlinx.android.synthetic.main.nav_header_main.view.*

class DrawerFragment : NoCollapseBottomSheetDialogFragment() {

    companion object {

        private const val KEY_URI = "uri"

        fun newInstance(photo: Uri?) = DrawerFragment().apply {
            arguments = Bundle().apply { putParcelable(KEY_URI, photo) }
        }
    }

    private val mainActivity get() = activity as MainActivity

    private var uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        uri = arguments!!.getParcelable<Uri>(KEY_URI)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme).apply {
        setCancelable(true)
        setContentView(R.layout.main_navigation)
    }

    override fun onStart() {
        super.onStart()

        dialog!!.findViewById<NavigationView>(R.id.mainActivityNavigation).apply {
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
                            DomainFactory.instance.updateToken(SaveService.Source.GUI, null)

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

                    Glide.with(this)
                            .load(uri)
                            .apply(RequestOptions.circleCropTransform())
                            .into(navHeaderPhoto)

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