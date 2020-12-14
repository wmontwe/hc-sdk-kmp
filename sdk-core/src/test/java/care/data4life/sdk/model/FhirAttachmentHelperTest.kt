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

package care.data4life.sdk.model

import care.data4life.fhir.stu3.model.Attachment
import care.data4life.fhir.stu3.model.DocumentReference
import care.data4life.fhir.stu3.model.DomainResource
import care.data4life.fhir.stu3.model.Identifier
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.fhir.stu3.util.FhirAttachmentHelper as Fhir3AttachmentHelper
import care.data4life.sdk.model.definitions.FhirAttachmentHelper
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class FhirAttachmentHelperTest {
    @Before
    fun setUp() {
        mockkStatic(Fhir3AttachmentHelper::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `it is a FhirAttachmentHelper`() {
        assertTrue((SdkFhirAttachmentHelper as Any) is FhirAttachmentHelper)
    }

    @Test
    fun `Given, hasAttachment is called with non FhirResource, it returns false`() {
        assertFalse(SdkFhirAttachmentHelper.hasAttachment("something"))
    }

    @Test
    fun `Given, hasAttachment is called with a FhirResource, it delegates to and returns of the wrapped Fhir3AttachmentHelper`() {
        // Given
        val resource1 = Mockito.mock(DocumentReference::class.java)
        val resource2 = Mockito.mock(DocumentReference::class.java)

        every { Fhir3AttachmentHelper.hasAttachment(resource1) } returns false
        every { Fhir3AttachmentHelper.hasAttachment(resource2) } returns true

        // Then
        assertFalse(SdkFhirAttachmentHelper.hasAttachment(resource1))
        assertTrue(SdkFhirAttachmentHelper.hasAttachment(resource2))

        verify(exactly = 1) { Fhir3AttachmentHelper.hasAttachment(resource1) }
        verify(exactly = 1) { Fhir3AttachmentHelper.hasAttachment(resource2) }
    }

    @Test
    fun `Given, getAttachment is called with a non FhirResource, it fails with a CoreRuntimeExceptionInternalFailure`() {
        try {
            // When
            SdkFhirAttachmentHelper.getAttachment("something")
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, getAttachment is called with FhirResource, it delegates to and returns of the wrapped Fhir3AttachmentHelper`() {
        // Given
        val resource = Mockito.mock(DocumentReference::class.java)
        @Suppress("UNCHECKED_CAST")
        val attachments = Mockito.mock(MutableList::class.java) as MutableList<Attachment>

        every { Fhir3AttachmentHelper.getAttachment(resource) } returns attachments

        // Then
        assertSame(
                attachments,
                SdkFhirAttachmentHelper.getAttachment(resource)
        )


        verify(exactly = 1) { Fhir3AttachmentHelper.getAttachment(resource) }
    }

    @Test
    fun `Given, getAttachment is called with FhirResource and the response of the wrapped Fhir3AttachmentHelper is null, it returns a empty List`() {
        // Given
        val resource = Mockito.mock(DocumentReference::class.java)

        every { Fhir3AttachmentHelper.getAttachment(resource) } returns null

        // When
        val result = SdkFhirAttachmentHelper.getAttachment(resource)

        // Then
        assertTrue(result.isEmpty())

        verify(exactly = 1) { Fhir3AttachmentHelper.getAttachment(resource) }
    }

    @Test
    fun `Given, updateAttachmentData is called with a non FhirResource and a HashMap Attachment to String, it fails with a CoreRuntimeExceptionInternalFailure`() {
        // Given
        @Suppress("UNCHECKED_CAST")
        val attachments = hashMapOf(
                Mockito.mock(Attachment::class.java) to "1",
                Mockito.mock(Attachment::class.java) to "2"
        ) as HashMap<Any, String?>

        try {
            // When
            SdkFhirAttachmentHelper.updateAttachmentData("something", attachments)
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, updateAttachmentData is called with a FhirResource and a HashMap, which contains non Attachment to String, it fails with a CoreRuntimeExceptionInternalFailure`() {
        // Given
        val resource = Mockito.mock(DocumentReference::class.java)

        @Suppress("UNCHECKED_CAST")
        val attachments = hashMapOf(
                Mockito.mock(Attachment::class.java) to "1",
                "b" to "2"
        ) as HashMap<Any, String?>

        try {
            // When
            SdkFhirAttachmentHelper.updateAttachmentData(resource, attachments)
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, updateAttachmentData is called with a FhirResource and null as attachment, it does nothing`() {
        // Given
        val resource = Mockito.mock(DocumentReference::class.java)

        every { Fhir3AttachmentHelper.updateAttachmentData(any(), any()) } returns Unit

        // When
        SdkFhirAttachmentHelper.updateAttachmentData(resource, null)

        // Then
        verify(exactly = 0) { Fhir3AttachmentHelper.updateAttachmentData(any(), any()) }
    }

    @Test
    fun `Given, updateAttachmentData is called with a FhirResource and a empty HashMap as attachement, it does nothing`() {
        // Given
        val resource = Mockito.mock(DocumentReference::class.java)

        every { Fhir3AttachmentHelper.updateAttachmentData(any(), any()) } returns Unit

        // When
        SdkFhirAttachmentHelper.updateAttachmentData(resource, hashMapOf())

        // Then
        verify(exactly = 0) { Fhir3AttachmentHelper.updateAttachmentData(any(), any()) }
    }

    @Test
    fun `Given, updateAttachmentData is called with a FhirResource and a HashMap Attachment to String, it delegates to and returns of the wrapped Fhir3AttachmentHelper`() {
        // Given
        val resource = Mockito.mock(DocumentReference::class.java)

        @Suppress("UNCHECKED_CAST")
        val attachments = hashMapOf(
                Mockito.mock(Attachment::class.java) to "1",
                Mockito.mock(Attachment::class.java) to "2"
        ) as HashMap<Any, String?>

        every {
            @Suppress("UNCHECKED_CAST")
            Fhir3AttachmentHelper.updateAttachmentData(resource, attachments as HashMap<Attachment, String>)
        } returns Unit

        // When
        SdkFhirAttachmentHelper.updateAttachmentData(resource, attachments)

        // Then
        verify(exactly = 1) {
            @Suppress("UNCHECKED_CAST")
            Fhir3AttachmentHelper.updateAttachmentData(resource, attachments as HashMap<Attachment, String>)
        }
    }

    @Test
    fun `Given, getIdentifiers is called with a non FhirResource, it fails with a CoreRuntimeExceptionInternalFailure`() {
        // Given
        try {
            // When
            SdkFhirAttachmentHelper.getIdentifier("something")
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, getIdentifiers is called with a FhirResource, it delegates to and returns of the wrapped Fhir3AttachmentHelper`() {
        // Given
        val resource = Mockito.mock(DomainResource::class.java)
        @Suppress("UNCHECKED_CAST")
        val identifiers = Mockito.mock(MutableList::class.java) as MutableList<Identifier>

        every { Fhir3AttachmentHelper.getIdentifier(resource) } returns identifiers

        // Then
        assertSame(
                identifiers,
                SdkFhirAttachmentHelper.getIdentifier(resource)
        )

        verify(exactly = 1) { Fhir3AttachmentHelper.getIdentifier(resource) }
    }

    @Test
    fun `Given, getIdentifiers is called with a FhirResource and the response of the wrapped Fhir3AttachmentHelper is null, it returns a empty List`() {
        // Given
        val resource = Mockito.mock(DomainResource::class.java)

        every { Fhir3AttachmentHelper.getIdentifier(resource) } returns null

        // When
        val result = SdkFhirAttachmentHelper.getIdentifier(resource)

        // Then
        assertTrue(result.isEmpty())

        verify(exactly = 1) { Fhir3AttachmentHelper.getIdentifier(resource) }
    }

    @Test
    fun `Given, setIdentifier is called with a non FhirResource and a List of Identifiers, it fails with a CoreRuntimeExceptionInternalFailure`() {
        // Given
        val identifier1 = Mockito.mock(Identifier::class.java)
        val identifier2 = Mockito.mock(Identifier::class.java)

        try {
            // When
            SdkFhirAttachmentHelper.setIdentifier("something", listOf(identifier1, identifier2))
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setIdentifier is called with a FhirResource and a List, which contains Identifiers, it fails with a CoreRuntimeExceptionInternalFailure`() {
        // Given
        val resource = Mockito.mock(DomainResource::class.java)
        val identifier = Mockito.mock(Identifier::class.java)

        try {
            // When
            SdkFhirAttachmentHelper.setIdentifier(resource, listOf(identifier, "something"))
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setIdentifier is called with a FhirResource and a empty List, it does nothing`() {
        // Given
        val resource = Mockito.mock(DomainResource::class.java)
        every { Fhir3AttachmentHelper.setIdentifier(any(), any()) } returns Unit

        // When
        SdkFhirAttachmentHelper.setIdentifier(resource, listOf())

        // Then
        verify(exactly = 0) { Fhir3AttachmentHelper.setIdentifier(any(), any()) }
    }


    @Test
    fun `Given, setIdentifier is called with a FhirResource and a List of Identifiers, it delegates to and returns of the wrapped Fhir3AttachmentHelper`() {
        // Given
        val resource = Mockito.mock(DomainResource::class.java)
        val identifiers = listOf(
                Mockito.mock(Identifier::class.java),
                Mockito.mock(Identifier::class.java)
        )

        every { Fhir3AttachmentHelper.setIdentifier(resource, identifiers) } returns Unit


        // When
        SdkFhirAttachmentHelper.setIdentifier(resource, identifiers)

        // Then
        verify(exactly = 1) { Fhir3AttachmentHelper.setIdentifier(resource, identifiers) }
    }

    @Test
    fun `Given, appendIdentifier is called with a non FhirResource, a Identifier and a Assigner, it fails with a CoreRuntimeExceptionInternalFailure`() {
        try {
            // When
            SdkFhirAttachmentHelper.appendIdentifier("somthing", "id", "anything")
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, appendIdentifier is called with a FhirResource, a Identifier and a Assigner, it delegates to and returns of the wrapped Fhir3AttachmentHelper`() {
        // Given
        val resource = Mockito.mock(DomainResource::class.java)
        val identifier = "id"
        val assigner = "me"

        every { Fhir3AttachmentHelper.appendIdentifier(resource, identifier, assigner) } returns Unit

        // When
        SdkFhirAttachmentHelper.appendIdentifier(resource, identifier, assigner)

        // Then
        verify(exactly = 1) { Fhir3AttachmentHelper.appendIdentifier(resource, identifier, assigner) }
    }
}
