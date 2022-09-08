package resource

class SourceContainer : ResourceContainer() {
    override val totalQuantity: Int
        get() = throw Exception("Source container has no totalQuantity")

    override fun add(resources: ResourceList): Boolean {
        return true
    }

    override fun mostPossibleToAdd(list: ResourceList): ResourceList {
        return list
    }

    override fun mostPossibleToRemove(list: ResourceList): ResourceList {
        return list
    }

    override fun remove(list: ResourceList): Boolean {
        return true
    }

    override fun clear() {
    }

    override fun copy(): ResourceContainer {
        return this
    }

    override fun getQuantity(resource: ResourceType): Int {
        throw Exception("Cannot get type of resources in a source container")
    }

    override fun toResourceList(): ResourceList {
        throw Exception("Cannot convert source container to resource list")
    }

}