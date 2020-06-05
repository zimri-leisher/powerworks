package routing.script

import resource.ResourceNode
import resource.ResourceType
import serialization.Input
import serialization.Output
import serialization.Serializer

class CompileException(message: String) : Throwable(message)

class RoutingLanguageParseError(val beginIndex: Int, val length: Int, val message: String)

class RoutingLanguageStatement(val text: String, tokens: Array<Token>? = null, baseNode: Node<Boolean>? = null) {

    val tokens: Array<Token> = tokens ?: tokenize()
    private var currentIndex = 0
    private var currentToken = this.tokens[currentIndex]
    val baseNode: Node<Boolean> = baseNode ?: compile()

    var error: RoutingLanguageParseError? = null

    private fun tokenize(): Array<Token> {
        fun getToken(word: String): List<Token> {
            if (word == "") {
                return emptyList()
            }
            val tokens = mutableListOf<Token>()
            val types = TokenType.values()
            val fullMatch = types.firstOrNull { it.regex matches word }
            if (fullMatch == null) {
                types.forEach {
                    val matchResult = it.regex.find(word)
                    // custom logic for resource type literal
                    if (it == TokenType.LITERAL_RESOURCE_TYPE) {
                        for (type in ResourceType.ALL) {
                            if (word.startsWith(type.technicalName)) {
                                tokens.add(Token(it, type.technicalName))
                                tokens.addAll(getToken(word.substring(type.technicalName.length)))
                                return tokens
                            }
                        }
                    }
                    if (matchResult != null && matchResult.range.first == 0 && (matchResult.range.last - matchResult.range.first) != 0) {
                        tokens.add(Token(it, matchResult.value))
                        tokens.addAll(getToken(word.removeRange(matchResult.range)))
                        return tokens
                    }
                }
                error = RoutingLanguageParseError(text.indexOf(word), word.length, "Unknown word '$word', try adding spaces around it")
                throw CompileException("Invalid token '$word'")
            } else {
                return listOf(Token(fullMatch, word))
            }
        }

        val tokenList = mutableListOf<Token>()
        // parenthesis
        var editedText = text.toLowerCase().replace("(", "( ").replace(")", " )")
        // some syntax sugar
        editedText = editedText.replace("!", " ! ")
        // operators
        editedText = editedText.replace("*", " * ").replace("/", " / ").replace("+", " + ").replace("-", " - ")
        // syntax sugar
        editedText = editedText.replace("network contains", "network_contains").replace("network quantity", "network_quantity")
        // add underscores to type names
        for (type in ResourceType.ALL) {
            if (editedText.contains(type.name.toLowerCase())) {
                editedText = editedText.replace(type.name.toLowerCase(), type.technicalName)
            }
        }
        editedText = editedText.replace("total quantity", "total_quantity").replace("network total quantity", "network_total_quantity")
        editedText = editedText.replace("quantity of", "quantity_of")
        // convert text into tokens w/ regex
        val words = editedText.split(" ")
                .mapNotNull { if (it.all { char -> char == ' ' }) null else it.trim() } //split by spaces, don't include blank words, trim off spaces

        for (word in words) {
            val matchingTokens = getToken(word)
            tokenList.addAll(matchingTokens)
        }

        return tokenList.toTypedArray()
    }

    fun evaluate(context: ResourceNode) = baseNode.visit(context)

    private fun compile(): Node<Boolean> {
        val base = nextExpression(OperatorPrecedence.LOWEST)
        if (!base.token.type.isBoolean) {
            throw CompileException("Statement does not evaluate to boolean")
        }
        return base as Node<Boolean>
    }

    private fun eat(requiredType: TokenType) {
        if (currentToken.type == requiredType) {
            if (currentIndex == tokens.lastIndex) {
                return
            }
            currentIndex += 1
            currentToken = tokens[currentIndex]
        } else {
            throw CompileException("Required $requiredType, but it was ${currentToken.type} at word index $currentIndex")
        }
    }

    private fun nextExpression(precedence: OperatorPrecedence): Node<*> {
        // the recursion here ensures that the first thing that gets called is factor, and then under it, in lowering priority,
        // calls to expression
        var node = if (precedence == OperatorPrecedence.HIGHEST) {
            nextFactor()
        } else {
            nextExpression(precedence + 1)
        }
        // then go along the tokens until we have eaten all the ones of the current precedence
        while (currentToken.type.precedence == precedence) {
            val token = currentToken
            if (token.type.category == TokenCategory.OP_BINARY_INFIX) {
                eat(token.type)
                // infix means the (now previous) node is the first arg, next node is the next arg
                val leftArg = node
                val rightArg =
                        if (precedence == OperatorPrecedence.HIGHEST) nextFactor() else nextExpression(precedence + 1)
                node = token.toNode(leftArg, rightArg)
            } else if (token.type.category == TokenCategory.OP_UNARY_LEFT) {
                eat(token.type)
                val arg = node
                node = token.toNode(arg)
            }
        }
        return node
    }

    private fun nextFactor(): Node<*> {
        // a factor is an node that could begin an expression
        val token = currentToken
        if (token.type.category == TokenCategory.LITERAL) {
            eat(token.type)
            return token.toNode()
        } else if (token.type.category == TokenCategory.STATEMENT) {
            eat(token.type)
            return token.toNode()
        } else if (token.type.category == TokenCategory.OP_UNARY_RIGHT) {
            if (currentIndex != tokens.lastIndex) {
                eat(token.type)
                return token.toNode(
                        if (token.type.precedence == OperatorPrecedence.HIGHEST)
                            nextFactor() else
                            nextExpression(token.type.precedence + 1)
                )
            } else {
                throw CompileException("Expected an expression at word index $currentIndex, but the text ended")
            }
        } else if (token.type.category == TokenCategory.OP_BINARY_PREFIX) {
            if (currentIndex != tokens.lastIndex) {
                eat(token.type)
                return token.toNode(
                        if (token.type.precedence == OperatorPrecedence.HIGHEST)
                            nextFactor() else
                            nextExpression(token.type.precedence + 1),
                        if (token.type.precedence == OperatorPrecedence.HIGHEST)
                            nextFactor() else
                            nextExpression(token.type.precedence + 1)
                )
            }
        } else if (token.type == TokenType.OPEN_PAREN) {
            if (currentIndex != tokens.lastIndex) {
                eat(TokenType.OPEN_PAREN)
                val node = nextExpression(OperatorPrecedence.LOWEST)
                eat(TokenType.CLOSE_PAREN)
                return node
            } else {
                throw CompileException("Expected an expression at word index $currentIndex, but the text ended")
            }
        }
        throw CompileException("Incorrectly placed token $currentToken at word index $currentIndex")
    }

    override fun toString(): String {
        return text
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoutingLanguageStatement

        if (baseNode != other.baseNode) return false

        return true
    }

    override fun hashCode(): Int {
        return baseNode.hashCode()
    }
}

object RoutingLanguage {

    val TRUE = RoutingLanguageStatement("true")
    val FALSE = RoutingLanguageStatement("false")

    // TODO add specifying for destination nodes in routing language
    fun parse(text: String): RoutingLanguageStatement {
        if (text.toLowerCase() == "true")
            return TRUE
        if (text.toLowerCase() == "false" || text == "")
            return FALSE
        return RoutingLanguageStatement(text)
    }
}

class RoutingLanguageStatementSerializer : Serializer<RoutingLanguageStatement>() {

    override fun write(obj: Any, output: Output) {
        obj as RoutingLanguageStatement
        output.writeUTF(obj.text)
        output.write(obj.tokens)
        output.write(obj.baseNode)
    }

    override fun instantiate(input: Input): RoutingLanguageStatement {
        val text = input.readUTF()
        val tokens = input.read(Array<Token>::class.java)
        val baseNode = input.read(Node::class.java) as Node<Boolean>
        return RoutingLanguageStatement(text, tokens, baseNode)
    }
}