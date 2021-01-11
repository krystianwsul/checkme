package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import android.os.Parcelable

interface ViewModelState {

    fun toSerializableState(): Parcelable
}