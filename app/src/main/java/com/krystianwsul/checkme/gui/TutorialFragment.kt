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

        tutorialImage.setImageResource(when (position) {
            0 -> R.drawable.tutorial_1
            1 -> R.drawable.tutorial_2
            2 -> R.drawable.tutorial_3
            3 -> R.drawable.tutorial_4
            else -> throw IllegalArgumentException()
        })

        tutorialText.setText(when (position) {
            0 -> R.string.tutorial1
            1 -> R.string.tutorial2
            2 -> R.string.tutorial3
            3 -> R.string.tutorial4
            else -> throw IllegalArgumentException()
        })
    }
}
