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

package care.data4life.sample;

import android.content.Context;
import android.net.Uri;

import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import care.data4life.fhir.stu3.model.Attachment;
import care.data4life.fhir.stu3.model.CodeSystemDocumentReferenceStatus;
import care.data4life.fhir.stu3.model.CodeableConcept;
import care.data4life.fhir.stu3.model.Coding;
import care.data4life.fhir.stu3.model.DocumentReference;
import care.data4life.fhir.stu3.model.FhirDateTime;
import care.data4life.fhir.stu3.model.FhirInstant;
import care.data4life.fhir.stu3.model.Practitioner;
import care.data4life.fhir.stu3.util.FhirDateTimeParser;
import care.data4life.sdk.helpers.lang.DataRestrictionException;
import care.data4life.sdk.helpers.stu3.AttachmentBuilder;
import care.data4life.sdk.helpers.stu3.DocumentReferenceBuilder;
import care.data4life.sdk.helpers.stu3.PractitionerBuilder;

final class FHIRUtils {
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss[XXX]";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT, Locale.US);

    static Practitioner getFakePractitioner() {
        return PractitionerBuilder.buildWith(
                "Bruce",
                "Banner",
                "Dr.",
                "MD",
                "Walvisbaai 3",
                "2333ZA",
                "Den helder",
                "+31715269111",
                "www.webpage.com");
    }

    static CodeableConcept getFakePracticeSpeciality() {
        Coding coding = new Coding();
        coding.code = "General Medicine";
        coding.display = "General Medicine";
        coding.system = "http://www.ihe.net/xds/connectathon/practiceSettingCodes";

        CodeableConcept concept = new CodeableConcept();
        concept.coding = Collections.singletonList(coding);
        return concept;
    }

    static CodeableConcept getFakeDocumentReferenceType() {
        Coding coding = new Coding();
        coding.code = "34108-1";
        coding.display = "Outpatient Note";
        coding.system = "http://loinc.org";

        CodeableConcept concept = new CodeableConcept();
        concept.coding = Collections.singletonList(coding);
        return concept;
    }

    static Attachment buildAttachmentFromUri(Uri assetUri, Context ctx) throws DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation {
        byte[] data = FileUtils.getAsset(assetUri, ctx);
        if (data == null) return null;

        String dateTimeNow = DATE_TIME_FORMATTER.format(ZonedDateTime.now());
        FhirDateTime fhirDateTimeNow = FhirDateTimeParser.parseDateTime(dateTimeNow);
        String contentType = ctx.getContentResolver().getType(assetUri);
        contentType = contentType != null ? contentType : "application/octet-stream";
        String attachmentTitle = FileUtils.getFileName(ctx, assetUri);
        return AttachmentBuilder.buildWith(attachmentTitle, fhirDateTimeNow, contentType, data);
    }

    static DocumentReference buildDocument(String title, List<Attachment> attachments) {
        String indexedString = DATE_TIME_FORMATTER.format(ZonedDateTime.now());
        FhirInstant indexed = FhirDateTimeParser.parseInstant(indexedString);
        return DocumentReferenceBuilder.buildWith(
                title,
                indexed,
                CodeSystemDocumentReferenceStatus.CURRENT,
                attachments,
                getFakeDocumentReferenceType(),
                getFakePractitioner(),
                getFakePracticeSpeciality());
    }
}
