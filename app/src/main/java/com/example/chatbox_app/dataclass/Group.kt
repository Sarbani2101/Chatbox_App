package com.example.chatbox_app.dataclass

data class Group(
    val id: String,
    val name: String,
    val adminId: String,
    val members: MutableList<String>
)