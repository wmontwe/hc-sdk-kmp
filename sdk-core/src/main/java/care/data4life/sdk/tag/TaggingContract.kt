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

class TaggingContract {

    interface Service {
        fun appendDefaultTags(
                resource: Any,
                oldTags: HashMap<String, String>?
        ): HashMap<String, String>

        fun getTagFromType(resourceType: Class<Any>?): HashMap<String, String>

    }

    interface EncryptionService {

    }

    interface Helper {
        fun convertToTagList(tags: HashMap<String, String>): List<String>
        fun convertToTagMap(tagList: List<String>): HashMap<String, String>
    }
}
