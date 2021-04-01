package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay3.PublishRelay
import com.jakewharton.rxrelay3.ReplayRelay
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.firebase.snapshot.UntypedSnapshot
import com.krystianwsul.checkme.utils.getCurrentValue
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DatabaseRxTest {

    private lateinit var compositeDisposable: CompositeDisposable
    private lateinit var databaseObservable: PublishRelay<UntypedSnapshot>
    private lateinit var databaseRx: DatabaseRx<UntypedSnapshot>

    @Before
    fun before() {
        compositeDisposable = CompositeDisposable()
        databaseObservable = PublishRelay.create()
        databaseRx = DatabaseRx(compositeDisposable, databaseObservable)
    }

    @After
    fun after() {
        compositeDisposable.dispose()
    }

    @Test
    fun testObservable() {
        val replayRelay = ReplayRelay.create<Snapshot>()
        databaseRx.observable
                .subscribe(replayRelay)
                .addTo(compositeDisposable)

        databaseObservable.accept(TestUntypedSnapshot())
        databaseObservable.accept(TestUntypedSnapshot())
        databaseObservable.accept(TestUntypedSnapshot())

        assertEquals(replayRelay.values.size, 3)
    }

    @Test
    fun testFirst() {
        val testSnapshot = TestUntypedSnapshot()

        databaseObservable.accept(testSnapshot)
        databaseObservable.accept(TestUntypedSnapshot())
        databaseObservable.accept(TestUntypedSnapshot())

        assertEquals(databaseRx.first.getCurrentValue(), testSnapshot)
    }

    @Test
    fun testChanges() {
        val replayRelay = ReplayRelay.create<Snapshot>()
        databaseRx.changes
                .subscribe(replayRelay)
                .addTo(compositeDisposable)

        val testSnapshot2 = TestUntypedSnapshot()
        val testSnapshot3 = TestUntypedSnapshot()

        databaseObservable.accept(TestUntypedSnapshot())
        databaseObservable.accept(testSnapshot2)
        databaseObservable.accept(testSnapshot3)

        assertEquals(
                replayRelay.values.toList(),
                listOf(testSnapshot2, testSnapshot3)
        )
    }

    @Test
    fun testLatest() {
        val testSnapshot1 = TestUntypedSnapshot()
        databaseObservable.accept(testSnapshot1)
        assertEquals(databaseRx.latest().getCurrentValue(), testSnapshot1)

        val testSnapshot2 = TestUntypedSnapshot()
        databaseObservable.accept(testSnapshot2)
        assertEquals(databaseRx.latest().getCurrentValue(), testSnapshot2)

        val testSnapshot3 = TestUntypedSnapshot()
        databaseObservable.accept(testSnapshot3)
        assertEquals(databaseRx.latest().getCurrentValue(), testSnapshot3)
    }
}