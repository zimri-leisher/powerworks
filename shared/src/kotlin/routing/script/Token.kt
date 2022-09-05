package routing.script

import java.lang.Exception
import java.util.*

class ParseException(val at: IntRange, message: String) : Exception(message)

data class Token2(val type: TokenType2, val value: String)

class Parser(val text: String) {

    lateinit var tokens: Array<Token2>
    var currentIndex = 0
    val currentToken get() = tokens[currentIndex]

    val stack = Stack<Node2>()

    fun tokenize() {
        if (text.isEmpty()) {
            return
        }
        val newTokens = mutableListOf<Token2>()
        var index = 0
        while (index < text.lastIndex) {
            // get all types which match something starting at index
            val matching = TokenType2.ALL
                .map { it to it.match(text, index) }
                .filter { (_, range) -> range.first == index }
                .sortedBy { (type, _) -> type.priority.ordinal }
            if (matching.isEmpty()) {
                handleNoMatchError(index..text.indices.last)
            }
            val (type, range) = matching.first()
            newTokens.add(Token2(type, text.substring(range)))
            index = range.last
        }
    }

    fun eat(type: TokenType2) {
        if (currentToken.type == type) {
            if (currentIndex == tokens.lastIndex) {
                error("Required $type, but end of string was reached")
            }
            currentIndex += 1
        } else {
            error("Required $type, but ${currentToken.type} was found")
        }
    }

    fun nextExpression(): Node2 {
        val nextNode = currentToken.type.parseFrom(this)
        if(currentIndex != tokens.lastIndex) {
            stack.push(nextNode)
            return currentToken.type.parseFrom(this)
        }
        return nextNode
    }

    fun error(at: IntRange, message: String) {
        throw ParseException(at, message)
    }

    fun error(message: String) = error(currentIndex..currentIndex, message)

    private fun handleNoMatchError(range: IntRange) {
        if (text.isEmpty()) {
            error(range, "Statement is empty")
        } else {
            error(range, "Unknown word ${text.substring(range)}")
        }
    }
}

enum class TokenPriority {
    GROUPING,
    LITERAL,
    OPERATOR
}

sealed class TokenType2(val niceName: String, val priority: TokenPriority) {

    init {
        ALL.add(this)
    }

    abstract fun match(text: String, index: Int): IntRange
    abstract fun parseFrom(parser: Parser): Node2

    protected fun matchRegex(regex: Regex, text: String, index: Int): IntRange {
        return regex.matchAt(text, index)?.range ?: IntRange(-1, -1)
    }

    sealed class OpUnaryPrefix(val argType: Type<*>, val retType: Type<*>, niceName: String) :
        TokenType2(niceName, TokenPriority.OPERATOR) {
        final override fun parseFrom(parser: Parser): Node2 {
            parser.eat(this)
            val arg = parser.nextExpression()
            if (arg.type != argType) {
                parser.error("$argType must come after the `$niceName` operator, got ${arg.type} instead")
            }
            val ret = parseOpUnaryPrefix(arg)
            if (ret.type != retType) {
                parser.error("$niceName returned ${ret.type}, expected it to return $retType")
            }
            return ret
        }

        abstract fun parseOpUnaryPrefix(arg: Node2): Node2
    }

    sealed class OpUnaryPostfix(val argType: Type<*>, val retType: Type<*>, niceName: String) :
        TokenType2(niceName, TokenPriority.OPERATOR) {
        final override fun parseFrom(parser: Parser): Node2 {
            val arg = parser.stack.pop()
            if (arg.type != argType) {
                parser.error("$argType must come before the `$niceName` operator, got ${arg.type} instead")
            }
            val ret = parseOpUnaryPostfix(arg)
            if (ret.type != retType) {
                parser.error("$niceName returned ${ret.type}, expected it to return $retType")
            }
            return ret
        }

        abstract fun parseOpUnaryPostfix(arg: Node2): Node2

    }

    object Print : OpUnaryPostfix(Type.Any, Type.None, "print") {
        override fun match(text: String, index: Int) = matchRegex(Regex("""print"""), text, index)

        override fun parseOpUnaryPostfix(arg: Node2): Node2 {
            return Node2.Print(arg)
        }
    }

    object Not : OpUnaryPrefix(Type.Boolean, Type.Boolean, "not") {
        override fun match(text: String, index: Int) =
            matchRegex(Regex("""(¬)|(!)|(\bl?not\b)|(\\l?not\b)"""), text, index)

        override fun parseOpUnaryPrefix(arg: Node2): Node2 {
            return Node2.Not(arg)
        }
    }

    sealed class Literal(niceName: String) : TokenType2(niceName, TokenPriority.LITERAL) {
        override fun parseFrom(parser: Parser) = getValueFrom(parser.currentToken.value)

        abstract fun getValueFrom(text: String): Node2

        object Boolean : Literal("boolean") {
            override fun match(text: String, index: Int) = matchRegex(Regex("""(\btrue\b)|(\bfalse\b)|(\b([tTfFyYnN])\b)"""), text, index)
            override fun getValueFrom(text: String) = Node2.Literal.Boolean(text.toBoolean())
        }
    }

    object OpenParenthesis : TokenType2("(", TokenPriority.GROUPING) {
        override fun match(text: String, index: Int) = matchRegex(Regex("""\("""), text, index)

        override fun parseFrom(parser: Parser): Node2 {
            parser.eat(this)
            val expression = parser.nextExpression()
            parser.eat(CloseParenthesis)
            return expression
        }
    }

    object CloseParenthesis : TokenType2(")", TokenPriority.GROUPING) {
        override fun match(text: String, index: Int) = matchRegex(Regex("""\)"""), text, index)

        override fun parseFrom(parser: Parser): Node2 {
            parser.error("Unexpected $niceName")
            return Node2.Error
        }
    }

    companion object {
        val ALL = mutableListOf<TokenType2>()
    }
}