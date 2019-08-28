package apptentive.com.android.core

typealias TimeInterval = Double

const val UNDEFINED: Int = -1

fun toMilliseconds(time: TimeInterval) : Int = (time * 1000L).toInt()
fun toSeconds(time: Long) : TimeInterval = time * 0.001