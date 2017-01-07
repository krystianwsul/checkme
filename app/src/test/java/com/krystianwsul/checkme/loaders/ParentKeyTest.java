package com.krystianwsul.checkme.loaders;

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

import static org.mockito.Matchers.any;

@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {TextUtils.class})
public class ParentKeyTest {
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
    public void testEquals() throws Exception {
        String projectId = "asdf";

        CreateTaskLoader.ParentKey parentKey1 = new CreateTaskLoader.ProjectParentKey(projectId);
        CreateTaskLoader.ParentKey parentKey2 = new CreateTaskLoader.ProjectParentKey(projectId);

        Assert.assertTrue(parentKey1.equals(parentKey2));
    }
}