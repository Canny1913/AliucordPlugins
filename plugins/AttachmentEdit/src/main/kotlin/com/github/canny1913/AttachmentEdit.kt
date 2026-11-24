/*
 * This file is part of Aliucord, an Android Discord client mod.
 * Copyright (c) 2023 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 */

package com.github.canny1913

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.aliucord.Http
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.before
import com.aliucord.patcher.component1
import com.aliucord.patcher.component2
import com.aliucord.patcher.component3
import com.aliucord.patcher.component4
import com.aliucord.utils.GsonUtils
import com.aliucord.utils.RNSuperProperties
import com.aliucord.utils.SerializedName
import com.aliucord.utils.accessField
import com.discord.api.message.attachment.MessageAttachment
import com.discord.api.sticker.BaseSticker
import com.discord.models.domain.NonceGenerator
import com.discord.restapi.RestAPIParams
import com.discord.stores.StoreMessages
import com.discord.stores.StoreMessagesHolder
import com.discord.stores.StoreStream
import com.discord.utilities.attachments.AttachmentUtilsKt
import com.discord.utilities.rest.SendUtils
import com.discord.utilities.rest.SendUtilsKt
import com.discord.utilities.user.UserUtils
import com.discord.widgets.chat.MessageContent
import com.discord.widgets.chat.MessageManager.AttachmentValidationResult
import com.discord.widgets.chat.MessageManager.AttachmentsRequest
import com.discord.widgets.chat.MessageManager.ContentValidationResult
import com.discord.widgets.chat.MessageManager.`validateMessageContent$default`
import com.discord.widgets.chat.input.ChatInputViewModel
import com.discord.widgets.chat.input.WidgetChatInput
import com.discord.widgets.chat.input.`WidgetChatInput$configureSendListeners$7`
import com.lytefast.flexinput.model.Attachment
import okhttp3.MultipartBody
import rx.Observable
import rx.subjects.BehaviorSubject
import com.discord.api.message.Message as ApiMessage
import com.discord.models.message.Message as ModelMessage
import com.discord.widgets.chat.input.`ChatInputViewModel$sendMessage$onFilesTooLarge$1` as AttachmentTooLargeCallback
import com.discord.widgets.chat.input.`ChatInputViewModel$sendMessage$onMessageTooLong$1` as MessageTooLongCallback
import com.discord.widgets.chat.input.`WidgetChatInput$configureSendListeners$2` as SendMessageFunction
import com.discord.widgets.chat.input.`WidgetChatInput$configureSendListeners$3` as TrySendFunction

@AliucordPlugin
class AttachmentEdit : Plugin() {
    val MessageAttachment.id by accessField<Long>()
    var ModelMessage.nonceField by accessField<String>()
    var ModelMessage.hasLocalUploadsField by accessField<Boolean>()
    var ModelMessage.typeField by accessField<Int>()
    var ModelMessage.attachmentsField by accessField<List<MessageAttachment>>()
    val StoreMessages.holder by accessField<StoreMessagesHolder>()

    @Suppress("PropertyName", "unused")
    private companion object {
        const val DEFAULT_MAX_FILE_SIZE = 20
        var id = 1L

        class InitAttachmentUpload(val files: Array<File>) {
            class File(val filename: String, val file_size: Long, val id: String)
        }

        class InitAttachmentUploadRes(val attachments: Array<File>) {
            class File(val upload_url: String, val upload_filename: String)
        }

        class AttachmentPayload(val id: String, val filename: String?, val uploadedFilename: String?, val isSpoiler: Boolean)

        class PartialMessagePayload(
            @SerializedName("content") val content: String? = null,
            val nonce: String? = null,
            val channelId: String? = null,
            val type: Int? = null,
            val messageReference: RestAPIParams.Message.MessageReference? = null,
            val allowedMentions: RestAPIParams.Message.AllowedMentions? = null,
            @SerializedName("attachments") val attachments: List<AttachmentPayload>? = null,
        )

        class AppendAttachments(val attachments: List<MessageAttachment>) : ChatInputViewModel.Event(null)

    }

    @Suppress("UNCHECKED_CAST")
    override fun start(context: Context) {
        patcher.before<ChatInputViewModel>("handleStoreState",
            ChatInputViewModel.StoreState::class.java) { (_, state: ChatInputViewModel.StoreState) ->
            val loadedState = state as? ChatInputViewModel.StoreState.Loaded ?: return@before
            val previousViewState = viewState as? ChatInputViewModel.ViewState.Loaded ?: return@before
            val editing = loadedState.editingMessage?.message ?: return@before
            val isEditingNewMessage = loadedState.editingMessage != previousViewState.editingMessage
            if (!editing.attachments.isNullOrEmpty() && isEditingNewMessage) {
                val previousAttachments = editing.attachments.sortedBy { it.id }
                val eventSubject = ChatInputViewModel.`access$getEventSubject$p`(this)
                eventSubject.k.onNext(AppendAttachments(previousAttachments))
            }
        }
        patcher.before<WidgetChatInput>("handleEvent", ChatInputViewModel.Event::class.java) { (param, event: ChatInputViewModel.Event) ->
            if (event is AppendAttachments) {
                val flexFragment = WidgetChatInput.`access$getFlexInputFragment$p`(this)
                val flexVm = WidgetChatInput.`access$getFlexInputViewModel$p`(this)
                val mappedAttachments = event.attachments.map {
                    Attachment<Any>(-1, Uri.parse(it.f()), it.a(), it, it.h())
                }
                val selector = flexFragment.b()
                mappedAttachments.forEach { selector.toggleItemInternal(it) }
                flexVm.onAttachmentsUpdated(mappedAttachments)
                param.result = null
            }
        }
        @Suppress("MISSING_DEPENDENCY_CLASS")
        patcher.before<`WidgetChatInput$configureSendListeners$7`>("onSend",
            String::class.javaObjectType,
            List::class.java,
            Function1::class.java) { (param, _: String, rawAttachments: List<Attachment<*>>, onSendResult: Function1<Boolean, Unit>) ->
            if (!`$viewState`.ableToSendMessage || !`$viewState`.isEditing || `$viewState`.isOnCooldown || `$viewState`.selectedThreadDraft != null) return@before
            param.result = null

            val viewModel = WidgetChatInput.`access$getViewModel$p`(`this$0`)
            val meUser = StoreStream.getUsers().me
            val trySend = this.`$trySend$3` as TrySendFunction
            val sendMessage = trySend.`$sendMessage$2` as SendMessageFunction
            val messageManager = sendMessage.`$messageManager`
            val (oldAttachments, newAttachments) = rawAttachments.partition { it.id == -1L }
            val messageContent =
                WidgetChatInput.`access$getAutocomplete$p`(`this$0`).inputContent ?: MessageContent(sendMessage.`$chatInput`.text, emptyList())

            // Validate attachments and message content
            val contentValidation = `validateMessageContent$default`(messageManager,
                messageContent.textContent,
                emptyList<BaseSticker>(),
                meUser,
                false,
                null,
                16,
                null)
            if (contentValidation is ContentValidationResult.MessageTooLong) {
                val onMessageTooLongCallback = MessageTooLongCallback(viewModel)
                onMessageTooLongCallback.invoke(messageContent.textContent.length, contentValidation.maxMessageLength)
                return@before
            } else if (contentValidation is ContentValidationResult.EmptyContent) {
                if (rawAttachments.isEmpty()) {
                    return@before
                }
            }

            val attachmentCtx = viewModel.toAttachmentContext(newAttachments, context)
            val attachmentRequest = AttachmentsRequest(attachmentCtx.currentFileSizeMB, `$viewState`.maxFileSizeMB, attachmentCtx.attachments)
            val attachmentValidation = validateAttachments(context, attachmentRequest)
            if (attachmentValidation is AttachmentValidationResult.FilesTooLarge) {
                val attachmentTooLargeCallback = AttachmentTooLargeCallback(viewModel, attachmentCtx) {}
                attachmentTooLargeCallback.invoke(attachmentRequest.maxFileSizeMB, UserUtils.INSTANCE.isPremium(meUser))
                return@before
            }

            // everything looks good, so clear chat input and attachment view
            Utils.mainThread.post {
                WidgetChatInput.`clearInput$default`(`this$0`, null, false, 3, null)
                onSendResult(true)
            }

            val channelId = `$viewState`.channelId
            val messageId = `$viewState`.editingMessage.message.id
            val oldRawAttachments = oldAttachments.map { att ->
                val data = (att as Attachment<MessageAttachment>).data
                AttachmentPayload(data.id.toString(), null, null, att.spoiler)
            }.sortedBy { it.id }

            var id = oldAttachments.lastOrNull()?.id ?: 0

            Utils.threadPool.execute {
                val storeStream = StoreStream.Companion!!

                // For adding upload progress to chat view
                val fileUploads = newAttachments.map { attachment ->
                    val fileSize = SendUtilsKt.computeFileSizeBytes(attachment.uri, context.contentResolver)
                    val mimeType = AttachmentUtilsKt.getMimeType(attachment, context.contentResolver)
                    val progressObserver = BehaviorSubject.l0(0L)
                    SendUtils.FileUpload(
                        attachment.displayName,
                        fileSize,
                        MultipartBody.Part(null, null, null),
                        mimeType,
                        progressObserver)
                }
                storeStream.dispatcherYesThisIsIntentional.schedule {
                    if (newAttachments.isEmpty()) return@schedule
                    // Upload UI handlers require nonce, and we don't have one, so generate a fresh one
                    val newNonce = NonceGenerator.computeNonce(null).toString()
                    storeStream.messageUploads.bindUpload(newNonce, fileUploads)
                    val newLocalMessage = `$viewState`.editingMessage.message.apply {
                        nonceField = newNonce
                        hasLocalUploadsField = true
                        // local message type
                        typeField = -1
                        // to remove old attachments from ui if unselected
                        attachmentsField = attachments.filter { it.id in oldAttachments.map { old -> old.id } }
                    }
                    storeStream.messages.holder.addMessages(listOf(newLocalMessage))
                }

                // upload all attachments and collect raw attachment payloads
                val newRawAttachments = mutableListOf<AttachmentPayload>()
                newAttachments.forEachIndexed { index, attachment ->
                    val upload = fileUploads[index]
                    val rawAttachment = uploadAttachment(context.contentResolver,
                        ++id,
                        attachment.displayName,
                        upload.contentLength,
                        attachment.uri,
                        attachment.spoiler,
                        channelId,
                        upload.mimeType,
                        upload.bytesWrittenObservable)
                    newRawAttachments += rawAttachment
                }

                val newMessage = Http.Request.newDiscordRNRequest("/channels/$channelId/messages/$messageId", "PATCH")
                    .executeWithJson(GsonUtils.gsonRestApi,
                        PartialMessagePayload(content = messageContent.textContent.trimEnd().takeIf(String::isNotEmpty),
                            attachments = oldRawAttachments + newRawAttachments)).json(GsonUtils.gsonRestApi, ApiMessage::class.java)

                // add new edited message to UI
                storeStream.dispatcherYesThisIsIntentional.schedule {
                    if (newAttachments.isEmpty()) return@schedule
                    storeStream.messages.handleMessageCreate(listOf(newMessage))
                }
            }
        }
    }

    override fun stop(context: Context) {}

    private fun uploadAttachment(
        contentResolver: ContentResolver,
        id: Long = 0L,
        name: String,
        contentLength: Long,
        uri: Uri,
        spoiler: Boolean,
        channelId: Long,
        mimeType: String,
        progressObserver: Observable<Long>? = null
    ): AttachmentPayload {
        val inputStream = contentResolver.openInputStream(uri) ?: throw Exception("Failed to open file")
        inputStream.use { inputStream ->
            val initReq = Http.Request.newDiscordRNRequest("/channels/$channelId/attachments", "POST")
                .executeWithJson(InitAttachmentUpload(arrayOf(InitAttachmentUpload.File(name, contentLength, id.toString()))))
            if (!initReq.ok()) {
                throw Exception("Failed to upload ${initReq.statusCode}: ${initReq.text()}")
            }
            val initRes = initReq.json(InitAttachmentUploadRes::class.java).attachments[0]

            val uploadReq =
                Http.Request(initRes.upload_url, "PUT").setHeader("User-Agent", RNSuperProperties.userAgent).setHeader("Content-Type", mimeType)
                    .setHeader("Content-Length", contentLength.toString())
            uploadReq.conn.doOutput = true
            uploadReq.conn.setFixedLengthStreamingMode(contentLength)

            uploadReq.conn.outputStream.use { outputStream ->
                val progressSubject = progressObserver as? BehaviorSubject<Long>
                var totalBytes: Long = 0
                var currentBytes: Int
                val buffer = ByteArray(8192)
                while (inputStream.read(buffer).also { b -> currentBytes = b } > 0) {
                    outputStream.write(buffer, 0, currentBytes)
                    totalBytes += currentBytes
                    progressSubject?.onNext(totalBytes)
                }
                outputStream.flush()
            }
            uploadReq.execute().run {
                if (!ok()) {
                    throw Exception("Failed to upload ${statusCode}: ${text()}")
                }
            }

            return AttachmentPayload(id.toString(), name, initRes.upload_filename, spoiler)
        }
    }

    fun validateAttachments(context: Context, request: AttachmentsRequest?): AttachmentValidationResult {
        if (request == null) {
            return AttachmentValidationResult.EmptyAttachments.INSTANCE
        }
        val attachments: List<Attachment<*>>? = request.attachments
        if (!attachments.isNullOrEmpty()) {
            for (attachment in attachments) {
                val uri = attachment.uri
                val contentResolver = context.contentResolver
                val size = SendUtilsKt.computeFileSizeMegabytes(uri, contentResolver)
                if (size > request.maxFileSizeMB) {
                    return AttachmentValidationResult.FilesTooLarge(request)
                }
            }
            return AttachmentValidationResult.Success.INSTANCE
        }
        return AttachmentValidationResult.EmptyAttachments.INSTANCE
    }
}
