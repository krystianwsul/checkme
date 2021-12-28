package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.domainmodel.extensions.createCustomTime
import com.krystianwsul.checkme.domainmodel.extensions.updateCustomTime
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.models.cache.Invalidatable
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CustomTimesInvalidatableManagerTest {

    @get:Rule
    val domainFactoryRule = DomainFactoryRule()

    private val domainFactory get() = domainFactoryRule.domainFactory

    private fun domainUpdater(now: ExactTimeStamp.Local = ExactTimeStamp.Local.now) =
        TestDomainUpdater(domainFactory, now)

    @Test
    fun testLocalEdit() {
        val date = Date(2021, 12, 28)

        val now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val customTimeKey = domainUpdater(now).createCustomTime(
            DomainListenerManager.NotificationType.All,
            "custom time",
            DayOfWeek.values().associateWith { HourMinute(1, 0) },
        ).blockingGet()

        val testInvalidatable = TestInvalidatable()

        domainFactoryRule.rootModelChangeManager
            .customTimesInvalidatableManager
            .addInvalidatable(testInvalidatable)

        domainUpdater(now).updateCustomTime(
            DomainListenerManager.NotificationType.All,
            customTimeKey,
            "custom time",
            DayOfWeek.values().associateWith { HourMinute(2, 0) },
        ).blockingSubscribe()

        testInvalidatable.assertInvalidated()
    }

    @Test
    fun testRemoteMyUserChange() {
        val testInvalidatable = TestInvalidatable()

        domainFactoryRule.rootModelChangeManager
            .customTimesInvalidatableManager
            .addInvalidatable(testInvalidatable)

        domainFactory.myUserFactory.onNewSnapshot(
            Snapshot(domainFactory.myUserFactory.user.userKey.key, UserWrapper())
        )

        testInvalidatable.assertInvalidated()
    }

    class TestInvalidatable : Invalidatable {

        private var invalidated = false

        override fun invalidate() {
            invalidated = true
        }

        fun assertInvalidated() {
            assertTrue(invalidated)

            invalidated = false
        }
    }
}