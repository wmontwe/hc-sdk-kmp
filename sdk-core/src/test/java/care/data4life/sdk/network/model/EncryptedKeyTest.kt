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

package care.data4life.sdk.network.model

import care.data4life.sdk.util.Base64
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class EncryptedKeyTest {
    @Test
    fun `It fulfils EncryptedKeyMaker`() {
        val factory: Any = EncryptedKey
        assertTrue(factory is NetworkModelInternalContract.EncryptedKeyMaker)
    }

    @Test
    fun `Given create is called, it creates a new EncryptedKey`() {
        val key: Any = EncryptedKey.create("test".toByteArray())
        assertTrue(key is NetworkModelContract.EncryptedKey)
    }

    @Test
    fun `Given create is called, it encodes the given Key`() {
        // Given
        val expected = "potato"
        val givenValue = "test"

        mockkObject(Base64)
        every { Base64.encodeToString(givenValue.toByteArray()) } returns expected

        // When
        val key = EncryptedKey.create("test".toByteArray())

        // Then
        assertEquals(
            expected,
            key.base64Key
        )

        verify(exactly = 1) { Base64.encodeToString(givenValue.toByteArray()) }
        unmockkObject(Base64)
    }

    @Test
    fun `Given decode is called, it decodes the given Key`() {
        // Given
        val expected = "test".toByteArray()
        val storedValue = "potato"

        mockkObject(Base64)
        every { Base64.decode(storedValue) } returns expected

        // When
        val key = EncryptedKey(storedValue)

        // Then
        assertEquals(
            expected,
            key.decode()
        )
        unmockkObject(Base64)
    }
}
