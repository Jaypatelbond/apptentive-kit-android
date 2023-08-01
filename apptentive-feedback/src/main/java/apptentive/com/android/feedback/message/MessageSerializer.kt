package apptentive.com.android.feedback.message

import androidx.core.util.AtomicFile
import apptentive.com.android.core.DependencyProvider
import apptentive.com.android.encryption.Encryption
import apptentive.com.android.feedback.conversation.ConversationRoster
import apptentive.com.android.feedback.utils.FileStorageUtils
import apptentive.com.android.feedback.utils.FileUtil
import apptentive.com.android.platform.AndroidSharedPrefDataStore
import apptentive.com.android.platform.SharedPrefConstants
import apptentive.com.android.serialization.BinaryDecoder
import apptentive.com.android.serialization.BinaryEncoder
import apptentive.com.android.serialization.Decoder
import apptentive.com.android.serialization.Encoder
import apptentive.com.android.serialization.TypeSerializer
import apptentive.com.android.serialization.decodeList
import apptentive.com.android.serialization.decodeNullableString
import apptentive.com.android.serialization.encodeList
import apptentive.com.android.serialization.encodeNullableString
import apptentive.com.android.util.Log
import apptentive.com.android.util.LogTags
import apptentive.com.android.util.LogTags.MESSAGE_CENTER
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream

internal interface MessageSerializer {
    @Throws(MessageSerializerException::class)
    fun loadMessages(conversationRoster: ConversationRoster): List<DefaultMessageRepository.MessageEntry>

    @Throws(MessageSerializerException::class)
    fun saveMessages(messages: List<DefaultMessageRepository.MessageEntry>, conversationRoster: ConversationRoster)

    fun deleteAllMessages()

    fun setMessageFile(file: File)
}

internal class DefaultMessageSerializer(val encryption: Encryption) : MessageSerializer {

    private lateinit var messagesFile: File
    override fun loadMessages(conversationRoster: ConversationRoster): List<DefaultMessageRepository.MessageEntry> {
        setMessagesFileFromRoster(conversationRoster)
        return if (messagesFile.exists()) {
            Log.d(MESSAGE_CENTER, "Loading messages from MessagesFile")
            readMessageEntries()
        } else {
            Log.d(MESSAGE_CENTER, "MessagesFile doesn't exist")
            listOf()
        }
    }

    private fun setMessageFileFromRoster(roster: ConversationRoster) {
        Log.d(LogTags.CONVERSATION, "Setting message file from roster: $roster")
        roster.activeConversation?.let { activeConversation ->
            messagesFile = FileStorageUtils.getMessagesFileForActiveUser(activeConversation.path)
            Log.d(LogTags.CONVERSATION, "Using conversation file: $messagesFile")
        }
    }

    override fun saveMessages(messages: List<DefaultMessageRepository.MessageEntry>, conversationRoster: ConversationRoster) {
        setMessageFileFromRoster(conversationRoster)
        val start = System.currentTimeMillis()
        val atomicFile = AtomicFile(messagesFile)
        val stream = atomicFile.startWrite()
        val byteArrayOutputStream = ByteArrayOutputStream()
        try {
            val encoder = BinaryEncoder(DataOutputStream(byteArrayOutputStream))
            messageSerializer.encode(encoder, messages)
            val encryptedBytes = encryption.encrypt(byteArrayOutputStream.toByteArray())
            stream.use {
                stream.write(encryptedBytes)
                atomicFile.finishWrite(stream)
            }
        } catch (e: Exception) {
            atomicFile.failWrite(stream)
            throw MessageSerializerException("Unable to save messages", e)
        }

        Log.v(LogTags.CONVERSATION, "Messages saved (took ${System.currentTimeMillis() - start} ms)")
    }

    override fun deleteAllMessages() {
        FileUtil.deleteFile(messagesFile.path)
        Log.w(LogTags.CRYPTOGRAPHY, "Message cache is deleted to support the new encryption setting")
    }

    override fun setMessageFile(file: File) {
        messagesFile = file
    }

    private fun setMessagesFileFromRoster(roster: ConversationRoster) {
        val cachedSDKVersion = DependencyProvider.of<AndroidSharedPrefDataStore>()
            .getString(SharedPrefConstants.SDK_CORE_INFO, SharedPrefConstants.SDK_VERSION).ifEmpty { null }

        // Use the old messages.bin file for older SDKs < 6.2.0
        // SDK_VERSION is added in 6.1.0. It would be null for the SDKs < 6.1.0

        if (FileUtil.containsFiles(FileStorageUtils.CONVERSATION_DIR) &&
            cachedSDKVersion == null || cachedSDKVersion == "6.1.0"
        ) {
            messagesFile = FileStorageUtils.getMessagesFile()
        } else {
            roster.activeConversation?.let { activeConversation ->
                messagesFile = FileStorageUtils.getMessagesFileForActiveUser(activeConversation.path)
            }
        }
    }

    private fun readMessageEntries(): List<DefaultMessageRepository.MessageEntry> =
        try {
            val decryptedMessage = encryption.decrypt(FileInputStream(messagesFile))
            val inputStream = ByteArrayInputStream(decryptedMessage)
            val decoder = BinaryDecoder(DataInputStream(inputStream))
            messageSerializer.decode(decoder)
        } catch (e: EOFException) {
            throw MessageSerializerException("Unable to load messages: file corrupted", e)
        } catch (e: Exception) {
            throw MessageSerializerException("Unable to load conversation", e)
        }

    private val messageSerializer: TypeSerializer<List<DefaultMessageRepository.MessageEntry>> by lazy {
        object : TypeSerializer<List<DefaultMessageRepository.MessageEntry>> {
            override fun encode(encoder: Encoder, value: List<DefaultMessageRepository.MessageEntry>) {
                encoder.encodeList(value) { item ->
                    encoder.encodeNullableString(item.id)
                    encoder.encodeDouble(item.createdAt)
                    encoder.encodeString(item.nonce)
                    encoder.encodeString(item.messageState)
                    encoder.encodeString(item.messageJson)
                }
            }

            override fun decode(decoder: Decoder): List<DefaultMessageRepository.MessageEntry> {
                return decoder.decodeList {
                    DefaultMessageRepository.MessageEntry(
                        id = decoder.decodeNullableString(),
                        createdAt = decoder.decodeDouble(),
                        nonce = decoder.decodeString(),
                        messageState = decoder.decodeString(),
                        messageJson = decoder.decodeString()
                    )
                }
            }
        }
    }
}
