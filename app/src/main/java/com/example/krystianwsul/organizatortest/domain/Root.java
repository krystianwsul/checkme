package com.example.krystianwsul.organizatortest.domain;

import com.example.krystianwsul.organizatortest.domain.tasks.BranchTask;
import com.example.krystianwsul.organizatortest.domain.tasks.LeafTask;
import com.example.krystianwsul.organizatortest.domain.tasks.StubTask;
import com.example.krystianwsul.organizatortest.domain.tasks.TopTask;
import com.example.krystianwsul.organizatortest.domain.tasks.TrunkTask;
import com.example.krystianwsul.organizatortest.timing.Date;
import com.example.krystianwsul.organizatortest.timing.DateTime;
import com.example.krystianwsul.organizatortest.timing.TimeStamp;
import com.example.krystianwsul.organizatortest.timing.schedules.Schedule;
import com.example.krystianwsul.organizatortest.timing.schedules.SingleSchedule;
import com.example.krystianwsul.organizatortest.timing.schedules.WeeklySchedule;
import com.example.krystianwsul.organizatortest.timing.times.CustomTime;
import com.example.krystianwsul.organizatortest.timing.times.HourMinute;
import com.example.krystianwsul.organizatortest.timing.times.NormalTime;
import com.example.krystianwsul.organizatortest.timing.times.Time;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

/**
 * Created by Krystian on 10/17/2015.
 */
public class Root implements Iterable<TopTask> {
    private static Root mRoot;

    private ArrayList<TopTask> mChildren = new ArrayList<>();

    private Root() {
        Calendar calendarToday = Calendar.getInstance();

        Calendar calendarFewDaysAgo = Calendar.getInstance();
        calendarFewDaysAgo.add(Calendar.DATE, -10);

        Calendar calendarYesterday = Calendar.getInstance();
        calendarYesterday.add(Calendar.DATE, -1);

        Calendar calendarNextYear = Calendar.getInstance();
        calendarNextYear.add(Calendar.DATE, 365);

        Date today = new Date(calendarToday);
        Date yesterday = new Date(calendarYesterday);
        Date nextYear = new Date(calendarNextYear);

        HourMinute sixAm = new HourMinute(6, 0);
        HourMinute nineAm = new HourMinute(9, 0);

        HourMinute fivePm = new HourMinute(17, 0);

        Time afterWaking = new CustomTime("Po Wstaniu", sixAm, sixAm, sixAm, sixAm, sixAm, nineAm, nineAm);
        Time afterWork = new CustomTime("Po Pracy", fivePm, fivePm, fivePm, fivePm, fivePm, null, null);

        Schedule todayAfterWaking = new SingleSchedule(new DateTime(today, afterWaking));
        Schedule yesterdayAfterWaking = new SingleSchedule(new DateTime(yesterday, afterWaking));

        Schedule today15 = new SingleSchedule(new DateTime(today, new NormalTime(new HourMinute(15, 0))));
        Schedule yesterday16 = new SingleSchedule(new DateTime(yesterday, new NormalTime(new HourMinute(16, 0))));

        Schedule alwaysAfterWaking = new WeeklySchedule(new TimeStamp(calendarFewDaysAgo), new TimeStamp(calendarNextYear), afterWaking);
        Schedule alwaysAfterWork = new WeeklySchedule(new TimeStamp(calendarFewDaysAgo), new TimeStamp(calendarNextYear), afterWork);

        ArrayList<Time> afterWakingAfterWork = new ArrayList<>();
        afterWakingAfterWork.add(afterWaking);
        afterWakingAfterWork.add(afterWork);
        Schedule alwaysAfterWakingAfterWork = new WeeklySchedule(new TimeStamp(calendarFewDaysAgo), new TimeStamp(calendarNextYear), afterWakingAfterWork);

        TrunkTask zakupy = new TrunkTask("Zakupy", today15);

        zakupy.addChild(new LeafTask("halls", zakupy));

        BranchTask biedronka = new BranchTask("Biedronka", zakupy);
        biedronka.addChild(new LeafTask("piersi", biedronka));
        biedronka.addChild(new LeafTask("czosnek", biedronka));
        zakupy.addChild(biedronka);

        mChildren.add(zakupy);

        StubTask rachunek = new StubTask("Zapłacić rachunek", yesterday16);
        mChildren.add(rachunek);

        StubTask banany = new StubTask("Banany", todayAfterWaking);
        mChildren.add(banany);

        StubTask iliotibial = new StubTask("Iliotibial band stretch", alwaysAfterWakingAfterWork);
        mChildren.add(iliotibial);

        StubTask hamstring = new StubTask("Hamstring stretch", alwaysAfterWork);
        mChildren.add(hamstring);
    }

    public static Root getInstance() {
        if (mRoot == null)
            mRoot = new Root();
        return mRoot;
    }

    public Iterator<TopTask> iterator() {
        return mChildren.iterator();
    }
}
