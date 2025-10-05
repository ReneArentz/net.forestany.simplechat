package net.forestany.simplechat

import java.time.LocalDateTime
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import net.forestany.forestj.lib.net.sock.com.Communication
import net.forestany.simplechat.chat.Message

class GlobalInstance {
    companion object {
        @Volatile
        private var instance: GlobalInstance? = null

        fun get(): GlobalInstance {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = GlobalInstance()
                    }
                }
            }

            return instance!!
        }
    }

    var o_threadLobby: Thread? = null
    var o_threadChat: Thread? = null

    var o_communicationLobby: Communication? = null
    var o_communicationChat: Communication? = null

    var o_lastPing: LocalDateTime? = null
    var b_isServer: Boolean = false
    var b_connected: Boolean = false
    var s_user: String = "NO_USER_SPECIFIED"

    private var o_lockLobbyEntries = ReentrantLock()
    private var m_clientLobbyEntries: MutableMap<LocalDateTime, String> = HashMap()

    fun getClientLobbyEntries(): MutableMap<LocalDateTime, String> {
        var m_foo: MutableMap<LocalDateTime, String>

        o_lockLobbyEntries.withLock {
            m_foo = m_clientLobbyEntries.toMutableMap()
        }

        return m_foo
    }

    fun addClientLobbyEntry(p_o_value: LocalDateTime, p_s_value: String) {
        o_lockLobbyEntries.withLock {
            m_clientLobbyEntries.put(p_o_value, p_s_value)
        }
    }

    fun removeClientLobbyEntry(p_o_value: LocalDateTime) {
        o_lockLobbyEntries.withLock {
            m_clientLobbyEntries.remove(p_o_value)
        }
    }

    fun removeClientLobbyEntryByValue(p_s_value: String) {
        o_lockLobbyEntries.withLock {
            if (m_clientLobbyEntries.containsValue(p_s_value)) {
                var o_key: LocalDateTime? = null

                for ((key, value) in m_clientLobbyEntries) {
                    if (value.contentEquals(p_s_value)) {
                        o_key = key
                    }
                }

                if (o_key != null) {
                    m_clientLobbyEntries.remove(o_key)
                }
            }
        }
    }

    fun clearClientLobbyEntries() {
        o_lockLobbyEntries.withLock {
            m_clientLobbyEntries.clear()
        }
    }

    private var o_lockChatQueue = ReentrantLock()
    private var q_chatQueue: Queue<Message> = LinkedList()

    fun addChatMessageToQueue(p_o_value: Message) {
        o_lockChatQueue.withLock {
            q_chatQueue.add(p_o_value)
        }
    }

    fun getChatMessageFromQueue(): Message? {
        var o_foo: Message? = null

        o_lockChatQueue.withLock {
            if (q_chatQueue.size > 0) {
                o_foo = q_chatQueue.remove()
            }
        }

        return o_foo
    }

    fun clearChatMessageQueue() {
        o_lockChatQueue.withLock {
            q_chatQueue.clear()
        }
    }

    private val o_lockPreferences = ReentrantLock()
    private val m_preferences: MutableMap<String, Any?> = HashMap()

    fun getPreferences(): MutableMap<String, Any?> {
        var m_foo: MutableMap<String, Any?>

        o_lockPreferences.withLock {
            m_foo = m_preferences.toMutableMap()
        }

        return m_foo
    }

    fun addPreference(p_s_value: String, p_o_value: Any?) {
        o_lockPreferences.withLock {
            m_preferences.put(p_s_value, p_o_value)
        }
    }

    fun clearPreferences() {
        o_lockPreferences.withLock {
            m_preferences.clear()
        }
    }

    private var o_lockMessageBox = ReentrantLock()
    private var o_messageBox: net.forestany.forestj.lib.net.msg.MessageBox = net.forestany.forestj.lib.net.msg.MessageBox(1, 1500)

    fun enqueueMessageBox(p_o_foo: Any) {
        o_lockMessageBox.withLock {
            o_messageBox.enqueueObject(p_o_foo)
        }
    }

    fun dequeueMessageBox(): Any? {
        var o_foo: Any?

        o_lockMessageBox.withLock {
            o_foo = o_messageBox.dequeueObject()
        }

        return o_foo
    }

    fun getMessageBoxAmount(): Int {
        var i_foo: Int

        o_lockMessageBox.withLock {
            i_foo = o_messageBox.messageAmount
        }

        return i_foo
    }

    fun clearMessageBox() {
        o_lockMessageBox.withLock {
            do {
                o_messageBox.messageAmount
            } while (o_messageBox.dequeueObject() != null)
        }
    }
}