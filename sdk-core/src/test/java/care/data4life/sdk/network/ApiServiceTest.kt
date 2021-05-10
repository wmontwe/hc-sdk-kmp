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

package care.data4life.sdk.network

import care.data4life.auth.AuthorizationService
import care.data4life.sdk.ApiService
import care.data4life.sdk.lang.D4LRuntimeException
import care.data4life.sdk.network.NetworkingContract.Companion.PARAM_TEK
import care.data4life.sdk.network.model.CommonKeyResponse
import care.data4life.sdk.network.model.DocumentUploadResponse
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.UserInfo
import care.data4life.sdk.network.model.VersionList
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.CLIENT_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.COMMON_KEY_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.RECORD_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.verify
import io.mockk.verifyOrder
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.CertificatePinner
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ApiServiceTest {
    private val authService: AuthorizationService = mockk()
    private val env: NetworkingContract.Environment = mockk(relaxed = true)
    private val ihcService: IHCService = mockk()
    private lateinit var service: NetworkingContract.Service

    // TODO: Remove the fat constructor
    @Before
    fun setup() {
        clearAllMocks()

        val certificateBuilder: CertificatePinner.Builder = mockk()
        val retrofit: Retrofit = mockk()

        mockkConstructor(CertificatePinner.Builder::class)
        mockkConstructor(Retrofit.Builder::class)

        every {
            anyConstructed<CertificatePinner.Builder>().add(any(), any())
        } returns certificateBuilder
        every {
            certificateBuilder.build()
        } returns mockk(relaxed = true)

        every { env.getApiBaseURL(any()) } returns "http://IhateFatConstructors.com"

        every { anyConstructed<Retrofit.Builder>().build() } returns retrofit
        every { retrofit.create(IHCService::class.java) } returns ihcService

        service = ApiService(
            authService,
            env,
            CLIENT_ID,
            "something",
            "you should not care",
            mockk(relaxed = true),
            "what so ever",
            false
        )
    }

    // TODO: Remove the fat constructor
    @After
    fun tearDown() {
        unmockkConstructor(CertificatePinner.Builder::class)
        unmockkConstructor(Retrofit.Builder::class)
    }

    @Test
    fun `It fulfils NetworkingContractService`() {

        val service: Any = ApiService(
            mockk(relaxed = true),
            env,
            CLIENT_ID,
            "something",
            "you should not care",
            mockk(relaxed = true),
            "what so ever",
            false
        )

        assertTrue(service is NetworkingContract.Service)
    }

    // keys
    @Test
    fun `Given, fetchCommonKey is called with an Alias, UserId and a CommonKeyId, it delegates it to the IHCService and returns its result`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val commonKeyId = COMMON_KEY_ID
        val result: Single<CommonKeyResponse> = mockk()

        every { ihcService.fetchCommonKey(alias, userId, commonKeyId) } returns result

        // When
        val actual = service.fetchCommonKey(alias, userId, commonKeyId)

        // Then
        assertSame(
            actual = actual,
            expected = result
        )

        verify(exactly = 1) {
            ihcService.fetchCommonKey(alias, userId, commonKeyId)
        }
    }

    @Test
    fun `Given, uploadTagEncryptionKey is called with an Alias, UserId and a EncryptedKey, it delegates it to the IHCService and returns its result`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val encryptedKey = "key"
        val result: Completable = mockk()

        val mappedKey = mapOf(
            PARAM_TEK to encryptedKey
        )

        every { ihcService.uploadTagEncryptionKey(alias, userId, mappedKey) } returns result

        // When
        val actual = service.uploadTagEncryptionKey(alias, userId, encryptedKey)

        // Then
        assertSame(
            actual = actual,
            expected = result
        )

        verify(exactly = 1) {
            ihcService.uploadTagEncryptionKey(alias, userId, mappedKey)
        }
    }

    // records
    @Test
    fun `Given, createRecord is called with an Alias, UserId and a EncryptedRecord, it delegates it to the IHCService and returns its result`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val record: EncryptedRecord = mockk()
        val result: Single<EncryptedRecord> = mockk()

        every { ihcService.createRecord(alias, userId, record) } returns result

        // When
        val actual = service.createRecord(alias, userId, record)

        // Then
        assertSame(
            actual = actual,
            expected = result
        )

        verify(exactly = 1) {
            ihcService.createRecord(alias, userId, record)
        }
    }

    @Test
    fun `Given, updateRecord is called with an Alias, UserId, RecordId and a EncryptedRecord, it delegates it to the IHCService and returns its result`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val recordId = RECORD_ID
        val record: EncryptedRecord = mockk()
        val result: Single<EncryptedRecord> = mockk()

        every { ihcService.updateRecord(alias, userId, recordId, record) } returns result

        // When
        val actual = service.updateRecord(alias, userId, recordId, record)

        // Then
        assertSame(
            actual = actual,
            expected = result
        )

        verify(exactly = 1) {
            ihcService.updateRecord(alias, userId, recordId, record)
        }
    }

    @Test
    fun `Given, fetchRecord is called with an Alias, UserId and a RecordId, it delegates it to the IHCService and returns its result`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val recordId = RECORD_ID
        val result: Single<EncryptedRecord> = mockk()

        every { ihcService.fetchRecord(alias, userId, recordId) } returns result

        // When
        val actual = service.fetchRecord(alias, userId, recordId)

        // Then
        assertSame(
            actual = actual,
            expected = result
        )

        verify(exactly = 1) {
            ihcService.fetchRecord(alias, userId, recordId)
        }
    }

    @Test
    fun `Given, searchRecords is called with its appropriate parameter, it delegates it to the IHCService and returns its result`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val startDate = "somewhen"
        val endDate = "somewhen else"
        val pageSize = 23
        val offset = 42
        val tags = "tags"
        val result: Observable<List<EncryptedRecord>> = mockk()

        every {
            service.searchRecords(alias, userId, startDate, endDate, pageSize, offset, tags)
        } returns result

        // When
        val actual = service.searchRecords(
            alias,
            userId,
            startDate,
            endDate,
            pageSize,
            offset,
            tags
        )

        // Then
        assertSame(
            actual = actual,
            expected = result
        )

        verify(exactly = 1) {
            ihcService.searchRecords(alias, userId, startDate, endDate, pageSize, offset, tags)
        }
    }

    @Test
    fun `Given, getCount is called with an Alias, UserId and Tags, it delegates it to the IHCService, parses the result and returns it`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val tags = RECORD_ID
        val amount = "23"
        val response: Response<Void> = mockk()
        val headers: Headers = mockk()

        every { ihcService.getRecordsHeader(alias, userId, tags) } returns Single.just(response)
        every { response.headers() } returns headers
        every { headers[NetworkingContract.HEADER_TOTAL_COUNT] } returns amount

        // When
        val actual = service.getCount(alias, userId, tags).blockingGet()

        // Then
        assertSame(
            actual = actual,
            expected = 23
        )

        verify(exactly = 1) {
            ihcService.getRecordsHeader(alias, userId, tags)
        }
    }

    @Test
    fun `Given, deleteRecord is called with an Alias, UserId and a RecordId, it delegates it to the IHCService and returns its result`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val recordId = RECORD_ID
        val result: Completable = mockk()

        every { ihcService.deleteRecord(alias, userId, recordId) } returns result

        // When
        val actual = service.deleteRecord(alias, userId, recordId)

        // Then
        assertSame(
            actual = actual,
            expected = result
        )

        verify(exactly = 1) {
            ihcService.deleteRecord(alias, userId, recordId)
        }
    }

    // attachments
    @Test
    fun `Given, uploadDocument is called with an Alias, UserId and a data payload, while building a RequestBody out of it, it delegates it to the IHCService and returns the DocumentID`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val payload = ByteArray(12)
        val request = payload.toRequestBody(NetworkingContract.MEDIA_TYPE_OCTET_STREAM.toMediaType())
        val response: DocumentUploadResponse = mockk()
        val result = "Done"

        val slot = slot<RequestBody>()

        every { ihcService.uploadDocument(alias, userId, capture(slot)) } answers {
            if (slot.captured.contentType() == request.contentType() &&
                // This hard to test, we can apply only a weak indicator
                slot.captured.contentLength() == request.contentLength()
            ) {
                Single.just(response)
            } else {
                throw RuntimeException("uploadDocument has differing RequestBodies")
            }
        }

        every { response.documentId } returns result

        // When
        val actual = service.uploadDocument(alias, userId, payload).blockingGet()

        // Then
        assertSame(
            actual = actual,
            expected = result
        )

        verify(exactly = 1) {
            ihcService.uploadDocument(alias, userId, any())
        }
    }

    @Test
    fun `Given, downloadDocument is called with an Alias, UserId and a DocumentId, it delegates it to the IHCService and returns the Resonses Datablob`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val documentId = "doc"
        val response: ResponseBody = mockk()
        val document = ByteArray(23)

        every { ihcService.downloadDocument(alias, userId, documentId) } returns Single.just(response)
        every { response.bytes() } returns document

        // When
        val actual = service.downloadDocument(alias, userId, documentId).blockingGet()

        // Then
        assertSame(
            actual = actual,
            expected = document
        )

        verify(exactly = 1) {
            ihcService.downloadDocument(alias, userId, documentId)
        }
    }

    @Test
    fun `Given, deleteDocument is called with an Alias, UserId and a DocumentId, it delegates it to the IHCService and returns always true`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val documentId = "doc"

        every { ihcService.deleteDocument(alias, userId, documentId) } returns Single.just(mockk())

        // When
        val actual = service.deleteDocument(alias, userId, documentId).blockingGet()

        // Then
        assertTrue(actual)

        verify(exactly = 1) {
            ihcService.deleteDocument(alias, userId, documentId)
        }
    }

    // Mixed
    @Test
    fun `Given, fetchUserInfo is called with an Alias, it delegates it to the IHCService, subscripts on the Schedulers returns the result`() {
        // Given
        val alias = ALIAS
        val response: Single<UserInfo> = mockk()

        every { ihcService.fetchUserInfo(alias) } returns response
        every { response.subscribeOn(Schedulers.io()) } returns response

        // When
        val actual = service.fetchUserInfo(alias)

        // Then
        assertSame(
            actual = actual,
            expected = response
        )

        verifyOrder {
            ihcService.fetchUserInfo(alias)
            response.subscribeOn(Schedulers.io())
        }
    }

    @Test
    fun `Given, fetchVersionInfo is called, it delegates it to the IHCService, subscripts on the Schedulers returns the result`() {
        // Given
        val response: Single<VersionList> = mockk()

        every { ihcService.fetchVersionInfo() } returns response
        every { response.subscribeOn(Schedulers.io()) } returns response

        // When
        val actual = service.fetchVersionInfo()

        // Then
        assertSame(
            actual = actual,
            expected = response
        )

        verifyOrder {
            ihcService.fetchVersionInfo()
            response.subscribeOn(Schedulers.io())
        }
    }

    @Test
    fun `Given, logout is called with an Alias, it fails if service was initialized with a static token`() {
        // Given
        val service = ApiService(
            mockk(relaxed = true),
            env,
            CLIENT_ID,
            "something",
            "you should not care",
            mockk(relaxed = true),
            "what so ever",
            "token".toByteArray(),
            false
        )
        val alias = ALIAS

        // Then
        val error = assertFailsWith<D4LRuntimeException> {
            // When
            service.logout(alias)
        }

        assertEquals(
            actual = error.message,
            expected = "Cannot log out when using a static access token!"
        )
    }

    @Test
    fun `Given, logout is called with an Alias, it resolves the accessToken, delegates it with the Alias to the IHCService and resturns its result`() {
        // Given
        val alias = ALIAS
        val token = "token"
        val result = "ignore me"

        every { authService.getRefreshToken(alias) } returns token
        every { ihcService.logout(alias, token) } returns Completable.fromCallable { result }

        // When
        val actual = service.logout(alias).blockingGet()

        // Then
        assertNull(actual)

        verify(exactly = 1) { ihcService.logout(alias, token) }
    }
}