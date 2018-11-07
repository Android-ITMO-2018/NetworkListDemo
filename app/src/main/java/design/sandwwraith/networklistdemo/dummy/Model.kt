package design.sandwwraith.networklistdemo.dummy

data class Photo(
    val id: String,
    val width: Int,
    val height: Int,
    val likes: Int,
    val user: User,
    val urls: Links,
    val description: String?
)

data class User(val name: String, val id: String)

data class Links(val full: String, val regular: String, val small: String)

