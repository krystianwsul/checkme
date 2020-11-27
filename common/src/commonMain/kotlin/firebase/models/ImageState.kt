package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.utils.Serializable

sealed class ImageState : Serializable {

    abstract val uuid: String?

    data class Local(override val uuid: String) : ImageState() {

        override fun hashCode() = uuid.hashCode()

        override fun equals(other: Any?): Boolean {
            if (other === this) return true

            return if (other is ImageState) {
                when (other) {
                    is Local -> uuid == other.uuid
                    is Remote -> uuid == other.uuid
                    is Uploading -> false
                }
            } else {
                false
            }
        }
    }

    data class Remote(override val uuid: String) : ImageState() {

        override fun hashCode() = uuid.hashCode()

        override fun equals(other: Any?): Boolean {
            if (other === this) return true

            return if (other is ImageState) {
                when (other) {
                    is Local -> uuid == other.uuid
                    is Remote -> uuid == other.uuid
                    is Uploading -> false
                }
            } else {
                false
            }
        }
    }

    object Uploading : ImageState() {

        override val uuid: String? = null
    }
}