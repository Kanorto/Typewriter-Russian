package com.typewritermc.engine.paper.interaction

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessage_v1_19_3
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisguisedChat
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.typewritermc.engine.paper.extensions.packetevents.sendPacketTo
import com.typewritermc.engine.paper.plugin
import com.typewritermc.engine.paper.snippets.snippet
import com.typewritermc.engine.paper.utils.plainText
import com.typewritermc.engine.paper.utils.server
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.java.KoinJavaComponent.get
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max
import kotlin.math.min

private val darkenLimit by snippet(
    "chat.darken-limit",
    12,
    "The amount of messages displayed in the chat history during a dialogue"
)
private val spacing by snippet("chat.spacing", 3, "The amount of padding between the dialogue and the chat history")

class ChatHistoryHandler :
    PacketListenerAbstract(PacketListenerPriority.HIGH), Listener {

    fun initialize() {
        PacketEvents.getAPI().eventManager.registerListener(this)
        server.pluginManager.registerSuspendingEvents(this, plugin)
    }

    private val chatIndices = mutableMapOf<UUID, Int>()
    private val histories = mutableMapOf<UUID, ChatHistory>()

    // When the serer sends a message to the player
    override fun onPacketSend(event: PacketSendEvent?) {
        try {
            if (event == null) return
            val message = findMessage(event) ?: return
            val component = message.message
            val history = getHistory(event.user.uuid)
            if (component is TextComponent && component.content().startsWith("no-index")) {
                if (component.content().endsWith("resend")) return

                history.allowedMessageThrough()
                return
            }
            if (component.shouldSaveMessage()) {
                history.addMessage(message)
            }

            if (history.shouldBlockMessage()) {
                event.isCancelled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun findMessage(event: PacketSendEvent): Message? {
        return when (event.packetType) {
            PacketType.Play.Server.CHAT_MESSAGE -> {
                val packet = WrapperPlayServerChatMessage(event)
                val message =
                    packet.message as? ChatMessage_v1_19_3 ?: return Message.TextMessage(packet.message.chatContent)
                val component = message.unsignedChatContent.orElseGet {
                    Component.translatable("chat.type.text", message.chatFormatting.name, message.chatContent)
                }
                Message.PlayerMessage(component, packet)
            }

            PacketType.Play.Server.DISGUISED_CHAT -> {
                val packet = WrapperPlayServerDisguisedChat(event)
                Message.TextMessage(
                    packet.chatFormatting.type.chatDecoration.decorate(
                        packet.message,
                        packet.chatFormatting
                    )
                )
            }

            PacketType.Play.Server.SYSTEM_CHAT_MESSAGE -> {
                val packet = WrapperPlayServerSystemChatMessage(event)
                if (packet.isOverlay) return null
                Message.TextMessage(packet.message)
            }

            else -> null
        }
    }

    @Suppress("RedundantIf")
    fun Component.shouldSaveMessage(): Boolean {
        if (this is TranslatableComponent && key() == "multiplayer.message_not_delivered") {
            return false
        }
        return true
    }

    fun getHistory(pid: UUID): ChatHistory {
        return histories.getOrPut(pid) { ChatHistory() }
    }

    fun getHistory(player: Player): ChatHistory = getHistory(player.uniqueId)

    fun blockMessages(player: Player) {
        getHistory(player).startBlocking()
    }

    fun unblockMessages(player: Player) {
        getHistory(player).stopBlocking()
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        chatIndices.remove(event.player.uniqueId)
        histories.remove(event.player.uniqueId)
    }

    fun shutdown() {
        PacketEvents.getAPI().eventManager.unregisterListener(this)
    }
}

val Player.chatHistory: ChatHistory
    get() = get<ChatHistoryHandler>(ChatHistoryHandler::class.java).getHistory(this)

fun Player.startBlockingMessages() = chatHistory.startBlocking()
fun Player.stopBlockingMessages() = chatHistory.stopBlocking()

class ChatHistory {
    private val messages = ConcurrentLinkedQueue<Message>()
    private var blocking = false
    private var blockingState: BlockingStatus = BlockingStatus.FullBlocking

    private var ignore = false

    fun startBlocking() {
        if (blocking) return
        blockingState = BlockingStatus.PartialBlocking(0)
        blocking = true
    }

    fun stopBlocking() {
        blocking = false
    }

    fun shouldBlockMessage(): Boolean = blocking && !ignore

    internal fun addMessage(message: Message) {
        if (ignore) {
            // If we are ignoring messages, we don't add them to the history.
            return
        }
        if (blocking) {
            blockingState = blockingState.addMessage()
        }
        messages.add(message)
        while (messages.size > 100 && messages.peek().canDelete) {
            messages.poll()
        }
    }

    fun hasMessage(message: Component): Boolean {
        return messages.any { it.message == message }
    }

    fun clear() {
        if (ignore) return
        messages.clear()
    }

    fun allowedMessageThrough() {
        if (ignore) return
        blockingState = BlockingStatus.FullBlocking
    }

    private fun clearMessage() = "\n".repeat(100 - min(messages.size, darkenLimit))

    fun resendMessages(player: Player, clear: Boolean = true) {
        ignore = true
        when (val status = blockingState) {
            is BlockingStatus.FullBlocking -> {
                var msg: Message = Message.TextMessage(Component.text(clearMessage()))
                for (message in messages) {
                    if (message.canMerge) {
                        msg = msg.merge(message)
                        continue
                    }
                    msg.send(player)
                    message.send(player)
                    msg = Message.Empty
                }
                msg.send(player)
            }

            is BlockingStatus.PartialBlocking -> {
                messages.reversed().take(status.newMessages).forEach { it.send(player) }
            }
        }
        blockingState = BlockingStatus.PartialBlocking(0)
        ignore = false
    }

    fun composeDarkMessage(message: Component, clear: Boolean = true): Component {
        // Start with "no-index" to prevent the server from adding the message to the history
        var msg = Component.text("no-index")
        if (clear) msg = msg.append(Component.text(clearMessage()))
        messages.drop(max(0, messages.size - darkenLimit)).take(darkenLimit).forEach {
            msg = msg.append(it.darkenMessage)
        }
        msg = msg.append(Component.text("\n".repeat(spacing)))
        return msg.append(message)
    }

    fun composeEmptyMessage(message: Component, clear: Boolean = true): Component {
        // Start with "no-index" to prevent the server from adding the message to the history
        var msg = Component.text("no-index")
        if (clear) msg = msg.append(Component.text(clearMessage()))
        return msg.append(message)
    }
}

sealed interface BlockingStatus {
    fun addMessage(): BlockingStatus

    // When it only stopped messages from being sent, but not allowed messages to be sent.
    data class PartialBlocking(val newMessages: Int) : BlockingStatus {
        override fun addMessage(): BlockingStatus = copy(newMessages = newMessages + 1)
    }

    // When a message was allowed through.
    data object FullBlocking : BlockingStatus {
        override fun addMessage(): BlockingStatus = this
    }
}

internal interface Message {
    val message: Component
    val darkenMessage: Component
    val canMerge: Boolean
    val canDelete: Boolean

    fun send(player: Player)

    fun merge(other: Message): Message {
        require(canMerge) { "Cannot merge messages that cannot be merged." }
        require(other.canMerge) { "Cannot merge with another message that cannot be merged." }
        return TextMessage(message.append(Component.text("\n")).append(other.message))
    }

    object Empty : Message {
        override val message: Component = Component.empty()
        override val darkenMessage: Component = Component.empty()
        override val canMerge: Boolean = true
        override val canDelete: Boolean = true

        override fun send(player: Player) {
        }
    }

    data class TextMessage(override val message: Component) : Message {
        override val canMerge: Boolean = true
        override val canDelete: Boolean = true

        override val darkenMessage: Component by lazy(LazyThreadSafetyMode.NONE) {
            Component.text("${message.plainText()}\n").color(TextColor.color(0x7d8085))
        }

        override fun send(player: Player) {
            player.sendMessage(message)
        }
    }

    data class PlayerMessage(
        override val message: Component,
        var packet: WrapperPlayServerChatMessage?
    ) : Message {
        override val canMerge: Boolean = packet == null
        override val canDelete: Boolean = packet == null

        override val darkenMessage: Component by lazy(LazyThreadSafetyMode.NONE) {
            Component.text("${message.plainText()}\n").color(TextColor.color(0x7d8085))
        }

        override fun send(player: Player) {
            if (packet != null) {
                packet!!.sendPacketTo(player)
                packet = null // Clear the packet to prevent resending
            } else {
                player.sendMessage(message)
            }
        }
    }
}