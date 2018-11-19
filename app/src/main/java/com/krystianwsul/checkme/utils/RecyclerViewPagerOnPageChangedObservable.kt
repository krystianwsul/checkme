package com.krystianwsul.checkme.utils

import android.support.v7.widget.RecyclerView
import com.lsjwzh.widget.recyclerviewpager.RecyclerViewPager
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable

fun RecyclerViewPager.pageSelections() = RecyclerViewPagerOnPageChangedObservable(this).distinctUntilChanged()

class RecyclerViewPagerOnPageChangedObservable(private val recyclerViewPager: RecyclerViewPager) : Observable<Int>() {

    override fun subscribeActual(observer: Observer<in Int>) = observer.onSubscribe(Listener(observer))

    private inner class Listener(observer: Observer<in Int>) : MainThreadDisposable() {

        private val onPageChangedListener = RecyclerViewPager.OnPageChangedListener { _, position ->
            if (!isDisposed)
                observer.onNext(position)
        }

        private val onScrollListener = object : RecyclerView.OnScrollListener() {

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (!isDisposed && newState == RecyclerView.SCROLL_STATE_IDLE)
                    observer.onNext(recyclerViewPager.currentPosition)
            }
        }

        init {
            recyclerViewPager.addOnPageChangedListener(onPageChangedListener)
            recyclerViewPager.addOnScrollListener(onScrollListener)
        }

        override fun onDispose() {
            recyclerViewPager.removeOnPageChangedListener(onPageChangedListener)
            recyclerViewPager.removeOnScrollListener(onScrollListener)
        }
    }
}