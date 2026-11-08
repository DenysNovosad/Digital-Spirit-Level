package com.example.digitalspiritlevel.repository

data class RemoteControlResponse(
    val id: Int,
    val title: String,
    val completed: Boolean
)

data class ControlCommand(
    val id: Int,
    val enabled: Boolean,
    val description: String
)

fun RemoteControlResponse.toControlCommand() = ControlCommand(
    id = id,
    enabled = completed,
    description = title
)
