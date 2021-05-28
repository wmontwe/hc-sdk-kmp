/*
 * Copyright (c) 2021 D4L data4life gGmbH / All rights reserved.
 *
 * D4L owns all legal rights, title and interest in and to the Software Development Kit ("SDK"),
 * including any intellectual property rights that subsist in the SDK.
 *
 * The SDK and its documentation may be accessed and used for viewing/review purposes only.
 * Any usage of the SDK for other purposes, including usage for the development of
 * applications/third-party applications shall require the conclusion of a license agreement
 * between you and D4L.
 *
 * If you are interested in licensing the SDK for your own applications/third-party
 * applications and/or if you’d like to contribute to the development of the SDK, please
 * contact D4L by email to help@data4life.care.
 */

package care.data4life.sdk.client

import care.data4life.sdk.SdkContract
import care.data4life.sdk.auth.AuthContract
import care.data4life.sdk.call.CallContract
import care.data4life.sdk.call.CallHandler
import care.data4life.sdk.call.Callback
import care.data4life.sdk.call.Task
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.resource.DataResource
import care.data4life.sdk.resource.ResourceContract
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.test.util.GenericTestDataProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.threeten.bp.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DataRecordClientTest {
    private val recordService: RecordContract.Service = mockk()
    private val userService: AuthContract.UserService = mockk()
    private val callHandler: CallHandler = mockk()
    private lateinit var client: SdkContract.DataRecordClient

    @Before
    fun setUp() {
        client = DataRecordClient(
            userService,
            recordService,
            callHandler
        )
    }

    @Test
    fun `Given create is called, with a Resource, Annotations and a Callback it returns the corresponding Task`() {
        // Given
        val resource: DataResource = mockk()
        val annotations: Annotations = mockk()
        val callback: Callback<CallContract.Record<ResourceContract.DataResource>> = mockk()

        val userId = GenericTestDataProvider.USER_ID
        val expectedRecord: CallContract.Record<DataResource> = mockk()
        val record: Single<CallContract.Record<DataResource>> = Single.just(expectedRecord)
        val expected: Task = mockk()
        val observer = slot<Single<CallContract.Record<ResourceContract.DataResource>>>()

        every { userService.finishLogin(true) } returns Single.just(true)
        every { userService.userID } returns Single.just(userId)
        every {
            recordService.createRecord(userId, resource, annotations)
        } returns record
        every {
            callHandler.executeSingle(capture(observer), callback)
        } answers {
            assertEquals<CallContract.Record<ResourceContract.DataResource>>(
                expected = expectedRecord as CallContract.Record<ResourceContract.DataResource>,
                actual = observer.captured.blockingGet()
            )
            expected
        }

        // When
        val actual = client.create(resource, annotations, callback)

        // Then
        assertSame(
            expected = expected,
            actual = actual
        )
    }

    @Test
    fun `Given update is called, with a RecordId, a Resource, Annotations and a Callback it returns the corresponding Task`() {
        // Given
        val resource: DataResource = mockk()
        val annotations: Annotations = mockk()
        val callback: Callback<CallContract.Record<ResourceContract.DataResource>> = mockk()
        val recordId = GenericTestDataProvider.RECORD_ID

        val userId = GenericTestDataProvider.USER_ID
        val expectedRecord: CallContract.Record<DataResource> = mockk()
        val record: Single<CallContract.Record<DataResource>> = Single.just(expectedRecord)
        val expected: Task = mockk()
        val observer = slot<Single<CallContract.Record<ResourceContract.DataResource>>>()

        every { userService.finishLogin(true) } returns Single.just(true)
        every { userService.userID } returns Single.just(userId)
        every {
            recordService.updateRecord<DataResource>(userId, recordId, resource, annotations)
        } returns record
        every {
            callHandler.executeSingle(capture(observer), callback)
        } answers {
            assertEquals<CallContract.Record<ResourceContract.DataResource>>(
                expected = expectedRecord as CallContract.Record<ResourceContract.DataResource>,
                actual = observer.captured.blockingGet()
            )
            expected
        }

        // When
        val actual = client.update(recordId, resource, annotations, callback)

        // Then
        assertSame(
            expected = expected,
            actual = actual
        )
    }

    @Test
    fun `Given fetch is called, with a RecordId and a Callback it returns the corresponding Task`() {
        // Given
        val callback: Callback<CallContract.Record<ResourceContract.DataResource>> = mockk()
        val recordId = GenericTestDataProvider.RECORD_ID

        val userId = GenericTestDataProvider.USER_ID
        val expectedRecord: CallContract.Record<DataResource> = mockk()
        val record: Single<CallContract.Record<DataResource>> = Single.just(expectedRecord)
        val expected: Task = mockk()
        val observer = slot<Single<CallContract.Record<ResourceContract.DataResource>>>()

        every { userService.finishLogin(true) } returns Single.just(true)
        every { userService.userID } returns Single.just(userId)
        every {
            recordService.fetchDataRecord<DataResource>(userId, recordId)
        } returns record
        every {
            callHandler.executeSingle(capture(observer), callback)
        } answers {
            assertEquals<CallContract.Record<ResourceContract.DataResource>>(
                expected = expectedRecord as CallContract.Record<ResourceContract.DataResource>,
                actual = observer.captured.blockingGet()
            )
            expected
        }

        // When
        val actual = client.fetch(recordId, callback)

        // Then
        assertSame(
            expected = expected,
            actual = actual
        )
    }

    @Test
    fun `Given search is called, with a ResourceType, Annotations, a Startdate, a Enddate, Pagesize, Offset and a Callback it returns the corresponding Task`() {
        // Given
        val callback: Callback<List<CallContract.Record<ResourceContract.DataResource>>> = mockk()
        val annotations: Annotations = mockk()
        val startDate: LocalDate = mockk()
        val endDate: LocalDate = mockk()
        val pageSize = 23
        val offset = 42

        val userId = GenericTestDataProvider.USER_ID
        val expectedRecords: List<CallContract.Record<DataResource>> = mockk()
        val records: Single<List<CallContract.Record<DataResource>>> = Single.just(expectedRecords)
        val expected: Task = mockk()
        val observer = slot<Single<List<CallContract.Record<ResourceContract.DataResource>>>>()

        every { userService.finishLogin(true) } returns Single.just(true)
        every { userService.userID } returns Single.just(userId)
        every {
            recordService.fetchDataRecords<DataResource>(
                userId,
                annotations,
                startDate,
                endDate,
                pageSize,
                offset
            )
        } returns records
        every {
            callHandler.executeSingle(capture(observer), callback)
        } answers {
            assertEquals(
                expected = expectedRecords,
                actual = observer.captured.blockingGet()
            )
            expected
        }

        // When
        val actual = client.search(
            annotations,
            startDate,
            endDate,
            pageSize,
            offset,
            callback
        )

        // Then
        assertSame(
            expected = expected,
            actual = actual
        )
    }

    @Test
    fun `Given count is called, with a ResourceType, Annotations and a Callback it returns the corresponding Task`() {
        // Given
        val resourceType = DataResource::class.java
        val annotations: Annotations = mockk()
        val callback: Callback<Int> = mockk()

        val userId = "Potato"
        val expectedAmount = 23
        val amount = Single.just(23)
        val expected: Task = mockk()
        val observer = slot<Single<Int>>()

        every { userService.finishLogin(true) } returns Single.just(true)
        every { userService.userID } returns Single.just(userId)
        every {
            recordService.countDataRecords(resourceType, userId, annotations)
        } returns amount
        every {
            callHandler.executeSingle(capture(observer), callback)
        } answers {
            assertEquals(
                expected = expectedAmount,
                actual = observer.captured.blockingGet()
            )
            expected
        }

        // When
        val actual = client.count(annotations, callback)

        // Then
        assertSame(
            expected = expected,
            actual = actual
        )
    }

    @Test
    fun `Given delete is called, with a RecordId and a Callback, it returns the corresponding Task`() {
        // Given
        val callback: Callback<Boolean> = mockk()

        val recordId = GenericTestDataProvider.RECORD_ID
        val userId = GenericTestDataProvider.USER_ID
        val result = Completable.fromCallable { true }
        val expected: Task = mockk()
        val observer = slot<Single<Boolean>>()

        every { userService.finishLogin(true) } returns Single.just(true)
        every { userService.userID } returns Single.just(userId)
        every {
            recordService.deleteRecord(userId, recordId)
        } returns result
        every {
            callHandler.executeSingle(capture(observer), callback)
        } answers {
            assertTrue(observer.captured.blockingGet())
            expected
        }

        // When
        val actual = client.delete(recordId, callback)

        // Then
        assertSame(
            expected = expected,
            actual = actual
        )
    }
}