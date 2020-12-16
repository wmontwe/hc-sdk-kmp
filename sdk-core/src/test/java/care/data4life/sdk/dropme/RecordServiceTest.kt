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
package care.data4life.sdk.dropme

/*
class RecordServiceTest : RecordServiceTestBase() {

    @Before
    fun setup() {
        init()
    }

    @After
    fun tearDown() {
        stop()
    }

    //region utility methods
    @Test
    fun `Given, extractUploadData is called with a non FhirResource, it returns null`() {
        // When
        val data = recordService.extractUploadData("something")
        // Then
        Truth.assertThat(data).isNull()
    }


    @Test
    fun extractUploadData_shouldReturnExtractedData() {
        // Given
        val document = buildDocumentReference()

        // When
        val data = recordService.extractUploadData(document)

        // Then
        Truth.assertThat(data).hasSize(1)
        Truth.assertThat(data!![document.content[0].attachment]).isEqualTo(DATA)
        inOrder.verify(recordService).extractUploadData(document)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnNull_whenInadequateResourceProvided() {
        // Given
        val organization = Organization()

        // When
        val data = recordService.extractUploadData(organization)

        // Then
        Truth.assertThat(data).isNull()
        inOrder.verify(recordService).extractUploadData(organization)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnNull_whenContentIsNull() {
        // Given
        val content: List<DocumentReferenceContent>? = null
        val document = DocumentReference(
                null,
                null,
                null,
                content
        )

        // When
        val data = recordService.extractUploadData(document)

        // Then
        Truth.assertThat(data).isNull()
        inOrder.verify(recordService).extractUploadData(document)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnNull_whenAttachmentIsNull() {
        // Given
        val attachment: Attachment? = null
        val content = DocumentReferenceContent(attachment)
        val document = DocumentReference(
                null,
                null,
                null,
                listOf(content)
        )

        // When
        val data = recordService.extractUploadData(document)

        // Then
        Truth.assertThat(data).isNull()
        inOrder.verify(recordService).extractUploadData(document)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnNull_whenAttachmentDataIsNull() {
        // Given
        val document = buildDocumentReference()
        document.content[0].attachment.data = null

        // When
        val data = recordService.extractUploadData(document)

        // Then
        Truth.assertThat(data).isNull()
        inOrder.verify(recordService).extractUploadData(document)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun `Given, removeOrRestoreUploadData is called with REMOVE, a DecryptedFhir3Record, a Resource and Attachment, it delegates it to removeUploadData`() {
        // Given
        val document = buildDocumentReference()

        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedFhir3Record::class.java) as DecryptedFhir3Record<DomainResource>

        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .removeUploadData(decryptedRecord)

        // When
        val record = recordService.removeOrRestoreUploadData(
                RecordService.RemoveRestoreOperation.REMOVE,
                decryptedRecord,
                document,
                mockUploadData
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(mockDecryptedFhir3Record)

        inOrder.verify(recordService).removeUploadData(decryptedRecord)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Ignore
    fun `Given, removeUploadData is called with a non DecryptedFhir3Record, it reflects the given record`() {
        // Given
        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedDataRecord::class.java) as DecryptedBaseRecord<Any>
        val attachments = mutableListOf(
                Mockito.mock(Attachment::class.java),
                Mockito.mock(Attachment::class.java)
        )

        Mockito.`when`(decryptedRecord.resource).thenReturn(mockDataResource)

        every { FhirAttachmentHelper.getAttachment(mockCarePlan) } returns attachments
        every { FhirAttachmentHelper.updateAttachmentData(mockCarePlan, null) } returns Unit

        // When
        val record = recordService.removeUploadData(decryptedRecord)

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 0) { FhirAttachmentHelper.getAttachment(mockCarePlan) }
        verify(exactly = 0) { FhirAttachmentHelper.updateAttachmentData(mockCarePlan, null) }
    }

    @Test
    fun `Given, removeUploadData is called with a DecryptedFhir3Record, it removes the existing Attachments`() {
        // Given
        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedFhir3Record::class.java) as DecryptedFhir3Record<DomainResource>
        val attachments = mutableListOf(
                Mockito.mock(Attachment::class.java),
                Mockito.mock(Attachment::class.java)
        )

        Mockito.`when`(decryptedRecord.resource).thenReturn(mockCarePlan)

        every { FhirAttachmentHelper.getAttachment(mockCarePlan) } returns attachments
        every { FhirAttachmentHelper.updateAttachmentData(mockCarePlan, null) } returns Unit

        // When
        val record = recordService.removeUploadData(decryptedRecord)

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 1) { FhirAttachmentHelper.getAttachment(mockCarePlan) }
        verify(exactly = 1) { FhirAttachmentHelper.updateAttachmentData(mockCarePlan, null) }
    }

    @Test
    fun `Given, removeUploadData is called with a DecryptedFhir3Record, it does nothing, if no Attachments exists`() {
        // Given
        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedFhir3Record::class.java) as DecryptedFhir3Record<DomainResource>

        Mockito.`when`(decryptedRecord.resource).thenReturn(mockCarePlan)

        every { FhirAttachmentHelper.getAttachment(mockCarePlan) } returns null
        every { FhirAttachmentHelper.updateAttachmentData(mockCarePlan, null) } returns Unit

        // When
        val record = recordService.removeUploadData(decryptedRecord)

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 1) { FhirAttachmentHelper.getAttachment(mockCarePlan) }
        verify(exactly = 0) { FhirAttachmentHelper.updateAttachmentData(mockCarePlan, null) }
    }

    @Test
    fun `Given, removeOrRestoreUploadData is called with RESTORE, a DecryptedFhir3Record, a Resource and Attachment, it delegates it to restoreUploadData`() {
        // Given
        val document = buildDocumentReference()

        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedFhir3Record::class.java) as DecryptedFhir3Record<DomainResource>

        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .restoreUploadData(
                        decryptedRecord,
                        document,
                        mockUploadData
                )

        // When
        val record = recordService.removeOrRestoreUploadData(
                RecordService.RemoveRestoreOperation.RESTORE,
                decryptedRecord,
                document,
                mockUploadData
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(mockDecryptedFhir3Record)

        inOrder.verify(recordService).restoreUploadData(
                decryptedRecord,
                document,
                mockUploadData
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun `Given, restoreUploadData is called with a non DecryptedFhir3Record, a Resource and Attachment, it reflects the given Record`() {
        val document = buildDocumentReference()
        val decryptedRecord = mockkClass(DecryptedDataRecord::class)
        val attachments = mutableListOf(
                Mockito.mock(Attachment::class.java),
                Mockito.mock(Attachment::class.java)
        )

        every { decryptedRecord.resource } returns mockDataResource.value
        every { FhirAttachmentHelper.getAttachment(mockCarePlan) } returns attachments
        every { FhirAttachmentHelper.updateAttachmentData(mockCarePlan, any()) } returns Unit

        // When
        @Suppress("UNCHECKED_CAST")
        val record = recordService.restoreUploadData(
                decryptedRecord as DecryptedBaseRecord<Any>,
                document,
                mockUploadData
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 0) { decryptedRecord.resource = any() }
    }

    @Test
    fun `Given, restoreUploadData is called with a DecryptedFhir3Record, a non FhirResource and Attachment, it reflects the given Record`() {
        val document = "something"
        val decryptedRecord = mockkClass(DecryptedFhir3Record::class)
        val attachments = mutableListOf(
                Mockito.mock(Attachment::class.java),
                Mockito.mock(Attachment::class.java)
        )

        every { decryptedRecord.resource } returns mockCarePlan
        every { FhirAttachmentHelper.getAttachment(mockCarePlan) } returns attachments
        every { FhirAttachmentHelper.updateAttachmentData(mockCarePlan, any()) } returns Unit

        // When
        @Suppress("UNCHECKED_CAST")
        val record = recordService.restoreUploadData(
                decryptedRecord as DecryptedBaseRecord<Any>,
                document,
                mockUploadData
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)
    }

    @Test
    fun `Given, restoreUploadData is called with a DecryptedFhir3Record, a Resource and Attachment, it sets the given Resource to the DecryptedFhir3Record`() {
        val document = buildDocumentReference()

        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedFhir3Record::class.java) as DecryptedFhir3Record<DomainResource>
        val attachments = mutableListOf(
                Mockito.mock(Attachment::class.java),
                Mockito.mock(Attachment::class.java)
        )

        Mockito.`when`(decryptedRecord.resource).thenReturn(mockCarePlan)

        every { FhirAttachmentHelper.getAttachment(mockCarePlan) } returns attachments
        every { FhirAttachmentHelper.updateAttachmentData(mockCarePlan, any()) } returns Unit

        // When
        val record = recordService.restoreUploadData(
                decryptedRecord,
                document,
                mockUploadData
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        Mockito.verify(decryptedRecord, times(1)).resource = document
    }

    @Test
    fun `Given, restoreUploadData is called with a DecryptedFhir3Record, null as a Resource and Attachment, it does not set a new Resource for the DecryptedFhir3Record`() {
        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = mockkClass(DecryptedFhir3Record::class) as DecryptedFhir3Record<DomainResource>
        val attachments = mutableListOf(
                Mockito.mock(Attachment::class.java),
                Mockito.mock(Attachment::class.java)
        )

        every { decryptedRecord.resource } returns mockCarePlan

        every { FhirAttachmentHelper.getAttachment(mockCarePlan) } returns attachments
        every { FhirAttachmentHelper.updateAttachmentData(mockCarePlan, any()) } returns Unit

        // When
        val record = recordService.restoreUploadData(
                decryptedRecord,
                null,
                mockUploadData
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 0) { decryptedRecord.resource = any() }
    }

    @Test
    fun `Given, restoreUploadData is called with a DecryptedFhir3Record, a Resource and Attachment, it removes the existing Attachments`() {
        val document = buildDocumentReference()

        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedFhir3Record::class.java) as DecryptedFhir3Record<DomainResource>
        val attachments = mutableListOf(
                Mockito.mock(Attachment::class.java),
                Mockito.mock(Attachment::class.java)
        )

        Mockito.`when`(decryptedRecord.resource).thenReturn(mockCarePlan)

        every { FhirAttachmentHelper.getAttachment(mockCarePlan) } returns attachments
        every { FhirAttachmentHelper.updateAttachmentData(mockCarePlan, mockUploadData) } returns Unit

        // When
        val record = recordService.restoreUploadData(
                decryptedRecord,
                document,
                mockUploadData
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 1) { FhirAttachmentHelper.getAttachment(mockCarePlan) }
        verify(exactly = 1) { FhirAttachmentHelper.updateAttachmentData(mockCarePlan, mockUploadData) }
    }

    @Test
    fun `Given, restoreUploadData is called with a DecryptedFhir3Record, a Resource and Attachment, it does nothing, if no Attachments exists`() {
        val document = buildDocumentReference()

        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedFhir3Record::class.java) as DecryptedFhir3Record<DomainResource>

        Mockito.`when`(decryptedRecord.resource).thenReturn(mockCarePlan)

        every { FhirAttachmentHelper.getAttachment(mockCarePlan) } returns null
        every { FhirAttachmentHelper.updateAttachmentData(mockCarePlan, any()) } returns Unit

        // When
        val record = recordService.restoreUploadData(
                decryptedRecord,
                document,
                mockUploadData
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 1) { FhirAttachmentHelper.getAttachment(mockCarePlan) }
        verify(exactly = 0) { FhirAttachmentHelper.updateAttachmentData(mockCarePlan, mockUploadData) }
    }

    @Test
    @Ignore
    fun `Given, restoreUploadData is called with a DecryptedFhir3Record, a Resource and null as Attachment, it returns the DecryptedFhir3Record without invoking more actions`() {
        val document = buildDocumentReference()

        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedFhir3Record::class.java) as DecryptedFhir3Record<DomainResource>
        val attachments = mutableListOf(
                Mockito.mock(Attachment::class.java),
                Mockito.mock(Attachment::class.java)
        )

        Mockito.`when`(decryptedRecord.resource).thenReturn(mockCarePlan)

        every { FhirAttachmentHelper.getAttachment(mockCarePlan) } returns attachments
        every { FhirAttachmentHelper.updateAttachmentData(mockCarePlan, mockUploadData) } returns Unit

        // When
        val record = recordService.restoreUploadData(
                decryptedRecord,
                document,
                null
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 0) { FhirAttachmentHelper.getAttachment(mockCarePlan) }
        verify(exactly = 0) { FhirAttachmentHelper.updateAttachmentData(mockCarePlan, mockUploadData) }
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun cleanObsoleteAdditionalIdentifiers_shouldCleanObsoleteIdentifiers() {
        //given
        val currentId = ADDITIONAL_ID
        val obsoleteId = ADDITIONAL_ID.replaceFirst(ATTACHMENT_ID.toRegex(), "obsoleteId")
        val otherId = "otherId"
        val currentIdentifier = FhirAttachmentHelper.buildIdentifier(currentId, ASSIGNER)
        val obsoleteIdentifier = FhirAttachmentHelper.buildIdentifier(obsoleteId, ASSIGNER)
        val otherIdentifier = FhirAttachmentHelper.buildIdentifier(otherId, ASSIGNER)
        val identifiers: MutableList<Identifier> = arrayListOf()
        identifiers.add(currentIdentifier)
        identifiers.add(obsoleteIdentifier)
        identifiers.add(otherIdentifier)
        val doc = buildDocumentReference()
        doc.content[0].attachment.id = ATTACHMENT_ID
        doc.identifier = identifiers

        //when
        recordService.cleanObsoleteAdditionalIdentifiers(doc)

        //then
        Truth.assertThat(doc.identifier).hasSize(2)
        Truth.assertThat(doc.identifier!![0]).isEqualTo(currentIdentifier)
        Truth.assertThat(doc.identifier!![1]).isEqualTo(otherIdentifier)
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun setAttachmentIdForDownloadType_shouldSetAttachmentId() {
        //given
        val attachment = AttachmentBuilder.buildAttachment(ATTACHMENT_ID)
        val additionalId = FhirAttachmentHelper.buildIdentifier(ADDITIONAL_ID, ASSIGNER)
        val attachments = listOf(attachment)
        val identifiers = listOf(additionalId)

        //when downloadType is Full
        recordService.setAttachmentIdForDownloadType(attachments, identifiers, DownloadType.Full)
        //then
        Truth.assertThat(attachment.id).isEqualTo(ATTACHMENT_ID)

        //given
        attachment.id = ATTACHMENT_ID
        //when downloadType is Medium
        recordService.setAttachmentIdForDownloadType(attachments, identifiers, DownloadType.Medium)
        //then
        Truth.assertThat(attachment.id).isEqualTo(ATTACHMENT_ID + SPLIT_CHAR + PREVIEW_ID)

        //given
        attachment.id = ATTACHMENT_ID
        //when downloadType is Small
        recordService.setAttachmentIdForDownloadType(attachments, identifiers, DownloadType.Small)
        //then
        Truth.assertThat(attachment.id).isEqualTo(ATTACHMENT_ID + SPLIT_CHAR + THUMBNAIL_ID)
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun extractAdditionalAttachmentIds_shouldExtractAdditionalIds() {
        //given
        val additionalIdentifier = FhirAttachmentHelper.buildIdentifier(ADDITIONAL_ID, ASSIGNER)

        //when
        val additionalIds = recordService.extractAdditionalAttachmentIds(listOf(additionalIdentifier), ATTACHMENT_ID)

        //then
        val d4lNamespacePos = 0
        Truth.assertThat(additionalIds).hasLength(RecordService.DOWNSCALED_ATTACHMENT_IDS_SIZE)
        Truth.assertThat(additionalIds!![d4lNamespacePos]).isEqualTo(RecordService.DOWNSCALED_ATTACHMENT_IDS_FMT)
        Truth.assertThat(additionalIds[RecordService.FULL_ATTACHMENT_ID_POS]).isEqualTo(ATTACHMENT_ID)
        Truth.assertThat(additionalIds[RecordService.PREVIEW_ID_POS]).isEqualTo(PREVIEW_ID)
        Truth.assertThat(additionalIds[RecordService.THUMBNAIL_ID_POS]).isEqualTo(THUMBNAIL_ID)
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun extractAdditionalAttachmentIds_shouldReturnNull_whenAdditionalIdentifiersAreNull() {
        //when
        val additionalIds = recordService.extractAdditionalAttachmentIds(null, ATTACHMENT_ID)

        //then
        Truth.assertThat(additionalIds).isNull()
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun extractAdditionalAttachmentIds_shouldReturnNull_whenAdditionalIdentifiersAreNotAdditionalAttachmentIds() {
        //given
        val identifier = FhirAttachmentHelper.buildIdentifier("otherId", ASSIGNER)

        //when
        val additionalIds = recordService.extractAdditionalAttachmentIds(listOf(identifier), ATTACHMENT_ID)

        //then
        Truth.assertThat(additionalIds).isNull()
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun splitAdditionalAttachmentId_shouldSplitAdditionalId() {
        //given
        val additionalIdentifier = FhirAttachmentHelper.buildIdentifier(ADDITIONAL_ID, ASSIGNER)

        //when
        val additionalIds = recordService.splitAdditionalAttachmentId(additionalIdentifier)

        //then
        val d4lNamespacePos = 0
        Truth.assertThat(additionalIds).hasLength(RecordService.DOWNSCALED_ATTACHMENT_IDS_SIZE)
        Truth.assertThat(additionalIds!![d4lNamespacePos]).isEqualTo(RecordService.DOWNSCALED_ATTACHMENT_IDS_FMT)
        Truth.assertThat(additionalIds[RecordService.FULL_ATTACHMENT_ID_POS]).isEqualTo(ATTACHMENT_ID)
        Truth.assertThat(additionalIds[RecordService.PREVIEW_ID_POS]).isEqualTo(PREVIEW_ID)
        Truth.assertThat(additionalIds[RecordService.THUMBNAIL_ID_POS]).isEqualTo(THUMBNAIL_ID)
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun splitAdditionalAttachmentId_shouldReturnNull_whenAdditionalIdentifierIsNull() {
        //given
        val additionalIdentifier = FhirAttachmentHelper.buildIdentifier(null, ASSIGNER)
        //when
        val additionalIds = recordService.splitAdditionalAttachmentId(additionalIdentifier)
        //then
        Truth.assertThat(additionalIds).isNull()
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun splitAdditionalAttachmentId_shouldReturnNull_whenAdditionalIdentifierIsNotAdditionalAttachmentId() {
        //given
        val additionalIdentifier = FhirAttachmentHelper.buildIdentifier("otherId", ASSIGNER)

        //when
        val additionalIds = recordService.splitAdditionalAttachmentId(additionalIdentifier)

        //then
        Truth.assertThat(additionalIds).isNull()
    }

    @Test
    fun splitAdditionalAttachmentId_shouldThrow_whenAdditionalAttachmentIdIsMalformed() {
        //given
        val malformedAdditionalId = ADDITIONAL_ID + SPLIT_CHAR + "unexpectedId"
        val additionalIdentifier = FhirAttachmentHelper.buildIdentifier(malformedAdditionalId, ASSIGNER)

        //when
        try {
            recordService.splitAdditionalAttachmentId(additionalIdentifier)
            Assert.fail("Exception expected!")
        } catch (ex: DataValidationException.IdUsageViolation) {

            //then
            Truth.assertThat(ex.message).isEqualTo(malformedAdditionalId)
        }
    }

    @Test
    fun updateAttachmentMeta_shouldUpdateAttachmentMeta() {
        //given
        val attachment = Attachment()
        val data = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xDB.toByte())
        val dataBase64 = encodeToString(data)
        val oldSize = 0
        val oldHash = "oldHash"
        attachment.data = dataBase64
        attachment.size = oldSize
        attachment.hash = oldHash

        //when
        recordService.updateAttachmentMeta(attachment)

        //then
        Truth.assertThat(attachment.data).isEqualTo(dataBase64)
        Truth.assertThat(attachment.size).isEqualTo(data.size)
        Truth.assertThat(attachment.hash).isEqualTo("obkanHeotP32HiKllYhs/aRLUAc=")
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun checkForUnsupportedData_shouldReturnSuccessfully() {
        // Given
        val pdf = arrayOfNulls<Byte>(DATA_SIZE_MAX_BYTES)
        System.arraycopy(
                MimeType.PDF.byteSignature()[0] as Any,
                0,
                pdf,
                0,
                MimeType.PDF.byteSignature()[0]?.size!!
        )
        val doc = buildDocumentReference(unboxByteArray(pdf))

        // When
        recordService.checkDataRestrictions(doc)

        // Then
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun checkForUnsupportedData_shouldThrow_forUnsupportedData() {
        // Given
        val invalidData = byteArrayOf(0x00)
        val doc = buildDocumentReference(invalidData)

        // When
        try {
            recordService.checkDataRestrictions(doc)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataRestrictionException.UnsupportedFileType::class.java)
        }

        // Then
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun checkForUnsupportedData_shouldThrow_whenFileSizeLimitIsReached() {
        // Given
        val invalidSizePdf = arrayOfNulls<Byte>(DATA_SIZE_MAX_BYTES + 1)
        System.arraycopy(
                MimeType.PDF.byteSignature()[0] as Any,
                0,
                invalidSizePdf,
                0,
                MimeType.PDF.byteSignature()[0]?.size!!
        )
        val doc = buildDocumentReference(unboxByteArray(invalidSizePdf))

        // When
        try {
            recordService.checkDataRestrictions(doc)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataRestrictionException.MaxDataSizeViolation::class.java)
        }

        // Then
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun buildMeta_shouldBuildMeta_whenUpdatedDateMillisecondsArePresent() {
        // Given
        val updatedDateWithMilliseconds = "2019-02-28T17:21:08.234123"
        Mockito.`when`(mockDecryptedFhir3Record.customCreationDate).thenReturn("2019-02-28")
        Mockito.`when`(mockDecryptedFhir3Record.updatedDate).thenReturn(updatedDateWithMilliseconds)

        // When
        val meta = recordService.buildMeta(mockDecryptedFhir3Record)

        // Then
        Truth.assertThat(meta.createdDate).isEqualTo(LocalDate.of(2019, 2, 28))
        Truth.assertThat(meta.updatedDate).isEqualTo(LocalDateTime.of(2019, 2, 28, 17, 21, 8, 234123000))
        inOrder.verify(recordService).buildMeta(mockDecryptedFhir3Record)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun buildMeta_shouldBuildMeta_whenUpdatedDateMillisecondsAreNotPresent() {
        // Given
        val updatedDateWithMilliseconds = "2019-02-28T17:21:08"
        Mockito.`when`(mockDecryptedFhir3Record.customCreationDate).thenReturn("2019-02-28")
        Mockito.`when`(mockDecryptedFhir3Record.updatedDate).thenReturn(updatedDateWithMilliseconds)

        // When
        val meta = recordService.buildMeta(mockDecryptedFhir3Record)

        // Then
        Truth.assertThat(meta.createdDate).isEqualTo(
                LocalDate.of(2019, 2, 28)
        )
        Truth.assertThat(meta.updatedDate).isEqualTo(
                LocalDateTime.of(2019, 2, 28, 17, 21, 8)
        )
        inOrder.verify(recordService).buildMeta(mockDecryptedFhir3Record)
        inOrder.verifyNoMoreInteractions()
    }

    //endregion
    @Test
    @Throws(InterruptedException::class)
    fun deleteRecord_shouldDeleteRecord() {
        // Given
        Mockito.`when`(mockApiService.deleteRecord(ALIAS, RECORD_ID, USER_ID)).thenReturn(Completable.complete())

        // When
        val subscriber = recordService.deleteRecord(USER_ID, RECORD_ID).test().await()

        // Then
        subscriber.assertNoErrors().assertComplete()
        inOrder.verify(recordService).deleteRecord(USER_ID, RECORD_ID)
        inOrder.verify(mockApiService).deleteRecord(ALIAS, RECORD_ID, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class)
    fun deleteRecords_shouldDeleteRecords() {
        // Given
        Mockito.doReturn(Completable.complete()).`when`(recordService).deleteRecord(RECORD_ID, USER_ID)
        val ids = listOf(RECORD_ID, RECORD_ID)

        // When
        val observer = recordService.deleteRecords(ids, USER_ID).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(result.failedDeletes).hasSize(0)
        Truth.assertThat(result.successfulDeletes).hasSize(2)
        inOrder.verify(recordService).deleteRecords(ids, USER_ID)
        inOrder.verify(recordService, Mockito.times(2)).deleteRecord(RECORD_ID, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class,
            IOException::class,
            DataValidationException.ModelVersionNotSupported::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun downloadRecord_shouldReturnDownloadedRecord() {
        // Given
        Mockito.`when`(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID))
                .thenReturn(Single.just(mockEncryptedRecord))
        Mockito.`when`(mockCarePlan.resourceType).thenReturn(CarePlan.resourceType)
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .downloadData(mockDecryptedFhir3Record, USER_ID)
        @Suppress("UNCHECKED_CAST")
        every { SdkRecordFactory.getInstance(mockDecryptedFhir3Record) } returns mockRecord as BaseRecord<DomainResource>

        // When
        val observer = recordService.downloadRecord<CarePlan>(RECORD_ID, USER_ID).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(result.meta).isEqualTo(mockMeta)
        Truth.assertThat(result.fhirResource).isEqualTo(mockCarePlan)
        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).downloadData(mockDecryptedFhir3Record, USER_ID)
        inOrder.verify(recordService).checkDataRestrictions(mockCarePlan)
        inOrder.verify(recordService).assignResourceId(mockDecryptedFhir3Record)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class,
            IOException::class,
            DataValidationException.ModelVersionNotSupported::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun downloadRecord_shouldThrow_forUnsupportedData() {
        // Given
        val invalidData = byteArrayOf(0x00)
        val doc = buildDocumentReference(invalidData)
        Mockito.`when`(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID))
                .thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService).downloadData(mockDecryptedFhir3Record, USER_ID)
        Mockito.`when`(mockDecryptedFhir3Record.resource).thenReturn(doc)

        // When
        val observer = recordService.downloadRecord<DomainResource>(RECORD_ID, USER_ID).test().await()

        // Then
        val errors = observer.errors()
        Truth.assertThat(errors).hasSize(1)
        Truth.assertThat(errors[0]).isInstanceOf(DataRestrictionException.UnsupportedFileType::class.java)
        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).downloadData(mockDecryptedFhir3Record, USER_ID)
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class,
            IOException::class,
            DataValidationException.ModelVersionNotSupported::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun downloadRecord_shouldThrow_forFileSizeLimitationBreach() {
        // Given
        val invalidSizePdf = arrayOfNulls<Byte>(DATA_SIZE_MAX_BYTES + 1)
        System.arraycopy(
                MimeType.PDF.byteSignature()[0] as Any,
                0,
                invalidSizePdf,
                0,
                MimeType.PDF.byteSignature()[0]?.size!!
        )
        val doc = buildDocumentReference(unboxByteArray(invalidSizePdf))
        Mockito.`when`(mockApiService
                .fetchRecord(ALIAS, USER_ID, RECORD_ID))
                .thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .downloadData(mockDecryptedFhir3Record, USER_ID)
        Mockito.`when`(mockDecryptedFhir3Record.resource).thenReturn(doc)

        // When
        val observer = recordService.downloadRecord<DomainResource>(RECORD_ID, USER_ID).test().await()

        // Then
        val errors = observer.errors()
        Truth.assertThat(errors).hasSize(1)
        Truth.assertThat(errors[0]).isInstanceOf(DataRestrictionException.MaxDataSizeViolation::class.java)
        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).downloadData(mockDecryptedFhir3Record, USER_ID)
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class)
    fun downloadRecords_shouldReturnDownloadedRecords() {
        // Given
        val recordIds = listOf(RECORD_ID, RECORD_ID)
        Mockito.doReturn(Single.just(mockRecord))
                .`when`(recordService)
                .downloadRecord<DomainResource>(RECORD_ID, USER_ID)

        // When
        val observer = recordService.downloadRecords<CarePlan>(recordIds, USER_ID).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(result.failedDownloads).hasSize(0)
        Truth.assertThat(result.successfulDownloads).hasSize(2)
        inOrder.verify(recordService).downloadRecords<DomainResource>(recordIds, USER_ID)
        inOrder.verify(recordService, Mockito.times(2))
                .downloadRecord<DomainResource>(RECORD_ID, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Ignore
    @Throws(InterruptedException::class,
            IOException::class,
            DataValidationException.ModelVersionNotSupported::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun downloadAttachment_shouldDownloadAttachment() {
        // Given
        Mockito.`when`(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID))
                .thenReturn(Single.just(mockEncryptedRecord))
        val document = buildDocumentReference()
        val attachment = AttachmentBuilder.buildAttachment(ATTACHMENT_ID)
        document.content[0].attachment = attachment
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                document,
                null,
                arrayListOf(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        )
        Mockito.doReturn(decryptedRecord)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        val attachments = ArrayList<Attachment>()
        attachments.add(attachment)
        Mockito.`when`(
                mockAttachmentService.download(
                        ArgumentMatchers.argThat { arg -> arg.contains(attachment) },
                        ArgumentMatchers.eq(mockAttachmentKey),
                        ArgumentMatchers.eq(USER_ID)
                )
        ).thenReturn(Single.just(attachments))

        // when
        val test = recordService.downloadAttachment(RECORD_ID, ATTACHMENT_ID, USER_ID, DownloadType.Full).test().await()

        // then
        val result = test
                .assertNoErrors()
                .assertComplete()
                .assertValue(attachment)
                .values()[0]
        Truth.assertThat(result.id).isEqualTo(ATTACHMENT_ID)
    }

    @Test
    @Ignore
    @Throws(IOException::class,
            InterruptedException::class,
            DataValidationException.ModelVersionNotSupported::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun downloadAttachments_shouldDownloadAttachments() {
        // Given
        Mockito.`when`(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)).thenReturn(Single.just(mockEncryptedRecord))
        val document = buildDocumentReference()
        val attachment = AttachmentBuilder.buildAttachment(ATTACHMENT_ID)
        val secondAttachmentId = "secondId"
        val secondAttachment = AttachmentBuilder.buildAttachment(secondAttachmentId)
        document.content[0].attachment = attachment
        document.content = listOf(document.content[0], DocumentReferenceContent(secondAttachment))
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                document,
                null,
                arrayListOf(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        )
        Mockito.doReturn(decryptedRecord).`when`(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        val attachments = ArrayList<Attachment>()
        attachments.add(attachment)
        attachments.add(secondAttachment)
        Mockito.`when`(
                mockAttachmentService.download(
                        ArgumentMatchers.argThat { arg -> arg.containsAll(listOf(attachment, secondAttachment)) },
                        ArgumentMatchers.eq(mockAttachmentKey),
                        ArgumentMatchers.eq(USER_ID)
                )
        ).thenReturn(Single.just(attachments))

        // when
        val attachmentIds = listOf(ATTACHMENT_ID, secondAttachmentId)
        val test = recordService.downloadAttachments(RECORD_ID, attachmentIds, USER_ID, DownloadType.Full).test().await()

        // then
        val result = test
                .assertNoErrors()
                .assertComplete()
                .assertValue(attachments)
                .values()[0]
        Truth.assertThat(result[0].id).isEqualTo(ATTACHMENT_ID)
        Truth.assertThat(result[1].id).isEqualTo(secondAttachmentId)
    }

    @Test
    @Throws(IOException::class,
            InterruptedException::class,
            DataValidationException.ModelVersionNotSupported::class)
    fun downloadAttachments_shouldThrow_whenInvalidAttachmentIdsProvided() {
        //given
        Mockito.`when`(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID))
                .thenReturn(Single.just(mockEncryptedRecord))
        val document = buildDocumentReference()
        document.content[0].attachment.id = ATTACHMENT_ID
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                document,
                null,
                arrayListOf(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        )
        Mockito.doReturn(decryptedRecord).`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        val attachmentIds = listOf(ATTACHMENT_ID, "invalidAttachmentId")

        //when
        val test = recordService.downloadAttachments(RECORD_ID, attachmentIds, USER_ID, DownloadType.Full).test().await()

        //then
        val errors = test.errors()
        Truth.assertThat(errors).hasSize(1)
        Truth.assertThat(errors[0]).isInstanceOf(DataValidationException.IdUsageViolation::class.java)
        Truth.assertThat(errors[0]!!.message).isEqualTo("Please provide correct attachment ids!")
    }
}

 */