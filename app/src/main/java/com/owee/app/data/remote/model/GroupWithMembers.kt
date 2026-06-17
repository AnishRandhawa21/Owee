package com.owee.app.data.remote.model

data class GroupWithMembers(
    val group: Group,
    val members: List<User>,
    val memberCount: Int = members.size,
    val currentUserRole: String = "member"
)