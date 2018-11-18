package com.krystianwsul.checkme.viewmodels

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity


inline fun <reified T : ViewModel> FragmentActivity.getViewModel() = ViewModelProviders.of(this)[T::class.java]

inline fun <reified T : DomainViewModel<*>> Fragment.getViewModel() = ViewModelProviders.of(this)[T::class.java]