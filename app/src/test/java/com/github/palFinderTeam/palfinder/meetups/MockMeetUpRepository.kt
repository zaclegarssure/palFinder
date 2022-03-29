package com.github.palFinderTeam.palfinder.meetups

import android.icu.util.Calendar
import com.github.palFinderTeam.palfinder.tag.Category
import com.github.palFinderTeam.palfinder.utils.Location
import com.github.palFinderTeam.palfinder.utils.Response
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class MockMeetUpRepository : MeetUpRepository {
    val db: HashMap<String, MeetUp> = hashMapOf()
    private var counter = 0

    override suspend fun getMeetUpData(meetUpId: String): MeetUp? {
        return db[meetUpId]
    }

    override suspend fun createMeetUp(newMeetUp: MeetUp): String? {
        val key = counter.toString()
        db[key] = newMeetUp.copy(uuid = key)
        counter.inc()
        return key
    }

    override suspend fun editMeetUp(meetUpId: String, field: String, value: Any): String? {
        if (db.containsKey(meetUpId)) {
            val oldVal = db[meetUpId]!!
            db[meetUpId] = when(field) {
                "name" -> oldVal.copy(name = value as String)
                "capacity" -> oldVal.copy(capacity = value as Int)
                "creator" -> oldVal.copy(creatorId = value as String)
                "description" -> oldVal.copy(description = value as String)
                "startDate" -> oldVal.copy(startDate = value as Calendar)
                "endDate" -> oldVal.copy(endDate = value as Calendar)
                "hasMaxCapacity" -> oldVal.copy(hasMaxCapacity = value as Boolean)
                "icon" -> oldVal.copy(iconId = value as String)
                "location" -> oldVal.copy(location = value as Location)
                "participants" -> oldVal.copy(participantsId = value as List<String>)
                "tags" -> oldVal.copy(tags = value as Set<Category>)
                else -> oldVal
            }
            return meetUpId
        }
        return null
    }

    override suspend fun editMeetUp(meetUpId: String, meetUp: MeetUp): String? {
        return if (db.containsKey(meetUpId)) {
            db[meetUpId] = meetUp
            meetUpId
        } else {
            null
        }
    }

    override fun getMeetUpsAroundLocation(
        location: Location,
        radiusInKm: Double
    ): Flow<Response<List<MeetUp>>> {
        return flow {
            val meetUps = db.values.filter { meetUp ->
                meetUp.location.distanceInKm(location) <= radiusInKm
            }

            emit(Response.Success(meetUps))
        }
    }

    @ExperimentalCoroutinesApi
    override fun getAllMeetUps(): Flow<List<MeetUp>> {
        return flow {
            emit(db.values.toList())
        }
    }

    @ExperimentalCoroutinesApi
    override fun getAllMeetUpsResponse(): Flow<Response<List<MeetUp>>> {
        return getAllMeetUps().map {
            Response.Success(it)
        }
    }

    override suspend fun joinMeetUp(
        meetUpId: String,
        userId: String,
        now: Calendar
    ): Response<Unit> {
        return if (db.containsKey(meetUpId)) {
            val meetUp = db[meetUpId] ?: return Response.Failure("Could not find meetup")
            if (meetUp.isParticipating(userId)) {
                return Response.Success(Unit)
            }
            // We ignore the date because it is tedious to mock
            if (meetUp.isFull()) {
                return Response.Failure("Cannot join, it is full.")
            }
            if (meetUp.creatorId == userId) {
                return Response.Failure("Cannot leave your own meetup.")
            }

            db[meetUpId] = meetUp.copy(participantsId = meetUp.participantsId.plus(userId))
            Response.Success(Unit)
        } else {
            Response.Failure("Could not join meetup")
        }
    }

    override suspend fun leaveMeetUp(meetUpId: String, userId: String): Response<Unit> {
        return try {
            val meetUp = getMeetUpData(meetUpId) ?: return Response.Failure("Could not find meetup.")
            if (!meetUp.isParticipating(userId)) {
                return Response.Failure("Cannot leave a meetup which was not joined before")
            }
            db[meetUpId] = meetUp.copy(participantsId = meetUp.participantsId.minus(userId))
            Response.Success(Unit)
        } catch (e: Exception) {
            Response.Failure(e.message.orEmpty())
        }
    }

    fun clearDB() {
        db.clear()
    }
}