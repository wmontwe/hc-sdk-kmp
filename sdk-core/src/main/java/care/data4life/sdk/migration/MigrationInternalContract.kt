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

package care.data4life.sdk.migration

internal class MigrationInternalContract {
    interface CompatibilityEncoder {
        fun encode(tagValue: String): QueryCompatibilityTag
        fun normalize(tagValue: String): String

        companion object {
            val JS_LEGACY_ENCODING_REPLACEMENTS = mapOf(
                "%2A" to "%2a",
                "%2D" to "%2d",
                "%2E" to "%2e",
                "%5F" to "%5f",
                "%7E" to "%7e"
            )
        }
    }

    data class QueryCompatibilityTag(
        val validEncoding: String,
        val kmpLegacyEncoding: String,
        val jsLegacyEncoding: String,
        val iosLegacyEncoding: String
    )
}
