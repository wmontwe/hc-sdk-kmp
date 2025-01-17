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

package care.data4life.sdk.network.model

import care.data4life.sdk.crypto.GCKey
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.model.ModelContract
import care.data4life.sdk.network.model.NetworkModelInternalContract.DecryptedFhir4Record
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.Tags

data class DecryptedR4Record<T : Fhir4Resource>(
    override var identifier: String?,
    override var resource: T,
    override var tags: Tags,
    override var annotations: Annotations,
    override var customCreationDate: String?,
    override var updatedDate: String?,
    override var dataKey: GCKey,
    override var attachmentsKey: GCKey?,
    override var modelVersion: Int,
    override var status: ModelContract.RecordStatus
) : DecryptedFhir4Record<T>
