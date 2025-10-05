package net.forestany.simplechat.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.forestany.simplechat.MainActivity
import net.forestany.simplechat.R

class ChatViewAdapter(
    private val context: Context,
    private var messages: List<Message>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TAG = "ChatViewAdapter"
        private const val VIEW_TYPE_RECEIVED = 1
        private const val VIEW_TYPE_SENDED = 2
        private const val VIEW_TYPE_STATUS = 3
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            VIEW_TYPE_RECEIVED -> {
                return ViewHolderReceived(
                    LayoutInflater.from(context).inflate(R.layout.chat_received, parent, false)
                )
            }

            VIEW_TYPE_SENDED -> {
                return ViewHolderSended(
                    LayoutInflater.from(context).inflate(R.layout.chat_sended, parent, false)
                )
            }

            else -> {
                return ViewHolderStatus(
                    LayoutInflater.from(context).inflate(R.layout.chat_status, parent, false)
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun getItemViewType(position: Int): Int {
        if (position < messages.size) {
            val o_message: Message = messages[position]

            if (o_message.message.startsWith("%")) {
                return VIEW_TYPE_STATUS
            }

            if (o_message.received) {
                return VIEW_TYPE_RECEIVED
            }
        }

        return VIEW_TYPE_SENDED
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position < messages.size) {
            when (holder.itemViewType) {
                VIEW_TYPE_RECEIVED -> {
                    (holder as ViewHolderReceived).bind(position, messages[position])
                }

                VIEW_TYPE_SENDED -> {
                    (holder as ViewHolderSended).bind(position, messages[position])
                }

                VIEW_TYPE_STATUS -> {
                    (holder as ViewHolderStatus).bind(position, messages[position])
                }
            }
        } else {
            when (holder.itemViewType) {
                VIEW_TYPE_RECEIVED -> {
                    (holder as ViewHolderReceived).bind()
                }

                VIEW_TYPE_SENDED -> {
                    (holder as ViewHolderSended).bind()
                }

                VIEW_TYPE_STATUS -> {
                    (holder as ViewHolderStatus).bind()
                }
            }
        }
    }

    fun addData(p_o_message: Message) {
        messages = messages.plus(p_o_message)
        notifyItemInserted(if (messages.isEmpty()) 1 else messages.size)
    }

    fun clearWaitMessage() {
        var b_clearWaitMessage = false

        if (messages.isNotEmpty()) {
            for (message in messages) {
                if (message.message == MainActivity.CHAT_WAIT_MESSAGE) {
                    b_clearWaitMessage = true
                }
            }

            if (b_clearWaitMessage) {
                val a_foo: List<Message> = messages.subList(1, messages.size)
                messages = a_foo
            }
        }
    }

    fun clearMessageList() {
        val a_foo: List<Message> = listOf()
        messages = a_foo
        notifyItemRangeChanged(0, 0)
    }

    inner class ViewHolderReceived(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tV_userReceived = itemView.findViewById<TextView>(R.id.user_received)
        private val tV_textReceived = itemView.findViewById<TextView>(R.id.text_received)
        private val tV_timeReceived = itemView.findViewById<TextView>(R.id.time_received)

        fun bind(id: Int, p_o_message: Message) {
            tV_userReceived.text = p_o_message.user
            tV_textReceived.text = p_o_message.message
            tV_timeReceived.text = java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(p_o_message.timestamp)
        }

        fun bind() {

        }
    }

    inner class ViewHolderSended(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tV_userSended = itemView.findViewById<TextView>(R.id.user_sended)
        private val tV_textSended = itemView.findViewById<TextView>(R.id.text_sended)
        private val tV_timeSended = itemView.findViewById<TextView>(R.id.time_sended)

        fun bind(p_i_id: Int, p_o_message: Message) {
            tV_userSended.text = p_o_message.user
            tV_textSended.text = p_o_message.message
            tV_timeSended.text = java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(p_o_message.timestamp)
        }

        fun bind() {

        }
    }

    inner class ViewHolderStatus(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tV_textStatus = itemView.findViewById<TextView>(R.id.text_status)

        fun bind(p_i_id: Int, p_o_message: Message) {
            var s_foo = ""

            if (p_o_message.message == MainActivity.CHAT_WAIT_MESSAGE) {
                s_foo = itemView.context.getString(R.string.chat_waiting)
            }

            if (p_o_message.message == MainActivity.CHAT_EXIT_MESSAGE) {
                s_foo = itemView.context.getString(R.string.chat_exit, p_o_message.user)
            }

            if (p_o_message.message == MainActivity.CHAT_LOST_MESSAGE) {
                s_foo = itemView.context.getString(R.string.chat_lost, p_o_message.user)
            }

            tV_textStatus.text = s_foo
        }

        fun bind() {

        }
    }
}