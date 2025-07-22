package ru.duester.procal

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ru.duester.procal.databinding.ActivityMainBinding
import ru.duester.procal.views.FunctionEditorActivity
import ru.duester.procal.views.ProgramListAdapter

class MainActivity : AppCompatActivity(), ProgramListAdapter.ProgramClickListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var database: SQLiteDatabase
    private lateinit var adapter: ProgramListAdapter
    private var calculator: Calculator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        database = dbHelper.writableDatabase

        setupRecyclerView()
        loadPrograms()

        binding.fabAddProgram.setOnClickListener {
            showAddProgramDialog()
        }

        binding.btnStepInto.setOnClickListener {
            calculator?.let { calc ->
                try {
                    if (calc.stepInto()) {
                        updateDebuggerView()
                    } else {
                        Toast.makeText(this, R.string.program_finished, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        getString(R.string.program_error, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.btnStepOver.setOnClickListener {
            calculator?.let { calc ->
                try {
                    if (calc.stepOver()) {
                        updateDebuggerView()
                    } else {
                        Toast.makeText(this, R.string.program_finished, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        getString(R.string.program_error, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ProgramListAdapter(this)
        binding.recyclerViewPrograms.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewPrograms.adapter = adapter
    }

    private fun loadPrograms() {
        val programs = mutableListOf<Program>()
        val cursor = database.query(
            DatabaseHelper.TABLE_PROGRAMS,
            arrayOf(
                DatabaseHelper.COLUMN_PROGRAM_ID,
                DatabaseHelper.COLUMN_PROGRAM_NAME,
                DatabaseHelper.COLUMN_PROGRAM_CREATED_AT
            ), null, null, null, null,
            "${DatabaseHelper.COLUMN_PROGRAM_CREATED_AT} desc"
        )
        cursor.use {
            while (it.moveToNext()) {
                programs.add(
                    Program(
                        id = it.getLong(0),
                        name = it.getString(1),
                        createdAt = it.getLong(2)
                    )
                )
            }
        }
        adapter.submitList(programs)
    }

    private fun showAddProgramDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_program_edit, null)
        val editText = dialogView.findViewById<EditText>(R.id.editTextProgramName)
        AlertDialog.Builder(this)
            .setTitle(R.string.program_new)
            .setView(dialogView)
            .setPositiveButton(R.string.program_new_create) { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    addProgram(name)
                } else {
                    Toast.makeText(this, R.string.program_name_empty, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.program_new_cancel, null)
            .show()
    }

    private fun addProgram(name: String) {
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_PROGRAM_NAME, name)
            put(DatabaseHelper.COLUMN_PROGRAM_CREATED_AT, System.currentTimeMillis())
        }
        val id = database.insert(DatabaseHelper.TABLE_PROGRAMS, null, values)
        if (id != -1L) {
            val funcValues = ContentValues().apply {
                put(DatabaseHelper.COLUMN_FUNCTION_PROGRAM_ID, id)
                put(DatabaseHelper.COLUMN_FUNCTION_NAME, "main")
                put(DatabaseHelper.COLUMN_FUNCTION_IS_MAIN, 1)
            }
            database.insert(DatabaseHelper.TABLE_FUNCTIONS, null, funcValues)
            loadPrograms()
            Toast.makeText(this, R.string.program_new_created, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.program_new_failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onProgramClick(program: Program) {
        showProgramOptionsDialog(program)
    }

    private fun showProgramOptionsDialog(program: Program) {
        val options = arrayOf(
            getString(R.string.program_options_edit),
            getString(R.string.program_otions_run),
            getString(R.string.program_otions_delete)
        )
        AlertDialog.Builder(this)
            .setTitle(program.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editProgram(program)
                    1 -> runProgram(program)
                    2 -> deleteProgram(program)
                }
            }
            .show()
    }

    private fun editProgram(program: Program) {
        val intent = Intent(this, FunctionEditorActivity::class.java).apply {
            putExtra("program_id", program.id)
            putExtra("program_name", program.name)
        }
        startActivity(intent)
    }

    private fun runProgram(program: Program) {
        val functions = mutableListOf<Function>()
        val cursorFunc = database.query(
            DatabaseHelper.TABLE_FUNCTIONS,
            arrayOf(
                DatabaseHelper.COLUMN_FUNCTION_ID,
                DatabaseHelper.COLUMN_FUNCTION_NAME,
                DatabaseHelper.COLUMN_FUNCTION_IS_MAIN
            ),
            "${DatabaseHelper.COLUMN_FUNCTION_PROGRAM_ID} = ?",
            arrayOf(program.id.toString()),
            null, null, null
        )
        cursorFunc.use {
            while (it.moveToNext()) {
                functions.add(
                    Function(
                        id = it.getLong(0),
                        programId = program.id,
                        name = it.getString(1),
                        isMain = it.getInt(2) == 1
                    )
                )
            }
        }
        if (functions.isEmpty()) {
            Toast.makeText(this, R.string.program_functions_empty, Toast.LENGTH_SHORT).show()
            return
        }

        val commands = mutableListOf<Command>()
        val cursorCmd = database.query(
            DatabaseHelper.TABLE_COMMANDS,
            arrayOf(
                DatabaseHelper.COLUMN_COMMAND_ID,
                DatabaseHelper.COLUMN_COMMAND_FUNCTION_ID,
                DatabaseHelper.COLUMN_COMMAND_POSITION,
                DatabaseHelper.COLUMN_COMMAND_TYPE,
                DatabaseHelper.COLUMN_COMMAND_ARG
            ),
            "${DatabaseHelper.COLUMN_COMMAND_FUNCTION_ID} IN (${functions.joinToString(", ") { it.id.toString() }})",
            null, null, null,
            "${DatabaseHelper.COLUMN_COMMAND_FUNCTION_ID}, ${DatabaseHelper.COLUMN_COMMAND_POSITION}"
        )
        cursorCmd.use {
            while (it.moveToNext()) {
                commands.add(
                    Command.fromType(
                        type = it.getString(3),
                        id = it.getLong(0),
                        functionId = it.getLong(1),
                        position = it.getInt(2),
                        arg = it.getString(4)
                    )
                )
            }
        }

        calculator = Calculator().apply {
            loadProgram(functions, commands)
            start()
            setInputCallback { prompt ->
                runOnUiThread {
                    showInputDialog(prompt)
                }
            }
        }

        binding.debuggerView.visibility = View.VISIBLE
        updateDebuggerView()
    }

    @SuppressLint("InflateParams")
    private fun showInputDialog(prompt: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_input, null)
        val editText = dialogView.findViewById<EditText>(R.id.editTextInput)
        val textView = dialogView.findViewById<TextView>(R.id.textViewPrompt)
        textView.text = prompt

        AlertDialog.Builder(this)
            .setTitle(R.string.input_title)
            .setView(dialogView)
            .setPositiveButton(R.string.input_ok) { _, _ ->
                try {
                    val value = editText.text.toString().toDouble()
                    calculator?.provideInput(value)
                    updateDebuggerView()
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, R.string.input_invalid_number, Toast.LENGTH_SHORT).show()
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun deleteProgram(program: Program) {
        AlertDialog.Builder(this)
            .setTitle(R.string.program_delete_title)
            .setMessage(getString(R.string.program_delete_message, program.name))
            .setPositiveButton(R.string.program_delete_delete) { _, _ ->
                database.delete(
                    DatabaseHelper.TABLE_PROGRAMS,
                    "${DatabaseHelper.COLUMN_PROGRAM_ID} = ?",
                    arrayOf(program.id.toString())
                )
                loadPrograms()
            }
            .setNegativeButton(R.string.program_delete_cancel, null)
            .show()
    }

    private fun updateDebuggerView() {
        calculator?.let { calc ->
            binding.textViewCurrentFunction.text =
                getString(R.string.program_current_function, calc.getCurrentFunction())
            binding.textViewCurrentPosition.text =
                getString(R.string.program_current_position, calc.getCurrentPosition().toString())
            val stack = calc.getStack()
            binding.textViewStack.text =
                if (stack.isEmpty()) getString(R.string.program_stack_empty)
                else getString(R.string.program_stack, stack.joinToString("\n"))

            if (calc.isWaitingForInput()) {
                binding.textViewStatus.text = getString(
                    R.string.program_status,
                    getString(R.string.program_status_waiting_for_input)
                )
            } else if (calc.isRunning()) {
                binding.textViewStatus.text =
                    getString(R.string.program_status, getString(R.string.program_status_running))
            } else {
                binding.textViewStatus.text =
                    getString(R.string.program_status, getString(R.string.program_status_finished))
            }
        }
    }
}