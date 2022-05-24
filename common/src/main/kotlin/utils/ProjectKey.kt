package com.krystianwsul.common.utils

sealed class ProjectKey<T : ProjectType> : Parcelable, Serializable {

    companion object {

        private const val KEY_LENGTH = 20
        private const val PUSH_CHARS = "-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz"

        private val MULTIPLIER = Ordinal(PUSH_CHARS.length)

        private val MAX_KEY = PUSH_CHARS.last()
            .toString()
            .repeat(KEY_LENGTH)

        private fun keyToRawOrdinal(key: String): Ordinal {
            return key.reversed().mapIndexed { index, char ->
                check(index < KEY_LENGTH)

                val currMultiplier = MULTIPLIER.pow(index)

                currMultiplier * char.code
            }.sum()
        }

        private val MAX_RAW_ORDINAL = keyToRawOrdinal(MAX_KEY)

        fun keyToOrdinal(key: String) = keyToRawOrdinal(key).also {
            check(it < MAX_RAW_ORDINAL)
        } - MAX_RAW_ORDINAL

        fun fromJson(json: String): ProjectKey<*> {
            val (typeStr, key) = json.split(':')

            return Type.valueOf(typeStr).newKey(key)
        }
    }

    abstract val key: String
    abstract val type: Type

    fun getOrdinal() = keyToOrdinal(key)

    fun toJson() = "$type:$key"

    @Parcelize
    data class Private(override val key: String) : ProjectKey<ProjectType.Private>(), Comparable<Private> {

        override val type get() = Type.PRIVATE

        override fun compareTo(other: Private) = key.compareTo(other.key)

        fun toUserKey() = UserKey(key)
    }

    @Parcelize
    data class Shared(override val key: String) : ProjectKey<ProjectType.Shared>(), Comparable<Shared> {

        override val type get() = Type.SHARED

        override fun compareTo(other: Shared) = key.compareTo(other.key)
    }

    // don't change names, they're used in to/from/Json
    enum class Type {

        PRIVATE {

            override fun newKey(key: String) = Private(key)
        },
        SHARED {

            override fun newKey(key: String) = Shared(key)
        };

        abstract fun newKey(key: String): ProjectKey<*>
    }
}