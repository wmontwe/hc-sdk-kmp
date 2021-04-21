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

package care.data4life.sdk

import care.data4life.crypto.GCKey
import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.attachment.AttachmentContract.ImageResizer.Companion.DEFAULT_JPEG_QUALITY_PERCENT
import care.data4life.sdk.attachment.AttachmentContract.ImageResizer.Companion.DEFAULT_PREVIEW_SIZE_PX
import care.data4life.sdk.attachment.AttachmentContract.ImageResizer.Companion.DEFAULT_THUMBNAIL_SIZE_PX
import care.data4life.sdk.model.Meta
import care.data4life.sdk.model.ModelContract.ModelVersion.Companion.CURRENT
import care.data4life.sdk.network.model.CommonKeyResponse
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.test.util.GenericTestDataProvider.DATE_FORMATTER
import care.data4life.sdk.test.util.GenericTestDataProvider.DATE_TIME_FORMATTER
import care.data4life.sdk.util.Base64
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Single
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter

class RecordServiceModuleTestFlowHelper(
    private val apiService: ApiService,
    private val fileService: AttachmentContract.FileService,
    private val imageResizer: AttachmentContract.ImageResizer
) {
    private val mdHandle = MessageDigest.getInstance("MD5")

    fun md5(str: String): String {
        mdHandle.update(str.toByteArray())
        return DatatypeConverter
            .printHexBinary(mdHandle.digest())
            .toUpperCase()
            .also { mdHandle.reset() }
    }

    fun encode(tag: String): String {
        return URLEncoder.encode(tag, StandardCharsets.UTF_8.displayName())
            .replace(".", "%2e")
            .replace("+", "%20")
            .replace("*", "%2a")
            .replace("-", "%2d")
            .replace("_", "%5f")
            .toLowerCase()
    }

    fun prepareTags(
        tags: Map<String, String>
    ): List<String> {
        val encodedTags = mutableListOf<String>()
        tags.forEach { (key, value) ->
            encodedTags.add("$key=${encode(value)}")
        }

        return encodedTags
    }

    private fun prepareLegacyTag(
        key: String,
        value: String
    ): String = "${key.toLowerCase()}=${value.toLowerCase()}"

    fun prepareCompatibilityTags(
        tags: Map<String, String>
    ): Pair<List<String>, List<String>> {
        val encodedTags = prepareTags(tags)
        val legacyTags = tags.map { (key, value) -> prepareLegacyTag(key, value) }

        return Pair(encodedTags, legacyTags)
    }

    private fun prepareLegacyAnnotations(
        annotations: List<String>
    ): List<String> = annotations.map { "custom=${it.toLowerCase()}" }

    fun prepareAnnotations(
        annotations: List<String>
    ): List<String> = annotations.map { "custom=${encode(it)}" }

    fun prepareCompatibilityAnnotations(
        annotations: List<String>
    ): Pair<List<String>, List<String>> {
        val encodedAnnotations = prepareAnnotations(annotations)
        val legacyAnnotations = prepareLegacyAnnotations(annotations)

        return Pair(encodedAnnotations, legacyAnnotations)
    }

    fun mergeTags(
        set1: List<String>,
        set2: List<String>
    ): List<String> = mutableListOf<String>().also {
        it.addAll(set1)
        it.addAll(set2)
    }

    fun hashAndEncodeTagsAndAnnotations(
        tagsAndAnnotations: List<String>
    ): List<String> = tagsAndAnnotations.map { Base64.encodeToString(md5(it)) }

    fun uploadAttachment(
        attachmentKey: GCKey,
        userId: String,
        payload: Pair<ByteArray, String>,
        resized: Pair<Pair<ByteArray, String>, Pair<ByteArray, String>?>? = null
    ) {
        every {
            fileService.uploadFile(attachmentKey, userId, payload.first)
        } returns Single.just(payload.second)

        resizing(payload.first, userId, attachmentKey, resized)
    }

    private fun resizing(
        data: ByteArray,
        userId: String,
        attachmentKey: GCKey,
        resizedImages: Pair<Pair<ByteArray, String>, Pair<ByteArray, String>?>?
    ) {
        if (resizedImages == null) {
            every { imageResizer.isResizable(data) } returns false
        } else {
            every { imageResizer.isResizable(data) } returns true

            resizeImage(
                data,
                resizedImages.first.first,
                resizedImages.first.second,
                DEFAULT_PREVIEW_SIZE_PX,
                userId,
                attachmentKey
            )

            if (resizedImages.second is Pair<*, *>) {
                resizeImage(
                    data,
                    resizedImages.second!!.first,
                    resizedImages.second!!.second,
                    DEFAULT_THUMBNAIL_SIZE_PX,
                    userId,
                    attachmentKey
                )
            } else {
                resizeImage(
                    data,
                    null,
                    null,
                    DEFAULT_THUMBNAIL_SIZE_PX,
                    userId,
                    attachmentKey
                )
            }
        }
    }

    private fun resizeImage(
        data: ByteArray,
        resizedImage: ByteArray?,
        imageId: String?,
        targetHeight: Int,
        userId: String,
        attachmentKey: GCKey
    ) {
        every {
            imageResizer.resizeToHeight(
                data,
                targetHeight,
                DEFAULT_JPEG_QUALITY_PERCENT
            )
        } returns resizedImage

        if (resizedImage is ByteArray) {
            every {
                fileService.uploadFile(attachmentKey, userId, resizedImage)
            } returns Single.just(imageId)
        }
    }

    fun prepareStoredOrUnstoredCommonKeyRun(
        alias: String,
        userId: String,
        commonKeyId: String,
        useStoredCommonKey: Boolean
    ): EncryptedKey? {
        return if (useStoredCommonKey) {
            null
        } else {
            runWithoutStoredCommonKey(alias, userId, commonKeyId)
        }
    }

    private fun runWithoutStoredCommonKey(
        alias: String,
        userId: String,
        commonKeyId: String
    ): EncryptedKey {
        val commonKeyResponse: CommonKeyResponse = mockk()
        val encryptedCommonKey: EncryptedKey = mockk()

        every {
            apiService.fetchCommonKey(alias, userId, commonKeyId)
        } returns Single.just(commonKeyResponse)
        every { commonKeyResponse.commonKey } returns encryptedCommonKey

        return encryptedCommonKey
    }

    private fun createEncryptedRecord(
        id: String?,
        commonKeyId: String,
        tags: List<String>,
        annotations: List<String>,
        body: String,
        dates: Pair<String?, String?>,
        keys: Pair<EncryptedKey, EncryptedKey?>
    ): EncryptedRecord = EncryptedRecord(
        commonKeyId,
        id,
        tags
            .toMutableList()
            .also { it.addAll(annotations) }
            .map { Base64.encodeToString(md5(it)) },
        body,
        dates.first,
        keys.first,
        keys.second,
        CURRENT,
        dates.second
    )

    fun buildEncryptedRecord(
        id: String?,
        commonKeyId: String,
        tags: List<String>,
        annotations: List<String>,
        body: String,
        dates: Pair<String?, String?>,
        keys: Pair<EncryptedKey, EncryptedKey?>
    ): EncryptedRecord = createEncryptedRecord(
        id,
        commonKeyId,
        tags,
        annotations,
        md5(body),
        dates,
        keys
    )

    fun buildEncryptedRecordWithEncodedBody(
        id: String?,
        commonKeyId: String,
        tags: List<String>,
        annotations: List<String>,
        body: String,
        dates: Pair<String?, String?>,
        keys: Pair<EncryptedKey, EncryptedKey?>
    ): EncryptedRecord = createEncryptedRecord(
        id,
        commonKeyId,
        tags,
        annotations,
        Base64.encodeToString(md5(body)),
        dates,
        keys
    )

    fun prepareEncryptedFhirRecord(
        recordId: String?,
        resource: String,
        tags: List<String>,
        annotations: List<String>,
        commonKeyId: String,
        encryptedDataKey: EncryptedKey,
        encryptedAttachmentsKey: EncryptedKey?,
        creationDate: String,
        updateDate: String?
    ): EncryptedRecord = buildEncryptedRecord(
        recordId,
        commonKeyId,
        tags,
        annotations,
        resource,
        Pair(creationDate, updateDate),
        Pair(encryptedDataKey, encryptedAttachmentsKey)
    )

    fun prepareEncryptedDataRecord(
        recordId: String?,
        resource: String,
        tags: List<String>,
        annotations: List<String>,
        commonKeyId: String,
        encryptedDataKey: EncryptedKey,
        encryptedAttachmentsKey: EncryptedKey?,
        creationDate: String,
        updateDate: String?
    ): EncryptedRecord = buildEncryptedRecordWithEncodedBody(
        recordId,
        commonKeyId,
        tags,
        annotations,
        resource,
        Pair(creationDate, updateDate),
        Pair(encryptedDataKey, encryptedAttachmentsKey)
    )

    fun buildMeta(
        customCreationDate: String,
        updatedDate: String
    ): Meta = Meta(
        LocalDate.parse(customCreationDate, DATE_FORMATTER),
        LocalDateTime.parse(updatedDate, DATE_TIME_FORMATTER)
    )
}
