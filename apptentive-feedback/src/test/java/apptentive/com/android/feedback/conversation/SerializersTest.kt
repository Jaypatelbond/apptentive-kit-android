package apptentive.com.android.feedback.conversation

import apptentive.com.android.feedback.engagement.Event
import apptentive.com.android.feedback.engagement.criteria.DateTime
import apptentive.com.android.feedback.engagement.interactions.InteractionId
import apptentive.com.android.feedback.mockAppRelease
import apptentive.com.android.feedback.mockDevice
import apptentive.com.android.feedback.mockEngagementData
import apptentive.com.android.feedback.mockPerson
import apptentive.com.android.feedback.mockSdk
import apptentive.com.android.feedback.model.Conversation
import apptentive.com.android.feedback.model.CustomData
import apptentive.com.android.feedback.model.EngagementData
import apptentive.com.android.feedback.model.EngagementRecord
import apptentive.com.android.feedback.model.Person
import apptentive.com.android.feedback.utils.VersionCode
import apptentive.com.android.feedback.utils.VersionName
import apptentive.com.android.serialization.BinaryDecoder
import apptentive.com.android.serialization.BinaryEncoder
import apptentive.com.android.serialization.TypeSerializer
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class SerializersTest {
    @Test
    fun versionCodeSerializer() {
        val value: VersionCode = 1234567890
        checkSerializer(Serializers.versionCodeSerializer, value)
    }

    @Test
    fun versionNameSerializer() {
        val versionName: VersionName = "1.2.3"
        checkSerializer(Serializers.versionNameSerializer, versionName)
    }

    @Test
    fun interactionIdSerializer() {
        val interactionId: InteractionId = "1234567890"
        checkSerializer(Serializers.interactionIdSerializer, interactionId)
    }

    @Test
    fun dateTimeSerializer() {
        checkSerializer(Serializers.dateTimeSerializer, DateTime(1234567890.0))
    }

    @Test
    fun customDataSerializer() {
        checkSerializer(Serializers.customDataSerializer, CustomData())
        checkSerializer(
            Serializers.customDataSerializer,
            CustomData(
                content = mapOf(
                    "key1" to true,
                    "key2" to 10.toByte(),
                    "key3" to 20.toShort(),
                    "key4" to 30,
                    "key5" to 40.toLong(),
                    "key6" to 3.14f,
                    "key7" to 4.14,
                    "key8" to "štríñg",
                    "key9" to null
                )
            )
        )
    }

    @Test
    fun deviceSerializer() {
        checkSerializer(Serializers.deviceSerializer, mockDevice)
    }

    @Test
    fun personSerializer() {
        checkSerializer(Serializers.personSerializer, Person())
        checkSerializer(Serializers.personSerializer, mockPerson)
    }

    @Test
    fun sdkSerializer() {
        checkSerializer(Serializers.sdkSerializer, mockSdk)
    }

    @Test
    fun appReleaseSerializer() {
        checkSerializer(Serializers.appReleaseSerializer, mockAppRelease)
    }

    @Test
    fun getEngagementRecordSerializer() {
        checkSerializer(Serializers.engagementRecordSerializer, EngagementRecord())
        val record = EngagementRecord(
            totalInvokes = 3,
            versionNameLookup = mutableMapOf("1.0.0" to 2L, "1.0.1" to 1L),
            versionCodeLookup = mutableMapOf(100L to 2L, 101L to 1L),
            lastInvoked = DateTime(1234567890.0)
        )
        checkSerializer(Serializers.engagementRecordSerializer, record)
    }

    @Test
    fun eventSerializer() {
        checkSerializer(Serializers.eventSerializer, Event.local("event1"))
        checkSerializer(Serializers.eventSerializer, Event.internal("event2"))
    }

    @Test
    fun engagementDataSerializer() {
        checkSerializer(Serializers.engagementDataSerializer, EngagementData())
        checkSerializer(Serializers.engagementDataSerializer, mockEngagementData)
    }

    @Test
    fun conversationSerializer() {
        val conversation = Conversation(
            localIdentifier = "local_identifier",
            conversationToken = "conversation_token",
            conversationId = "conversation_id",
            device = mockDevice,
            person = mockPerson,
            sdk = mockSdk,
            appRelease = mockAppRelease,
            engagementData = mockEngagementData
        )
        checkSerializer(Serializers.conversationSerializer, conversation)
    }

    @Test
    @Ignore
    fun engagementManifestSerializer() {
    }

    private fun <T> checkSerializer(serializer: TypeSerializer<T>, value: T) {
        val output = ByteArrayOutputStream()
        val encoder = BinaryEncoder(DataOutputStream(output))
        serializer.encode(encoder, value)
        val input = ByteArrayInputStream(output.toByteArray())
        val decoder = BinaryDecoder(DataInputStream(input))
        val actual = serializer.decode(decoder)
        assertThat(actual).isEqualTo(value)
    }
}
