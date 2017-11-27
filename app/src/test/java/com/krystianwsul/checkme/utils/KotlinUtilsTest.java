package com.krystianwsul.checkme.utils;

import com.krystianwsul.checkme.utils.time.DayOfWeek;

import junit.framework.Assert;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KotlinUtilsTest {

    @Test
    public void testEmpty() {
        List<DayOfWeek> input = new ArrayList<>();
        List<List<DayOfWeek>> output = KotlinUtils.INSTANCE.getRanges(input);
        List<List<DayOfWeek>> answer = new ArrayList<>();
        Assert.assertTrue(output.equals(answer));
    }

    @Test
    public void testSingle() {
        Assert.assertTrue(KotlinUtils.INSTANCE.getRanges(Collections.singletonList(DayOfWeek.WEDNESDAY)).equals(Collections.singletonList(Collections.singletonList(DayOfWeek.WEDNESDAY))));
    }

    @Test
    public void testDoubleAdjacent() {
        List<DayOfWeek> input = Arrays.asList(DayOfWeek.TUESDAY, DayOfWeek.MONDAY);
        List<List<DayOfWeek>> output = KotlinUtils.INSTANCE.getRanges(input);
        List<List<DayOfWeek>> answer = Collections.singletonList(Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY));
        Assert.assertTrue(output.equals(answer));
    }

    @Test
    public void testDoubleNotAdjacent() {
        Assert.assertTrue(KotlinUtils.INSTANCE.getRanges(Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.TUESDAY)).equals(Arrays.asList(Collections.singletonList(DayOfWeek.TUESDAY), Collections.singletonList(DayOfWeek.SATURDAY))));
    }

    @Test
    public void testMissingWednesday() {
        Assert.assertTrue(KotlinUtils.INSTANCE.getRanges(Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.TUESDAY)).equals(Arrays.asList(Arrays.asList(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY), Arrays.asList(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY))));
    }
}
