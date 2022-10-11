package routing.script

import resource.ResourceNode

// script should take in resource type, quantity, input/output, priority requests and output that but modified

sealed class Node2(val type: Type<*>) {

    abstract fun eval(context: ResourceNode): Any?

    object Error : Node2(Type.None) {
        override fun eval(context: ResourceNode): Any {
            return Unit
        }
    }

    sealed class OpUnary(val arg: Node2, type: Type<*>) : Node2(type)

    class Not(arg: Node2) : OpUnary(arg, Type.Boolean) {
        override fun eval(context: ResourceNode): Any {
            return !(arg.eval(context) as Boolean)
        }
    }

    class Print(arg: Node2) : OpUnary(arg, Type.None) {
        override fun eval(context: ResourceNode): Any? {
            print(arg.eval(context))
            return Unit
        }
    }

    sealed class Literal<R>(val value: R, type: Type<R>) : Node2(type) {
        override fun eval(context: ResourceNode): Any? {
            return value
        }

        class Boolean(value: kotlin.Boolean) : Literal<kotlin.Boolean>(value, Type.Boolean)
        class Number(value: kotlin.Number) : Literal<kotlin.Number>(value, Type.Number)
        class Int(value: kotlin.Int) : Literal<kotlin.Int>(value, Type.Int)
        class Double(value: kotlin.Double) : Literal<kotlin.Double>(value, Type.Double)
    }
}