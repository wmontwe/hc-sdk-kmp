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

import care.data4life.fhir.stu3.model.DocumentReference
import care.data4life.sdk.attachment.AttachmentService
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.fhir.FhirService
import care.data4life.sdk.model.DownloadType
import care.data4life.sdk.model.ModelVersion
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.tag.TagEncryptionService
import care.data4life.sdk.tag.TaggingService
import care.data4life.sdk.util.Base64.encodeToString
import care.data4life.sdk.util.HashUtil.sha1
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Single
import org.junit.Before
import org.junit.Test

class RecordServiceIntegration : RecordServiceIntegrationBase() {
    @Before
    fun setUp() {
        apiService = mockk()
        cryptoService = mockk()
        fileService = mockk()
        imageResizer = mockk()
        errorHandler = mockk()

        recordService = RecordService(
                RecordServiceTestBase.PARTNER_ID,
                RecordServiceTestBase.ALIAS,
                apiService,
                TagEncryptionService(
                        cryptoService
                ),
                TaggingService(CLIENT_ID),
                FhirService(
                        cryptoService
                ),
                AttachmentService(
                        fileService,
                        imageResizer
                ),
                cryptoService,
                errorHandler
        )

        dataKey = mockk()
        attachmentKey = mockk()
        tagEncryptionKey = mockk()
        commonKey = mockk()
        encryptedDataKey = mockk()
        encryptedAttachmentKey = mockk()
    }

    private fun downloadAttachmentsFlow(
            fileId: String,
            payload: ByteArray,
            tags: Map<String, String>
    ) {
        every {
            apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)
        } returns Single.just(encryptedRecord)

        // decrypt tags
        every { cryptoService.fetchTagEncryptionKey() } returns tagEncryptionKey
        every {
            cryptoService.symDecrypt(tagEncryptionKey, eq(
                    tags["partner"]!!.toByteArray()
            ), IV)
        } returns tags["partner"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(tagEncryptionKey, eq(
                    tags["client"]!!.toByteArray()
            ), IV)
        } returns tags["client"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(tagEncryptionKey, eq(
                    tags["fhirversion"]!!.toByteArray()
            ), IV)
        } returns tags["fhirversion"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(tagEncryptionKey, eq(
                    tags["resourcetype"]!!.toByteArray()
            ), IV)
        } returns tags["resourcetype"]!!.toByteArray()

        // decrypt record
        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedAttachmentKey) } returns Single.just(attachmentKey)
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(dataKey)

        // decrypt Resource
        every { cryptoService.decryptString(dataKey, encryptedBody) } returns Single.just(stringifiedResource)

        // Get attachment
        every {
            fileService.downloadFile(attachmentKey, USER_ID, fileId)
        } returns Single.just(payload)
    }

    @Test
    fun `Given, downloadAttachments is called with its appropriate parameter, it returns a List of Attachments`() {
        // Given
        val payload = "hello test my old friend".toByteArray()

        val tags = mapOf(
                "partner" to "partner=TEST",
                "client" to "client=TEST",
                "fhirversion" to "fhirversion=3.0.1",
                "resourcetype" to "resourcetype=documentreference"
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"${encodeToString(sha1(payload))}\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#${ATTACHMENT_ID}#${PREVIEW_ID}#${ASSIGNER}\"}],\"resourceType\":\"DocumentReference\"}"
        encryptedBody = "ZW5jcnlwdGVk"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1URVNU", "Y2xpZW50PVRFU1Q=", "ZmhpcnZlcnNpb249My4wLjE=", "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                ModelVersion.CURRENT
        ).also { it.updatedDate = UPDATE_DATE }


        downloadAttachmentsFlow(
                "42",
                payload,
                tags
        )

        // When
        val result = (recordService as RecordService).downloadAttachments(
                RECORD_ID,
                listOf("42"),
                USER_ID,
                DownloadType.Full
        ).blockingGet()

        // Then
        Truth.assertThat(result).hasSize(1)
        Truth.assertThat(result[0]).isInstanceOf(Fhir3Attachment::class.java)
        Truth.assertThat(result[0].data).isNotEmpty()
    }

    @Test
    fun `Given, downloadAttachments is called with its appropriate parameter, it fails, if the referenced Resource is Fhir4`() {
        // Given
        val payload = "hello test my old friend".toByteArray()

        val tags = mapOf(
                "partner" to "partner=TEST",
                "client" to "client=TEST",
                "fhirversion" to "fhirversion=4.0.1",
                "resourcetype" to "resourcetype=documentreference"
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"${encodeToString(sha1(payload))}\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#${ATTACHMENT_ID}#${PREVIEW_ID}#${ASSIGNER}\"}],\"resourceType\":\"DocumentReference\"}"
        encryptedBody = "ZW5jcnlwdGVk"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1URVNU", "Y2xpZW50PVRFU1Q=", "ZmhpcnZlcnNpb249NC4wLjE=", "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                ModelVersion.CURRENT
        ).also { it.updatedDate = UPDATE_DATE }


        downloadAttachmentsFlow(
                "42",
                payload,
                tags
        )

        // When
        try {
            (recordService as RecordService).downloadAttachments(
                    RECORD_ID,
                    listOf("42"),
                    USER_ID,
                    DownloadType.Full
            ).blockingGet()
        } catch (e: Exception) {
            // Then
            Truth.assertThat(e).isInstanceOf(ClassCastException::class.java)
        }
    }

    @Test
    fun `Given, countRecords is called with its appropriate parameter, it returns the amount of Records`() {
        val encodedResourceType = "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"

        val tags = mapOf(
                "partner" to "partner=TEST",
                "client" to "client=TEST",
                "fhirversion" to "fhirversion=3.0.1",
                "resourcetype" to "resourcetype=documentreference"
        )

        // encrypt tags
        every { cryptoService.fetchTagEncryptionKey() } returns tagEncryptionKey
        every {
            cryptoService.symEncrypt(tagEncryptionKey, eq(
                    tags["resourcetype"]!!.toByteArray()
            ), IV)
        } returns tags["resourcetype"]!!.toByteArray()

        every { apiService.getCount(ALIAS, USER_ID, listOf(encodedResourceType)) } returns Single.just(23)

        // When
        val result = (recordService as RecordService).countRecords(
                DocumentReference::class.java,
                USER_ID
        ).blockingGet()

        // Then
        Truth.assertThat(result).isEqualTo(23)
    }
}
