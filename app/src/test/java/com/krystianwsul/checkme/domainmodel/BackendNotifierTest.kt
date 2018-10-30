package com.krystianwsul.checkme.domainmodel

import android.text.TextUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.*

@RunWith(PowerMockRunner::class)
@PrepareForTest(TextUtils::class)
class BackendNotifierTest {

    @Before
    fun setUp() {
        PowerMockito.mockStatic(TextUtils::class.java)

        PowerMockito.`when`<String>(TextUtils.join(any(String::class.java), any(List::class.java))).thenAnswer { invocation ->
            val separator = invocation.arguments[0] as String
            val list = invocation.arguments[1] as List<*>
            Assert.assertTrue(!list.isEmpty())

            val answer = StringBuilder()
            answer.append(list[0])

            for (i in 1 until list.size) {
                answer.append(separator)
                answer.append(list[i])
            }

            answer.toString()
        }
    }

    @Test
    fun testOneProjectDevelopment() {
        val projects = TreeSet(listOf("-KXvJTar2cCxxrGCtN_w"))
        val correctUrl = "https://check-me-add47.appspot.com/notify?projects=-KXvJTar2cCxxrGCtN_w&senderToken=asdf"

        val url = BackendNotifier.getUrl(projects, false, ArrayList(), "asdf")

        Assert.assertTrue(correctUrl == url)
    }

    @Test
    fun testOneProjectProduction() {
        val projects = TreeSet(listOf("-KXvJTar2cCxxrGCtN_w"))
        val correctUrl = "https://check-me-add47.appspot.com/notify?projects=-KXvJTar2cCxxrGCtN_w&production=1&senderToken=asdf"

        val url = BackendNotifier.getUrl(projects, true, ArrayList(), "asdf")

        Assert.assertTrue(correctUrl == url)
    }

    @Test
    fun testTwoProjectsDevelopment() {
        val projects = TreeSet(Arrays.asList("-KXvJTar2cCxxrGCtN_w", "asdf"))
        val correctUrl = "https://check-me-add47.appspot.com/notify?projects=-KXvJTar2cCxxrGCtN_w&projects=asdf&senderToken=asdf"

        val url = BackendNotifier.getUrl(projects, false, ArrayList(), "asdf")

        Assert.assertTrue(correctUrl == url)
    }

    @Test
    fun testTwoProjectsProduction() {
        val projects = TreeSet(Arrays.asList("-KXvJTar2cCxxrGCtN_w", "asdf"))
        val correctUrl = "https://check-me-add47.appspot.com/notify?projects=-KXvJTar2cCxxrGCtN_w&projects=asdf&production=1&senderToken=asdf"

        val url = BackendNotifier.getUrl(projects, true, ArrayList(), "asdf")

        Assert.assertTrue(correctUrl == url)
    }

    @Test
    fun testTwoProjectsTwoUsersProduction() {
        val projects = TreeSet(Arrays.asList("-KXvJTar2cCxxrGCtN_w", "asdf"))
        val userKeys = TreeSet(Arrays.asList("a", "b"))
        val correctUrl = "https://check-me-add47.appspot.com/notify?projects=-KXvJTar2cCxxrGCtN_w&projects=asdf&userKeys=a&userKeys=b&production=1&senderToken=asdf"

        val url = BackendNotifier.getUrl(projects, true, userKeys, "asdf")

        Assert.assertTrue(correctUrl == url)
    }
}