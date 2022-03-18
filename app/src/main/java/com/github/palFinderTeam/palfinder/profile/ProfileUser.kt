package com.github.palFinderTeam.palfinder.profile

import android.icu.util.Calendar
import android.util.Log
import com.github.palFinderTeam.palfinder.utils.PrettyDate
import com.github.palFinderTeam.palfinder.utils.image.ImageInstance
import com.google.firebase.firestore.DocumentSnapshot
import java.io.Serializable

/**
 * A class to hold the data for a user to be displayed on the profile activity
 * Username as unique identifier
 */

data class ProfileUser(
    val uuid: String,
    val username: String,
    val name: String,
    val surname: String,
    val joinDate: Calendar,
    val pfp: ImageInstance
) : Serializable {

    companion object {
        const val JOIN_FORMAT = "Joined %s"

        /**
         * Provide a way to convert a Firestore query result, in a ProfileUser.
         */
        fun DocumentSnapshot.toProfileUser(): ProfileUser? {
            return try {
                val uuid = id
                val username = getString("username")!!
                val name = getString("name")!!
                val surname = getString("surname")!!
                val picture = getString("picture")!!
                val joinDate = getDate("join_date")!!

                val joinDateCal = Calendar.getInstance().apply { time = joinDate }

                ProfileUser(uuid, username, name, surname, joinDateCal, ImageInstance(picture))
            } catch (e: Exception) {
                Log.e("ProfileUser", "Error deserializing user", e)
                null
            }
        }
    }

    /**
     * @return a representation which is Firestore friendly of the UserProfile.
     */
    fun toFirestoreData(): HashMap<String, Any> {
        return hashMapOf(
            "name" to name,
            "surname" to surname,
            "username" to username,
            "join_date" to joinDate.time,
            "picture" to pfp.imgURL
        )
    }

    fun fullName(): String {
        return "$name $surname"
    }

    fun atUsername(): String {
        return "@$username"
    }

    fun prettyJoinTime(): String {
        val prettyDate = PrettyDate()
        return String.format(JOIN_FORMAT, prettyDate.timeDiff(joinDate))
    }
}
