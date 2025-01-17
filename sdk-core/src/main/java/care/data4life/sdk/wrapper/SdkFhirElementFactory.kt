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

import care.data4life.sdk.fhir.Fhir3ElementFactory
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4ElementFactory
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.CoreRuntimeException

internal object SdkFhirElementFactory : WrapperContract.FhirElementFactory {
    private val fhir3Indicator = Fhir3Resource::class.java.`package`
    private val fhir4Indicator = Fhir4Resource::class.java.`package`

    @Throws(CoreRuntimeException.InternalFailure::class)
    override fun getFhirTypeForClass(resourceType: Class<out Any>): String? {
        @Suppress("UNCHECKED_CAST")
        return when (resourceType.`package`) {
            fhir3Indicator -> Fhir3ElementFactory.getFhirTypeForClass(resourceType as Class<Fhir3Resource>)
            fhir4Indicator -> Fhir4ElementFactory.getFhirTypeForClass(resourceType as Class<Fhir4Resource>)
            else -> throw CoreRuntimeException.InternalFailure()
        }
    }

    override fun resolveFhirVersion(resourceType: Class<out Any>): FhirContract.FhirVersion {
        return when (resourceType.`package`) {
            fhir3Indicator -> FhirContract.FhirVersion.FHIR_3
            fhir4Indicator -> FhirContract.FhirVersion.FHIR_4
            else -> FhirContract.FhirVersion.UNKNOWN
        }
    }

    override fun getFhir3ClassForType(resourceType: String): Class<out Fhir3Resource>? {
        val clazz = Fhir3ElementFactory.getClassForFhirType(resourceType)

        @Suppress("UNCHECKED_CAST")
        return if (clazz == null) null else clazz as Class<out Fhir3Resource>
    }

    override fun getFhir4ClassForType(resourceType: String): Class<out Fhir4Resource>? {
        val clazz = Fhir4ElementFactory.getClassForFhirType(resourceType)

        @Suppress("UNCHECKED_CAST")
        return if (clazz == null) null else (clazz as Class<out Fhir4Resource>)
    }
}
