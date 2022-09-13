/*
 * Copyright (c) 2022 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.autofill.ui.credential.saving.declines

import com.duckduckgo.app.CoroutineTestRule
import com.duckduckgo.autofill.store.AutofillStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AutofillDisablingDeclineCounterTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val autofillStore: AutofillStore = mock()
    private lateinit var testee: AutofillDisablingDeclineCounter

    @Before
    fun before() {
        whenever(autofillStore.autofillDeclineCount).thenReturn(0)
        whenever(autofillStore.monitorDeclineCounts).thenReturn(true)
    }

    @Test
    fun whenInitialisedThenNoPreviouslyStoredDomain() = runTest {
        initialiseDeclineCounter()
        assertNull(testee.currentSessionPreviousDeclinedDomain)
    }

    @Test
    fun whenInitialisedThenNoPreviouslySessionDeclinedCount() = runTest {
        initialiseDeclineCounter()
        assertEquals(0, testee.currentSessionDomainDeclineCount)
    }

    @Test
    fun whenNotMonitoringDeclineCountsThenShouldNotRecordNewDeclines() = runTest {
        whenever(autofillStore.monitorDeclineCounts).thenReturn(false)
        initialiseDeclineCounter()

        testee.userDeclinedToSaveCredentials("example.com")
        assertDeclineNotRecorded()
    }

    @Test
    fun whenMonitoringDeclineCountsThenShouldRecordNewDeclines() = runTest {
        whenever(autofillStore.monitorDeclineCounts).thenReturn(true)
        initialiseDeclineCounter()

        testee.userDeclinedToSaveCredentials("example.com")
        assertDeclineRecorded(expectedNewValue = 1)
    }

    @Test
    fun whenNewDomainMatchesOldDomainThenDeclineNotRecorded() = runTest {
        initialiseDeclineCounter()
        testee.currentSessionPreviousDeclinedDomain = "example.com"
        testee.userDeclinedToSaveCredentials("example.com")
        assertDeclineNotRecorded()
    }

    @Test
    fun whenNewDomainDoesNotMatchOldDomainThenDeclineRecorded() = runTest {
        initialiseDeclineCounter()
        testee.currentSessionPreviousDeclinedDomain = "foo.com"
        testee.userDeclinedToSaveCredentials("example.com")
        assertDeclineRecorded(expectedNewValue = 1)
    }

    @Test
    fun whenDeclineOnNewDomainWithNoPreviousDomainThenDomainStored() = runTest {
        initialiseDeclineCounter()
        testee.currentSessionPreviousDeclinedDomain = null
        testee.userDeclinedToSaveCredentials("example.com")
        assertEquals("example.com", testee.currentSessionPreviousDeclinedDomain)
    }

    @Test
    fun whenDeclineOnNewDomainWithAPreviousDomainThenDomainStored() = runTest {
        initialiseDeclineCounter()
        testee.currentSessionPreviousDeclinedDomain = "foo.com"
        testee.userDeclinedToSaveCredentials("example.com")
        assertEquals("example.com", testee.currentSessionPreviousDeclinedDomain)
    }

    @Test
    fun whenDeclineIncreasesTotalCountAboveThresholdButInMemoryCountIsBelowThenShouldNotOfferToDisable() = runTest {
        initialiseDeclineCounter()
        configureGlobalDeclineCountAtThreshold()
        testee.currentSessionDomainDeclineCount = 0
        val result = testee.userDeclinedToSaveCredentials("example.com")
        assertFalse(result)
    }

    @Test
    fun whenDeclineTotalCountBelowThresholdButInMemoryCountIsAtThresholdThenShouldNotOfferToDisable() = runTest {
        initialiseDeclineCounter()
        whenever(autofillStore.autofillDeclineCount).thenReturn(0)
        testee.currentSessionDomainDeclineCount = 10
        val result = testee.userDeclinedToSaveCredentials("example.com")
        assertFalse(result)
    }

    @Test
    fun whenDeclineIncreasesTotalCountAtThresholdAndInMemoryCountAtThresholdThenShouldOfferToDisable() = runTest {
        initialiseDeclineCounter()
        configureGlobalDeclineCountAtThreshold()
        assertFalse(testee.userDeclinedToSaveCredentials("a.com"))
        assertTrue(testee.userDeclinedToSaveCredentials("b.com"))
    }

    private fun configureGlobalDeclineCountAtThreshold() {
        whenever(autofillStore.autofillDeclineCount).thenReturn(3)
    }

    private fun assertDeclineNotRecorded() {
        verify(autofillStore, never()).autofillDeclineCount = any()
    }

    private fun assertDeclineRecorded(expectedNewValue: Int) {
        verify(autofillStore).autofillDeclineCount = eq(expectedNewValue)
    }

    private fun TestScope.initialiseDeclineCounter() {
        whenever(autofillStore.autofillEnabled).thenReturn(true)
        testee = AutofillDisablingDeclineCounter(
            autofillStore = autofillStore,
            appCoroutineScope = this,
            dispatchers = coroutineTestRule.testDispatcherProvider
        )
    }
}
