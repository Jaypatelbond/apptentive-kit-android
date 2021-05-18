/*
 * Copyright (c) 2017, Apptentive, Inc. All Rights Reserved.
 * Please refer to the LICENSE file for the terms and conditions
 * under which redistribution and use of this file is permitted.
 */

package com.apptentive.android.sdk.conversation;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apptentive.android.sdk.ApptentiveLog;
import com.apptentive.android.sdk.Encryption;
import com.apptentive.android.sdk.Level;
import com.apptentive.android.sdk.encryption.EncryptionFactory;
import com.apptentive.android.sdk.serialization.ObjectSerialization;
import com.apptentive.android.sdk.storage.SerializerException;
import com.apptentive.android.sdk.util.StringUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

import static com.apptentive.android.sdk.ApptentiveLog.hideIfSanitized;
import static com.apptentive.android.sdk.ApptentiveLogTag.CONVERSATION;
import static com.apptentive.android.sdk.conversation.ConversationState.ANONYMOUS;
import static com.apptentive.android.sdk.conversation.ConversationState.ANONYMOUS_PENDING;
import static com.apptentive.android.sdk.conversation.ConversationState.LEGACY_PENDING;
import static com.apptentive.android.sdk.conversation.ConversationState.LOGGED_IN;
import static com.apptentive.android.sdk.util.Constants.CONVERSATION_METADATA_FILE;
import static com.apptentive.android.sdk.util.Constants.CONVERSATION_METADATA_FILE_LEGACY_V1;
import static com.apptentive.android.sdk.util.Constants.PAYLOAD_ENCRYPTION_KEY_TRANSFORMATION;

/**
 * Class responsible for managing conversations.
 * <pre>
 *   - Saving/Loading conversations from/to files.
 *   - Switching conversations when users login/logout.
 *   - Creating anonymous conversation.
 * </pre>
 */
public class ConversationManager {

	private final WeakReference<Context> contextRef;

	/**
	 * A basic directory for storing conversation-related data.
	 */
	private final File conversationsStorageDir;

	/**
	 * An encryption for securing SDK files.
	 */
	private final Encryption encryption;

	/**
	 * Current state of conversation metadata.
	 */
	private ConversationMetadata conversationMetadata;

	private Conversation activeConversation;

	public ConversationManager(@NonNull Context context, @NonNull File conversationsStorageDir, @NonNull Encryption encryption) {
		if (context == null) {
			throw new IllegalArgumentException("Context is null");
		}

		if (conversationsStorageDir == null) {
			throw new IllegalArgumentException("Conversation storage dir is null");
		}

		if (encryption == null) {
			throw new IllegalArgumentException("Encryption is null");
		}

		this.contextRef = new WeakReference<>(context.getApplicationContext());
		this.conversationsStorageDir = conversationsStorageDir;
		this.encryption = encryption;

	}

	//region Conversations

	/**
	 * Attempts to load an active conversation. Returns <code>false</code> if active conversation is
	 * missing or cannot be loaded
	 */
	public boolean loadActiveConversation(Context context) {

		if (context == null) {
			throw new IllegalArgumentException("Context is null");
		}

		try {
			// resolving metadata
			ApptentiveLog.v(CONVERSATION, "Resolving metadata...");
			conversationMetadata = resolveMetadata();
			if (ApptentiveLog.canLog(Level.VERBOSE)) {
				printMetadata(conversationMetadata, "Loaded Metadata");
			}

			if (activeConversation != null) {
				return true;
			}

		} catch (Exception e) {
			ApptentiveLog.e(CONVERSATION, e, "Exception while loading active conversation");

		}

		return false;
	}

	private @Nullable Conversation loadActiveConversationGuarded() throws ConversationLoadException {
		// try to load an active conversation from metadata first
		try {
			if (conversationMetadata.hasItems()) {
				return loadConversationFromMetadata(conversationMetadata);
			}
		} catch (Exception e) {
			ApptentiveLog.e(e, "Exception while loading conversation");

			// do not re-create a conversation if the last loading was unsuccessful
			throw new ConversationLoadException("Unable to load conversation", e);
		}

		return null;
	}

	/**
	 * Attempts to load an existing conversation based on metadata file
	 *
	 * @return <code>null</code> is only logged out conversations available
	 */
	private @Nullable Conversation loadConversationFromMetadata(ConversationMetadata metadata) throws SerializerException, ConversationLoadException {
		// we're going to scan metadata in attempt to find existing conversations
		ConversationMetadataItem item;

		// if the user was logged in previously - we should have an active conversation
		item = metadata.findItem(LOGGED_IN);
		if (item != null) {
			ApptentiveLog.i(CONVERSATION, "Loading 'logged-in' conversation...");
			return loadConversation(item);
		}

		// if no users were logged in previously - we might have an anonymous conversation
		item = metadata.findItem(ANONYMOUS);
		if (item != null) {
			ApptentiveLog.i(CONVERSATION, "Loading 'anonymous' conversation...");
			return loadConversation(item);
		}

		// we only have LOGGED_OUT conversations
		ApptentiveLog.i(CONVERSATION, "No active conversations to load: only 'logged-out' conversations available");
		return null;
	}


	private Conversation loadConversation(ConversationMetadataItem item) throws SerializerException, ConversationLoadException {

		// logged-in conversations should use an encryption key which was received from the backend.
		Encryption conversationEncryption = encryption;
		String payloadEncryptionKey = null;
		if (LOGGED_IN.equals(item.getConversationState())) {
			payloadEncryptionKey = item.getConversationEncryptionKey();
			if (payloadEncryptionKey == null) {
				throw new ConversationLoadException("Missing conversation encryption key");
			}
			conversationEncryption = createPayloadEncryption(payloadEncryptionKey);
		}

		// TODO: use same serialization logic across the project
		final Conversation conversation = new Conversation(item.getDataFile(), item.getMessagesFile(), conversationEncryption, payloadEncryptionKey);
		conversation.setState(item.getConversationState()); // set the state same as the item's state
		conversation.setUserId(item.getUserId());
		conversation.setConversationToken(item.getConversationToken()); // TODO: this would be overwritten by the next call

		conversation.loadConversationData();
		
		// check inconsistency
		conversation.checkInternalConsistency();

		return conversation;
	}

	//endregion

	//region Metadata

	private ConversationMetadata resolveMetadata() throws ConversationMetadataLoadException {

		try {
			// attempt to load the encrypted metadata file
			File metaFile = new File(conversationsStorageDir, CONVERSATION_METADATA_FILE);
			if (metaFile.exists()) {
				ApptentiveLog.v(CONVERSATION, "Loading metadata file: %s", metaFile);
				return ObjectSerialization.deserialize(metaFile, ConversationMetadata.class, encryption);
			}

			// attempt to load the legacy metadata file
			metaFile = new File(conversationsStorageDir, CONVERSATION_METADATA_FILE_LEGACY_V1);
			if (metaFile.exists()) {
				ApptentiveLog.v(CONVERSATION, "Loading legacy v1 metadata file: %s", metaFile);
				try {
					return ObjectSerialization.deserialize(metaFile, ConversationMetadata.class);
				} finally {
					// we need to delete the legacy file to avoid the data being loaded next time
					boolean fileDeleted = metaFile.delete();
					ApptentiveLog.v(CONVERSATION, "Legacy metadata file deleted: %b", fileDeleted);
				}
			}

			ApptentiveLog.v(CONVERSATION, "No metadata files");
		} catch (Exception e) {
			ApptentiveLog.e(CONVERSATION, e, "Exception while loading conversation metadata");

			// if we fail to load the metadata - we would not create a new one - just throw an exception
			throw new ConversationMetadataLoadException("Unable to load metadata", e);
		}

		return new ConversationMetadata();
	}

	//endregion

	//region Debug

	private void printMetadata(ConversationMetadata metadata, String title) {
		List<ConversationMetadataItem> items = metadata.getItems();
		if (items.isEmpty()) {
			ApptentiveLog.v(CONVERSATION, "%s (%d item(s))", title, items.size());
			return;
		}

		Object[][] rows = new Object[1 + items.size()][];
		rows[0] = new Object[]{
			"state",
			"localId",
			"conversationId",
			"userId",
			"dataFile",
			"messagesFile",
			"conversationToken",
			"payloadEncryptionKey"
		};
		int index = 1;
		for (ConversationMetadataItem item : items) {
			rows[index++] = new Object[]{
				item.getConversationState(),
				item.getLocalConversationId(),
				item.getConversationId(),
				item.getUserId(),
				hideIfSanitized(item.getDataFile()),
				hideIfSanitized(item.getMessagesFile()),
				hideIfSanitized(item.getConversationToken()),
				hideIfSanitized(item.getConversationEncryptionKey())
			};
		}

		ApptentiveLog.v(CONVERSATION, "%s (%d item(s))\n%s", title, items.size(), StringUtils.table(rows));
	}

	//endregion

	//region Helpers

	private Encryption createPayloadEncryption(String payloadEncryptionKey) {
		return EncryptionFactory.createEncryption(payloadEncryptionKey, PAYLOAD_ENCRYPTION_KEY_TRANSFORMATION);
	}

	//endregion
}
