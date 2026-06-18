package com.owee.app.data.repository

import com.owee.app.data.remote.SupabaseProvider
import com.owee.app.data.remote.model.Group
import com.owee.app.data.remote.model.GroupMember
import com.owee.app.data.remote.model.GroupWithMembers
import com.owee.app.data.remote.model.User
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class GroupRepository {

    // ─── Create ───────────────────────────────────────────────────────────────

    suspend fun createGroup(
        name: String,
        creatorId: String,
        memberIds: List<String>
    ): Group? {
        return try {
            android.util.Log.d("GroupRepository", "Creating group: name=$name, creatorId=$creatorId, members=$memberIds")

            val group = SupabaseProvider.client
                .from("groups")
                .insert(
                    Group(
                        name = name.trim(),
                        created_by = creatorId
                    )
                ) {
                    select()
                }
                .decodeSingle<Group>()

            android.util.Log.d("GroupRepository", "Group created: ${group.id}")

            val groupId = group.id ?: return null

            val memberRows = buildList {
                add(GroupMember(group_id = groupId, user_id = creatorId, role = "owner"))
                memberIds.forEach { friendId ->
                    add(GroupMember(group_id = groupId, user_id = friendId, role = "member"))
                }
            }

            android.util.Log.d("GroupRepository", "Inserting members: $memberRows")

            SupabaseProvider.client
                .from("group_members")
                .insert(memberRows)

            android.util.Log.d("GroupRepository", "Members inserted successfully")

            group
        } catch (e: Exception) {
            android.util.Log.e("GroupRepository", "createGroup failed", e)
            null
        }
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    suspend fun getGroupsForUser(userId: String): List<GroupWithMembers> {
        android.util.Log.d("GroupRepository", "Fetching groups for user: $userId")
        return try {
            // 1. Get all group_ids this user belongs to
            val memberships = SupabaseProvider.client
                .from("group_members")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<GroupMember>()

            android.util.Log.d("GroupRepository", "Found ${memberships.size} memberships for $userId")
            if (memberships.isEmpty()) return emptyList()

            val groupIds = memberships.map { it.group_id }

            // 2. Fetch those groups
            val groups = SupabaseProvider.client
                .from("groups")
                .select {
                    filter { isIn("id", groupIds) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<Group>()

            android.util.Log.d("GroupRepository", "Fetched ${groups.size} groups details")
            if (groups.isEmpty()) return emptyList()

            // 3. Fetch all members for those groups in one query
            val allMembers = SupabaseProvider.client
                .from("group_members")
                .select {
                    filter { isIn("group_id", groupIds) }
                }
                .decodeList<GroupMember>()

            // 4. Fetch user profiles for all member user_ids
            val userIds = allMembers.map { it.user_id }.distinct()
            val users = SupabaseProvider.client
                .from("users")
                .select {
                    filter { isIn("id", userIds) }
                }
                .decodeList<User>()

            // 5. Assemble GroupWithMembers list
            val result = groups.map { group ->
                val groupMemberships = allMembers.filter { it.group_id == group.id }
                val groupMemberIds = groupMemberships.map { it.user_id }
                val groupUsers = users.filter { it.id in groupMemberIds }

                val currentUserMembership = groupMemberships.find { it.user_id == userId }

                GroupWithMembers(
                    group = group,
                    members = groupUsers,
                    memberCount = groupUsers.size,
                    currentUserRole = currentUserMembership?.role ?: "member"
                )
            }
            android.util.Log.d("GroupRepository", "Assembled ${result.size} GroupWithMembers")
            result
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("GroupRepository", "getGroupsForUser failed", e)
            emptyList()
        }
    }

    suspend fun deleteGroup(groupId: String): Boolean {
        return try {
            SupabaseProvider.client
                .from("groups")
                .delete {
                    filter { eq("id", groupId) }
                }
            true
        } catch (e: Exception) {
            android.util.Log.e("GroupRepository", "deleteGroup failed", e)
            false
        }
    }
}
