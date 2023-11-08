package apptentive.com.android.feedback.payload

import apptentive.com.android.encryption.AESEncryption23
import apptentive.com.android.feedback.conversation.MockEncryptedConversationCredential
import apptentive.com.android.feedback.model.Message
import apptentive.com.android.feedback.model.payloads.MessagePayload
import apptentive.com.android.feedback.utils.MultipartParser
import apptentive.com.android.serialization.json.JsonConverter
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

internal class PayloadTokenUpdaterTest {
    @Test
    fun testUpdateMessageJson() {
        val originalJsonData = "{\"session_id\":\"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx\",\"client_created_at\":1699375136.316,\"body\":\"ABC\",\"nonce\":\"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx\",\"client_created_at_utc_offset\":-28800,\"token\":\"mockedConversationToken\"}".toByteArray()

        val updatedJsonData = PayloadTokenUpdater.updateJSON("abc123", PayloadType.Message, originalJsonData)

        val json = JsonConverter.toMap(String(updatedJsonData, Charsets.UTF_8))

        assertEquals("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", json["session_id"])
        Assert.assertTrue(1698774495.52 < json["client_created_at"] as Double)
        assertEquals("ABC", json["body"])
        assertEquals("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", json["nonce"])
        assertEquals("abc123", json["token"])
    }

    @Test
    fun testUpdateEventJson() {
        val originalJsonData = "{\"event\":{\"session_id\":\"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx\",\"client_created_at\":1699375136.316,\"label\":\"ABC\",\"nonce\":\"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx\",\"client_created_at_utc_offset\":-28800,\"token\":\"mockedConversationToken\"}}".toByteArray()

        val updatedJsonData = PayloadTokenUpdater.updateJSON("abc123", PayloadType.Event, originalJsonData)

        val json = JsonConverter.toMap(String(updatedJsonData, Charsets.UTF_8))
        val nestedJson = json["event"] as Map<String, Any>

        assertEquals("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", nestedJson["session_id"])
        Assert.assertTrue(1698774495.52 < nestedJson["client_created_at"] as Double)
        assertEquals("ABC", nestedJson["label"])
        assertEquals("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", nestedJson["nonce"])
        assertEquals("abc123", nestedJson["token"])
    }

    @Test
    fun testEncryptDecrypt() {
        val conversationCredential = MockEncryptedConversationCredential()

        val originalJsonData = "{\"event\":{\"session_id\":\"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx\",\"client_created_at\":1699375136.316,\"label\":\"ABC\",\"nonce\":\"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx\",\"client_created_at_utc_offset\":-28800,\"token\":\"mockedConversationToken\"}}".toByteArray()

        val encryptedJsonData = PayloadTokenUpdater.encrypt(originalJsonData, conversationCredential.payloadEncryptionKey!!)
        assertNotEquals(encryptedJsonData.hashCode(), originalJsonData.hashCode())
        val decryptedJsonData = PayloadTokenUpdater.decrypt(encryptedJsonData, conversationCredential.payloadEncryptionKey!!)

        assertTrue(originalJsonData.contentEquals(decryptedJsonData))
    }

    @Test
    fun testEventFullTokenUpdate() {
        val conversationCredential = MockEncryptedConversationCredential()

        val originalJsonData = "{\"event\":{\"session_id\":\"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx\",\"client_created_at\":1699375136.316,\"label\":\"ABC\",\"nonce\":\"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx\",\"client_created_at_utc_offset\":-28800,\"token\":\"mockedConversationToken\"}}".toByteArray()

        val encryptedJsonData = PayloadTokenUpdater.encrypt(originalJsonData, conversationCredential.payloadEncryptionKey!!)

        val updatedEncryptedJsonData = PayloadTokenUpdater.updateEmbeddedToken("abc123", conversationCredential.payloadEncryptionKey!!, PayloadType.Event, MediaType.applicationOctetStream, encryptedJsonData)

        val decryptedJsonData = PayloadTokenUpdater.decrypt(updatedEncryptedJsonData, conversationCredential.payloadEncryptionKey!!)

        val json = JsonConverter.toMap(String(decryptedJsonData, Charsets.UTF_8))
        val nestedJson = json["event"] as Map<String, Any>

        assertEquals("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", nestedJson["session_id"])
        Assert.assertTrue(1698774495.52 < nestedJson["client_created_at"] as Double)
        assertEquals("ABC", nestedJson["label"])
        assertEquals("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", nestedJson["nonce"])
        assertEquals("abc123", nestedJson["token"])
    }

    @Test
    fun testMessageFullTokenUpdate() {
        val conversationCredential = MockEncryptedConversationCredential()

        val originalJsonData = "{\"session_id\":\"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx\",\"client_created_at\":1699375136.316,\"body\":\"ABC\",\"nonce\":\"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx\",\"client_created_at_utc_offset\":-28800,\"token\":\"mockedConversationToken\"}".toByteArray()

        val encryptedJsonData = PayloadTokenUpdater.encrypt(originalJsonData, conversationCredential.payloadEncryptionKey!!)

        val updatedEncryptedJsonData = PayloadTokenUpdater.updateEmbeddedToken("abc123", conversationCredential.payloadEncryptionKey!!, PayloadType.Message, MediaType.applicationOctetStream, encryptedJsonData)

        val decryptedJsonData = PayloadTokenUpdater.decrypt(updatedEncryptedJsonData, conversationCredential.payloadEncryptionKey!!)

        val json = JsonConverter.toMap(String(decryptedJsonData, Charsets.UTF_8))

        assertEquals("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", json["session_id"])
        Assert.assertTrue(1698774495.52 < json["client_created_at"] as Double)
        assertEquals("ABC", json["body"])
        assertEquals("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", json["nonce"])
        assertEquals("abc123", json["token"])
    }

    @Test
    fun testMultipartFullTokenUpdate() {
        val attachments = listOf(
            Message.Attachment(
                "1", "image/jpeg",
                localFilePath = File(
                    javaClass.getResource("/dog.jpg")?.path ?: ""
                ).absolutePath,
                originalName = "dog.jpg"
            )
        )
        val messagePayload = MessagePayload("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", automated = false, body = "ABC", hidden = false, attachments = attachments)
        val conversationCredential = MockEncryptedConversationCredential()

        val payloadData = messagePayload.toPayloadData(conversationCredential)

        val updatedEncryptedPayloadData = PayloadTokenUpdater.updateEmbeddedToken("abc123", conversationCredential.payloadEncryptionKey!!, PayloadType.Message, MediaType.multipartEncrypted("s16u0iwtqlokf4v9cpgne8a2amdrxz735hjby"), payloadData.sidecarFilename.data)

        val inputStream = ByteArrayInputStream(updatedEncryptedPayloadData)
        val parser = MultipartParser(inputStream, "s16u0iwtqlokf4v9cpgne8a2amdrxz735hjby")

        assertEquals(2, parser.numberOfParts)

        val firstPart = parser.getPartAtIndex(0)!!

        assertEquals(
            "Content-Disposition: form-data;name=\"message\"\r\n" +
                "Content-Type: application/octet-stream\r\n",
            firstPart.multipartHeaders
        )

        val decryptedContent = AESEncryption23(conversationCredential.payloadEncryptionKey!!).decryptPayloadData(firstPart.content)
        val decryptedPart = MultipartParser.parsePart(ByteArrayInputStream(decryptedContent), 0L..decryptedContent.size + 2) // TODO: Why do we have to add 2 here?

        assertEquals(
            "Content-Disposition: form-data;name=\"message\"\r\n" +
                "Content-Type: application/json;charset=UTF-8\r\n",
            decryptedPart!!.multipartHeaders
        )

        val json = JsonConverter.toMap(String(decryptedPart.content, Charsets.UTF_8))
        assertTrue(1698774495.52 < json["client_created_at"] as Double)
        assertEquals("ABC", json["body"])
        assertEquals("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", json["nonce"])
        assertEquals("abc123", json["token"])

        val secondPart = parser.getPartAtIndex(1)!!

        assertEquals(
            "Content-Disposition: form-data;name=\"file[]\";filename=\"dog.jpg\"\r\n" +
                "Content-Type: application/octet-stream\r\n",
            secondPart.multipartHeaders
        )

        val decryptedContent2 = AESEncryption23(conversationCredential.payloadEncryptionKey!!).decryptPayloadData(secondPart.content)
        val decryptedPart2 = MultipartParser.parsePart(ByteArrayInputStream(decryptedContent2), 0L..decryptedContent2.size + 2) // TODO: Why do we have to add 2 here?

        assertEquals(
            "Content-Disposition: form-data;name=\"file[]\";filename=\"dog.jpg\"\r\n" +
                "Content-Type: image/jpeg\r\n",
            decryptedPart2!!.multipartHeaders
        )

        assertTrue(decryptedPart2.content.isNotEmpty())
    }
}
