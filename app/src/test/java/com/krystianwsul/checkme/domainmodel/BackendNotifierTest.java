package com.krystianwsul.checkme.domainmodel;

import android.text.TextUtils;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.mockito.Matchers.any;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TextUtils.class})
public class BackendNotifierTest {
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(TextUtils.class);

        PowerMockito.when(TextUtils.join(any(String.class), any(List.class))).thenAnswer(new Answer<String>() {
            @SuppressWarnings("unchecked")
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                String separator = (String) invocation.getArguments()[0];
                List<String> list = (List<String>) invocation.getArguments()[1];
                Assert.assertTrue(!list.isEmpty());

                StringBuilder answer = new StringBuilder();
                answer.append(list.get(0));

                for (int i = 1; i < list.size(); i++) {
                    answer.append(separator);
                    answer.append(list.get(i));
                }

                return answer.toString();
            }
        });
    }

    @Test
    public void testOneProjectDevelopment() {
        Set<String> projects = new TreeSet<>(Collections.singletonList("-KXvJTar2cCxxrGCtN_w"));
        String correctUrl = "http://check-me-add47.appspot.com/notify?projects=-KXvJTar2cCxxrGCtN_w&sender=asdf";

        String url = BackendNotifier.getUrl(projects, false, "asdf");

        Assert.assertTrue(correctUrl.equals(url));
    }

    @Test
    public void testOneProjectProduction() {
        Set<String> projects = new TreeSet<>(Collections.singletonList("-KXvJTar2cCxxrGCtN_w"));
        String correctUrl = "http://check-me-add47.appspot.com/notify?projects=-KXvJTar2cCxxrGCtN_w&production=1&sender=asdf";

        String url = BackendNotifier.getUrl(projects, true, "asdf");

        Assert.assertTrue(correctUrl.equals(url));
    }

    @Test
    public void testTwoProjectsDevelopment() {
        Set<String> projects = new TreeSet<>(Arrays.asList("-KXvJTar2cCxxrGCtN_w", "asdf"));
        String correctUrl = "http://check-me-add47.appspot.com/notify?projects=-KXvJTar2cCxxrGCtN_w&projects=asdf&sender=asdf";

        String url = BackendNotifier.getUrl(projects, false, "asdf");

        Assert.assertTrue(correctUrl.equals(url));
    }

    @Test
    public void testTwoProjectsProduction() {
        Set<String> projects = new TreeSet<>(Arrays.asList("-KXvJTar2cCxxrGCtN_w", "asdf"));
        String correctUrl = "http://check-me-add47.appspot.com/notify?projects=-KXvJTar2cCxxrGCtN_w&projects=asdf&production=1&sender=asdf";

        String url = BackendNotifier.getUrl(projects, true, "asdf");

        Assert.assertTrue(correctUrl.equals(url));
    }
}