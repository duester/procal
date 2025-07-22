package ru.duester.procal.views

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import ru.duester.procal.Function
import ru.duester.procal.R
import ru.duester.procal.databinding.DialogFunctionEditBinding

class FunctionEditorDialog(
    private val function: Function? = null,
    private val onSave: (name: String/*, isExported: Boolean*/) -> Unit
) : DialogFragment() {
    private lateinit var binding: DialogFunctionEditBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogFunctionEditBinding.inflate(LayoutInflater.from(context))
        function?.let {
            binding.editTextFunctionName.setText(it.name)
            //binding.checkboxIsExported.isChecked = it.isExported
        }
        return AlertDialog.Builder(requireContext())
            .setTitle(if (function == null) R.string.function_edit_new else R.string.function_edit_edit)
            .setView(binding.root)
            .setPositiveButton(R.string.function_edit_save) { _, _ ->
                val name = binding.editTextFunctionName.text.toString().trim()
                if (name.isNotEmpty()) {
                    onSave(name/*, binding.checkboxIsExported.isChecked*/)
                } else {
                    binding.textInputLayout.error =
                        getString(R.string.function_edit_error_name_empty)
                }
            }
            .setNegativeButton(getString(R.string.function_edit_cancel), null)
            .create()
    }

    override fun onStart() {
        super.onStart()
        binding.editTextFunctionName.requestFocus()
        (dialog as? AlertDialog)?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }
}