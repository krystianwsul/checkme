package com.krystianwsul.checkme.firebase.records;

import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.ScheduleKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.any;

@SuppressWarnings("CanBeFinal")
@RunWith(PowerMockRunner.class)
@PrepareForTest({TextUtils.class})
public class RemoteInstanceRecordTest {
    @Mock
    private DomainFactory mDomainFactory;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(TextUtils.class);

        PowerMockito.when(TextUtils.isEmpty(any(CharSequence.class))).thenAnswer((Answer<Boolean>) invocation -> {
            CharSequence a = (CharSequence) invocation.getArguments()[0];
            return !(a != null && a.length() > 0);
        });

        Mockito.when(mDomainFactory.getCustomTimeKey(any(String.class), any(String.class))).thenAnswer((Answer<CustomTimeKey>) invocation -> {
            String remoteProjectId = (String) invocation.getArguments()[0];
            String remoteCustomTimeId = (String) invocation.getArguments()[1];

            return new CustomTimeKey(remoteProjectId, remoteCustomTimeId);
        });

        Mockito.when(mDomainFactory.getRemoteCustomTimeId(any(String.class), any(CustomTimeKey.class))).thenAnswer((Answer<String>) invocation -> {
            CustomTimeKey customTimeKey = (CustomTimeKey) invocation.getArguments()[1];

            return customTimeKey.getRemoteCustomTimeId();
        });
    }

    @Test
    public void testScheduleKeyToStringHourMinute() {
        ScheduleKey scheduleKey = new ScheduleKey(new Date(2016, 11, 26), new TimePair(new HourMinute(9, 47)));

        String key = "2016-11-26-9-47";

        Assert.assertTrue(RemoteInstanceRecord.Companion.scheduleKeyToString(mDomainFactory, "asdf", scheduleKey).equals(key));
    }

    @Test
    public void testScheduleKeyToStringCustomTime() {
        ScheduleKey scheduleKey = new ScheduleKey(new Date(2016, 11, 27), new TimePair(new CustomTimeKey("asdf", "-KX_IHXkMcoAqwTBfN_k")));

        String key = "2016-11-27--KX_IHXkMcoAqwTBfN_k";

        String otherKey = RemoteInstanceRecord.Companion.scheduleKeyToString(mDomainFactory, "asdf", scheduleKey);

        Assert.assertTrue(otherKey.equals(key));
    }

    @Test
    public void stringToScheduleKeyHourMinute() {
        ScheduleKey scheduleKey = new ScheduleKey(new Date(2016, 11, 26), new TimePair(new HourMinute(9, 47)));

        String key = "2016-11-26-9-47";

        Assert.assertTrue(RemoteInstanceRecord.Companion.stringToScheduleKey(mDomainFactory, "asdf", key).equals(scheduleKey));
    }

    @Test
    public void stringToScheduleKeyCustomTime() {
        ScheduleKey scheduleKey = new ScheduleKey(new Date(2016, 11, 27), new TimePair(new CustomTimeKey("asdf", "-KX_IHXkMcoAqwTBfN_k")));

        String key = "2016-11-27--KX_IHXkMcoAqwTBfN_k";

        Assert.assertTrue(RemoteInstanceRecord.Companion.stringToScheduleKey(mDomainFactory, "asdf", key).equals(scheduleKey));
    }
}