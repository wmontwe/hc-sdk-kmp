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

import java.util.HashMap
import kotlin.reflect.KClass

class TaggingContract {
    
    interface Service {
        fun appendDefaultTags(
                resourceType: String?,
                oldTags: HashMap<String, String>?
        ): HashMap<String, String>

        fun _appendDefaultTags(
                resource: Any,
                oldTags: HashMap<String, String>?
        ): HashMap<String, String>

        fun getTagFromType(
                resourceType: String?
        ): HashMap<String, String>

        fun _getTagFromType(
                resourceType: Class<Any>?
        ): HashMap<String, String>
        
    }
    
    interface EncryptionService {
        
    }
    
    interface Helper {

    }
}
