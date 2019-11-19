package apptentive.com.android.feedback.payload

import apptentive.com.android.util.Result

class SerialPayloadSender(
    private val payloadService: PayloadService,
    private val payloadQueue: PayloadQueue
) : PayloadSender {
    var active: Boolean = true
        set(value) {
            val oldValue = field
            field = value
            if (value && !oldValue) {
                sendNextUnsentPayload()
            }
        }

    private var busySending: Boolean = false

    override fun sendPayload(payload: Payload, callback: (Result<Payload>) -> Unit) {
        payloadQueue.enqueuePayload(payload)
        sendNextUnsentPayload()
    }

    private fun handleSentPayload(payload: Payload) {
        payloadQueue.deletePayload(payload)
        sendNextUnsentPayload()
    }

    private fun handleFailedPayload(payload: Payload, error: Throwable) {
        payloadQueue.deletePayload(payload)
        sendNextUnsentPayload()
        error.printStackTrace()
    }

    private fun sendNextUnsentPayload() {
        if (!active) {
            // TODO: log message: con't send while being inactive
            return
        }

        if (busySending) {
            // TODO: log message: con't send while still busy
            return
        }


        val nextPayload = payloadQueue.nextUnsentPayload()
            ?: // TODO: log message 'all done'
            return

        busySending = true

        payloadService.sendPayload(nextPayload) {
            busySending = false

            when (it) {
                is Result.Success -> handleSentPayload(nextPayload)
                is Result.Error -> handleFailedPayload(nextPayload, it.error)
            }
        }
    }


    fun setPayloadService(service: PayloadService) {
        TODO()
    }
}