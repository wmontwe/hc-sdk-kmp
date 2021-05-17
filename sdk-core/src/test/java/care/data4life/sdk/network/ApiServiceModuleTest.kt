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

import care.data4life.auth.AuthorizationContract
import care.data4life.sdk.NetworkConnectivityService
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_ALIAS
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_AUTHORIZATION
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_GC_SDK_VERSION
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_TOTAL_COUNT
import care.data4life.sdk.network.model.CommonKeyResponse
import care.data4life.sdk.network.model.DocumentUploadResponse
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.UserInfo
import care.data4life.sdk.network.model.Version
import care.data4life.sdk.network.model.VersionList
import care.data4life.sdk.network.typeadapter.EncryptedKeyTypeAdapter
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.AUTH_TOKEN
import care.data4life.sdk.test.util.GenericTestDataProvider.CLIENT_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.COMMON_KEY_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.DOCUMENT_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.RECORD_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import care.data4life.sdk.util.Base64
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiServiceModuleTest {
    private val authService: AuthorizationContract.Service = mockk()
    private val env: NetworkingContract.Environment = mockk()
    private lateinit var server: MockWebServer
    private lateinit var service: NetworkingContract.Service
    private val moshi = Moshi.Builder().add(EncryptedKeyTypeAdapter()).build()
    private val clientName = "test"
    private val clientId = CLIENT_ID
    private val secret = "geheim"

    data class TestConnection(
        override val isConnected: Boolean
    ) : NetworkConnectivityService

    @Before
    fun setUp() {
        server = MockWebServer()

        every { env.getApiBaseURL(any()) } returns "https://www.example.com"
        every { env.getCertificatePin(any()) } returns NetworkingContract.DATA4LIFE_CARE

        service = ApiService(
            authService,
            env,
            clientId,
            secret,
            "not important",
            TestConnection(true),
            clientName,
            false
        )

        resetService(server.url("/"))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // ToDo: Remove that after refactoring
    private fun resetService(baseUrl: HttpUrl) {
        val client = (service as ApiService).client!!
            .newBuilder()
            .connectTimeout(100, TimeUnit.MILLISECONDS)
            .readTimeout(100, TimeUnit.MILLISECONDS)
            .writeTimeout(100, TimeUnit.MILLISECONDS)
            .callTimeout(100, TimeUnit.MILLISECONDS)
            .build()

        (service as ApiService).resetService(client, baseUrl)
    }

    private fun simulateAuthService(alias: String, token: String) {
        every { authService.getAccessToken(alias) } returns token
    }

    private fun addResponsesAndStart(responses: List<MockResponse>) {
        responses.forEach { response ->
            server.enqueue(response)
        }
    }

    @Test
    fun `Given, fetchCommonKey is called with an Alias, UserId and a CommonKeyId, it fetches a CommonKeyResponse`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val commonKeyId = COMMON_KEY_ID
        val authToken = AUTH_TOKEN
        val commonKeyResponse = CommonKeyResponse(
            EncryptedKey(Base64.encodeToString("test"))
        )
        val adapter = moshi.adapter(CommonKeyResponse::class.java)

        val response = MockResponse().setBody(adapter.toJson(commonKeyResponse))

        addResponsesAndStart(listOf(response))
        simulateAuthService(alias, authToken)

        // When
        val actual = service.fetchCommonKey(alias, userId, commonKeyId).blockingGet()

        // Then
        assertEquals(
            actual = actual,
            expected = commonKeyResponse
        )

        val request = server.takeRequest()

        assertEquals(
            actual = request.path,
            expected = "/users/$userId/commonkeys/$commonKeyId"
        )
        assertNull(request.headers[HEADER_ALIAS])
        assertEquals(
            actual = request.headers[HEADER_AUTHORIZATION],
            expected = "Bearer $authToken"
        )
        assertEquals(
            actual = request.headers[HEADER_GC_SDK_VERSION],
            expected = "Android $clientName"
        )
        assertEquals(
            actual = request.method,
            expected = "GET"
        )
    }

    @Test
    fun `Given, createRecord is called with an Alias, UserId and a EncryptedRecord, it uploads the given Record and returns the modified responded one`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val record = EncryptedRecord(
            _commonKeyId = null,
            identifier = null,
            encryptedTags = listOf("tags"),
            encryptedBody = "body",
            encryptedDataKey = EncryptedKey(Base64.encodeToString("test")),
            encryptedAttachmentsKey = null,
            customCreationDate = "today",
            updatedDate = "tomorrow",
            modelVersion = 23
        )
        val authToken = AUTH_TOKEN
        val recordResponse = record.copy("id", "id2")
        val adapter = moshi.adapter(EncryptedRecord::class.java)

        val response = MockResponse().setBody(adapter.toJson(recordResponse))

        addResponsesAndStart(listOf(response))
        simulateAuthService(alias, authToken)

        // When
        val actual = service.createRecord(alias, userId, record).blockingGet()

        // Then
        assertEquals(
            actual = actual,
            expected = recordResponse
        )

        val request = server.takeRequest()

        assertEquals(
            actual = request.path,
            expected = "/users/$userId/records"
        )
        assertNull(request.headers[HEADER_ALIAS])
        assertEquals(
            actual = request.headers[HEADER_AUTHORIZATION],
            expected = "Bearer $authToken"
        )
        assertEquals(
            actual = request.method,
            expected = "POST"
        )
    }

    @Test
    fun `Given, updateRecord is called with an Alias, UserId, RecordId and a EncryptedRecord, it uploads the given Record and returns the modified responded one`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val recordId = RECORD_ID
        val record = EncryptedRecord(
            _commonKeyId = null,
            identifier = null,
            encryptedTags = listOf("tags"),
            encryptedBody = "body",
            encryptedDataKey = EncryptedKey(Base64.encodeToString("test")),
            encryptedAttachmentsKey = null,
            customCreationDate = "today",
            updatedDate = "tomorrow",
            modelVersion = 23
        )
        val authToken = AUTH_TOKEN
        val recordResponse = record.copy("id", "id2")
        val adapter = moshi.adapter(EncryptedRecord::class.java)

        val response = MockResponse().setBody(adapter.toJson(recordResponse))

        addResponsesAndStart(listOf(response))
        simulateAuthService(alias, authToken)

        // When
        val actual = service.updateRecord(alias, userId, recordId, record).blockingGet()

        // Then
        assertEquals(
            actual = actual,
            expected = recordResponse
        )

        val request = server.takeRequest()

        assertEquals(
            actual = request.path,
            expected = "/users/$userId/records/$recordId"
        )
        assertNull(request.headers[HEADER_ALIAS])
        assertEquals(
            actual = request.headers[HEADER_AUTHORIZATION],
            expected = "Bearer $authToken"
        )
        assertEquals(
            actual = request.method,
            expected = "PUT"
        )
    }

    @Test
    fun `Given, fetchRecord is called with an Alias, UserId and a RecordId, it returns the requested record`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val recordId = RECORD_ID
        val recordResponse = EncryptedRecord(
            _commonKeyId = null,
            identifier = null,
            encryptedTags = listOf("tags"),
            encryptedBody = "body",
            encryptedDataKey = EncryptedKey(Base64.encodeToString("test")),
            encryptedAttachmentsKey = null,
            customCreationDate = "today",
            updatedDate = "tomorrow",
            modelVersion = 23
        )
        val authToken = AUTH_TOKEN
        val adapter = moshi.adapter(EncryptedRecord::class.java)

        val response = MockResponse().setBody(adapter.toJson(recordResponse))

        addResponsesAndStart(listOf(response))
        simulateAuthService(alias, authToken)

        // When
        val actual = service.fetchRecord(alias, userId, recordId).blockingGet()

        // Then
        assertEquals(
            actual = actual,
            expected = recordResponse
        )

        val request = server.takeRequest()

        assertEquals(
            actual = request.path,
            expected = "/users/$userId/records/$recordId"
        )
        assertNull(request.headers[HEADER_ALIAS])
        assertEquals(
            actual = request.headers[HEADER_AUTHORIZATION],
            expected = "Bearer $authToken"
        )
        assertEquals(
            actual = request.method,
            expected = "GET"
        )
    }

    @Test
    fun `Given, searchRecords is called with its appropriate parameter, it returns the requested record`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val startDate = "somewhen"
        val endDate = "somewhen else"
        val pageSize = 23
        val offset = 42
        val tags = "tag1,tag2,tag"

        val recordResponse = EncryptedRecord(
            _commonKeyId = null,
            identifier = null,
            encryptedTags = listOf("tags"),
            encryptedBody = "body",
            encryptedDataKey = EncryptedKey(Base64.encodeToString("test")),
            encryptedAttachmentsKey = null,
            customCreationDate = "today",
            updatedDate = "tomorrow",
            modelVersion = 23
        )
        val authToken = AUTH_TOKEN
        val adapter = moshi.adapter(List::class.java)

        val response = MockResponse().setBody(adapter.toJson(listOf(recordResponse)))

        addResponsesAndStart(listOf(response))
        simulateAuthService(alias, authToken)

        // When
        val actual = service.searchRecords(
            alias,
            userId,
            startDate,
            endDate,
            pageSize,
            offset,
            tags
        ).blockingFirst()

        // Then
        assertEquals(
            actual = actual,
            expected = listOf(recordResponse)
        )

        val request = server.takeRequest()

        assertEquals(
            actual = request.path!!.split('?')[0],
            expected = "/users/$userId/records"
        )
        assertNull(request.headers[HEADER_ALIAS])
        assertEquals(
            actual = request.headers[HEADER_AUTHORIZATION],
            expected = "Bearer $authToken"
        )
        assertEquals(
            actual = request.requestUrl!!.queryParameter("start_date"),
            expected = startDate
        )
        assertEquals(
            actual = request.requestUrl!!.queryParameter("end_date"),
            expected = endDate
        )
        assertEquals(
            actual = request.requestUrl!!.queryParameter("limit"),
            expected = pageSize.toString()
        )
        assertEquals(
            actual = request.requestUrl!!.queryParameter("offset"),
            expected = offset.toString()
        )
        assertEquals(
            actual = request.requestUrl!!.queryParameter("tags"),
            expected = tags
        )
        assertEquals(
            actual = request.method,
            expected = "GET"
        )
    }

    @Test
    fun `Given, getCount is called with an Alias, UserId and Tags, it returns the amount of records`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val tags = "tag1,tag2,tag"
        val amount = 23
        val authToken = AUTH_TOKEN

        val response = MockResponse().setHeader(HEADER_TOTAL_COUNT, amount)

        addResponsesAndStart(listOf(response))
        simulateAuthService(alias, authToken)

        // When
        val actual = service.getCount(alias, userId, tags).blockingGet()

        // Then
        assertEquals(
            actual = actual,
            expected = amount
        )

        val request = server.takeRequest()

        assertEquals(
            actual = request.path,
            expected = "/users/$userId/records?tags=${tags.replace(",", "%2C")}"
        )
        assertNull(request.headers[HEADER_ALIAS])
        assertEquals(
            actual = request.headers[HEADER_AUTHORIZATION],
            expected = "Bearer $authToken"
        )
        assertEquals(
            actual = request.method,
            expected = "HEAD"
        )
    }

    @Test
    fun `Given, deleteRecord is called with an Alias, UserId and a RecordId, it returns a completable action`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val recordId = RECORD_ID
        val authToken = AUTH_TOKEN

        val response = MockResponse().setBody("done")

        addResponsesAndStart(listOf(response))
        simulateAuthService(alias, authToken)

        // When
        val actual = service.deleteRecord(alias, userId, recordId).blockingGet()

        // Then
        assertNull(actual)

        val request = server.takeRequest()

        assertEquals(
            actual = request.path,
            expected = "/users/$userId/records/$recordId"
        )
        assertNull(request.headers[HEADER_ALIAS])
        assertEquals(
            actual = request.headers[HEADER_AUTHORIZATION],
            expected = "Bearer $authToken"
        )
        assertEquals(
            actual = request.method,
            expected = "DELETE"
        )
    }

    // Attachments
    @Test
    fun `Givne, uploadDocument is called with an Alias, UserId and a data payload, it returns the document id`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val documentId = DOCUMENT_ID
        val authToken = AUTH_TOKEN
        val data = ByteArray(42)
        val documentUploadResponse = DocumentUploadResponse(documentId)
        val adapter = moshi.adapter(DocumentUploadResponse::class.java)

        val response = MockResponse().setBody(adapter.toJson(documentUploadResponse))

        addResponsesAndStart(listOf(response))
        simulateAuthService(alias, authToken)

        // When
        val actual = service.uploadDocument(alias, userId, data).blockingGet()

        // Then
        assertEquals(
            actual = actual,
            expected = documentId
        )

        val request = server.takeRequest()

        assertEquals(
            actual = request.path,
            expected = "/users/$userId/documents"
        )
        assertNull(request.headers[HEADER_ALIAS])
        assertEquals(
            actual = request.headers[HEADER_AUTHORIZATION],
            expected = "Bearer $authToken"
        )
        assertEquals(
            actual = request.method,
            expected = "POST"
        )
    }

    @Test
    fun `Given, downloadDocument is called with an Alias, UserId and a DocumentId, it returns a document`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val documentId = DOCUMENT_ID
        val authToken = AUTH_TOKEN
        val payload = "test"

        val response = MockResponse().setBody(payload)

        addResponsesAndStart(listOf(response))
        simulateAuthService(alias, authToken)

        // When
        val actual = service.downloadDocument(alias, userId, documentId).blockingGet()

        // Then
        assertTrue(actual.contentEquals(payload.toByteArray()))

        val request = server.takeRequest()

        assertEquals(
            actual = request.path,
            expected = "/users/$userId/documents/$documentId"
        )
        assertNull(request.headers[HEADER_ALIAS])
        assertEquals(
            actual = request.headers[HEADER_AUTHORIZATION],
            expected = "Bearer $authToken"
        )
        assertEquals(
            actual = request.method,
            expected = "GET"
        )
    }

    // TODO: FIX this
    @Test
    @Ignore("This should work, but it does not, figure out why!")
    fun `Given, deleteDocument is called with an Alias, UserId and a DocumentId, it returns a completable action`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val documentId = DOCUMENT_ID
        val authToken = AUTH_TOKEN

        val response = MockResponse().setResponseCode(204)

        addResponsesAndStart(listOf(response))
        simulateAuthService(alias, authToken)

        // When
        val actual = service.deleteDocument(alias, userId, documentId).blockingGet()

        // Then
        assertTrue(actual)

        val request = server.takeRequest()

        assertEquals(
            actual = request.path,
            expected = "/users/$userId/documents/$documentId"
        )
        assertNull(request.headers[HEADER_ALIAS])
        assertEquals(
            actual = request.headers[HEADER_AUTHORIZATION],
            expected = "Bearer $authToken"
        )
        assertEquals(
            actual = request.method,
            expected = "DELETE"
        )
    }

    // Misc
    @Test
    fun `Given, fetchUserInfo is called with an Alias, it returns a UserInfo`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val authToken = AUTH_TOKEN
        val info = UserInfo(
            userId,
            EncryptedKey(Base64.encodeToString("test")),
            COMMON_KEY_ID,
            EncryptedKey(Base64.encodeToString("test2"))
        )
        val adapter = moshi.adapter(UserInfo::class.java)

        val response = MockResponse().setBody(adapter.toJson(info))

        addResponsesAndStart(listOf(response))
        simulateAuthService(alias, authToken)

        // When
        val actual = service.fetchUserInfo(alias).blockingGet()

        // Then
        assertEquals(
            actual = actual,
            expected = info
        )

        val request = server.takeRequest()

        assertEquals(
            actual = request.path,
            expected = "/userinfo"
        )
        assertNull(request.headers[HEADER_ALIAS])
        assertEquals(
            actual = request.headers[HEADER_AUTHORIZATION],
            expected = "Bearer $authToken"
        )
        assertEquals(
            actual = request.method,
            expected = "GET"
        )
    }

    @Test
    fun `Given, fetchVersionInfo is called with an Alias, it returns a VersionList`() {
        // Given
        val alias = ALIAS
        val authToken = AUTH_TOKEN
        val info = VersionList(
            listOf(
                Version(42, "you should never come here", "totally done")
            )
        )
        val adapter = moshi.adapter(VersionList::class.java)

        val response = MockResponse().setBody(adapter.toJson(info))

        addResponsesAndStart(listOf(response))
        simulateAuthService(alias, authToken)

        // When
        val actual = service.fetchVersionInfo().blockingGet()

        // Then
        assertEquals(
            actual = actual,
            expected = info
        )

        val request = server.takeRequest()

        assertEquals(
            actual = request.path,
            expected = "/sdk/v1/android/versions.json"
        )
        assertNull(request.headers[HEADER_ALIAS])
        assertNull(request.headers[HEADER_AUTHORIZATION])
        assertEquals(
            actual = request.method,
            expected = "GET"
        )
    }

    @Test
    fun `Given, logout is called with an Alias, it returns a completable action`() {
        // Given
        val alias = ALIAS
        val authToken = AUTH_TOKEN
        val response = MockResponse().setBody("done")

        every { authService.getRefreshToken(alias) } returns authToken

        addResponsesAndStart(listOf(response))
        simulateAuthService(alias, authToken)

        // When
        val actual = service.logout(alias).blockingGet()

        // Then
        assertNull(actual)

        val request = server.takeRequest()

        assertEquals(
            actual = request.path,
            expected = "/oauth/revoke"
        )
        assertEquals(
            actual = request.body.toString(),
            expected = "[text=token=$authToken]"
        )
        assertEquals(
            actual = request.headers[HEADER_ALIAS],
            expected = alias
        )
        assertEquals(
            actual = request.headers[HEADER_AUTHORIZATION],
            expected = "Basic ${Base64.encodeToString("$clientId:$secret")}"
        )
        assertEquals(
            actual = request.method,
            expected = "POST"
        )
    }
}
