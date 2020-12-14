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

/*
abstract class RecordServiceTestBase {
    //SUT
    internal lateinit var recordService: RecordService
    internal lateinit var mockApiService: ApiService
    internal lateinit var mockTagEncryptionService: TagEncryptionService
    internal lateinit var mockTaggingService: TaggingService
    internal lateinit var mockFhirService: FhirService
    internal lateinit var mockAttachmentService: AttachmentContract.Service
    internal lateinit var mockCryptoService: CryptoService
    private lateinit var mockErrorHandler: D4LErrorHandler
    internal lateinit var mockCarePlan: CarePlan
    internal lateinit var mockDataResource: DataResource
    internal lateinit var mockDocumentReference: DocumentReference
    internal lateinit var mockTags: HashMap<String, String>
    internal lateinit var mockUploadData: HashMap<Attachment, String?>
    internal lateinit var mockEncryptedTags: List<String>
    internal lateinit var mockEncryptedAnnotations: List<String>
    internal lateinit var mockDataKey: GCKey
    internal lateinit var mockCommonKey: GCKey
    internal lateinit var mockAttachmentKey: GCKey
    internal lateinit var mockEncryptedDataKey: EncryptedKey
    internal lateinit var mockEncryptedAttachmentKey: EncryptedKey
    internal lateinit var mockEncryptedRecord: EncryptedRecord
    internal lateinit var mockAnnotatedEncryptedRecord: EncryptedRecord
    internal lateinit var mockDecryptedFhir3Record: DecryptedFhir3Record<DomainResource>
    internal lateinit var mockAnnotatedDecryptedFhirRecord: DecryptedFhir3Record<DomainResource>
    internal lateinit var mockDecryptedDataRecord: DecryptedDataRecord
    internal lateinit var mockMeta: Meta
    private lateinit var mockD4LException: D4LException
    internal lateinit var mockRecord: Record<CarePlan>
    internal lateinit var mockDataRecord: DataRecord
    internal lateinit var inOrder: InOrder
    internal lateinit var mockDecryptedRecordBuilder: DecryptedRecordBuilder
    internal lateinit var mockRecordFactory: RecordFactory

    private lateinit var mockitoSession: MockitoSession

    @Suppress("UNCHECKED_CAST")
    fun init() {
        mockApiService = Mockito.mock(ApiService::class.java)
        mockTagEncryptionService = Mockito.mock(TagEncryptionService::class.java)
        mockTaggingService = Mockito.mock(TaggingService::class.java)
        mockFhirService = Mockito.mock(FhirService::class.java)
        mockAttachmentService = Mockito.mock(AttachmentContract.Service::class.java)
        mockCryptoService = Mockito.mock(CryptoService::class.java)
        mockErrorHandler = Mockito.mock(D4LErrorHandler::class.java)
        recordService = Mockito.spy(
                RecordService(
                        PARTNER_ID,
                        ALIAS,
                        mockApiService,
                        mockTagEncryptionService,
                        mockTaggingService,
                        mockFhirService,
                        mockAttachmentService,
                        mockCryptoService,
                        mockErrorHandler
                )
        )
        mockCarePlan = Mockito.mock(CarePlan::class.java)
        mockDataResource = DataResource(ByteArray(23))
        mockDocumentReference = Mockito.mock(DocumentReference::class.java)
        mockTags = Mockito.mock<HashMap<*, *>>(HashMap::class.java) as HashMap<String, String>
        mockUploadData = Mockito.mock<HashMap<*, *>>(HashMap::class.java) as HashMap<Attachment, String?>
        mockEncryptedTags = Mockito.mock<MutableList<*>>(MutableList::class.java) as List<String>
        mockEncryptedAnnotations = Mockito.mock<MutableList<*>>(MutableList::class.java) as List<String>
        mockDataKey = Mockito.mock(GCKey::class.java)
        mockAttachmentKey = Mockito.mock(GCKey::class.java)
        mockCommonKey = Mockito.mock(GCKey::class.java)
        mockEncryptedDataKey = Mockito.mock(EncryptedKey::class.java)
        mockEncryptedAttachmentKey = Mockito.mock(EncryptedKey::class.java)
        mockEncryptedRecord = Mockito.mock(EncryptedRecord::class.java)
        mockAnnotatedEncryptedRecord = Mockito.mock(EncryptedRecord::class.java)
        mockDecryptedFhir3Record = Mockito.mock(DecryptedFhir3Record::class.java) as DecryptedFhir3Record<DomainResource>
        mockAnnotatedDecryptedFhirRecord = Mockito.mock(DecryptedFhir3Record::class.java) as DecryptedFhir3Record<DomainResource>
        mockDecryptedDataRecord = Mockito.mock(DecryptedDataRecord::class.java)
        mockMeta = Mockito.mock(Meta::class.java)
        mockD4LException = Mockito.mock(D4LException::class.java)
        mockRecord = Mockito.mock(Record::class.java) as Record<CarePlan>
        mockDataRecord = Mockito.mock(DataRecord::class.java)
        mockDecryptedRecordBuilder = Mockito.mock(DecryptedRecordBuilderImpl::class.java)
        mockRecordFactory = Mockito.mock(RecordFactory::class.java)

        Mockito.`when`(mockRecord.fhirResource).thenReturn(mockCarePlan)
        Mockito.`when`(mockRecord.meta).thenReturn(mockMeta)

        Mockito.`when`(mockDecryptedFhir3Record.tags).thenReturn(mockTags)
        Mockito.`when`(mockDecryptedFhir3Record.dataKey).thenReturn(mockDataKey)
        Mockito.`when`(mockDecryptedFhir3Record.resource).thenReturn(mockCarePlan)
        Mockito.`when`(mockDecryptedFhir3Record.modelVersion).thenReturn(ModelVersion.CURRENT)

        Mockito.`when`<HashMap<*, *>?>(mockAnnotatedDecryptedFhirRecord.tags).thenReturn(mockTags)
        Mockito.`when`(mockAnnotatedDecryptedFhirRecord.dataKey).thenReturn(mockDataKey)
        Mockito.`when`(mockAnnotatedDecryptedFhirRecord.resource).thenReturn(mockCarePlan)
        Mockito.`when`(mockAnnotatedDecryptedFhirRecord.modelVersion).thenReturn(ModelVersion.CURRENT)
        Mockito.`when`(mockAnnotatedDecryptedFhirRecord.annotations).thenReturn(ANNOTATIONS)

        Mockito.`when`<HashMap<*, *>?>(mockDecryptedDataRecord.tags).thenReturn(mockTags)
        Mockito.`when`(mockDecryptedDataRecord.dataKey).thenReturn(mockDataKey)
        Mockito.`when`(mockDecryptedDataRecord.resource).thenReturn(mockDataResource.value)
        Mockito.`when`(mockDecryptedDataRecord.identifier).thenReturn("id")
        Mockito.`when`(mockDecryptedDataRecord.modelVersion).thenReturn(ModelVersion.CURRENT)
        Mockito.`when`(mockDecryptedDataRecord.annotations).thenReturn(ANNOTATIONS)

        Mockito.`when`(mockTags[RESOURCE_TYPE]).thenReturn(CarePlan.resourceType)

        Mockito.`when`(mockEncryptedRecord.encryptedTags).thenReturn(mockEncryptedTags)
        Mockito.`when`(mockEncryptedRecord.encryptedDataKey).thenReturn(mockEncryptedDataKey)
        Mockito.`when`(mockEncryptedRecord.encryptedBody).thenReturn(ENCRYPTED_RESOURCE)
        Mockito.`when`(mockEncryptedRecord.modelVersion).thenReturn(ModelVersion.CURRENT)
        Mockito.`when`(mockEncryptedRecord.identifier).thenReturn(RECORD_ID)
        Mockito.`when`(mockEncryptedRecord.customCreationDate).thenReturn(CREATION_DATE)

        Mockito.`when`(mockAnnotatedEncryptedRecord.encryptedTags).thenReturn(mockEncryptedTags)
        Mockito.`when`(mockAnnotatedEncryptedRecord.encryptedDataKey).thenReturn(mockEncryptedDataKey)
        Mockito.`when`(mockAnnotatedEncryptedRecord.encryptedBody).thenReturn(ENCRYPTED_RESOURCE)
        Mockito.`when`(mockAnnotatedEncryptedRecord.modelVersion).thenReturn(ModelVersion.CURRENT)
        Mockito.`when`(mockAnnotatedEncryptedRecord.identifier).thenReturn(RECORD_ID)
        Mockito.`when`(mockAnnotatedEncryptedRecord.customCreationDate).thenReturn(CREATION_DATE)

        Mockito.`when`(mockErrorHandler.handleError(ArgumentMatchers.any(Exception::class.java))).thenReturn(mockD4LException)

        mockkConstructor(DecryptedRecordBuilderImpl::class)
        mockkObject(SdkRecordFactory)

        inOrder = Mockito.inOrder(
                mockApiService,
                mockTagEncryptionService,
                mockTaggingService,
                mockFhirService,
                mockAttachmentService,
                mockCryptoService,
                mockErrorHandler,
                mockDecryptedRecordBuilder,
                recordService
        )

        mockkStatic(LocalDate::class)
        every { LocalDate.now(any() as Clock) } returns LOCAL_DATE

        mockkStatic(DateTimeFormatter::class)
        every { DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US) } returns DATE_FORMATTER

        mockkStatic(FhirAttachmentHelper::class)

        mockitoSession = Mockito.mockitoSession().startMocking()
    }

    fun stop() {
        mockitoSession.finishMocking()
        unmockkAll()
    }

    companion object {
        internal const val PARTNER_ID = "partnerId"
        internal const val USER_ID = "userId"
        internal const val ENCRYPTED_RESOURCE = "encryptedResource"
        internal val ENCRYPTED_APPDATA = ByteArray(42)
        internal const val RESOURCE_TYPE = "resourcetype"
        internal const val RECORD_ID = "recordId"
        internal const val ALIAS = "alias"
        internal const val DATA = "data"
        internal const val DATA_HASH = "dataHash"
        internal const val ATTACHMENT_ID = "attachmentId"
        internal const val THUMBNAIL_ID = "thumbnailId"
        internal const val PREVIEW_ID = "previewId"
        internal const val ASSIGNER = "assigner"
        internal const val ADDITIONAL_ID = RecordService.DOWNSCALED_ATTACHMENT_IDS_FMT +
                SPLIT_CHAR +
                ATTACHMENT_ID +
                SPLIT_CHAR +
                PREVIEW_ID +
                SPLIT_CHAR +
                THUMBNAIL_ID
        internal val LOCAL_DATE = LocalDate.of(2001, 1, 1)
        internal val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
        internal val UTC_ZONE_ID = ZoneId.of("UTC")
        internal val ANNOTATIONS = listOf("potato", "tomato", "soup")
        internal const val CREATION_DATE = "2020-05-03"

        fun buildDocumentReference(): DocumentReference {
            val content = buildDocRefContent(AttachmentBuilder.buildAttachment(null))
            val contents: MutableList<DocumentReference.DocumentReferenceContent> = ArrayList()
            contents.add(content)
            return DocumentReference(
                    null,
                    null,
                    null,
                    contents
            )
        }

        fun buildDocRefContent(attachment: Attachment): DocumentReference.DocumentReferenceContent {
            return DocumentReference.DocumentReferenceContent(attachment)
        }

        fun buildDocumentReference(data: ByteArray?): DocumentReference {
            val doc = buildDocumentReference()
            doc.content[0].attachment.data = Base64.encodeToString(data!!)
            return doc
        }

        fun unboxByteArray(array: Array<Byte?>): ByteArray {
            val result = ByteArray(array.size)
            var i = 0
            for (b in array) result[i++] = b ?: 0
            return result
        }
    }
}

 */
