package com.billbuddy.shared.groups

import com.billbuddy.shared.db.BillBuddyDatabase
import com.billbuddy.shared.model.Group
import com.billbuddy.shared.model.User
import com.benasher44.uuid.uuid4 // For generating unique IDs

// A simple in-memory store for now, to be replaced/augmented by SQLDelight
// private val groups = mutableMapOf<String, Group>()

class GroupRepository(private val database: BillBuddyDatabase) {

    fun createGroup(name: String, creator: User): Group {
        val groupId = uuid4().toString()
        val inviteCode = uuid4().toString().take(6).uppercase() // Simple invite code

        // For now, store members as a JSON string or in a separate table if using SQLDelight extensively.
        // This example assumes we'll handle member serialization carefully or simplify.
        // Let's assume for now we're just storing the creator as the first member.
        // A proper solution would involve a GroupMembers table.

        database.groupQueries.insertGroup(
            id = groupId,
            name = name,
            created_by_user_id = creator.id,
            invite_code = inviteCode
        )
        // Add creator to GroupMember table
        database.groupMemberQueries.addMember(id = uuid4().toString(), group_id = groupId, user_id = creator.id)


        // Fetch the group and its members to return the complete Group object
        return getGroupById(groupId) ?: throw IllegalStateException("Failed to create or retrieve group")
    }

    fun getGroupById(groupId: String): Group? {
        val groupEntity = database.groupQueries.selectGroupById(groupId).executeAsOneOrNull() ?: return null
        val memberEntities = database.groupMemberQueries.getMembersForGroup(groupId).executeAsList()

        // We need a way to get User objects from user_ids.
        // This might involve another query to a User table or passing a list of known users.
        // For now, let's assume we can fetch them (this part needs more robust user fetching later).
        val members = memberEntities.mapNotNull { memberEntity ->
            database.userQueries.selectUserById(memberEntity.user_id).executeAsOneOrNull()?.let { userEntity ->
                // Assuming userEntity does not have photoUrl based on current User table schema in .sq
                User(id = userEntity.id, email = userEntity.email, displayName = userEntity.displayName, photoUrl = null)
            }
        }

        return Group(
            id = groupEntity.id,
            name = groupEntity.name,
            members = members,
            createdBy = groupEntity.created_by_user_id,
            inviteCode = groupEntity.invite_code
        )
    }

    fun addMemberToGroup(groupId: String, user: User): Group? {
        val group = getGroupById(groupId) ?: return null // Ensure group exists
        if (group.members.any { it.id == user.id }) return group // User already a member

        database.groupMemberQueries.addMember(id = uuid4().toString(), group_id = groupId, user_id = user.id)
        return getGroupById(groupId) // Return updated group
    }

    fun getGroupsForUser(userId: String): List<Group> {
        val groupEntities = database.groupQueries.selectGroupsByUserId(userId).executeAsList()
        return groupEntities.mapNotNull { getGroupById(it.id) } // Reuse getGroupById to populate members
    }

    // Placeholder for generating a shareable invite link/code
    fun generateInviteLink(groupId: String): String? {
        val group = getGroupById(groupId)
        return group?.inviteCode?.let { "billbuddy://join?code=$it" }
    }
}
