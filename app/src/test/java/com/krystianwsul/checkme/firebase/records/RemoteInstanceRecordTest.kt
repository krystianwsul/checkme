package com.krystianwsul.checkme.firebase.records

import android.text.TextUtils
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.ScheduleKey
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.TimePair
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(TextUtils::class)
class RemoteInstanceRecordTest {
    @Mock
    private val mDomainFactory: DomainFactory? = null

    @Before
    fun setUp() {
        PowerMockito.mockStatic(TextUtils::class.java)

        PowerMockito.`when`(TextUtils.isEmpty(any(CharSequence::class.java))).thenAnswer { invocation: InvocationOnMock ->
            (invocation.arguments[0] as? CharSequence).let { a ->
                !(a != null && a.isNotEmpty())
            }
        }

        Mockito.`when`(mDomainFactory!!.getCustomTimeKey(any(String::class.java), any(String::class.java))).thenAnswer { invocation ->
            val remoteProjectId = invocation.arguments[0] as String
            val remoteCustomTimeId = invocation.arguments[1] as String

            CustomTimeKey(remoteProjectId, remoteCustomTimeId)
        }

        Mockito.`when`(mDomainFactory.getRemoteCustomTimeId(any(String::class.java), any(CustomTimeKey::class.java))).thenAnswer { invocation: InvocationOnMock ->
            val (_, _, remoteCustomTimeId) = invocation.arguments[1] as CustomTimeKey

            remoteCustomTimeId
        }
    }

    @Test
    fun testScheduleKeyToStringHourMinute() {
        val scheduleKey = ScheduleKey(Date(2016, 11, 26), TimePair(HourMinute(9, 47)))

        val key = "2016-11-26-9-47"

        Assert.assertTrue(RemoteInstanceRecord.scheduleKeyToString(mDomainFactory!!, "asdf", scheduleKey) == key)
    }

    @Test
    fun testScheduleKeyToStringCustomTime() {
        val scheduleKey = ScheduleKey(Date(2016, 11, 27), TimePair(CustomTimeKey("asdf", "-KX_IHXkMcoAqwTBfN_k")))

        val key = "2016-11-27--KX_IHXkMcoAqwTBfN_k"

        val otherKey = RemoteInstanceRecord.scheduleKeyToString(mDomainFactory!!, "asdf", scheduleKey)

        Assert.assertTrue(otherKey == key)
    }

    @Test
    fun stringToScheduleKeyHourMinute() {
        val scheduleKey = ScheduleKey(Date(2016, 11, 26), TimePair(HourMinute(9, 47)))

        val key = "2016-11-26-9-47"

        Assert.assertTrue(RemoteInstanceRecord.stringToScheduleKey(mDomainFactory!!, "asdf", key) == scheduleKey)
    }

    @Test
    fun stringToScheduleKeyCustomTime() {
        val scheduleKey = ScheduleKey(Date(2016, 11, 27), TimePair(CustomTimeKey("asdf", "-KX_IHXkMcoAqwTBfN_k")))

        val key = "2016-11-27--KX_IHXkMcoAqwTBfN_k"

        Assert.assertTrue(RemoteInstanceRecord.stringToScheduleKey(mDomainFactory!!, "asdf", key) == scheduleKey)
    }
}