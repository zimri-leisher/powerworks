package resource

interface ResourceOrderer {
    /**
     * Given a [ResourceOrder], return the [ResourceFlow] necessary to satisfy that order.
     */
    fun getNecessaryFlow(order: ResourceOrder): ResourceFlow
}

interface ResourceConduit {
    /**
     * Given a [ResourceFlow], return the amount of that flow that this can take
     */
    fun maxFlow(flow: ResourceFlow): ResourceFlow
}