package apptentive.com.android.love

import android.app.Application
import apptentive.com.android.concurrent.Promise
import apptentive.com.android.core.DependencyProvider
import apptentive.com.android.core.Provider
import apptentive.com.android.util.Resource

object ApptentiveLove {
    private var client: LoveClient = LoveClient.NULL

    var person: Person
        get() = client.person
        set(value) {
            client.person = value
        }

    fun register(application: Application, apptentiveKey: String, apptentiveSignature: String) {
        DependencyProvider.register(application)

        client = LoveDefaultClient(application.applicationContext, apptentiveKey, apptentiveSignature)
        DependencyProvider.register<LoveSender>(client)
    }

    fun getEntities(): Promise<Resource<List<LoveEntitySnapshot>>> {
        return client.getEntities()
    }

    fun send(
        entity: LoveEntity,
        onSend: ((entity: LoveEntity) -> Unit)? = null,
        onError: ((entity: LoveEntity, error: Exception) -> Unit)? = null
    ) {
        client.send(entity, onSend, onError)
    }
}