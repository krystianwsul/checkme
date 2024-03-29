package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.utils.Serializable

sealed interface ImageState : Serializable {

    val uuid: String?

    sealed interface Displayable : ImageState {

        override val uuid: String
    }

    data class Local(override val uuid: String) : Displayable {

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

    data class Remote(override val uuid: String) : Displayable {

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

    object Uploading : ImageState {

        override val uuid: String? = null
    }
}