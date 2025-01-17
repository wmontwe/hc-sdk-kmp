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

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

private fun Int.toHex(): String = Integer.toHexString(this).toUpperCase(Locale.US)

object UrlEncoding : WrapperContract.UrlEncoding {
    private val specialChars = listOf('*', '-', '_', '.')

    private fun replaceSpecial(char: Char): String {
        return when (char) {
            '+' -> "%20"
            in specialChars -> "%${char.toInt().toHex()}"
            else -> char.toString()
        }
    }

    override fun encode(str: String): String {
        return URLEncoder.encode(str, StandardCharsets.UTF_8.displayName())
            .map(::replaceSpecial)
            .joinToString("")
    }

    override fun decode(
        str: String
    ): String = URLDecoder.decode(str, StandardCharsets.UTF_8.displayName())
}
