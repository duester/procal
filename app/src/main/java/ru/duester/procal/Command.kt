package ru.duester.procal

sealed class Command {
    abstract val id: Long
    abstract val functionId: Long
    abstract val position: Int

    data class PushNumber(
        override val id: Long,
        override val functionId: Long,
        override val position: Int,
        val value: Double
    ) : Command()

    data class Add(
        override val id: Long,
        override val functionId: Long,
        override val position: Int
    ) : Command()

    data class Subtract(
        override val id: Long,
        override val functionId: Long,
        override val position: Int
    ) : Command()

    data class Multiply(
        override val id: Long,
        override val functionId: Long,
        override val position: Int
    ) : Command()

    data class Divide(
        override val id: Long,
        override val functionId: Long,
        override val position: Int
    ) : Command()

    data class Sin(
        override val id: Long,
        override val functionId: Long,
        override val position: Int
    ) : Command()

    data class Cos(
        override val id: Long,
        override val functionId: Long,
        override val position: Int
    ) : Command()

    data class Tan(
        override val id: Long,
        override val functionId: Long,
        override val position: Int
    ) : Command()

    data class Duplicate(
        override val id: Long,
        override val functionId: Long,
        override val position: Int
    ) : Command()

    data class Input(
        override val id: Long,
        override val functionId: Long,
        override val position: Int,
        val prompt: String
    ) : Command()

    data class Label(
        override val id: Long,
        override val functionId: Long,
        override val position: Int,
        val name: String
    ) : Command()

    data class Goto(
        override val id: Long,
        override val functionId: Long,
        override val position: Int,
        val labelName: String
    ) : Command()

    data class CallFunction(
        override val id: Long,
        override val functionId: Long,
        override val position: Int,
        val functionName: String
    ) : Command()

    companion object {
        fun fromType(
            type: String,
            id: Long,
            functionId: Long,
            position: Int,
            arg: String? = null
        ): Command {
            return when (type) {
                "PUSH" -> PushNumber(id, functionId, position, arg?.toDouble() ?: 0.0)
                "ADD" -> Add(id, functionId, position)
                "SUB" -> Subtract(id, functionId, position)
                "MUL" -> Multiply(id, functionId, position)
                "DIV" -> Divide(id, functionId, position)
                "SIN" -> Sin(id, functionId, position)
                "COS" -> Cos(id, functionId, position)
                "TAN" -> Tan(id, functionId, position)
                "DUP" -> Duplicate(id, functionId, position)
                "INPUT" -> Input(id, functionId, position, arg ?: "")
                "LABEL" -> Label(id, functionId, position, arg ?: "")
                "GOTO" -> Goto(id, functionId, position, arg ?: "")
                "CALL" -> CallFunction(id, functionId, position, arg ?: "")
                else -> throw IllegalArgumentException("Unknown command type: $type")
            }
        }
    }
}