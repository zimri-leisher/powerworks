package routing.script

import resource.ResourceType
import serialization.Id

enum class TokenType(
        val regex: Regex,
        val category: TokenCategory,
        val toNode: TokenType.(value: String, arguments: Array<out Node<*>>) -> Node<*> = { _, _ ->
            throw CompileException(
                    "TokenType $this has no node equivalent"
            )
        },
        val precedence: OperatorPrecedence = OperatorPrecedence.TOKEN_NOT_OPERATOR,
        val isBoolean: Boolean = false
) {
    NOT(
            Regex("""(¬)|(!)|(\bl?not\b)|(\\l?not\b)"""),
            TokenCategory.OP_UNARY_RIGHT,
            { value, args -> Not(Token(this, value), args[0] as Node<Boolean>) },
            OperatorPrecedence.HIGHEST,
            true
    ),
    AND(
            Regex("""(∧)|(&&)|(&)|(\bl?and\b)|(\\l?and\b)"""),
            TokenCategory.OP_BINARY_INFIX,
            { value, args -> And(Token(this, value), args[0] as Node<Boolean>, args[1] as Node<Boolean>) },
            OperatorPrecedence.HIGH,
            true
    ),
    OR(
            Regex("""(∨)|(\|?\|)|(\\l?or\b)|(\bl?or\b)"""),
            TokenCategory.OP_BINARY_INFIX,
            { value, args -> Or(Token(this, value), args[0] as Node<Boolean>, args[1] as Node<Boolean>) },
            OperatorPrecedence.HIGH,
            true
    ),
    XOR(
            Regex("""(⊕)|(\bl?xor\b)|(\\l?xor\b)|(\\oplus)"""),
            TokenCategory.OP_BINARY_INFIX,
            { value, args -> ExclusiveOr(Token(this, value), args[0] as Node<Boolean>, args[1] as Node<Boolean>) },
            OperatorPrecedence.HIGH,
            true
    ),
    IMPLIES(
            Regex("""(=?⇒)|(\bimplies\b)|(\\implies\b)"""),
            TokenCategory.OP_BINARY_INFIX,
            { value, args -> Implies(Token(this, value), args[0] as Node<Boolean>, args[1] as Node<Boolean>) },
            OperatorPrecedence.MEDIUM,
            true
    ),
    IFF(
            Regex("""(⇔)|(\bl?iff\b)|(\\l?iff\b)"""),
            TokenCategory.OP_BINARY_INFIX,
            { value, args -> IfAndOnlyIf(Token(this, value), args[0] as Node<Boolean>, args[1] as Node<Boolean>) },
            OperatorPrecedence.LOW,
            true
    ),
    PLUS(
            Regex("""\+|(\bplus\b)"""),
            TokenCategory.OP_BINARY_INFIX,
            { value, args -> Plus(Token(this, value), args[0] as Node<Number>, args[1] as Node<Number>) },
            OperatorPrecedence.HIGH
    ),
    MINUS(
            Regex("""-|(\bminus\b)"""),
            TokenCategory.OP_BINARY_INFIX,
            { value, args -> Minus(Token(this, value), args[0] as Node<Number>, args[1] as Node<Number>) },
            OperatorPrecedence.HIGH
    ),
    MULTIPLY(
            Regex("""\*|\btimes\b"""),
            TokenCategory.OP_BINARY_INFIX,
            { value, args -> Multiply(Token(this, value), args[0] as Node<Number>, args[1] as Node<Number>) },
            OperatorPrecedence.MEDIUM
    ),
    DIVIDE(
            Regex("""[/÷]"""),
            TokenCategory.OP_BINARY_INFIX,
            { value, args -> Divide(Token(this, value), args[0] as Node<Number>, args[1] as Node<Number>) },
            OperatorPrecedence.MEDIUM
    ),
    GREATER_THAN(
            Regex("""(?<!=)>(?!=)"""),
            TokenCategory.OP_BINARY_INFIX,
            { value, args -> GreaterThan(Token(this, value), args[0] as Node<Number>, args[1] as Node<Number>) },
            OperatorPrecedence.LOW,
            true
    ),
    GREATER_THAN_OR_EQUAL(
            Regex(""">="""),
            TokenCategory.OP_BINARY_INFIX,
            { value, args -> GreaterThanOrEqual(Token(this, value), args[0] as Node<Number>, args[1] as Node<Number>) },
            OperatorPrecedence.LOW,
            true
    ),
    LESS_THAN(
            Regex("""(?<!=)<(?!=)"""),
            TokenCategory.OP_BINARY_INFIX,
            { value, args -> LessThan(Token(this, value), args[0] as Node<Number>, args[1] as Node<Number>) },
            OperatorPrecedence.LOW,
            true
    ),
    LESS_THAN_OR_EQUAL(
            Regex("""<="""),
            TokenCategory.OP_BINARY_INFIX,
            { value, args -> LessThanOrEqual(Token(this, value), args[0] as Node<Number>, args[1] as Node<Number>) },
            OperatorPrecedence.LOW,
            true
    ),
    EQUALS(
            Regex("""(?<!([<>]))=(?!([<>]))"""),
            TokenCategory.OP_BINARY_INFIX,
            { value, args -> Equal(Token(this, value), args[0] as Node<Any>, args[1] as Node<Any>) },
            OperatorPrecedence.LOWEST,
            true
    ),
    QUANTITY_OF(
            Regex("""quantity_of"""),
            TokenCategory.OP_UNARY_RIGHT,
            { value, args -> QuantityOf(Token(this, value), args[0] as Node<ResourceType>) },
            OperatorPrecedence.HIGHEST
    ),
    NETWORK_QUANTITY_OF(
            Regex("""(network_quantity_of)"""),
            TokenCategory.OP_UNARY_RIGHT,
            { value, args -> NetworkQuantityOf(Token(this, value), args[0] as Node<ResourceType>) },
            OperatorPrecedence.HIGHEST
    ),
    TOTAL_QUANTITY(
            Regex("""total_quantity"""),
            TokenCategory.STATEMENT,
            { value, args -> TotalQuantity(Token(this, value)) },
            OperatorPrecedence.HIGHEST
    ),
    NETWORK_TOTAL_QUANTITY(
            Regex("""(network_total_quantity)|(total_network_quantity)"""),
            TokenCategory.STATEMENT,
            { value, args -> TotalNetworkQuantity(Token(this, value)) },
            OperatorPrecedence.HIGHEST
    ),
    OPEN_PAREN(
            Regex("""\("""),
            TokenCategory.GROUPING
    ),
    CLOSE_PAREN(
            Regex("""\)"""),
            TokenCategory.GROUPING
    ),
    LITERAL_BOOLEAN(
            Regex("""(\btrue\b)|(\bfalse\b)|(\b([tTfFyYnN])\b)"""),
            TokenCategory.LITERAL,
            { value, _ -> BooleanLiteral(Token(this, value)) },
            isBoolean = true
    ),
    LITERAL_INT(
            Regex("""-?\d+"""),
            TokenCategory.LITERAL,
            { value, _ -> IntLiteral(Token(this, value)) }
    ),
    LITERAL_DOUBLE(
            Regex("""[0-9]+(\.[0-9]+)"""),
            TokenCategory.LITERAL,
            { value, _ -> DoubleLiteral(Token(this, value)) }
    ),
    LITERAL_RESOURCE_TYPE(
            Regex("""a^"""), //we will use a custom checker for resource type literals
            TokenCategory.LITERAL,
            { value, _ -> ResourceTypeLiteral(Token(this, value)) }
    )
}

enum class TokenCategory {
    GROUPING,
    LITERAL,
    STATEMENT,
    OP_UNARY_LEFT,
    OP_UNARY_RIGHT,
    OP_BINARY_INFIX,

    // no support for postfix because it requires a stack
    OP_BINARY_PREFIX
}

enum class OperatorPrecedence {
    TOKEN_NOT_OPERATOR,
    LOWEST,
    LOW,
    MEDIUM,
    HIGH,
    HIGHEST;

    operator fun plus(i: Int) = values()[ordinal + i]
}

data class Token(@Id(1) val type: TokenType, @Id(2) val value: String) {

    private constructor() : this(TokenType.LITERAL_BOOLEAN, "false")

    fun toNode(vararg args: Node<*>) = type.toNode(type, value, args)
}