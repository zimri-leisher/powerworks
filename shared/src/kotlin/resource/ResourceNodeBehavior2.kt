package resource

class ResourceFilter {
    // containers request resources
    //      container resource requests can be set by players or by the game
    //      for example, a player may want to fill a chest with a certain amount of materials
    // or a crafting block may want a certain set of materials
    // do we give the player the last say on what a block requests/filters?
    // i think so, but we want to make sure that they can find mistakes easily like this
    // we'll want a tool that resets blocks to default filters
    // that means that we'll want a generic function for blocks that resets their requests
    // do that later ig
    // how will requests work?
    // on change crafter recipe
    //      inv.request(recipe.ingredients, Priority.DEMAND)
    //      we want this to resolve to a ResourceOrder
    //      ResourceOrder will be converted to a set of transactions
    //      get a transaction executor
    fun check(type: ResourceType, quantity: Int, )
}