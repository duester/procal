package ru.duester.procal.views

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ru.duester.procal.Command
import ru.duester.procal.DatabaseHelper
import ru.duester.procal.R
import ru.duester.procal.databinding.ActivityFunctionEditorBinding

class FunctionEditorActivity : AppCompatActivity(), CommandListAdapter.CommandClickListener {
    private lateinit var binding: ActivityFunctionEditorBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var database: SQLiteDatabase
    private lateinit var adapter: CommandListAdapter
    private var programId: Long = -1
    private var functionId: Long = -1
    private var functionName: String = ""
    private var isMainFunction: Boolean = false
    private val commands = mutableListOf<Command>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFunctionEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        programId = intent.getLongExtra("program_id", -1)
        functionName = intent.getStringExtra("function_name") ?: "main"
        isMainFunction = functionName == "main"
        dbHelper = DatabaseHelper(this)
        database = dbHelper.writableDatabase

        setupToolbar()
        setupRecyclerView()
        loadFunction()
        loadCommands()
        binding.fabAddCommand.setOnClickListener {
            showAddCommandDialog()
        }
    }

    private fun setupToolbar() {
        //setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = functionName
    }

    private fun setupRecyclerView() {
        adapter = CommandListAdapter(commands, this)
        binding.recyclerViewCommands.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewCommands.adapter = adapter
    }

    private fun loadFunction() {
        val cursor = database.query(
            DatabaseHelper.TABLE_FUNCTIONS,
            arrayOf(DatabaseHelper.COLUMN_FUNCTION_ID),
            "${DatabaseHelper.COLUMN_FUNCTION_PROGRAM_ID} = ? and ${DatabaseHelper.COLUMN_FUNCTION_NAME} = ?",
            arrayOf(programId.toString(), functionName),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                functionId = it.getLong(0)
            } else {
                val values = ContentValues().apply {
                    put(DatabaseHelper.COLUMN_FUNCTION_PROGRAM_ID, programId)
                    put(DatabaseHelper.COLUMN_FUNCTION_NAME, functionName)
                    put(DatabaseHelper.COLUMN_FUNCTION_IS_MAIN, if (isMainFunction) 1 else 0)
                }
                functionId = database.insert(DatabaseHelper.TABLE_FUNCTIONS, null, values)
                if (functionId == -1L) {
                    Toast.makeText(this, R.string.function_edit_failed, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadCommands() {
        commands.clear()
        val cursor = database.query(
            DatabaseHelper.TABLE_COMMANDS,
            arrayOf(
                DatabaseHelper.COLUMN_COMMAND_ID,
                DatabaseHelper.COLUMN_COMMAND_TYPE,
                DatabaseHelper.COLUMN_COMMAND_ARG
            ),
            "${DatabaseHelper.COLUMN_COMMAND_FUNCTION_ID} = ?",
            arrayOf(functionId.toString()),
            null, null,
            "${DatabaseHelper.COLUMN_COMMAND_POSITION} asc"
        )
        cursor.use {
            var position = 0
            while (it.moveToNext()) {
                val cmd = Command.fromType(
                    type = it.getString(1),
                    id = it.getLong(0),
                    functionId = functionId,
                    position = position++,
                    arg = it.getString(2)
                )
                commands.add(cmd)
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun showAddCommandDialog() {
        val dialog = CommandEditorDialog { type, arg ->
            addCommand(type, arg)
        }
        dialog.show(supportFragmentManager, "CommandEditorDialog")
    }

    private fun addCommand(type: String, arg: String?) {
        val position = commands.size
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_COMMAND_FUNCTION_ID, functionId)
            put(DatabaseHelper.COLUMN_COMMAND_POSITION, position)
            put(DatabaseHelper.COLUMN_COMMAND_TYPE, type)
            arg?.let { put(DatabaseHelper.COLUMN_COMMAND_ARG, arg) }
        }
        val id = database.insert(DatabaseHelper.TABLE_COMMANDS, null, values)
        if (id != -1L) {
            val cmd = Command.fromType(type, id, functionId, position, arg)
            commands.add(cmd)
            adapter.notifyItemInserted(position)
        } else {
            Toast.makeText(this, R.string.function_add_command_failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCommandClick(command: Command) {
        showCommandOptionsDialog(command)
    }

    private fun showCommandOptionsDialog(command: Command) {
        val options = arrayOf(
            getString(R.string.function_command_edit),
            getString(R.string.function_command_delete),
            getString(R.string.function_command_move_up),
            getString(R.string.function_command_move_down)
        )
        AlertDialog.Builder(this)
            .setTitle(command.toString())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editCommand(command)
                    1 -> deleteCommand(command)
                    2 -> moveCommandUp(command)
                    3 -> moveCommandDown(command)
                }
            }
            .show()
    }

    private fun editCommand(command: Command) {
        val dialog = CommandEditorDialog(command) { type, arg ->
            updateCommand(command, type, arg)
        }
        dialog.show(supportFragmentManager, "CommandEditorDialog")
    }

    private fun updateCommand(oldCommand: Command, type: String, arg: String?) {
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_COMMAND_TYPE, type)
            if (arg != null) {
                put(DatabaseHelper.COLUMN_COMMAND_ARG, arg)
            } else {
                putNull(DatabaseHelper.COLUMN_COMMAND_ARG)
            }
        }
        val rows = database.update(
            DatabaseHelper.TABLE_COMMANDS,
            values,
            "${DatabaseHelper.COLUMN_COMMAND_ID} = ?",
            arrayOf(oldCommand.id.toString())
        )
        if (rows > 0) {
            val newCommand = Command.fromType(
                type,
                oldCommand.id,
                oldCommand.functionId,
                oldCommand.position,
                arg
            )
            val index = commands.indexOfFirst { it.id == oldCommand.id }
            if (index != -1) {
                commands[index] = newCommand
                adapter.notifyItemChanged(index)
            }
        } else {
            Toast.makeText(this, R.string.function_update_command_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteCommand(command: Command) {
        val position = command.position
        database.delete(
            DatabaseHelper.TABLE_COMMANDS,
            "${DatabaseHelper.COLUMN_COMMAND_ID} = ?",
            arrayOf(command.id.toString())
        )
        val updateValues = ContentValues().apply {
            put(DatabaseHelper.COLUMN_COMMAND_POSITION, position - 1)
        }
        database.update(
            DatabaseHelper.TABLE_COMMANDS,
            updateValues,
            "${DatabaseHelper.COLUMN_COMMAND_FUNCTION_ID} = ? " +
                    "and ${DatabaseHelper.COLUMN_COMMAND_POSITION} > ?",
            arrayOf(functionId.toString(), position.toString())
        )
        commands.removeAt(position)
        loadCommands()
    }

    private fun moveCommandUp(command: Command) {
        if (command.position == 0) return
        database.beginTransaction()
        try {
            val prevPos = command.position - 1
            val prevCmd = commands[prevPos]
            val values1 = ContentValues().apply {
                put(DatabaseHelper.COLUMN_COMMAND_POSITION, prevPos)
            }
            database.update(
                DatabaseHelper.TABLE_COMMANDS,
                values1,
                "${DatabaseHelper.COLUMN_COMMAND_ID} = ?",
                arrayOf(command.id.toString())
            )
            val values2 = ContentValues().apply {
                put(DatabaseHelper.COLUMN_COMMAND_POSITION, command.position)
            }
            database.update(
                DatabaseHelper.TABLE_COMMANDS,
                values2,
                "${DatabaseHelper.COLUMN_COMMAND_ID} = ?",
                arrayOf(prevCmd.id.toString())
            )

            database.setTransactionSuccessful()
            loadCommands()
        } finally {
            database.endTransaction()
        }
    }

    private fun moveCommandDown(command: Command) {
        if (command.position == commands.size - 1) return
        database.beginTransaction()
        try {
            val nextPos = command.position + 1
            val nextCmd = commands[nextPos]
            val values1 = ContentValues().apply {
                put(DatabaseHelper.COLUMN_COMMAND_POSITION, nextPos)
            }
            database.update(
                DatabaseHelper.TABLE_COMMANDS,
                values1,
                "${DatabaseHelper.COLUMN_COMMAND_ID} = ?",
                arrayOf(command.id.toString())
            )
            val values2 = ContentValues().apply {
                put(DatabaseHelper.COLUMN_COMMAND_POSITION, command.position)
            }
            database.update(
                DatabaseHelper.TABLE_COMMANDS,
                values2,
                "${DatabaseHelper.COLUMN_COMMAND_ID} = ?",
                arrayOf(nextCmd.id.toString())
            )

            database.setTransactionSuccessful()
            loadCommands()
        } finally {
            database.endTransaction()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_function_editor, menu)
        /*if (isMainFunction) {
            menu.findItem(R.id.action_export_function).isVisible = false
        }*/
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_function -> {
                showAddFunctionDialog()
                true
            }
            /*R.id.action_export_function -> {
                exportCurrentFunction()
                true
            }*/
            R.id.action_settings -> {
                showSettings()
                true
            }
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAddFunctionDialog() {
        val dialog = FunctionEditorDialog { name/*, isExported*/ ->
            addFunction(name/*, isExported*/)
        }
        dialog.show(supportFragmentManager, "FunctionEditorDialog")
    }

    private fun addFunction(name: String/*, isExported: Boolean*/) {
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_FUNCTION_PROGRAM_ID, programId)
            put(DatabaseHelper.COLUMN_FUNCTION_NAME, name)
            //put(DatabaseHelper.COLUMN_FUNCTION_IS_EXPORTED, if (isExported) 1 else 0)
            put(DatabaseHelper.COLUMN_FUNCTION_IS_MAIN, 0)
        }
        val id = database.insert(DatabaseHelper.TABLE_FUNCTIONS, null, values)
        if (id != -1L) {
            Toast.makeText(this, R.string.function_new_created, Toast.LENGTH_SHORT).show()
            // loadFunctions()
        } else {
            Toast.makeText(this, getString(R.string.function_new_failed), Toast.LENGTH_SHORT).show()
        }
    }

    /*private fun exportCurrentFunction() {
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_FUNCTION_IS_EXPORTED, 1)
        }
        database.update(
            DatabaseHelper.TABLE_FUNCTIONS,
            values,
            "${DatabaseHelper.COLUMN_FUNCTION_ID} = ?",
            arrayOf(functionId.toString())
        )
        Toast.makeText(this, R.string.function_exported, Toast.LENGTH_SHORT).show()
    }*/

    private fun showSettings() {
        AlertDialog.Builder(this)
            .setTitle(R.string.function_settings)
            .setMessage(getString(R.string.function_settings_message))
            .setPositiveButton(getString(R.string.function_settings_ok), null)
            .show()
    }
}