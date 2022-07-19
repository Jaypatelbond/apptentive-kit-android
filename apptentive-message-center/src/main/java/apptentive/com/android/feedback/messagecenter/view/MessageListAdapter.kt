package apptentive.com.android.feedback.messagecenter.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import apptentive.com.android.feedback.messagecenter.R
import apptentive.com.android.feedback.model.Message
import apptentive.com.android.feedback.utils.convertToDate
import com.google.android.material.textview.MaterialTextView

class MessageListAdapter() : ListAdapter<Message, MessageViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.apptentive_item_message_bubble, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = getItem(position)
        with(holder.itemView) {
            val groupTimestamp = findViewById<MaterialTextView>(R.id.apptentive_message_group_time_stamp)
            val inboundLayout = findViewById<ConstraintLayout>(R.id.apptentive_message_inbound)
            val outboundLayout = findViewById<ConstraintLayout>(R.id.apptentive_message_outbound)
            val inboundText = findViewById<MaterialTextView>(R.id.apptentive_message_inbound_text)
            val inboundStatus = findViewById<MaterialTextView>(R.id.apptentive_message_inbound_time_stamp)
            val outboundText = findViewById<MaterialTextView>(R.id.apptentive_message_outbound_text)
            val outboundStatus = findViewById<MaterialTextView>(R.id.apptentive_message_outbound_time_stamp)

            groupTimestamp.isVisible = message.groupTimestamp != null
            groupTimestamp.text = message.groupTimestamp

            if (message.inbound) { // Message from US to the BACKEND
                inboundLayout.visibility = View.VISIBLE
                outboundLayout.visibility = View.GONE
                inboundText.text = message.body
                // TODO don't want to introduce a string file temporarily. Message status should be fetched from manifest!
                val status = if (message.messageStatus == Message.Status.Saved) Message.Status.Sent else message.messageStatus
                inboundStatus.text = "$status \u2022 ${convertToDate(message.createdAt)}"
            } else { // Message from the BACKEND to US
                inboundLayout.visibility = View.GONE
                outboundLayout.visibility = View.VISIBLE
                outboundText.text = message.body
                outboundStatus.text = convertToDate(message.createdAt)
            }
        }
    }

    override fun getItemCount(): Int = currentList.size

    private class DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Message, newItem: Message) =
            oldItem == newItem
    }
}

class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view)