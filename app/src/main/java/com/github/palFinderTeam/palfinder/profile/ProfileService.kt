package com.github.palFinderTeam.palfinder.profile

import com.github.palFinderTeam.palfinder.utils.Repository
import com.github.palFinderTeam.palfinder.utils.Response
import kotlinx.coroutines.flow.Flow

interface ProfileService: Repository {
    /**
     * Fetch a profile from database.
     *
     * @param userId Id of the user to fetch.
     *
     * @return the user profile or null if something wrong occurs.
     */
    suspend fun fetchUserProfile(userId: String): ProfileUser?

    /**
     * Fetch multiple profile concurrently from database.
     *
     * @param userIds Ids of every users to fetch.
     *
     * @return the user profiles or null if something wrong occurs.
     */
    suspend fun fetchUsersProfile(userIds: List<String>): List<ProfileUser>?

    /**
     * Edit one field of a profile in database.
     *
     * @param userId id of the profile to update.
     * @param field name of the field to update.
     * @param value new value to apply.
     *
     * @return the userId or null if something wrong occurs.
     */
    suspend fun editUserProfile(userId: String, field: String, value: Any): String?

    /**
     * Edit one a profile with a whole new profile.
     *
     * @param userId id of the profile to update.
     * @param userProfile new profile to apply.
     *
     * @return the userId or null if something wrong occurs.
     */
    suspend fun editUserProfile(userId: String, userProfile: ProfileUser): String?

    /**
     * Create a profile in DB.
     *
     * @param newUserProfile profile of the new user.
     *
     * @return the user id or null if something wrong occurs.
     */
    suspend fun createProfile(newUserProfile: ProfileUser): String?

    /**
     * Fetch a profile from database and exposes it as a flow.
     *
     * @param userId Id of the user to fetch.
     *
     * @return a flow emitting Response regarding the state of the request.
     */
    fun fetchProfileFlow(userId: String): Flow<Response<ProfileUser>>

    /**
     * @return the userId of the logged in user or null if not
     */
    fun getLoggedInUserID(): String?

    /**
     * Checks if user exists in database
     *
     * @param userId Id of the user
     *
     * @return boolean
     */
    suspend fun doesUserIDExist(userId: String): Boolean
}