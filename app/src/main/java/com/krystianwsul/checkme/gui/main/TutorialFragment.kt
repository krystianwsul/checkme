package com.krystianwsul.checkme.gui.main


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.FragmentTutorialBinding
import com.krystianwsul.checkme.gui.base.AbstractFragment
import com.krystianwsul.checkme.gui.utils.ResettableProperty

class TutorialFragment : AbstractFragment() {

    companion object {

        private const val POSITION_KEY = "position"

        fun newInstance(position: Int) = TutorialFragment().apply {
            arguments = Bundle().apply { putInt(POSITION_KEY, position) }
        }
    }

    private var position = 0

    private val bindingProperty = ResettableProperty<FragmentTutorialBinding>()
    private var binding by bindingProperty

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        position = requireArguments().getInt(POSITION_KEY)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = FragmentTutorialBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tutorialSignIn?.visibility = if ((activity as TutorialActivity).help) View.GONE else View.INVISIBLE

        binding.tutorialImage.setImageResource(when (position) {
            0 -> R.drawable.tutorial_1
            1 -> R.drawable.tutorial_2
            2 -> R.drawable.tutorial_3
            3 -> R.drawable.tutorial_4
            else -> throw IllegalArgumentException()
        })

        binding.tutorialText.setText(when (position) {
            0 -> R.string.tutorial1
            1 -> R.string.tutorial2
            2 -> R.string.tutorial3
            3 -> R.string.tutorial4
            else -> throw IllegalArgumentException()
        })
    }

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }
}
