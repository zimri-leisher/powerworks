package routing

import resource.ResourceNode
import resource.ResourceType

class EvaluationException(message: String) : Throwable(message)

private enum class TokenType(val regex: Regex, val category: TokenCategory = TokenCategory.OTHER, val precedence: TokenPrecedence = TokenPrecedence.OTHER, val validTopLevelType: Boolean = false) {
    INTEGER(Regex("""\d+"""),
            TokenCategory.LITERAL),
    BOOLEAN(Regex("""true|false"""),
            TokenCategory.LITERAL, validTopLevelType = true),
    RESOURCE_TYPE(Regex(ResourceType.ALL.joinToString(separator = "|") { it.name.replace(" ", "_").toLowerCase() }),
            TokenCategory.LITERAL),
    PLUS(Regex("""\+"""),
            TokenCategory.BINARY_OPERATOR, TokenPrecedence.MEDIUM),
    MINUS(Regex("""-"""),
            TokenCategory.BINARY_OPERATOR, TokenPrecedence.MEDIUM),
    MULTIPLY(Regex("""\*"""),
            TokenCategory.BINARY_OPERATOR, TokenPrecedence.HIGH),
    DIVIDE(Regex("""/"""),
            TokenCategory.BINARY_OPERATOR, TokenPrecedence.HIGH),
    GREATER_THAN(Regex(""">"""),
            TokenCategory.BINARY_OPERATOR, TokenPrecedence.LOW, validTopLevelType = true),
    GREATER_THAN_OR_EQUAL_TO(Regex(""">="""),
            TokenCategory.BINARY_OPERATOR, TokenPrecedence.LOW, validTopLevelType = true),
    LESS_THAN(Regex("""<"""),
            TokenCategory.BINARY_OPERATOR, TokenPrecedence.LOW, validTopLevelType = true),
    LESS_THAN_OR_EQUAL_TO(Regex("""<="""),
            TokenCategory.BINARY_OPERATOR, TokenPrecedence.LOW, validTopLevelType = true),
    EQUALS(Regex("""="""),
            TokenCategory.BINARY_OPERATOR, TokenPrecedence.LOW, validTopLevelType = true),
    CONTAINS(Regex("""contains"""),
            TokenCategory.RIGHT_OPERATOR, TokenPrecedence.MEDIUM, validTopLevelType = true),
    NETWORK_CONTAINS(Regex("""network_contains"""),
            TokenCategory.RIGHT_OPERATOR, TokenPrecedence.MEDIUM, validTopLevelType = true),
    QUANTITY(Regex("""quantity"""),
            TokenCategory.RIGHT_OPERATOR, TokenPrecedence.MEDIUM),
    NETWORK_QUANTITY(Regex("""network_quantity"""),
            TokenCategory.RIGHT_OPERATOR, TokenPrecedence.MEDIUM),
    TOTAL_QUANTITY(Regex("""total_quantity"""),
            TokenCategory.STATEMENT, TokenPrecedence.MEDIUM),
    NETWORK_TOTAL_QUANTITY(Regex("""network_total_quantity"""),
            TokenCategory.STATEMENT, TokenPrecedence.MEDIUM),
    OF(Regex("""of""")),
    LEFT_PARENTHESIS(Regex("""\(""")),
    RIGHT_PARENTHESIS(Regex("""\)"""));
}

private enum class BinaryOperator(val tokenType: TokenType, private val evaluate: (left: String, right: String, context: ResourceNode) -> Any?) {
    PLUS(TokenType.PLUS, { left, right, _ -> left.toInt() + right.toInt() }),
    MINUS(TokenType.MINUS, { left, right, _ -> left.toInt() - right.toInt() }),
    MULTIPLY(TokenType.MULTIPLY, { left, right, _ -> left.toInt() * right.toInt() }),
    DIVIDE(TokenType.DIVIDE, { left, right, _ -> left.toInt() / right.toInt() }),
    GREATER_THAN(TokenType.GREATER_THAN, { left, right, _ -> left.toInt() > right.toInt() }),
    GREATER_THAN_OR_EQUAL_TO(TokenType.GREATER_THAN_OR_EQUAL_TO, { left, right, _ -> left.toInt() >= right.toInt() }),
    LESS_THAN(TokenType.LESS_THAN, { left, right, _ -> left.toInt() < right.toInt() }),
    LESS_THAN_OR_EQUAL_TO(TokenType.LESS_THAN_OR_EQUAL_TO, { left, right, _ -> left.toInt() <= right.toInt() }),
    EQUALS(TokenType.EQUALS, { left, right, _ -> left == right });

    fun evaluate(left: String, right: String, context: ResourceNode): String {
        return evaluate.invoke(left, right, context).toString()
    }
}

private enum class SingleOperator(val tokenType: TokenType, private val evaluate: (arg: String, context: ResourceNode) -> Any?) {
    CONTAINS(TokenType.CONTAINS, { arg, context -> ResourceType.getType(arg.replace("_", " "))?.let { context.attachedContainer.getQuantity(it) >= 1 } }),
    NETWORK_CONTAINS(TokenType.NETWORK_CONTAINS, { arg, context -> ResourceType.getType(arg.replace("_", " "))?.let { context.network.getQuantity(it) >= 1 } }),
    QUANTITY(TokenType.QUANTITY, { arg, context ->
        val type = ResourceType.getType(arg.replace("_", " "))
        if (type != null) context.attachedContainer.getQuantity(type)
        else null
    }),
    NETWORK_QUANTITY(TokenType.NETWORK_QUANTITY, { arg, context -> ResourceType.getType(arg.replace("_", " "))?.let { context.network.getQuantity(it) } });

    fun evaluate(arg: String, context: ResourceNode): String {
        return evaluate.invoke(arg, context).toString()
    }
}

private enum class Statements(val tokenType: TokenType, private val evaluate: (context: ResourceNode) -> Any?) {
    TOTAL_QUANTITY(TokenType.TOTAL_QUANTITY, {
        it.attachedContainer.totalQuantity }),
    NETWORK_TOTAL_QUANTITY(TokenType.NETWORK_TOTAL_QUANTITY, { it.network.totalQuantity });

    fun evaluate(context: ResourceNode): String {
        return evaluate.invoke(context).toString()
    }
}

private enum class TokenCategory {
    LITERAL, BINARY_OPERATOR, RIGHT_OPERATOR, LEFT_OPERATOR, STATEMENT, OTHER
}

private enum class TokenPrecedence {
    OTHER, LOW, MEDIUM, HIGH;

    operator fun plus(int: Int) = values()[ordinal + int]
}


private data class Token(val type: TokenType, val value: String) {
    override fun toString() = "$type - $value"
}

sealed class Node(val token: Token) {
    fun visit(context: ResourceNode): String {
        when (token.type.category) {
            TokenCategory.LITERAL -> return token.value
            TokenCategory.BINARY_OPERATOR -> {
                this as BinaryOperation
                val left = left.visit(context)
                val right = right.visit(context)
                val func = BinaryOperator.values().first { it.tokenType == token.type }
                return func.evaluate(left, right, context)
            }
            TokenCategory.RIGHT_OPERATOR, TokenCategory.LEFT_OPERATOR -> {
                this as SingleOperation
                val arg = arg.visit(context)
                val func = SingleOperator.values().first { it.tokenType == token.type }
                return func.evaluate(arg, context)
            }
            TokenCategory.STATEMENT -> {
                this as Statement
                val func = Statements.values().first { it.tokenType == token.type }
                return func.evaluate(context)
            }
            else -> return ""
        }
    }
}

private class Literal(token: Token) : Node(token) {
    override fun toString() = token.value
}

private class Statement(token: Token) : Node(token) {
    override fun toString() = token.value
}

private class BinaryOperation(val left: Node, val right: Node, token: Token) : Node(token) {
    override fun toString() = "($left ${token.value} $right)"
}

private class SingleOperation(val arg: Node, token: Token) : Node(token) {
    override fun toString() = "(${token.value} $arg)"
}

private class RoutingLanguageParseError(val beginIndex: Int, val length: Int, val message: String)

private class RoutingLanguageParse(val text: String) {

    private val tokens: Array<Token>
    private var currentIndex = 0

    var error: RoutingLanguageParseError? = null

    init {
        val tokenList = mutableListOf<Token>()
        // parenthesis
        var editedText = text.replace("(", "( ").replace(")", " )")
        // operators
        editedText = editedText.replace("*", " * ").replace("/", " / ").replace("+", " + ").replace("-", " - ")
        // syntax sugar
        editedText = editedText.replace("network contains", "network_contains").replace("network quantity", "network_quantity")
        editedText = editedText.replace("total quantity", "total_quantity").replace("network total quantity", "network_total_quantity")
        for (word in editedText.split(" ").mapNotNull { if (it.all { char -> char == ' ' }) null else it.trim() }) {
            val matchingToken = TokenType.values().firstOrNull { it.regex.matchEntire(word) != null }
            if (matchingToken != null) {
                tokenList.add(Token(matchingToken, word))
            } else {
                error = RoutingLanguageParseError(text.indexOf(word), word.length, "Unknown word '$word', try adding spaces around it")
                throw EvaluationException("Invalid token '$word'")
            }
        }
        tokens = tokenList.toTypedArray()
    }

    private var currentToken = tokens[0]

    fun parse(): Node {
        val n = nextExpression(TokenPrecedence.LOW)
        if (!n.token.type.validTopLevelType) {
            error = RoutingLanguageParseError(0, text.length, "Can't be equal to true or false")
            throw EvaluationException("$n doesn't evaluate to boolean")
        } else {
            return n
        }
    }

    private fun eat(requiredType: TokenType) {
        if (currentToken.type == requiredType) {
            if (currentIndex == tokens.lastIndex) {
                return
            }
            currentIndex += 1
            currentToken = tokens[currentIndex]
        } else {
            throw EvaluationException("Required $requiredType, but it was ${currentToken.type} at word index $currentIndex")
        }
    }

    private fun nextExpression(precedence: TokenPrecedence): Node {
        var node = if (precedence == TokenPrecedence.HIGH) {
            nextFactor()
        } else {
            nextExpression(precedence + 1)
        }
        while (currentToken.type.precedence == precedence) {
            val token = currentToken
            if (token.type.category == TokenCategory.BINARY_OPERATOR) {
                eat(token.type)
                node = BinaryOperation(node, if (precedence == TokenPrecedence.HIGH) nextFactor() else nextExpression(precedence + 1), token)
            } else if (token.type.category == TokenCategory.LEFT_OPERATOR) {
                eat(token.type)
                node = SingleOperation(node, token)
            }
        }
        return node
    }

    private fun nextFactor(): Node {
        // a factor is an part that could begin an expression
        val token = currentToken
        if (token.type.category == TokenCategory.LITERAL) {
            eat(token.type)
            return Literal(token)
        } else if (token.type.category == TokenCategory.STATEMENT) {
            eat(token.type)
            return Statement(token)
        } else if (token.type.category == TokenCategory.RIGHT_OPERATOR) {
            if (currentIndex != tokens.lastIndex) {
                eat(token.type)
                return SingleOperation(if (token.type.precedence == TokenPrecedence.HIGH) nextFactor() else nextExpression(token.type.precedence + 1), token)
            } else {
                throw EvaluationException("Expected an expression at word index $currentIndex, but the text ended")
            }
        } else if (token.type == TokenType.LEFT_PARENTHESIS) {
            if (currentIndex != tokens.lastIndex) {
                eat(TokenType.LEFT_PARENTHESIS)
                val node = nextExpression(TokenPrecedence.LOW)
                eat(TokenType.RIGHT_PARENTHESIS)
                return node
            } else {
                throw EvaluationException("Expected an expression at word index $currentIndex, but the text ended")
            }
        }
        throw EvaluationException("Incorrectly placed token $currentToken at word index $currentIndex")
    }
}

class RoutingLanguageStatement(private val node: Node) {

    fun evaluate(context: ResourceNode) = node.visit(context).toBoolean()

    override fun toString() = node.toString()

    companion object {
        val TRUE = RoutingLanguageStatement(RoutingLanguageParse("true").parse())
        val FALSE = RoutingLanguageStatement(RoutingLanguageParse("false").parse())
    }
}

object RoutingLanguage {

    // TODO add specifying for destination nodes in routing language
    fun parse(text: String): RoutingLanguageStatement {
        if (text.toLowerCase() == "true")
            return RoutingLanguageStatement.TRUE
        if (text.toLowerCase() == "false")
            return RoutingLanguageStatement.FALSE
        return RoutingLanguageStatement(RoutingLanguageParse(text).parse())
    }
}