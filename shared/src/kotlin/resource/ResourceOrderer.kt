package resource

interface ResourceOrderer {
    /**
     * Given a [ResourceOrder], return the [ResourceOrder] that this will permit.
     */
    fun getNecessaryFlow(order: ResourceOrder): ResourceFlow
}

interface ResourceConduit {
    fun maxFlow(flow: ResourceFlow): ResourceFlow
}

class Quantity {


    //

    fun cons(order: ResourceOrder, network: ResourceNetwork<*>, node: ResourceNode) {

    }

}

class LessThan {

    val left = 0
    val right = 1

    fun cons(order: ResourceOrder, network: ResourceNetwork<*>, node: ResourceNode) {
        // quantity of iron < 25
        // if order.type == iron
        //    if order.dir == in
        //
    }
}