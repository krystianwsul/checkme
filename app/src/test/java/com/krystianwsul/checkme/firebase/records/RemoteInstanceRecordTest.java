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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.any;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TextUtils.class})
public class RemoteInstanceRecordTest {
    @Mock
    private DomainFactory mDomainFactory;

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

        Mockito.when(mDomainFactory.getCustomTimeKey(any(String.class), any(String.class))).thenAnswer(new Answer<CustomTimeKey>() {
            @Override
            public CustomTimeKey answer(InvocationOnMock invocation) {
                String remoteProjectId = (String) invocation.getArguments()[0];
                String remoteCustomTimeId = (String) invocation.getArguments()[1];

                return new CustomTimeKey(remoteProjectId, remoteCustomTimeId);
            }
        });

        Mockito.when(mDomainFactory.getRemoteCustomTimeId(any(String.class), any(CustomTimeKey.class))).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) {
                CustomTimeKey customTimeKey = (CustomTimeKey) invocation.getArguments()[1];

                return customTimeKey.mRemoteCustomTimeId;
            }
        });
    }

    @Test
    public void testScheduleKeyToStringHourMinute() throws Exception {
        ScheduleKey scheduleKey = new ScheduleKey(new Date(2016, 11, 26), new TimePair(new HourMinute(9, 47)));

        String key = "2016-11-26-9-47";

        Assert.assertTrue(RemoteInstanceRecord.scheduleKeyToString(mDomainFactory, "asdf", scheduleKey).equals(key));
    }

    @Test
    public void testScheduleKeyToStringCustomTime() throws Exception {
        ScheduleKey scheduleKey = new ScheduleKey(new Date(2016, 11, 27), new TimePair(new CustomTimeKey("asdf", "-KX_IHXkMcoAqwTBfN_k")));

        String key = "2016-11-27--KX_IHXkMcoAqwTBfN_k";

        String otherKey = RemoteInstanceRecord.scheduleKeyToString(mDomainFactory, "asdf", scheduleKey);

        Assert.assertTrue(otherKey.equals(key));
    }

    @Test
    public void stringToScheduleKeyHourMinute() throws Exception {
        ScheduleKey scheduleKey = new ScheduleKey(new Date(2016, 11, 26), new TimePair(new HourMinute(9, 47)));

        String key = "2016-11-26-9-47";

        Assert.assertTrue(RemoteInstanceRecord.stringToScheduleKey(mDomainFactory, "asdf", key).equals(scheduleKey));
    }

    @Test
    public void stringToScheduleKeyCustomTime() throws Exception {
        ScheduleKey scheduleKey = new ScheduleKey(new Date(2016, 11, 27), new TimePair(new CustomTimeKey("asdf", "-KX_IHXkMcoAqwTBfN_k")));

        String key = "2016-11-27--KX_IHXkMcoAqwTBfN_k";

        Assert.assertTrue(RemoteInstanceRecord.stringToScheduleKey(mDomainFactory, "asdf", key).equals(scheduleKey));
    }
}