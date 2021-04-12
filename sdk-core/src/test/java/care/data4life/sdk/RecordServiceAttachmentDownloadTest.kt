/*
 * Copyright (c) 2020 D4L data4life gGmbH / All rights reserved.
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

package care.data4life.sdk

import care.data4life.crypto.GCKey
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.PARTNER_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Attachment
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.network.model.definitions.DecryptedBaseRecord
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.wrapper.SdkAttachmentFactory
import care.data4life.sdk.wrapper.SdkFhirAttachmentHelper
import care.data4life.sdk.wrapper.WrapperContract
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class RecordServiceAttachmentDownloadTest {
    private lateinit var recordService: RecordService
    private val apiService: ApiService = mockk()
    private val cryptoService: CryptoService = mockk()
    private val fhirService: FhirContract.Service = mockk()
    private val tagEncryptionService: TaggingContract.EncryptionService = mockk()
    private val taggingService: TaggingContract.Service = mockk()
    private val attachmentService: AttachmentContract.Service = mockk()
    private val errorHandler: SdkContract.ErrorHandler = mockk()

    @Before
    fun setup() {
        clearAllMocks()

        recordService = spyk(
                RecordService(
                        PARTNER_ID,
                        ALIAS,
                        apiService,
                        tagEncryptionService,
                        taggingService,
                        fhirService,
                        attachmentService,
                        cryptoService,
                        errorHandler,
                        mockk()
                )
        )

        mockkObject(SdkFhirAttachmentHelper)
        mockkObject(SdkAttachmentFactory)
    }

    @After
    fun tearDown() {
        unmockkObject(SdkFhirAttachmentHelper)
        unmockkObject(SdkAttachmentFactory)
    }

    @Test
    fun `Given, downloadData is called with a DecryptedRecord, which contains a DataResource, and UserId, it reflects it`() {
        // Given
        val resource: DataResource = mockk()
        val decryptedRecord: DecryptedBaseRecord<DataResource> = mockk()

        every { decryptedRecord.resource } returns resource

        // When
        val record = recordService.downloadData(decryptedRecord, USER_ID)

        // Then
        assertSame(
                actual = record,
                expected = decryptedRecord
        )

        verify { attachmentService.download(any(), any(), any()) wasNot Called }
    }

    @Test
    fun `Given, downloadData is called with a DecryptedRecord, which contains a Fhir3, and UserId, it reflects the record, if it is not capable of having attachments`() {
        // Given
        val resource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns false

        // When
        val record = recordService.downloadData(decryptedRecord, USER_ID)

        // Then
        assertSame(
                actual = record,
                expected = decryptedRecord
        )

        verify { attachmentService.download(any(), any(), any()) wasNot Called }
    }

    @Test
    fun `Given, downloadData is called with a DecryptedRecord, which contains a Fhir3, and UserId, it reflects the record, if it has no attachments`() {
        // Given
        val resource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns null

        // When
        val record = recordService.downloadData(decryptedRecord, USER_ID)

        // Then
        assertSame(
                actual = record,
                expected = decryptedRecord
        )

        verify { attachmentService.download(any(), any(), any()) wasNot Called }
    }

    @Test
    fun `Given, downloadData is called with a DecryptedRecord, which contains a Fhir3, and UserId, it fails if an attachment has no id`() {
        // Given
        val resource: Fhir3Resource = mockk()
        val attachmentKey: GCKey = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()

        val attachments: MutableList<Any> = mutableListOf(
                mockk()
        )

        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { wrappedAttachment.id } returns null

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment

        // When
        val error = assertFailsWith<DataValidationException.IdUsageViolation> {
            // Then
            recordService.downloadData(decryptedRecord, USER_ID)
        }

        assertEquals(
                actual = error.message,
                expected = "Attachment.id expected"
        )
    }

    @Test
    fun `Given, downloadData is called with a DecryptedRecord, which contains a Fhir3, and UserId, it downloads an attachment`() {
        // Given
        val resource: Fhir3Resource = mockk()
        val attachmentKey: GCKey = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()

        val attachments: MutableList<Any> = mutableListOf(
                mockk()
        )

        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { wrappedAttachment.id } returns "id"

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment

        every {
            attachmentService.download(
                    listOf(wrappedAttachment),
                    attachmentKey,
                    USER_ID
            )
        } returns Single.just(mockk())

        // When
        val record = recordService.downloadData(decryptedRecord, USER_ID)

        // Then
        assertSame(
                actual = record,
                expected = decryptedRecord
        )

        verifyOrder {
            attachmentService.download(
                    listOf(wrappedAttachment),
                    attachmentKey,
                    USER_ID
            )
        }
    }

    @Test
    fun `Given, downloadData is called with a DecryptedRecord, which contains a Fhir3, and UserId, it downloads an attachment, while resolving the attachmentKey`() {
        // Given
        val resource: Fhir3Resource = mockk()
        val attachmentKey: GCKey = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()

        val attachments: MutableList<Any> = mutableListOf(
                mockk()
        )

        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returnsMany listOf(null, attachmentKey)

        every { wrappedAttachment.id } returns "id"

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment

        every { cryptoService.generateGCKey() } returns Single.just(attachmentKey)
        every { decryptedRecord.attachmentsKey = attachmentKey } returns Unit

        every {
            attachmentService.download(
                    listOf(wrappedAttachment),
                    attachmentKey,
                    USER_ID
            )
        } returns Single.just(mockk())

        // When
        val record = recordService.downloadData(decryptedRecord, USER_ID)

        // Then
        assertSame(
                actual = record,
                expected = decryptedRecord
        )

        verifyOrder {
            cryptoService.generateGCKey()
            decryptedRecord.attachmentsKey
            attachmentService.download(
                    listOf(wrappedAttachment),
                    attachmentKey,
                    USER_ID
            )
        }
    }

    @Test
    fun `Given, downloadData is called with a DecryptedRecord, which contains a Fhir3, and UserId, it ignores Attachments, which are null`() {
        // Given
        val resource: Fhir3Resource = mockk()
        val attachmentKey: GCKey = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()

        val attachments: MutableList<Fhir3Attachment?> = mutableListOf(null, mockk())

        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returnsMany listOf(null, attachmentKey)

        every { wrappedAttachment.id } returns "id"

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(attachments[1]!!) } returns wrappedAttachment

        every { cryptoService.generateGCKey() } returns Single.just(attachmentKey)
        every { decryptedRecord.attachmentsKey = attachmentKey } returns Unit

        every {
            attachmentService.download(
                    listOf(wrappedAttachment),
                    attachmentKey,
                    USER_ID
            )
        } returns Single.just(mockk())

        // When
        val record = recordService.downloadData(decryptedRecord, USER_ID)

        // Then
        assertSame(
                actual = record,
                expected = decryptedRecord
        )

        verifyOrder {
            cryptoService.generateGCKey()
            decryptedRecord.attachmentsKey
            attachmentService.download(
                    listOf(wrappedAttachment),
                    attachmentKey,
                    USER_ID
            )
        }

        verify(exactly = 1) {
            attachmentService.download(
                    listOf(wrappedAttachment),
                    attachmentKey,
                    USER_ID
            )
        }
    }

    @Test
    fun `Given, downloadData is called with a DecryptedRecord, which contains a Fhir4, and UserId, it reflects the record, if it is not capable of having attachments`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns false

        // When
        val record = recordService.downloadData(decryptedRecord, USER_ID)

        // Then
        assertSame(
                actual = record,
                expected = decryptedRecord
        )

        verify { attachmentService.download(any(), any(), any()) wasNot Called }
    }

    @Test
    fun `Given, downloadData is called with a DecryptedRecord, which contains a Fhir4, and UserId, it reflects the record, if it has no attachments`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns null

        // When
        val record = recordService.downloadData(decryptedRecord, USER_ID)

        // Then
        assertSame(
                actual = record,
                expected = decryptedRecord
        )

        verify { attachmentService.download(any(), any(), any()) wasNot Called }
    }

    @Test
    fun `Given, downloadData is called with a DecryptedRecord, which contains a Fhir4, and UserId, it fails if an attachment has no id`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val attachmentKey: GCKey = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()

        val attachments: MutableList<Any> = mutableListOf(
                mockk()
        )

        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { wrappedAttachment.id } returns null

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment

        // When
        val error = assertFailsWith<DataValidationException.IdUsageViolation> {
            // Then
            recordService.downloadData(decryptedRecord, USER_ID)
        }

        assertEquals(
                actual = error.message,
                expected = "Attachment.id expected"
        )
    }

    @Test
    fun `Given, downloadData is called with a DecryptedRecord, which contains a Fhir4, and UserId, it downloads an attachment`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val attachmentKey: GCKey = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()

        val attachments: MutableList<Any> = mutableListOf(
                mockk()
        )

        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { wrappedAttachment.id } returns "id"

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment

        every {
            attachmentService.download(
                    listOf(wrappedAttachment),
                    attachmentKey,
                    USER_ID
            )
        } returns Single.just(mockk())

        // When
        val record = recordService.downloadData(decryptedRecord, USER_ID)

        // Then
        assertSame(
                actual = record,
                expected = decryptedRecord
        )

        verifyOrder {
            attachmentService.download(
                    listOf(wrappedAttachment),
                    attachmentKey,
                    USER_ID
            )
        }
    }

    @Test
    fun `Given, downloadData is called with a DecryptedRecord, which contains a Fhir4, and UserId, it ignores Attachments, which are null`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val attachmentKey: GCKey = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()

        val attachments: MutableList<Fhir4Attachment?> = mutableListOf(null, mockk())

        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returnsMany listOf(null, attachmentKey)

        every { wrappedAttachment.id } returns "id"

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(attachments[1]!!) } returns wrappedAttachment

        every { cryptoService.generateGCKey() } returns Single.just(attachmentKey)
        every { decryptedRecord.attachmentsKey = attachmentKey } returns Unit

        every {
            attachmentService.download(
                    listOf(wrappedAttachment),
                    attachmentKey,
                    USER_ID
            )
        } returns Single.just(mockk())

        // When
        val record = recordService.downloadData(decryptedRecord, USER_ID)

        // Then
        assertSame(
                actual = record,
                expected = decryptedRecord
        )

        verifyOrder {
            cryptoService.generateGCKey()
            decryptedRecord.attachmentsKey
            attachmentService.download(
                    listOf(wrappedAttachment),
                    attachmentKey,
                    USER_ID
            )
        }

        verify(exactly = 1) {
            attachmentService.download(
                    listOf(wrappedAttachment),
                    attachmentKey,
                    USER_ID
            )
        }
    }

    @Test
    fun `Given, downloadData is called with a DecryptedRecord, which contains a Fhir4, and UserId, it downloads an attachment, while resolving the attachmentKey`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val attachmentKey: GCKey = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()

        val attachments: MutableList<Any> = mutableListOf(
                mockk()
        )

        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returnsMany listOf(null, attachmentKey)

        every { wrappedAttachment.id } returns "id"

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment

        every { cryptoService.generateGCKey() } returns Single.just(attachmentKey)
        every { decryptedRecord.attachmentsKey = attachmentKey } returns Unit

        every {
            attachmentService.download(
                    listOf(wrappedAttachment),
                    attachmentKey,
                    USER_ID
            )
        } returns Single.just(mockk())

        // When
        val record = recordService.downloadData(decryptedRecord, USER_ID)

        // Then
        assertSame(
                actual = record,
                expected = decryptedRecord
        )

        verifyOrder {
            cryptoService.generateGCKey()
            decryptedRecord.attachmentsKey
            attachmentService.download(
                    listOf(wrappedAttachment),
                    attachmentKey,
                    USER_ID
            )
        }
    }
}