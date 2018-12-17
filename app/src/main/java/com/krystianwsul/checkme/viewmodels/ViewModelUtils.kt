package com.krystianwsul.checkme.viewmodels

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders


inline fun <reified T : ViewModel> FragmentActivity.getViewModel() = ViewModelProviders.of(this)[T::class.java]

inline fun <reified T : DomainViewModel<*>> Fragment.getViewModel() = ViewModelProviders.of(this)[T::class.java]