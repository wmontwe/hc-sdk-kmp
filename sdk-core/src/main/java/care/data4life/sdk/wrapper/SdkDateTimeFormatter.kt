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

package care.data4life.sdk.wrapper

import care.data4life.sdk.RecordService
import care.data4life.sdk.model.Meta
import care.data4life.sdk.model.ModelContract
import care.data4life.sdk.network.model.definitions.DecryptedBaseRecord
import care.data4life.sdk.wrapper.WrapperContract.DateTimeFormatter.Companion.DATE_FORMAT
import care.data4life.sdk.wrapper.WrapperContract.DateTimeFormatter.Companion.DATE_TIME_FORMAT
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeFormatterBuilder
import java.util.*

internal object SdkDateTimeFormatter : WrapperContract.DateTimeFormatter {
    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)
    val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
        .parseLenient()
        .appendPattern(DATE_TIME_FORMAT)
        .toFormatter(Locale.US)
    val UTC_ZONE_ID = ZoneId.of("UTC")

    override fun now(): String = formatDate(LocalDate.now(UTC_ZONE_ID))

    override fun formatDate(dateTime: LocalDate): String = DATE_FORMATTER.format(dateTime)

    override fun buildMeta(
        record: DecryptedBaseRecord<*>
    ): ModelContract.Meta = Meta(
        LocalDate.parse(record.customCreationDate, DATE_FORMATTER),
        LocalDateTime.parse(record.updatedDate, DATE_TIME_FORMATTER)
    )
}