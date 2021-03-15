package com.krystianwsul.checkme.utils

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.MainThreadDisposable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer

fun RecyclerView.pageSelections() = RecyclerViewPagerPageSelectedObservable(this).distinctUntilChanged()!!

val RecyclerView.currentPosition get() = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

class RecyclerViewPagerPageSelectedObservable(private val recyclerViewPager: RecyclerView) : Observable<Int>() {

    override fun subscribeActual(observer: Observer<in Int>) {
        Listener(observer)
    }

    private inner class Listener(private val observer: Observer<in Int>) : MainThreadDisposable() {

        private val onScrollListener = object : RecyclerView.OnScrollListener() {

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (!isDisposed && newState == RecyclerView.SCROLL_STATE_IDLE)
                    observer.onNext(recyclerViewPager.currentPosition)
            }
        }

        init {
            observer.onSubscribe(this)
            recyclerViewPager.addOnScrollListener(onScrollListener)
        }

        override fun onDispose() {
            recyclerViewPager.removeOnScrollListener(onScrollListener)
        }
    }
}