import com.krystianwsul.common.ErrorLogger

class JsErrorLogger(private val response: MutableList<String>) : ErrorLogger() {

    override val enabled = true

    override fun log(message: String) {
        console.log(message)
        response += message
    }

    override fun logMethod(obj: Any) = Unit

    override fun logMethod(obj: Any, message: String) = log(message)

    override fun logException(throwable: Throwable) {
        console.error(throwable)
        response += throwable.toString()
    }
}