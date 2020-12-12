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

import care.data4life.fhir.stu3.model.CarePlan
import care.data4life.fhir.stu3.model.DomainResource
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.SdkRecordFactory
import care.data4life.sdk.model.definitions.BaseRecord
import com.google.common.truth.Truth
import io.mockk.every
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.times
import java.io.IOException

class RecordServiceFetchRecordsTest : RecordServiceTestBase() {
    @Before
    fun setUp() {
        init()
    }

    @After
    fun tearDown() {
        stop()
    }

    @Test
    @Throws(
            InterruptedException::class,
            IOException::class,
            DataValidationException.ModelVersionNotSupported::class
    )
    fun `Given, fetchRecord is called with a RecordId and UserId, it returns a Record`() {
        // Given
        Mockito.`when`(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID))
                .thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockDecryptedFhirRecord)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        @Suppress("UNCHECKED_CAST")
        every { SdkRecordFactory.getInstance(mockDecryptedFhirRecord) } returns mockRecord as BaseRecord<DomainResource>

        // When
        val observer = recordService.fetchRecord<CarePlan>(RECORD_ID, USER_ID).test().await()

        // Then
        val record = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        Truth.assertThat(record).isSameInstanceAs(mockRecord)

        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).assignResourceId(mockDecryptedFhirRecord)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class)
    fun `Given, fetchRecords is called with multiple RecordIds and a UserId, it returns FetchedRecords`() {
        // Given
        Mockito.doReturn(Single.just(mockRecord))
                .`when`(recordService)
                .fetchRecord<DomainResource>(RECORD_ID, USER_ID)
        val ids = listOf(RECORD_ID, RECORD_ID)

        // When
        val observer = recordService.fetchRecords<CarePlan>(ids, USER_ID).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(result.successfulFetches).hasSize(2)
        Truth.assertThat(result.failedFetches).hasSize(0)
        inOrder.verify(recordService).fetchRecords<DomainResource>(ids, USER_ID)
        inOrder.verify(recordService, Mockito.times(2)).fetchRecord<DomainResource>(RECORD_ID, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class, IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, fetchRecords called with a UserId, a ResourceType, a StartDate, a EndDate, the PageSize and Offset, it returns FetchedRecords`() {
        // Given
        val encryptedRecords = listOf(mockEncryptedRecord, mockEncryptedRecord)
        Mockito.`when`(mockTaggingService.getTagFromType(CarePlan.resourceType)).thenReturn(mockTags)
        Mockito.`when`(mockTagEncryptionService.encryptTags(mockTags)).thenReturn(mockEncryptedTags)
        Mockito.`when`(
                mockTagEncryptionService.encryptAnnotations(listOf())
        ).thenReturn(mockEncryptedAnnotations)
        Mockito.`when`(
                mockApiService.fetchRecords(
                        ArgumentMatchers.eq(ALIAS),
                        ArgumentMatchers.eq(USER_ID),
                        ArgumentMatchers.isNull(),
                        ArgumentMatchers.isNull(),
                        ArgumentMatchers.eq(10),
                        ArgumentMatchers.eq(0),
                        ArgumentMatchers.eq(mockEncryptedTags)
                )
        ).thenReturn(Observable.just(encryptedRecords))
        Mockito.doReturn(mockDecryptedFhirRecord)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        @Suppress("UNCHECKED_CAST")
        every { SdkRecordFactory.getInstance(mockDecryptedFhirRecord) } returns mockRecord as BaseRecord<DomainResource>

        // When
        val observer = recordService.fetchRecords(
                USER_ID,
                CarePlan::class.java,
                null,
                null,
                10,
                0
        ).test().await()

        // Then
        val fetched = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(fetched).hasSize(2)
        Truth.assertThat(fetched[0].meta).isEqualTo(mockMeta)
        Truth.assertThat(fetched[0].fhirResource).isEqualTo(mockCarePlan)
        Truth.assertThat(fetched[1].meta).isEqualTo(mockMeta)
        Truth.assertThat(fetched[1].fhirResource).isEqualTo(mockCarePlan)
        inOrder.verify(mockTaggingService).getTagFromType(CarePlan.resourceType)
        inOrder.verify(mockTagEncryptionService).encryptTags(mockTags)
        inOrder.verify(mockTagEncryptionService).encryptAnnotations(listOf())
        inOrder.verify(mockApiService).fetchRecords(
                ArgumentMatchers.eq(ALIAS),
                ArgumentMatchers.eq(USER_ID),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq(10),
                ArgumentMatchers.eq(0),
                ArgumentMatchers.eq(mockEncryptedTags)
        )
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).assignResourceId(mockDecryptedFhirRecord)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).assignResourceId(mockDecryptedFhirRecord)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class, IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, fetchRecords called with a UserId, a ResourceType, Annotations, a StartDate, a EndDate, the PageSize and Offset, it returns FetchedRecords`() {
        // Given
        val encryptedRecords = listOf(mockEncryptedRecord, mockEncryptedRecord)
        Mockito.`when`(mockTaggingService.getTagFromType(CarePlan.resourceType)).thenReturn(mockTags)
        Mockito.`when`(mockTagEncryptionService.encryptTags(mockTags)).thenReturn(mockEncryptedTags)
        Mockito.`when`(
                mockTagEncryptionService.encryptAnnotations(ANNOTATIONS)
        ).thenReturn(mockEncryptedAnnotations)
        Mockito.`when`(
                mockApiService.fetchRecords(
                        ArgumentMatchers.eq(ALIAS),
                        ArgumentMatchers.eq(USER_ID),
                        ArgumentMatchers.isNull(),
                        ArgumentMatchers.isNull(),
                        ArgumentMatchers.eq(10),
                        ArgumentMatchers.eq(0),
                        ArgumentMatchers.eq(mockEncryptedTags)
                )
        ).thenReturn(Observable.just(encryptedRecords))
        Mockito.doReturn(mockAnnotatedDecryptedFhirRecord)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        @Suppress("UNCHECKED_CAST")
        every { SdkRecordFactory.getInstance(mockAnnotatedDecryptedFhirRecord) } returns mockRecord as BaseRecord<DomainResource>

        // When
        val observer = recordService.fetchRecords(
                USER_ID,
                CarePlan::class.java,
                ANNOTATIONS,
                null,
                null,
                10,
                0
        ).test().await()

        // Then
        val fetched = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(fetched).hasSize(2)
        Truth.assertThat(fetched[0].meta).isEqualTo(mockMeta)
        Truth.assertThat(fetched[0].fhirResource).isEqualTo(mockCarePlan)
        Truth.assertThat(fetched[1].meta).isEqualTo(mockMeta)
        Truth.assertThat(fetched[1].fhirResource).isEqualTo(mockCarePlan)
        inOrder.verify(mockTaggingService).getTagFromType(CarePlan.resourceType)
        inOrder.verify(mockTagEncryptionService).encryptTags(mockTags)
        inOrder.verify(mockTagEncryptionService).encryptAnnotations(ANNOTATIONS)
        inOrder.verify(mockApiService).fetchRecords(
                ArgumentMatchers.eq(ALIAS),
                ArgumentMatchers.eq(USER_ID),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq(10),
                ArgumentMatchers.eq(0),
                ArgumentMatchers.eq(mockEncryptedTags)
        )
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).assignResourceId(mockAnnotatedDecryptedFhirRecord)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).assignResourceId(mockAnnotatedDecryptedFhirRecord)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(
            InterruptedException::class,
            IOException::class,
            DataValidationException.ModelVersionNotSupported::class
    )
    fun `Given, fetchAppDataRecord is called with a RecordId and UserId, it returns a AppDataRecord`() {
        // Given
        Mockito.`when`(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)).thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockDecryptedDataRecord).`when`(recordService).decryptRecord<ByteArray>(mockEncryptedRecord, USER_ID)
        @Suppress("UNCHECKED_CAST")
        every { SdkRecordFactory.getInstance(mockDecryptedDataRecord) } returns mockDataRecord

        // When
        val observer = recordService.fetchAppDataRecord(RECORD_ID, USER_ID).test().await()

        // Then
        val record = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(record).isSameInstanceAs(mockDataRecord)

        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID)
        inOrder.verify(recordService).decryptRecord<ByteArray>(mockEncryptedRecord, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class, IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, fetchAppDataRecords is called with a UserId, Annotations, a StartDate, a EndDate, the PageSize and Offset, it returns FetchedRecords`() {
        // Given
        val encryptedRecords = listOf(mockEncryptedRecord, mockEncryptedRecord)
        Mockito
                .`when`(mockTaggingService.appendAppDataTags(ArgumentMatchers.eq(hashMapOf())))
                .thenReturn(mockTags)
        Mockito.`when`(mockTagEncryptionService.encryptTags(mockTags)).thenReturn(mockEncryptedTags)
        Mockito.`when`(
                mockTagEncryptionService.encryptAnnotations(ANNOTATIONS)
        ).thenReturn(mockEncryptedAnnotations)
        Mockito.`when`(
                mockApiService.fetchRecords(
                        ArgumentMatchers.eq(ALIAS),
                        ArgumentMatchers.eq(USER_ID),
                        ArgumentMatchers.isNull(),
                        ArgumentMatchers.isNull(),
                        ArgumentMatchers.eq(10),
                        ArgumentMatchers.eq(0),
                        ArgumentMatchers.eq(mockEncryptedTags)
                )
        ).thenReturn(Observable.just(encryptedRecords))
        Mockito.doReturn(mockDecryptedDataRecord)
                .`when`(recordService)
                .decryptRecord<ByteArray>(mockEncryptedRecord, USER_ID)
        @Suppress("UNCHECKED_CAST")
        every { SdkRecordFactory.getInstance(mockDecryptedDataRecord) } returns mockDataRecord

        // When
        val observer = recordService.fetchRecords(
                USER_ID,
                ANNOTATIONS,
                null,
                null,
                10,
                0
        ).test().await()

        // Then
        val fetched = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(fetched).hasSize(2)
        Truth.assertThat(fetched[0]).isSameInstanceAs(mockDataRecord)
        Truth.assertThat(fetched[1]).isSameInstanceAs(mockDataRecord)

        inOrder.verify(mockTaggingService).appendAppDataTags(ArgumentMatchers.eq(hashMapOf()))
        inOrder.verify(mockTagEncryptionService).encryptTags(mockTags)
        inOrder.verify(mockTagEncryptionService).encryptAnnotations(ANNOTATIONS)
        inOrder.verify(mockApiService).fetchRecords(
                ArgumentMatchers.eq(ALIAS),
                ArgumentMatchers.eq(USER_ID),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq(10),
                ArgumentMatchers.eq(0),
                ArgumentMatchers.eq(mockEncryptedTags)
        )
        inOrder.verify(recordService, times(2)).decryptRecord<ByteArray>(mockEncryptedRecord, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }
}
