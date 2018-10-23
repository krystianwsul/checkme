package com.krystianwsul.checkme.loaders

import android.text.TextUtils
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(value = [(TextUtils::class)])
class ParentKeyTest {

    @Before
    fun setUp() {
        PowerMockito.mockStatic(TextUtils::class.java)

        PowerMockito.`when`(TextUtils.isEmpty(any(CharSequence::class.java))).thenAnswer { invocation ->
            (invocation.arguments[0] as? CharSequence).let { a ->
                !(a != null && a.isNotEmpty())
            }
        }
    }

    @Test
    fun testEquals() {
        val projectId = "asdf"

        val parentKey1 = CreateTaskViewModel.ParentKey.ProjectParentKey(projectId)
        val parentKey2 = CreateTaskViewModel.ParentKey.ProjectParentKey(projectId)

        Assert.assertTrue(parentKey1 == parentKey2)
    }
}