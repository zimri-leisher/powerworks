package routing.script

import resource.ResourceNode
import resource.ResourceType
import serialization.Input
import serialization.Output
import serialization.Serializer

sealed class Node<R>(
        val token: Token) {
    abstract fun visit(context: ResourceNode): R

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Node<R>

        // we should be ok with values differing
        if (token.type != other.token.type) return false

        return true
    }

    override fun hashCode(): Int {
        return token.type.hashCode()
    }
}

sealed class Literal<R>(token: Token, val value: R) : Node<R>(token) {
    override fun visit(context: ResourceNode): R {
        return value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Literal<*>

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (value?.hashCode() ?: 0)
        return result
    }
}

class BooleanLiteral(token: Token) : Literal<Boolean>(token, token.value.toBoolean())

class IntLiteral(token: Token) : Literal<Int>(token, token.value.toInt())

class DoubleLiteral(token: Token) : Literal<Double>(token, token.value.toDouble())

class ResourceTypeLiteral(token: Token) : Literal<ResourceType>(token, ResourceType.getType(token.value)!!)

sealed class Statement<R>(token: Token) : Node<R>(token)

class TotalQuantity(token: Token) : Statement<Int>(token) {
    override fun visit(context: ResourceNode): Int {
        return context.attachedContainer.totalQuantity + context.attachedContainer.expected.totalQuantity
    }
}

class TotalNetworkQuantity(token: Token) : Statement<Int>(token) {
    override fun visit(context: ResourceNode): Int {
        return context.network.totalQuantity
    }
}

sealed class UnaryOperator<P, R>(token: Token, val arg: Node<P>) : Node<R>(token) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as UnaryOperator<P, R>

        if (arg != other.arg) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + arg.hashCode()
        return result
    }

    override fun toString(): String {
        return "(${token.value} $arg)"
    }

}

class Not(token: Token, arg: Node<Boolean>) : UnaryOperator<Boolean, Boolean>(token, arg) {
    override fun visit(context: ResourceNode): Boolean {
        return !arg.visit(context)
    }

}

class QuantityOf(token: Token, arg: Node<ResourceType>) : UnaryOperator<ResourceType, Int>(token, arg) {
    override fun visit(context: ResourceNode): Int {
        return context.attachedContainer.getQuantity(arg.visit(context)) + context.attachedContainer.expected[arg.visit(context)]
    }

}

class NetworkQuantityOf(token: Token, arg: Node<ResourceType>) : UnaryOperator<ResourceType, Int>(token, arg) {
    override fun visit(context: ResourceNode): Int {
        return context.network.getQuantity(arg.visit(context))
    }

}

sealed class BinaryOperator<P1, P2, R>(token: Token, val left: Node<P1>, val right: Node<P2>) : Node<R>(token) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as BinaryOperator<P1, P2, R>

        if (left != other.left) return false
        if (right != other.right) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + left.hashCode()
        result = 31 * result + right.hashCode()
        return result
    }

    override fun toString(): String {
        return "(${token.value} $left $right)"
    }

}

class Implies(token: Token, left: Node<Boolean>, right: Node<Boolean>) : BinaryOperator<Boolean, Boolean, Boolean>(token, left, right) {
    override fun visit(context: ResourceNode): Boolean {
        val leftValue = left.visit(context)
        val rightValue = right.visit(context)
        return !leftValue || (leftValue && rightValue)
    }
}

class IfAndOnlyIf(token: Token, left: Node<Boolean>, right: Node<Boolean>) : BinaryOperator<Boolean, Boolean, Boolean>(token, left, right) {
    override fun visit(context: ResourceNode): Boolean {
        val leftValue = left.visit(context)
        val rightValue = right.visit(context)
        return leftValue == rightValue
    }

}

class ExclusiveOr(token: Token, left: Node<Boolean>, right: Node<Boolean>) : BinaryOperator<Boolean, Boolean, Boolean>(token, left, right) {
    override fun visit(context: ResourceNode): Boolean {
        val leftValue = left.visit(context)
        val rightValue = right.visit(context)
        return leftValue != rightValue
    }

}

class Or(token: Token, left: Node<Boolean>, right: Node<Boolean>) : BinaryOperator<Boolean, Boolean, Boolean>(token, left, right) {
    override fun visit(context: ResourceNode): Boolean {
        val leftValue = left.visit(context)
        val rightValue = right.visit(context)
        return leftValue || rightValue
    }
}

class And(token: Token, left: Node<Boolean>, right: Node<Boolean>) : BinaryOperator<Boolean, Boolean, Boolean>(token, left, right) {
    override fun visit(context: ResourceNode): Boolean {
        val leftValue = left.visit(context)
        val rightValue = right.visit(context)
        return leftValue && rightValue
    }
}

class Plus(token: Token, left: Node<Number>, right: Node<Number>) : BinaryOperator<Number, Number, Number>(token, left, right) {
    override fun visit(context: ResourceNode): Number {
        val leftValue = left.visit(context)
        val rightValue = right.visit(context)
        if (leftValue is Int && rightValue is Int) {
            return leftValue + rightValue
        }
        return left.visit(context).toDouble() + right.visit(context).toDouble()
    }
}

class Minus(token: Token, left: Node<Number>, right: Node<Number>) : BinaryOperator<Number, Number, Number>(token, left, right) {
    override fun visit(context: ResourceNode): Number {
        val leftValue = left.visit(context)
        val rightValue = right.visit(context)
        if (leftValue is Int && rightValue is Int) {
            return leftValue - rightValue
        }
        return left.visit(context).toDouble() - right.visit(context).toDouble()
    }
}

class Multiply(token: Token, left: Node<Number>, right: Node<Number>) : BinaryOperator<Number, Number, Number>(token, left, right) {
    override fun visit(context: ResourceNode): Number {
        val leftValue = left.visit(context)
        val rightValue = right.visit(context)
        if (leftValue is Int && rightValue is Int) {
            return leftValue * rightValue
        }
        return left.visit(context).toDouble() * right.visit(context).toDouble()
    }
}

class Divide(token: Token, left: Node<Number>, right: Node<Number>) : BinaryOperator<Number, Number, Number>(token, left, right) {
    override fun visit(context: ResourceNode): Number {
        val leftValue = left.visit(context)
        val rightValue = right.visit(context)
        if (leftValue is Int && rightValue is Int) {
            return leftValue / rightValue
        }
        return left.visit(context).toDouble() / right.visit(context).toDouble()
    }
}

class GreaterThan(token: Token, left: Node<Number>, right: Node<Number>) : BinaryOperator<Number, Number, Boolean>(token, left, right) {
    override fun visit(context: ResourceNode): Boolean {
        val leftValue = left.visit(context)
        val rightValue = right.visit(context)
        if (leftValue is Int && rightValue is Int) {
            return leftValue > rightValue
        }
        return leftValue.toDouble() > rightValue.toDouble()
    }
}

class GreaterThanOrEqual(token: Token, left: Node<Number>, right: Node<Number>) : BinaryOperator<Number, Number, Boolean>(token, left, right) {
    override fun visit(context: ResourceNode): Boolean {
        val leftValue = left.visit(context)
        val rightValue = right.visit(context)
        if (leftValue is Int && rightValue is Int) {
            return leftValue >= rightValue
        }
        return leftValue.toDouble() >= rightValue.toDouble()
    }
}

class LessThan(token: Token, left: Node<Number>, right: Node<Number>) : BinaryOperator<Number, Number, Boolean>(token, left, right) {
    override fun visit(context: ResourceNode): Boolean {
        val leftValue = left.visit(context)
        val rightValue = right.visit(context)
        if (leftValue is Int && rightValue is Int) {
            return leftValue < rightValue
        }
        return leftValue.toDouble() < rightValue.toDouble()
    }
}

class LessThanOrEqual(token: Token, left: Node<Number>, right: Node<Number>) : BinaryOperator<Number, Number, Boolean>(token, left, right) {
    override fun visit(context: ResourceNode): Boolean {
        val leftValue = left.visit(context)
        val rightValue = right.visit(context)
        if (leftValue is Int && rightValue is Int) {
            return leftValue <= rightValue
        }
        return leftValue.toDouble() <= rightValue.toDouble()
    }
}

class Equal(token: Token, left: Node<Any>, right: Node<Any>) : BinaryOperator<Any, Any, Boolean>(token, left, right) {
    override fun visit(context: ResourceNode): Boolean {
        return left.visit(context) == right.visit(context)
    }
}

class NodeSerializer : Serializer<Node<*>>() {

    override fun write(obj: Any, output: Output) {
        obj as Node<*>
        output.write(obj.token)
        when (obj.token.type.category) {
            TokenCategory.OP_BINARY_INFIX, TokenCategory.OP_BINARY_PREFIX -> {
                obj as BinaryOperator<*, *, *>
                output.write(obj.left)
                output.write(obj.right)
            }
            TokenCategory.OP_UNARY_RIGHT, TokenCategory.OP_UNARY_LEFT -> {
                obj as UnaryOperator<*, *>
                output.write(obj.arg)
            }
        }
    }

    override fun instantiate(input: Input): Node<*> {
        val token = input.read(Token::class.java)
        val args = mutableListOf<Node<*>>()
        when (token.type.category) {
            TokenCategory.OP_BINARY_INFIX, TokenCategory.OP_BINARY_PREFIX -> {
                args.add(input.read(Node::class.java))
                args.add(input.read(Node::class.java))
            }
            TokenCategory.OP_UNARY_RIGHT, TokenCategory.OP_UNARY_LEFT -> {
                args.add(input.read(Node::class.java))
            }
        }
        return token.toNode(*args.toTypedArray())
    }

    override fun read(newInstance: Any, input: Input) {
    }
}