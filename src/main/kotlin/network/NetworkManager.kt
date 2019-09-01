package network


object NetworkManager {

    var server = false
    var client = false

    fun sendPacket(packet: Packet) {
        if(server) {
            Server.sendToClients(packet)
        }
        if(client) {
            Client.sendToServer(packet)
        }
    }

    fun update() {
        if(client) {
            Client.update()
        }
        if(server) {
            Server.update()
        }
    }

    fun close() {
        if(client) {
            Client.close()
        }
        if(server) {
            Server.close()
        }
    }

}