package apptentive.com.android

import apptentive.com.android.serialization.json.JsonConverter
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule

open class TestCase(
    private val logMessages: Boolean = false,
    private val logStackTraces: Boolean = false
) {
    @get:Rule
    val dependencyRule = DependencyProviderRule()

    private val results = mutableListOf<Any>()

    //region Before/After

    @Before
    open fun setUp() {
        results.clear()
    }

    //endregion

    //region Resources

    protected fun readText(path: String): String {
        return javaClass.classLoader!!.getResourceAsStream(path).bufferedReader().readText()
    }

    protected inline fun <reified T> readJson(path: String): T {
        val json = readText(path)
        return JsonConverter.fromJson(json)
    }

    //endregion

    //region Results

    protected fun addResult(result: Any) {
        results.add(result)
    }

    protected fun assertResults(vararg expected: Any, clearResults: Boolean = true) {
        assertEquals(expected.toList(), results)
        if (clearResults) {
            results.clear()
        }
    }

    //endregion
}