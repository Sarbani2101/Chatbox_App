package com.example.chatbox_app.dataclass

data class User(
    val name: String = "",
    val city: String = "",
    val status: String = "Available",
    val email: String = "",
    val uid: String = "",
    val latitude: Double = 0.0,  // Change to Double
    val longitude: Double = 0.0, // Change to Double
    val address: String = "",
    val profileImage: String = ""
){
    constructor() : this("", "", "", "", "", 0.0, 0.0, "", "")
}
