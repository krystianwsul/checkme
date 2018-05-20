package com.krystianwsul.checkme.gui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.krystianwsul.checkme.R
import kotlinx.android.synthetic.main.fragment_tutorial.*

class TutorialFragment : AbstractFragment() {

    companion object {

        private const val POSITION_KEY = "position"

        fun newInstance(position: Int) = TutorialFragment().apply {
            arguments = Bundle().apply {
                putInt(POSITION_KEY, position)
            }
        }
    }

    private var position = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        position = arguments!!.getInt(POSITION_KEY)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.fragment_tutorial, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tutorialText.text = "position $position"
    }
}
