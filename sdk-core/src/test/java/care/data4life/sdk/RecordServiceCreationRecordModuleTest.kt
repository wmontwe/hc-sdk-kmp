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

import care.data4life.fhir.r4.model.DocumentReference as Fhir4DocumentReference
import care.data4life.fhir.r4.model.Reference as Fhir4Reference
import care.data4life.fhir.stu3.model.DocumentReference as Fhir3DocumentReference
import care.data4life.fhir.stu3.model.Reference as Fhir3Reference
import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.attachment.AttachmentService
import care.data4life.sdk.attachment.FileService
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.crypto.GCKey
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.date.SdkDateTimeFormatter
import care.data4life.sdk.fhir.Fhir3Identifier
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Identifier
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.fhir.ResourceCryptoService
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.Record
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.NetworkModelContract
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.TagCryptoService
import care.data4life.sdk.tag.TaggingService
import care.data4life.sdk.tag.Tags
import care.data4life.sdk.test.fake.CryptoServiceFake
import care.data4life.sdk.test.fake.CryptoServiceIteration
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.ARBITRARY_DATA_KEY
import care.data4life.sdk.test.util.GenericTestDataProvider.ATTACHMENT_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.CLIENT_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.COMMON_KEY_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.CREATION_DATE
import care.data4life.sdk.test.util.GenericTestDataProvider.PARTNER_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.PDF_OVERSIZED
import care.data4life.sdk.test.util.GenericTestDataProvider.PDF_OVERSIZED_ENCODED
import care.data4life.sdk.test.util.GenericTestDataProvider.PREVIEW
import care.data4life.sdk.test.util.GenericTestDataProvider.PREVIEW_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.RECORD_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.THUMBNAIL
import care.data4life.sdk.test.util.GenericTestDataProvider.THUMBNAIL_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.UPDATE_DATE
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import care.data4life.sdk.test.util.TestResourceHelper
import care.data4life.sdk.test.util.TestResourceHelper.loadTemplate
import care.data4life.sdk.test.util.TestResourceHelper.loadTemplateWithAttachments
import care.data4life.sdk.util.Base64
import care.data4life.sdk.wrapper.SdkFhirParser
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.reactivex.Single
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class RecordServiceCreationRecordModuleTest {
    private val dataKey: GCKey = mockk()
    private val attachmentKey: GCKey = mockk()
    private val tagEncryptionKey: GCKey = mockk()
    private val commonKey: GCKey = mockk()
    private val encryptedDataKey: EncryptedKey = mockk()
    private val encryptedAttachmentKey: EncryptedKey = mockk()

    private lateinit var recordService: RecordContract.Service
    private val apiService: NetworkingContract.Service = mockk()
    private lateinit var flowHelper: RecordServiceModuleTestFlowHelper
    private lateinit var cryptoService: CryptoContract.Service
    private val imageResizer: AttachmentContract.ImageResizer = mockk()
    private val errorHandler: D4LErrorHandler = mockk()

    @Before
    fun setUp() {
        clearAllMocks()

        cryptoService = CryptoServiceFake()

        recordService = RecordService(
            PARTNER_ID,
            ALIAS,
            apiService,
            TagCryptoService(cryptoService),
            TaggingService(CLIENT_ID),
            ResourceCryptoService(cryptoService),
            AttachmentService(
                FileService(ALIAS, apiService, cryptoService),
                imageResizer
            ),
            cryptoService,
            errorHandler
        )

        flowHelper = RecordServiceModuleTestFlowHelper(
            apiService,
            imageResizer
        )
    }

    private fun createRecievedEncryptedRecord(
        encryptedRecord: EncryptedRecord,
        recordId: String,
        creationDate: String = CREATION_DATE,
        updatedDate: String = UPDATE_DATE
    ): EncryptedRecord = encryptedRecord.copy(
        identifier = recordId,
        customCreationDate = creationDate,
        updatedDate = updatedDate
    )

    private fun prepareFlow(
        alias: String,
        userId: String,
        encryptedUploadRecord: EncryptedRecord,
        uploadIteration: CryptoServiceIteration,
        encryptedReceivedRecord: EncryptedRecord,
        receivedIteration: CryptoServiceIteration
    ) {
        val actualRecord = slot<NetworkModelContract.EncryptedRecord>()
        (cryptoService as CryptoServiceFake).iteration = uploadIteration

        every {
            apiService.createRecord(alias, userId, capture(actualRecord))
        } answers {
            if (flowHelper.compareEncryptedRecords(actualRecord.captured, encryptedUploadRecord)) {
                Single.just(encryptedReceivedRecord).also {
                    (cryptoService as CryptoServiceFake).iteration = receivedIteration
                }
            } else {
                throw RuntimeException("Unexpected encrypted record\n${actualRecord.captured}")
            }
        }
    }

    private fun runFlow(
        encryptedUploadRecord: EncryptedRecord,
        serializedResource: String,
        tags: List<String>,
        annotations: Annotations,
        useStoredCommonKey: Boolean,
        commonKey: Pair<String, GCKey>,
        dataKey: Pair<GCKey, EncryptedKey>,
        attachmentKey: Pair<GCKey, EncryptedKey>?,
        tagEncryptionKey: GCKey,
        userId: String = USER_ID,
        alias: String = ALIAS,
        recordId: String = RECORD_ID,
        attachments: List<String>? = null
    ) {
        val encryptedCommonKey = flowHelper.prepareStoredOrUnstoredCommonKeyRun(
            alias,
            userId,
            commonKey.first,
            useStoredCommonKey
        )

        val keyOrder = flowHelper.makeKeyOrder(dataKey, attachmentKey)

        val resources = flowHelper.packResources(listOf(serializedResource), attachments)

        val uploadIteration = CryptoServiceIteration(
            gcKeyOrder = keyOrder,
            commonKey = commonKey.second,
            commonKeyId = commonKey.first,
            commonKeyIsStored = false,
            commonKeyFetchCalls = 1,
            encryptedCommonKey = null,
            dataKey = dataKey.first,
            encryptedDataKey = dataKey.second,
            attachmentKey = attachmentKey?.first,
            encryptedAttachmentKey = attachmentKey?.second,
            tagEncryptionKey = tagEncryptionKey,
            tagEncryptionKeyCalls = 1,
            resources = resources,
            tags = tags,
            annotations = annotations,
            hashFunction = { value -> flowHelper.md5(value) }
        )

        val encryptedReceivedRecord = createRecievedEncryptedRecord(encryptedUploadRecord, recordId)
        val receivedIteration = CryptoServiceIteration(
            gcKeyOrder = emptyList(),
            commonKey = commonKey.second,
            commonKeyId = commonKey.first,
            commonKeyIsStored = useStoredCommonKey,
            commonKeyFetchCalls = 0,
            encryptedCommonKey = encryptedCommonKey,
            dataKey = dataKey.first,
            encryptedDataKey = dataKey.second,
            attachmentKey = attachmentKey?.first,
            encryptedAttachmentKey = attachmentKey?.second,
            tagEncryptionKey = tagEncryptionKey,
            tagEncryptionKeyCalls = 1,
            resources = listOf(serializedResource),
            tags = tags,
            annotations = annotations,
            hashFunction = { value -> flowHelper.md5(value) }
        )

        prepareFlow(
            alias,
            userId,
            encryptedUploadRecord,
            uploadIteration,
            encryptedReceivedRecord,
            receivedIteration
        )
    }

    private fun runFhirFlow(
        serializedResource: String,
        tags: Tags,
        annotations: Annotations = emptyList(),
        useStoredCommonKey: Boolean = true,
        commonKey: Pair<String, GCKey> = COMMON_KEY_ID to this.commonKey,
        dataKey: Pair<GCKey, EncryptedKey> = this.dataKey to encryptedDataKey,
        tagEncryptionKey: GCKey = this.tagEncryptionKey,
        userId: String = USER_ID,
        alias: String = ALIAS,
        recordId: String = RECORD_ID
    ) {
        val encodedTags = flowHelper.prepareTags(tags)
        val encodedAnnotations = flowHelper.prepareAnnotations(annotations)

        val encryptedUploadRecord = flowHelper.prepareEncryptedRecord(
            null,
            serializedResource,
            encodedTags,
            encodedAnnotations,
            commonKey.first,
            dataKey.second,
            null,
            SdkDateTimeFormatter.now(),
            null
        )

        runFlow(
            encryptedUploadRecord,
            serializedResource,
            encodedTags,
            encodedAnnotations,
            useStoredCommonKey,
            commonKey,
            dataKey,
            null,
            tagEncryptionKey,
            userId,
            alias,
            recordId
        )
    }

    private fun runFhirFlowWithAttachment(
        serializedResource: String,
        attachmentData: ByteArray,
        tags: Tags,
        annotations: Annotations = emptyList(),
        useStoredCommonKey: Boolean = true,
        commonKey: Pair<String, GCKey> = COMMON_KEY_ID to this.commonKey,
        dataKey: Pair<GCKey, EncryptedKey> = this.dataKey to encryptedDataKey,
        attachmentKey: Pair<GCKey, EncryptedKey> = this.attachmentKey to encryptedAttachmentKey,
        tagEncryptionKey: GCKey = this.tagEncryptionKey,
        userId: String = USER_ID,
        alias: String = ALIAS,
        recordId: String = RECORD_ID,
        attachmentId: String = ATTACHMENT_ID,
        resizedImages: Pair<Pair<ByteArray, String>, Pair<ByteArray, String>?>? = null
    ) {
        val encodedTags = flowHelper.prepareTags(tags)
        val encodedAnnotations = flowHelper.prepareAnnotations(annotations)

        val encryptedUploadRecord = flowHelper.prepareEncryptedRecord(
            null,
            serializedResource,
            encodedTags,
            encodedAnnotations,
            commonKey.first,
            dataKey.second,
            attachmentKey.second,
            SdkDateTimeFormatter.now(),
            null
        )

        val mappedAttachments = flowHelper.mapAttachments(attachmentData, resizedImages)

        runFlow(
            encryptedUploadRecord,
            serializedResource,
            encodedTags,
            encodedAnnotations,
            useStoredCommonKey,
            commonKey,
            dataKey,
            attachmentKey,
            tagEncryptionKey,
            userId,
            alias,
            recordId,
            mappedAttachments
        )

        flowHelper.uploadAttachment(
            alias = alias,
            payload = Pair(attachmentData, attachmentId),
            userId = userId,
            resized = resizedImages
        )
    }

    private fun runArbitraryDataFlow(
        serializedResource: String,
        tags: Tags,
        annotations: Annotations = emptyList(),
        useStoredCommonKey: Boolean = true,
        commonKey: Pair<String, GCKey> = COMMON_KEY_ID to this.commonKey,
        dataKey: Pair<GCKey, EncryptedKey> = this.dataKey to encryptedDataKey,
        tagEncryptionKey: GCKey = this.tagEncryptionKey,
        userId: String = USER_ID,
        alias: String = ALIAS,
        recordId: String = RECORD_ID
    ) {
        val encodedTags = flowHelper.prepareTags(tags)
        val encodedAnnotations = flowHelper.prepareAnnotations(annotations)

        val encryptedUploadRecord = flowHelper.prepareEncryptedRecord(
            null,
            serializedResource,
            encodedTags,
            encodedAnnotations,
            commonKey.first,
            dataKey.second,
            null,
            SdkDateTimeFormatter.now(),
            null
        )

        runFlow(
            encryptedUploadRecord,
            serializedResource,
            encodedTags,
            encodedAnnotations,
            useStoredCommonKey,
            commonKey,
            dataKey,
            null,
            tagEncryptionKey,
            userId,
            alias,
            recordId
        )
    }

    // FHIR3
    @Test
    fun `Given, createFhir3Record is called with the appropriate payload without Annotations or Attachments, it creates a Record for Fhir3`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        )

        val template = loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID,
            PARTNER_ID
        )

        val resource = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        runFhirFlow(
            serializedResource = SdkFhirParser.fromResource(resource),
            tags = tags,
            useStoredCommonKey = false
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
            emptyList()
        ).blockingGet()

        // Then
        assertTrue(result is Record)
        assertTrue(result.resource.identifier!!.isNotEmpty())
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertTrue(result.annotations!!.isEmpty())
        assertEquals(
            expected = resource,
            actual = result.resource
        )
    }

    @Test
    fun `Given, createFhir3Record is called with the appropriate payload with Annotations and without Attachments, it creates a Record for Fhir3`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val template = loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID,
            PARTNER_ID
        )

        val resource = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        runFhirFlow(
            serializedResource = SdkFhirParser.fromResource(resource),
            tags = tags,
            annotations = annotations
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
            annotations = annotations
        ).blockingGet()

        // Then
        assertTrue(result is Record)
        assertTrue(result.resource.identifier!!.isNotEmpty())
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertEquals(
            actual = result.annotations,
            expected = annotations
        )
        assertEquals(
            expected = resource,
            actual = result.resource
        )
    }

    @Test
    fun `Given, createFhir3Record is called with the appropriate payload with Annotations and Attachments, it creates a Record for Fhir3`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val rawAttachment = TestResourceHelper.getByteResource("attachments", "sample.pdf")
        val attachment = Base64.encodeToString(rawAttachment)

        val template = loadTemplateWithAttachments(
            "common",
            "documentReference-with-attachment-template",
            RECORD_ID,
            PARTNER_ID,
            "Sample PDF",
            "application/pdf",
            attachment
        )

        val resource = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        val internalResource = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        internalResource.identifier!!.add(
            Fhir3Identifier().also {
                it.assigner = Fhir3Reference().also { ref -> ref.reference = PARTNER_ID }
                it.value = "d4l_f_p_t#$ATTACHMENT_ID"
            }
        )

        internalResource.content[0].attachment.id = ATTACHMENT_ID
        internalResource.content[0].attachment.data = null

        runFhirFlowWithAttachment(
            serializedResource = SdkFhirParser.fromResource(internalResource),
            attachmentData = rawAttachment,
            tags = tags,
            annotations = annotations,
            attachmentId = ATTACHMENT_ID
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
            annotations = annotations
        ).blockingGet()

        // Then
        assertTrue(result is Record)
        assertTrue(result.resource.identifier!!.isNotEmpty())
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertEquals(
            actual = result.annotations,
            expected = annotations
        )
        assertEquals(
            expected = resource,
            actual = result.resource
        )
        assertEquals(
            actual = result.resource.content.size,
            expected = 1
        )
        assertEquals(
            actual = result.resource.content[0].attachment.data,
            expected = attachment
        )
        assertEquals(
            actual = result.resource.content[0].attachment.id,
            expected = ATTACHMENT_ID
        )
        assertEquals(
            actual = result.resource.identifier?.get(1)?.value,
            expected = "d4l_f_p_t#$ATTACHMENT_ID"
        )
    }

    @Test
    fun `Given, createFhir3Record is called with the appropriate payload with Annotations and Attachments, it creates a Record for Fhir3, while resizing the attachment`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val rawAttachment = TestResourceHelper.getByteResource("attachments", "sample.png")
        val attachment = Base64.encodeToString(rawAttachment)

        val template = loadTemplateWithAttachments(
            "common",
            "documentReference-with-attachment-template",
            RECORD_ID,
            PARTNER_ID,
            "Sample PNG",
            "image/png",
            attachment
        )

        val resource = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        val internalResource = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        internalResource.identifier!!.add(
            Fhir3Identifier().also {
                it.assigner = Fhir3Reference().also { ref -> ref.reference = PARTNER_ID }
                it.value = "d4l_f_p_t#$ATTACHMENT_ID#$PREVIEW_ID#$THUMBNAIL_ID"
            }
        )

        internalResource.content[0].attachment.id = ATTACHMENT_ID
        internalResource.content[0].attachment.data = null

        val preview = Pair(PREVIEW, PREVIEW_ID)
        val thumbnail = Pair(THUMBNAIL, THUMBNAIL_ID)

        runFhirFlowWithAttachment(
            serializedResource = SdkFhirParser.fromResource(internalResource),
            attachmentData = rawAttachment,
            tags = tags,
            annotations = annotations,
            attachmentId = ATTACHMENT_ID,
            resizedImages = Pair(preview, thumbnail)
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
            annotations = annotations
        ).blockingGet()

        // Then
        assertTrue(result is Record)
        assertTrue(result.resource.identifier!!.isNotEmpty())
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertEquals(
            actual = result.annotations,
            expected = annotations
        )
        assertEquals(
            expected = resource,
            actual = result.resource
        )
        assertEquals(
            actual = result.resource.content.size,
            expected = 1
        )
        assertEquals(
            actual = result.resource.content[0].attachment.data,
            expected = attachment
        )
        assertEquals(
            actual = result.resource.content[0].attachment.id,
            expected = ATTACHMENT_ID
        )
        assertEquals(
            actual = result.resource.identifier?.get(1)?.value,
            expected = "d4l_f_p_t#$ATTACHMENT_ID#$PREVIEW_ID#$THUMBNAIL_ID"
        )
    }

    @Ignore("Gradle runs out of heap memory")
    @Test
    fun `Given, createFhir3Record is called with the appropriate payload with Annotations and Attachments, it fails due to a ill Attachment`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val rawAttachment = PDF_OVERSIZED
        val attachment = PDF_OVERSIZED_ENCODED

        val template = loadTemplateWithAttachments(
            "common",
            "documentReference-with-attachment-template",
            RECORD_ID,
            PARTNER_ID,
            "Sample PDF",
            "application/pdf",
            attachment
        )

        val resource = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        runFhirFlowWithAttachment(
            serializedResource = SdkFhirParser.fromResource(resource),
            attachmentData = rawAttachment,
            tags = tags,
            annotations = annotations,
            attachmentId = ATTACHMENT_ID
        )

        // Then
        assertFailsWith<DataValidationException.MaxDataSizeViolation> {
            // When
            recordService.createRecord(
                USER_ID,
                resource,
                annotations = annotations
            ).blockingGet()
        }
    }

    @Test
    fun `Given, createFhir3Record is called with the appropriate payload with Annotations and Attachments, it creates a Record for Fhir3, while resizing the attachment once`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val rawAttachment = TestResourceHelper.getByteResource("attachments", "sample.png")
        val attachment = Base64.encodeToString(rawAttachment)

        val template = loadTemplateWithAttachments(
            "common",
            "documentReference-with-attachment-template",
            RECORD_ID,
            PARTNER_ID,
            "Sample PNG",
            "image/png",
            attachment
        )

        val resource = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        val internalResource = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        internalResource.identifier!!.add(
            Fhir3Identifier().also {
                it.assigner = Fhir3Reference().also { ref -> ref.reference = PARTNER_ID }
                it.value = "d4l_f_p_t#$ATTACHMENT_ID#$PREVIEW_ID#$ATTACHMENT_ID"
            }
        )

        internalResource.content[0].attachment.id = ATTACHMENT_ID
        internalResource.content[0].attachment.data = null

        val preview = Pair(PREVIEW, PREVIEW_ID)

        runFhirFlowWithAttachment(
            serializedResource = SdkFhirParser.fromResource(internalResource),
            attachmentData = rawAttachment,
            tags = tags,
            annotations = annotations,
            attachmentId = ATTACHMENT_ID,
            resizedImages = Pair(preview, null)
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
            annotations = annotations
        ).blockingGet()

        // Then
        assertTrue(result is Record)
        assertTrue(result.resource.identifier!!.isNotEmpty())
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertEquals(
            actual = result.annotations,
            expected = annotations
        )
        assertEquals(
            expected = resource,
            actual = result.resource
        )
        assertEquals(
            actual = result.resource.content.size,
            expected = 1
        )
        assertEquals(
            actual = result.resource.content[0].attachment.data,
            expected = attachment
        )
        assertEquals(
            actual = result.resource.content[0].attachment.id,
            expected = ATTACHMENT_ID
        )
        assertEquals(
            actual = result.resource.identifier?.get(1)?.value,
            expected = "d4l_f_p_t#$ATTACHMENT_ID#$PREVIEW_ID#$ATTACHMENT_ID"
        )
    }

    // FHIR4
    @Test
    fun `Given, createFhir4Record is called with the appropriate payload without Annotations or Attachments, it creates a Record for Fhir4`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        )

        val template = loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID,
            PARTNER_ID
        )

        val resource = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        runFhirFlow(
            serializedResource = SdkFhirParser.fromResource(resource),
            tags = tags,
            useStoredCommonKey = false
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
            emptyList()
        ).blockingGet()

        // Then
        assertTrue(result is Fhir4Record)
        assertTrue(result.resource.identifier!!.isNotEmpty())
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertTrue(result.annotations.isEmpty())
        assertEquals(
            expected = resource,
            actual = result.resource
        )
    }

    @Test
    fun `Given, createFhir4Record is called with the appropriate payload with Annotations and without Attachments, it creates a Record for Fhir4`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val template = loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID,
            PARTNER_ID
        )

        val resource = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        runFhirFlow(
            serializedResource = SdkFhirParser.fromResource(resource),
            tags = tags,
            annotations = annotations
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
            annotations = annotations
        ).blockingGet()

        // Then
        assertTrue(result is Fhir4Record)
        assertTrue(result.resource.identifier!!.isNotEmpty())
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertEquals(
            actual = result.annotations,
            expected = annotations
        )
        assertEquals(
            expected = resource,
            actual = result.resource
        )
    }

    @Test
    fun `Given, createFhir4Record is called with the appropriate payload with Annotations and Attachments, it creates a Record for Fhir4`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val rawAttachment = TestResourceHelper.getByteResource("attachments", "sample.pdf")
        val attachment = Base64.encodeToString(rawAttachment)

        val template = loadTemplateWithAttachments(
            "common",
            "documentReference-with-attachment-template",
            RECORD_ID,
            PARTNER_ID,
            "Sample PDF",
            "application/pdf",
            attachment
        )

        val resource = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        val internalResource = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        internalResource.identifier!!.add(
            Fhir4Identifier().also {
                it.assigner = Fhir4Reference()
                    .also { ref -> ref.reference = PARTNER_ID }
                it.value = "d4l_f_p_t#$ATTACHMENT_ID"
            }
        )

        internalResource.content[0].attachment.id = ATTACHMENT_ID
        internalResource.content[0].attachment.data = null

        runFhirFlowWithAttachment(
            serializedResource = SdkFhirParser.fromResource(internalResource),
            attachmentData = rawAttachment,
            tags = tags,
            annotations = annotations,
            attachmentId = ATTACHMENT_ID
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
            annotations = annotations
        ).blockingGet()

        // Then
        assertTrue(result is Fhir4Record)
        assertTrue(result.resource.identifier!!.isNotEmpty())
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertEquals(
            actual = result.annotations,
            expected = annotations
        )
        assertEquals(
            expected = resource,
            actual = result.resource
        )
        assertEquals(
            actual = result.resource.content.size,
            expected = 1
        )
        assertEquals(
            actual = result.resource.content[0].attachment.data,
            expected = attachment
        )
        assertEquals(
            actual = result.resource.content[0].attachment.id,
            expected = ATTACHMENT_ID
        )
        assertEquals(
            actual = result.resource.identifier?.get(1)?.value,
            expected = "d4l_f_p_t#$ATTACHMENT_ID"
        )
    }

    @Test
    fun `Given, createFhir4Record is called with the appropriate payload with Annotations and Attachments, it creates a Record for Fhir4, while resizing the attachment`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val rawAttachment = TestResourceHelper.getByteResource("attachments", "sample.png")
        val attachment = Base64.encodeToString(rawAttachment)

        val template = loadTemplateWithAttachments(
            "common",
            "documentReference-with-attachment-template",
            RECORD_ID,
            PARTNER_ID,
            "Sample PNG",
            "image/png",
            attachment
        )

        val resource = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        val internalResource = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        internalResource.identifier!!.add(
            Fhir4Identifier().also {
                it.assigner = Fhir4Reference()
                    .also { ref -> ref.reference = PARTNER_ID }
                it.value = "d4l_f_p_t#$ATTACHMENT_ID#$PREVIEW_ID#$THUMBNAIL_ID"
            }
        )

        internalResource.content[0].attachment.id = ATTACHMENT_ID
        internalResource.content[0].attachment.data = null

        val preview = Pair(ByteArray(2), PREVIEW_ID)
        val thumbnail = Pair(ByteArray(1), THUMBNAIL_ID)

        runFhirFlowWithAttachment(
            serializedResource = SdkFhirParser.fromResource(internalResource),
            attachmentData = rawAttachment,
            tags = tags,
            annotations = annotations,
            attachmentId = ATTACHMENT_ID,
            resizedImages = Pair(preview, thumbnail)
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
            annotations = annotations
        ).blockingGet()

        // Then
        assertTrue(result is Fhir4Record)
        assertTrue(result.resource.identifier!!.isNotEmpty())
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertEquals(
            actual = result.annotations,
            expected = annotations
        )
        assertEquals(
            expected = resource,
            actual = result.resource
        )
        assertEquals(
            actual = result.resource.content.size,
            expected = 1
        )
        assertEquals(
            actual = result.resource.content[0].attachment.data,
            expected = attachment
        )
        assertEquals(
            actual = result.resource.content[0].attachment.id,
            expected = ATTACHMENT_ID
        )
        assertEquals(
            actual = result.resource.identifier?.get(1)?.value,
            expected = "d4l_f_p_t#$ATTACHMENT_ID#$PREVIEW_ID#$THUMBNAIL_ID"
        )
    }

    @Test
    fun `Given, createFhir4Record is called with the appropriate payload with Annotations and Attachments, it creates a Record for Fhir4, while resizing the attachment once`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val rawAttachment = TestResourceHelper.getByteResource("attachments", "sample.png")
        val attachment = Base64.encodeToString(rawAttachment)

        val template = loadTemplateWithAttachments(
            "common",
            "documentReference-with-attachment-template",
            RECORD_ID,
            PARTNER_ID,
            "Sample PNG",
            "image/png",
            attachment
        )

        val resource = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        val internalResource = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        internalResource.identifier!!.add(
            Fhir4Identifier().also {
                it.assigner = Fhir4Reference().also { ref -> ref.reference = PARTNER_ID }
                it.value = "d4l_f_p_t#$ATTACHMENT_ID#$PREVIEW_ID#$ATTACHMENT_ID"
            }
        )

        internalResource.content[0].attachment.id = ATTACHMENT_ID
        internalResource.content[0].attachment.data = null

        val preview = Pair(PREVIEW, PREVIEW_ID)

        runFhirFlowWithAttachment(
            serializedResource = SdkFhirParser.fromResource(internalResource),
            attachmentData = rawAttachment,
            tags = tags,
            annotations = annotations,
            attachmentId = ATTACHMENT_ID,
            resizedImages = Pair(preview, null)
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
            annotations = annotations
        ).blockingGet()

        // Then
        assertTrue(result is Fhir4Record)
        assertTrue(result.resource.identifier!!.isNotEmpty())
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertEquals(
            actual = result.annotations,
            expected = annotations
        )
        assertEquals(
            expected = resource,
            actual = result.resource
        )
        assertEquals(
            actual = result.resource.content.size,
            expected = 1
        )
        assertEquals(
            actual = result.resource.content[0].attachment.data,
            expected = attachment
        )
        assertEquals(
            actual = result.resource.content[0].attachment.id,
            expected = ATTACHMENT_ID
        )
        assertEquals(
            actual = result.resource.identifier?.get(1)?.value,
            expected = "d4l_f_p_t#$ATTACHMENT_ID#$PREVIEW_ID#$ATTACHMENT_ID"
        )
    }

    @Ignore("Gradle runs out of heap memory")
    @Test
    fun `Given, createFhir4Record is called with the appropriate payload with Annotations and Attachments, it fails due to a ill Attachment`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val rawAttachment = PDF_OVERSIZED
        val attachment = PDF_OVERSIZED_ENCODED

        val template = loadTemplateWithAttachments(
            "common",
            "documentReference-with-attachment-template",
            RECORD_ID,
            PARTNER_ID,
            "Sample PNG",
            "image/png",
            attachment
        )

        val resource = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        runFhirFlowWithAttachment(
            serializedResource = SdkFhirParser.fromResource(resource),
            attachmentData = rawAttachment,
            tags = tags,
            annotations = annotations,
            attachmentId = ATTACHMENT_ID
        )

        // Then
        assertFailsWith<DataValidationException.MaxDataSizeViolation> {
            // When
            recordService.createRecord(
                USER_ID,
                resource,
                annotations = annotations
            ).blockingGet()
        }
    }

    // Arbitrary Data
    @Test
    fun `Given, createDataRecord is called with the appropriate payload without Annotations, it creates a Record for Arbitrary Data`() {
        // Given
        val payload = "Me and my poney its name is Johny."
        val resource = DataResource(payload.toByteArray())

        val tags = mapOf(
            "flag" to ARBITRARY_DATA_KEY,
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID
        )

        runArbitraryDataFlow(
            serializedResource = payload,
            tags = tags,
            useStoredCommonKey = false
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
            emptyList()
        ).blockingGet()

        // Then
        assertTrue(result is DataRecord)
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertTrue(result.annotations.isEmpty())
        assertEquals(
            expected = resource,
            actual = result.resource
        )
    }

    @Test
    fun `Given, createDataRecord is called with the appropriate payload with Annotations, it creates a Record for Arbitrary Data`() {
        // Given
        val payload = "Me and my poney its name is Johny."
        val resource = DataResource(payload.toByteArray())

        val tags = mapOf(
            "flag" to ARBITRARY_DATA_KEY,
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        runArbitraryDataFlow(
            serializedResource = payload,
            tags = tags,
            annotations = annotations
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
            annotations
        ).blockingGet()

        // Then
        assertTrue(result is DataRecord)
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertEquals(
            actual = result.annotations,
            expected = annotations
        )
        assertEquals(
            expected = resource,
            actual = result.resource
        )
    }
}
