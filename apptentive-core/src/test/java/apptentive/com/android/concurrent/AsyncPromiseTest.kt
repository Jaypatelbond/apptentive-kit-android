package apptentive.com.android.concurrent

import apptentive.com.android.TestCase
import org.junit.Assert.assertTrue
import org.junit.Test

class AsyncPromiseTest : TestCase() {
    @Test
    fun testCallbacks() {
        val promise = AsyncPromise<String>()

        // use this queue to dispatch value and error
        val dispatchQueue = BlockingExecutionQueue("dispatch")

        // subscribe to callbacks
        promise.then { value ->
            // should be executed on the same queue
            assertTrue(dispatchQueue.isCurrent)
            addResult("value: $value")
        }
        promise.catch { error ->
            // should be executed on the same queue
            assertTrue(dispatchQueue.isCurrent)
            addResult("error: ${error.message}")
        }

        // notify on a separate queue
        dispatchQueue.dispatch {
            promise.resolve("test")
        }
        assertResults("value: test")

        dispatchQueue.dispatch {
            promise.reject(Exception("exception"))
        }
        assertResults("error: exception")
    }

    @Test
    fun testCallbacksCustomQueue() {
        val completionQueue = BlockingExecutionQueue("completion")

        // subscribe to callbacks
        val promise = AsyncPromise<String>(completionQueue)
        promise.then { value ->
            // should be executed on a "completion" queue
            assertTrue(completionQueue.isCurrent)
            addResult("value: $value")
        }
        promise.catch { error ->
            // should be executed on a "completion" queue
            assertTrue(completionQueue.isCurrent)
            addResult("error: ${error.message}")
        }

        // notify on a separate queue
        val dispatchQueue = BlockingExecutionQueue("dispatch")
        dispatchQueue.dispatch {
            promise.resolve("test")
        }
        assertResults("value: test")

        dispatchQueue.dispatch {
            promise.reject(Exception("exception"))
        }
        assertResults("error: exception")
    }

    @Test
    fun testCallbacksThenException() {
        val promise = AsyncPromise<String>()
        promise.then {
            throw Exception("exception")
        }
        promise.catch { error ->
            addResult("error: ${error.message}")
        }

        // notify on a separate queue
        val dispatchQueue = BlockingExecutionQueue("dispatch")
        dispatchQueue.dispatch {
            promise.resolve("test")
        }
        assertResults("error: exception")
    }

    @Test
    fun testCallbacksThenExceptionCustomQueue() {
        val completionQueue = BlockingExecutionQueue("completion")

        val promise = AsyncPromise<String>(completionQueue)
        promise.then {
            throw Exception("exception")
        }
        promise.catch { error ->
            addResult("error: ${error.message}")
        }

        // notify on a separate queue
        val dispatchQueue = BlockingExecutionQueue("dispatch")
        dispatchQueue.dispatch {
            promise.resolve("test")
        }
        assertResults("error: exception")
    }

    @Test
    fun testCallbacksCatchException() {
        val promise = AsyncPromise<String>()
        promise.then {
            throw Exception("exception")
        }
        promise.catch {
            throw Exception("another exception")
        }

        // notify on a separate queue
        val dispatchQueue = BlockingExecutionQueue("dispatch")
        dispatchQueue.dispatch {
            promise.resolve("test")
        }
        assertResults() // there will be no results but also no uncaught exceptions
    }

    @Test
    fun testCallbacksCatchExceptionCustomQueue() {
        val completionQueue = BlockingExecutionQueue("completion")

        val promise = AsyncPromise<String>(completionQueue)
        promise.then {
            throw Exception("exception")
        }
        promise.catch {
            throw Exception("another exception")
        }

        // notify on a separate queue
        val dispatchQueue = BlockingExecutionQueue("dispatch")
        dispatchQueue.dispatch {
            promise.resolve("test")
        }
        assertResults() // there will be no results but also no uncaught exceptions
    }
}