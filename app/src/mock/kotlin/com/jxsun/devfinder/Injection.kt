import com.jxsun.devfinder.feature.DevListActionProcessor

object Injection {
    fun getDevListActionProcessor(): DevListActionProcessor {
        return DevListActionProcessor()
    }
}