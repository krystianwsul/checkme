package com.krystianwsul.checkme.firebase.records;

import android.text.TextUtils;

import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.ScheduleKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.any;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TextUtils.class})
public class RemoteInstanceRecordTest {
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(TextUtils.class);

        PowerMockito.when(TextUtils.isEmpty(any(CharSequence.class))).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                CharSequence a = (CharSequence) invocation.getArguments()[0];
                return !(a != null && a.length() > 0);
            }
        });
    }

    @Test
    public void testScheduleKeyToStringHourMinute() throws Exception {
        ScheduleKey scheduleKey = new ScheduleKey(new Date(2016, 11, 26), new TimePair(new HourMinute(9, 47)));

        String key = "2016-11-26-9-47";

        Assert.assertTrue(RemoteInstanceRecord.scheduleKeyToString(scheduleKey).equals(key));
    }

    @Test
    public void testScheduleKeyToStringCustomTime() throws Exception {
        ScheduleKey scheduleKey = new ScheduleKey(new Date(2016, 11, 27), new TimePair(new CustomTimeKey("-KX_IHXkMcoAqwTBfN_k")));

        String key = "2016-11-27--KX_IHXkMcoAqwTBfN_k";

        Assert.assertTrue(RemoteInstanceRecord.scheduleKeyToString(scheduleKey).equals(key));
    }

    @Test
    public void stringToScheduleKeyHourMinute() throws Exception {
        ScheduleKey scheduleKey = new ScheduleKey(new Date(2016, 11, 26), new TimePair(new HourMinute(9, 47)));

        String key = "2016-11-26-9-47";

        Assert.assertTrue(RemoteInstanceRecord.stringToScheduleKey(key).equals(scheduleKey));
    }

    @Test
    public void stringToScheduleKeyCustomTime() throws Exception {
        ScheduleKey scheduleKey = new ScheduleKey(new Date(2016, 11, 27), new TimePair(new CustomTimeKey("-KX_IHXkMcoAqwTBfN_k")));

        String key = "2016-11-27--KX_IHXkMcoAqwTBfN_k";

        Assert.assertTrue(RemoteInstanceRecord.stringToScheduleKey(key).equals(scheduleKey));
    }
}