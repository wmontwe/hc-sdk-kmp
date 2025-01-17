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
package care.data4life.sdk.tag

import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.tag.TaggingContract.Companion.SEPARATOR
import care.data4life.sdk.tag.TaggingContract.Companion.TAG_APPDATA_KEY
import care.data4life.sdk.tag.TaggingContract.Companion.TAG_APPDATA_VALUE
import care.data4life.sdk.tag.TaggingContract.Companion.TAG_CLIENT
import care.data4life.sdk.tag.TaggingContract.Companion.TAG_FHIR_VERSION
import care.data4life.sdk.tag.TaggingContract.Companion.TAG_PARTNER
import care.data4life.sdk.tag.TaggingContract.Companion.TAG_RESOURCE_TYPE
import care.data4life.sdk.tag.TaggingContract.Companion.TAG_UPDATED_BY_CLIENT
import care.data4life.sdk.tag.TaggingContract.Companion.TAG_UPDATED_BY_PARTNER
import care.data4life.sdk.wrapper.SdkFhirElementFactory
import care.data4life.sdk.wrapper.WrapperContract

// TODO internal
class TaggingService(
    private val clientId: String
) : TaggingContract.Service {
    private val partnerId: String = clientId.substringBefore(SEPARATOR)
    private val fhirElementFactory: WrapperContract.FhirElementFactory = SdkFhirElementFactory

    private fun appendCommonDefaultTags(
        resourceType: String?,
        oldTags: Tags?
    ): MutableMap<String, String> {
        val tags = mutableMapOf<String, String>()
        if (oldTags != null && oldTags.isNotEmpty()) {
            tags.putAll(oldTags)
        }
        if (resourceType != null && resourceType.isNotEmpty()) {
            tags[TAG_RESOURCE_TYPE] = resourceType
        }
        if (!tags.containsKey(TAG_CLIENT)) {
            tags[TAG_CLIENT] = clientId
        } else {
            tags[TAG_UPDATED_BY_CLIENT] = clientId
        }
        if (!tags.containsKey(TAG_PARTNER)) {
            tags[TAG_PARTNER] = partnerId
        } else {
            tags[TAG_UPDATED_BY_PARTNER] = partnerId
        }
        return tags
    }

    override fun appendDefaultTags(
        resource: Any,
        oldTags: Tags?
    ): Tags {
        return when (resource) {
            is Fhir3Resource -> appendCommonDefaultTags(
                resource.resourceType,
                oldTags
            ).also { tags -> tagVersion(tags, FhirContract.FhirVersion.FHIR_3) }
            is Fhir4Resource -> appendCommonDefaultTags(
                resource.resourceType,
                oldTags
            ).also { tags -> tagVersion(tags, FhirContract.FhirVersion.FHIR_4) }
            else -> appendCommonDefaultTags(
                null,
                oldTags
            ).also { tags -> tags[TAG_APPDATA_KEY] = TAG_APPDATA_VALUE }
        }
    }

    private fun tagVersion(
        tags: MutableMap<String, String>,
        version: FhirContract.FhirVersion
    ) {
        if (version == FhirContract.FhirVersion.UNKNOWN) {
            tags[TAG_APPDATA_KEY] = TAG_APPDATA_VALUE
        } else {
            tags[TAG_FHIR_VERSION] = version.version
        }
    }

    override fun getTagsFromType(
        resourceType: Class<out Any>
    ): Tags {
        return mutableMapOf<String, String>().also { tags ->
            val version = fhirElementFactory.resolveFhirVersion(resourceType)
            tagVersion(tags, version)

            if (version != FhirContract.FhirVersion.UNKNOWN) {
                fhirElementFactory.getFhirTypeForClass(resourceType).also { resourceTagValue ->
                    if (resourceTagValue is String) {
                        tags[TAG_RESOURCE_TYPE] = resourceTagValue
                    }
                }
            }
        }
    }
}
