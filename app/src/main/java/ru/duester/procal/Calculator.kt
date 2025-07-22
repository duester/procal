package ru.duester.procal

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

class Calculator {
    private val stack = mutableListOf<Double>()
    private val callStack = mutableListOf<CallFrame>()
    private var currentFunction: String = "main"
    private var currentPosition: Int = 0
    private var labels: Map<String, Int> = mapOf()
    private var functions: Map<String, List<Command>> = mapOf()
    private var isRunning = false
    private var inputCallback: ((String) -> Unit)? = null
    private var inputValue: Double? = null
    private var waitingForInput = false

    data class CallFrame(val functionName: String, val position: Int)

    fun loadProgram(program: List<Function>, commands: List<Command>) {
        functions = program.groupBy { it.name }.mapValues { func ->
            commands.filter { it.functionId == func.value.first().id }.sortedBy { it.position }
        }

        labels = functions.flatMap { (_, cmds) ->
            cmds.filterIsInstance<Command.Label>()
        }.associate { it.name to it.position }
    }

    fun start() {
        if (functions.isEmpty()) throw IllegalStateException("No program loaded")
        if (!functions.containsKey("main")) throw IllegalStateException("No main function found")

        stack.clear()
        callStack.clear()
        currentFunction = "main"
        currentPosition = 0
        isRunning = true
    }

    fun stepInto(): Boolean {
        if (!isRunning) return false
        if (waitingForInput) return true

        val currentCommands = functions[currentFunction] ?: return false
        if (currentPosition >= currentCommands.size) {
            if (callStack.isEmpty()) {
                isRunning = false
                return false
            }
            val frame = callStack.removeAt(callStack.size - 1)
            currentFunction = frame.functionName
            currentPosition = frame.position + 1
            return true
        }

        val command = currentCommands[currentPosition]
        executeCommand(command)

        if (command !is Command.CallFunction || waitingForInput) {
            currentPosition++
        }

        return true
    }

    fun stepOver(): Boolean {
        if (!isRunning) return false
        if (waitingForInput) return true

        val currentCommands = functions[currentFunction] ?: return false
        if (currentPosition >= currentCommands.size) {
            if (callStack.isEmpty()) {
                isRunning = false
                return false
            }
            val frame = callStack.removeAt(callStack.size - 1)
            currentFunction = frame.functionName
            currentPosition = frame.position + 1
            return true
        }

        val command = currentCommands[currentPosition]
        if (command is Command.CallFunction) {
            callStack.add(CallFrame(currentFunction, currentPosition))
            currentFunction = command.functionName
            currentPosition = 0
            return true
        }

        executeCommand(command)
        currentPosition++
        return true
    }

    fun provideInput(value: Double) {
        inputValue = value
        waitingForInput = false
    }

    fun getStack(): List<Double> = stack.toList()
    fun getCurrentFunction(): String = currentFunction
    fun getCurrentPosition(): Int = currentPosition
    fun isWaitingForInput(): Boolean = waitingForInput
    fun isRunning(): Boolean = isRunning

    private fun executeCommand(command: Command) {

        when (command) {
            is Command.PushNumber -> stack.add(command.value)
            is Command.Add -> {
                checkStackSize(2, "ADD")
                val a = pop()
                val b = pop()
                push(a + b)
            }
            is Command.Subtract -> {
                checkStackSize(2, "SUB")
                val a = pop()
                val b = pop()
                push(b - a)
            }
            is Command.Multiply -> {
                checkStackSize(2, "MUL")
                val a = pop()
                val b = pop()
                push(a * b)
            }
            is Command.Divide -> {
                checkStackSize(2, "DIV")
                val a = pop()
                val b = pop()
                if (a == 0.0) throw ArithmeticException("Division by zero")
                push(b / a)
            }
            is Command.Sin -> {
                checkStackSize(1, "SIN")
                val a = pop()
                push(sin(a))
            }
            is Command.Cos -> {
                checkStackSize(1, "COS")
                val a = pop()
                push(cos(a))
            }
            is Command.Tan -> {
                checkStackSize(1, "TAN")
                val a = pop()
                push(tan(a))
            }
            is Command.Duplicate -> {
                checkStackSize(1, "DUP")
                val a = pop()
                push(a)
                push(a)
            }
            is Command.Input -> {
                waitingForInput = true
                inputCallback?.invoke(command.prompt)
            }
            is Command.Label -> {}
            is Command.Goto -> {
                val targetPosition = labels[command.labelName]
                    ?: throw IllegalArgumentException("Label not found: ${command.labelName}")
                currentPosition = targetPosition - 1 // будет увеличено на 1 после выполнения
            }
            is Command.CallFunction -> {
                if (!functions.containsKey(command.functionName)) {
                    throw IllegalArgumentException("Function not found: ${command.functionName}")
                }
                callStack.add(CallFrame(currentFunction, currentPosition))
                currentFunction = command.functionName
                currentPosition = 0
            }
        }
    }

    fun setInputCallback(callback: (String) -> Unit) {
        inputCallback = callback
    }

    private fun checkStackSize(argumentNumber: Int, command: String) {
        if (stack.size < argumentNumber) {
            throw IllegalStateException("Not enough values on stack for $command")
        }
    }

    private fun pop(): Double = stack.removeAt(stack.size - 1)
    private fun push(value: Double) = stack.add(value)
}