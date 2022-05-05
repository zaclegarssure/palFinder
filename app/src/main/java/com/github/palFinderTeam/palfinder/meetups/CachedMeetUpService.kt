package com.github.palFinderTeam.palfinder.meetups

import android.icu.util.Calendar
import com.github.palFinderTeam.palfinder.cache.FileCache
import com.github.palFinderTeam.palfinder.profile.ProfileUser
import com.github.palFinderTeam.palfinder.utils.Location
import com.github.palFinderTeam.palfinder.utils.Response
import com.github.palFinderTeam.palfinder.utils.context.ContextService
import com.github.palFinderTeam.palfinder.utils.generics.CachedRepository
import com.github.palFinderTeam.palfinder.utils.time.TimeService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CachedMeetUpService @Inject constructor(
    private val db: FirebaseMeetUpService,
    private val time: TimeService,
    private val contextProvider: ContextService
): MeetUpRepository{
    private var cacheJoined = FileCache("meetup_joined", JoinedMeetupListWrapper::class.java, true, contextProvider.get())

    private var cache = CachedRepository("meetup", MeetUp::class.java, db, time, contextProvider)

    override suspend fun create(obj: MeetUp): String? {
        val ret = cache.create(obj)
        if (ret != null){
            addJoinedMeetupToCache(ret)
            fetch(ret)
        }
        return ret
    }

    override suspend fun fetch(uuid: String): MeetUp? = cache.fetch(uuid)

    override suspend fun fetch(uuids: List<String>): List<MeetUp> = cache.fetch(uuids)

    override fun fetchAll(currentDate: Calendar?): Flow<List<MeetUp>> = cache.fetchAll(currentDate)

    override fun fetchFlow(uuid: String): Flow<Response<MeetUp>> = cache.fetchFlow(uuid)

    override suspend fun edit(uuid: String, obj: MeetUp): String? = cache.edit(uuid, obj)

    override suspend fun edit(uuid: String, field: String, value: Any): String? = cache.edit(uuid, field, value)

    override suspend fun exists(uuid: String): Boolean = cache.exists(uuid)

    private fun addJoinedMeetupToCache(meetUpId: String){
        val jml = if (cacheJoined.exist()){
            cacheJoined.get()
        } else {
            JoinedMeetupListWrapper(mutableListOf())
        }
        if (!jml.lst.contains(meetUpId)) {
            jml.lst.add(meetUpId)
            cacheJoined.store(jml)
        }
    }
    private fun removeJoinedMeetupFromCache(meetUpId: String){
        val jml = cacheJoined.get()
        while (jml.lst.contains(meetUpId)) {
            jml.lst.remove(meetUpId)
        }
        cacheJoined.store(jml)
    }
    private fun clearJoinedMeetupToCache(meetUpId: String){
        cacheJoined.store(JoinedMeetupListWrapper(mutableListOf()))
    }

    /**
     * Return List of all joined Meetup ID
     */
    fun getAllJoinedMeetupID(): List<String>{
        return if (cacheJoined.exist()){
            cacheJoined.get().lst
        } else {
            mutableListOf()
        }
    }

    override fun getMeetUpsAroundLocation(
        location: Location,
        radiusInKm: Double,
        currentDate: Calendar?
    ): Flow<Response<List<MeetUp>>> {
        return db.getMeetUpsAroundLocation(location, radiusInKm, currentDate)
    }


    override suspend fun joinMeetUp(meetUpId: String, userId: String, now: Calendar, profile: ProfileUser): Response<Unit> {
        return when(val ret = db.joinMeetUp(meetUpId, userId, now, profile)){
            is Response.Success -> {
                addJoinedMeetupToCache(meetUpId)
                ret
            }
            else -> ret
        }
    }

    override suspend fun leaveMeetUp(meetUpId: String, userId: String): Response<Unit> {
        return when(val ret = db.leaveMeetUp(meetUpId, userId)){
            is Response.Success -> {
                removeJoinedMeetupFromCache(meetUpId)
                ret
            }
            else -> ret
        }
    }

    override fun getUserMeetups(
        userId: String,
        currentDate: Calendar?
    ): Flow<Response<List<MeetUp>>> {
        return db.getUserMeetups(userId, currentDate)
    }

    private data class JoinedMeetupListWrapper(val lst: MutableList<String>)
}