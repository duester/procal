package ru.duester.procal.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.duester.procal.Program
import ru.duester.procal.R

class ProgramListAdapter(private val clickListener: ProgramClickListener) :
    ListAdapter<Program, ProgramListAdapter.ProgramViewHolder>(ProgramDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgramViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_program, parent, false)
        return ProgramViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProgramViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    interface ProgramClickListener {
        fun onProgramClick(program: Program)
    }

    inner class ProgramViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewName: TextView = itemView.findViewById(R.id.textViewProgramName)

        fun bind(program: Program) {
            textViewName.text = program.name
            itemView.setOnClickListener {
                clickListener.onProgramClick(program)
            }
        }
    }

    private class ProgramDiffCallback : DiffUtil.ItemCallback<Program>() {
        override fun areItemsTheSame(oldItem: Program, newItem: Program): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Program, newItem: Program): Boolean {
            return oldItem == newItem
        }

    }
}