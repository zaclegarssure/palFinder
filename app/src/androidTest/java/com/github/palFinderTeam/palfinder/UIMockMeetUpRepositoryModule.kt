package com.github.palFinderTeam.palfinder

import android.icu.util.Calendar
import com.github.palFinderTeam.palfinder.di.MeetUpModule
import com.github.palFinderTeam.palfinder.meetups.MeetUp
import com.github.palFinderTeam.palfinder.meetups.MeetUpRepository
import com.github.palFinderTeam.palfinder.profile.ProfileUser
import com.github.palFinderTeam.palfinder.tag.Category
import com.github.palFinderTeam.palfinder.utils.Location
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [MeetUpModule::class]
)
/**
 * Provide a mock meetup database for every UI tests.
 */
object UIMockMeetUpRepositoryModule {

    val mockRepository = UIMockRepository()

    @Singleton
    @Provides
    fun provideFirebaseMeetUpService(): MeetUpRepository {
        return mockRepository
    }

    /**
     * Copy of MockMeetUpRepository, just for scoping reason you cannot reuse it.
     */
    class UIMockRepository : MeetUpRepository {
        val db: HashMap<String, MeetUp> = hashMapOf()
        private var counter = 0

        override suspend fun getMeetUpData(meetUpId: String): MeetUp? {
            return db[meetUpId]
        }

        override suspend fun createMeetUp(newMeetUp: MeetUp): String? {
            val key = counter.toString()
            db[key] = newMeetUp
            counter.inc()
            return key
        }

        override suspend fun editMeetUp(meetUpId: String, field: String, value: Any): String? {
            if (db.containsKey(meetUpId)) {
                val oldVal = db[meetUpId]!!
                db[meetUpId] = when(field) {
                    "name" -> oldVal.copy(name = value as String)
                    "capacity" -> oldVal.copy(capacity = value as Int)
                    "creator" -> oldVal.copy(creator = value as ProfileUser)
                    "description" -> oldVal.copy(description = value as String)
                    "startDate" -> oldVal.copy(startDate = value as Calendar)
                    "endDate" -> oldVal.copy(endDate = value as Calendar)
                    "hasMaxCapacity" -> oldVal.copy(hasMaxCapacity = value as Boolean)
                    "icon" -> oldVal.copy(icon = value as String)
                    "location" -> oldVal.copy(location = value as Location)
                    "participants" -> oldVal.copy(participants = value as MutableList<ProfileUser>)
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

        override suspend fun getMeetUpsAroundLocation(
            location: Location,
            radiusInM: Double
        ): List<MeetUp>? {
            TODO("Not yet implemented")
        }
    }
}