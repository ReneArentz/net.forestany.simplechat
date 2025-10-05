package net.forestany.simplechat.chat

class Message(
    var received: Boolean = false,
    var user: String = "",
    var message: String = "",
    var timestamp: java.time.LocalDateTime? = null
)