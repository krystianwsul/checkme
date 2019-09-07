package com.krystianwsul.checkme.viewmodels

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


inline fun <reified T : ViewModel> FragmentActivity.getViewModel() = ViewModelProvider(this)[T::class.java]

inline fun <reified T : ViewModel> Fragment.getViewModel() = ViewModelProvider(this)[T::class.java]