package ru.duester.procal.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.duester.procal.Command
import ru.duester.procal.R

class CommandListAdapter(
    private val commands: List<Command>,
    private val clickListener: CommandClickListener
) : ListAdapter<Command, CommandListAdapter.CommandViewHolder>(CommandDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommandViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_command, parent, false)
        return CommandViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommandViewHolder, position: Int) {
        holder.bind(commands[position])
    }

    override fun getItemCount(): Int = commands.size

    interface CommandClickListener {
        fun onCommandClick(command: Command)
    }

    inner class CommandViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewPosition: TextView = itemView.findViewById(R.id.textViewCommandPosition)
        private val textViewCommand: TextView = itemView.findViewById(R.id.textViewCommand)

        fun bind(command: Command) {
            textViewPosition.text = "${command.position}."
            textViewCommand.text = when (command) {
                is Command.PushNumber -> "PUSH ${command.value}"
                is Command.Add -> "ADD"
                is Command.Subtract -> "SUB"
                is Command.Multiply -> "MUL"
                is Command.Divide -> "DIV"
                is Command.Sin -> "SIN"
                is Command.Cos -> "COS"
                is Command.Tan -> "TAN"
                is Command.Duplicate -> "DUP"
                is Command.Input -> "INPUT \"${command.prompt}\""
                is Command.Label -> "LABEL ${command.name}"
                is Command.Goto -> "GOTO ${command.labelName}"
                is Command.CallFunction -> "CALL ${command.functionName}"
            }
            itemView.setOnClickListener {
                clickListener.onCommandClick(command)
            }
        }
    }

    private class CommandDiffCallback : DiffUtil.ItemCallback<Command>() {
        override fun areItemsTheSame(oldItem: Command, newItem: Command): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Command, newItem: Command): Boolean {
            return oldItem == newItem
        }

    }
}