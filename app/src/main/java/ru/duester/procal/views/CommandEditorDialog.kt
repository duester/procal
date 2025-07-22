package ru.duester.procal.views

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import ru.duester.procal.Command
import ru.duester.procal.R
import ru.duester.procal.databinding.DialogCommandEditorBinding

class CommandEditorDialog(
    private val command: Command? = null,
    private val onSave: (type: String, arg: String?) -> Unit
) : DialogFragment() {
    private lateinit var binding: DialogCommandEditorBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogCommandEditorBinding.inflate(LayoutInflater.from(context))
        val commandTypes = listOf(
            "PUSH",
            "ADD",
            "SUB",
            "MUL",
            "DIV",
            "SIN",
            "COS",
            "TAN",
            "DUP",
            "INPUT",
            "LABEL",
            "GOTO",
            "CALL"
        )
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            commandTypes
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCommandType.adapter = adapter

        command?.let { cmd ->
            val type = when (cmd) {
                is Command.PushNumber -> "PUSH"
                is Command.Add -> "ADD"
                is Command.Subtract -> "SUB"
                is Command.Multiply -> "MUL"
                is Command.Divide -> "DIV"
                is Command.Sin -> "SIN"
                is Command.Cos -> "COS"
                is Command.Tan -> "TAN"
                is Command.Duplicate -> "DUP"
                is Command.Input -> "INPUT"
                is Command.Label -> "LABEL"
                is Command.Goto -> "GOTO"
                is Command.CallFunction -> "CALL"
            }
            val position = commandTypes.indexOf(type)
            if (position != -1) {
                binding.spinnerCommandType.setSelection(position)
            }
            val arg = when (cmd) {
                is Command.PushNumber -> cmd.value.toString()
                is Command.Input -> cmd.prompt
                is Command.Label -> cmd.name
                is Command.Goto -> cmd.labelName
                is Command.CallFunction -> cmd.functionName
                else -> null
            }
            if (arg != null) {
                binding.editTextArgument.setText(arg)
            }
        }

        binding.spinnerCommandType.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val type = commandTypes[position]
                    when (type) {
                        "PUSH", "INPUT", "LABEL", "GOTO", "CALL" -> {
                            binding.textInputLayoutArgument.visibility = View.VISIBLE
                            binding.editTextArgument.hint = when (type) {
                                "PUSH" -> "Number"
                                "INPUT" -> "Prompt"
                                "LABEL" -> "Label name"
                                "GOTO" -> "Label name"
                                "CALL" -> "Function name"
                                else -> "Argument"
                            }
                        }
                        else -> {
                            binding.textInputLayoutArgument.visibility = View.GONE
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        return AlertDialog.Builder(requireContext())
            .setTitle(if (command == null) R.string.command_edit_new else R.string.command_edit_edit)
            .setView(binding.root)
            .setPositiveButton(R.string.command_edit_save) { _, _ ->
                val type = commandTypes[binding.spinnerCommandType.selectedItemPosition]
                val arg = if (binding.textInputLayoutArgument.visibility == View.VISIBLE) {
                    binding.editTextArgument.text.toString().trim()
                        .takeIf { it.isNotEmpty() }
                } else {
                    null
                }
                onSave(type, arg)
            }
            .setNegativeButton(R.string.command_edit_cancel, null)
            .create()
    }
}