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

package care.data4life.sdk.wrapper

import care.data4life.sdk.fhir.Fhir4Identifier
import care.data4life.fhir.stu3.model.Identifier as Fhir3Identifier
import care.data4life.sdk.lang.CoreRuntimeException
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

class IdentifierFactoryTest {
    @Test
    fun `it is a AttachmentFactory`() {
        assertTrue((IdentifierFactory as Any) is WrapperFactoryContract.IdentifierFactory)
    }
    
    @Test
    fun `Given, wrap is called with a non Fhir3Identifier, it fails with a CoreRuntimeExceptionInternalFailure`() {
        try {
            // When
            IdentifierFactory.wrap("fail me!")
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }



    @Test
    fun `Given, wrap is called with a null, it return null`() {
        assertNull(IdentifierFactory.wrap(null))
    }

    @Test
    fun `Given, wrap is called with a Fhir3Identifier, it returns a Attachment`() {
        // Given
        val givenIdentifier = Mockito.mock(Fhir3Identifier::class.java)

        // When
        val wrapped: Any = IdentifierFactory.wrap(givenIdentifier)!!

        // Then
        assertTrue(wrapped is WrapperContract.Identifier)
    }

    @Test
    fun `Given, wrap is called with a Fhir4Identifier, it returns a Attachment`() {
        // Given
        val givenIdentifier = Mockito.mock(Fhir4Identifier::class.java)

        // When
        val wrapped: Any = IdentifierFactory.wrap(givenIdentifier)!!

        // Then
        assertTrue(wrapped is WrapperContract.Identifier)
    }
}
