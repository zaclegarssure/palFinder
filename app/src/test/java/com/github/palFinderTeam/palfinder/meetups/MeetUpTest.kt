package com.github.palFinderTeam.palfinder.meetups

import android.icu.util.Calendar
import com.github.palFinderTeam.palfinder.profile.ProfileUser
import com.github.palFinderTeam.palfinder.tag.Category
import com.github.palFinderTeam.palfinder.utils.Location
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat

class MeetUpTest {
    var meetUp: MeetUp? = null

    @Before
    fun initMeetup(){
        val date1 = Mockito.mock(Calendar::class.java)
        Mockito.`when`(date1.timeInMillis).thenReturn(0)

        val date2 = Mockito.mock(Calendar::class.java)
        Mockito.`when`(date2.timeInMillis).thenReturn(1)

        val user = ProfileUser("dummy","dummy","dummy", date1)

        meetUp = MeetUp(
            "dummy",
            user,
            "",
            "dummy",
            "dummy",
            date1,
            date2,
            Location(0.0,0.0),
            setOf(Category.DRINKING),
            true,
            2,
            mutableListOf(user)
        )
    }

    @Test
    fun isFullTest(){
        assertEquals(false, meetUp!!.isFull())
    }

    @Test
    fun canJoin(){
        val now = Mockito.mock(Calendar::class.java)
        Mockito.`when`(now.timeInMillis).thenReturn(0)

        assertEquals(true, meetUp!!.canJoin(now))
    }

    @Test
    fun cannotJoin(){
        val now = Mockito.mock(Calendar::class.java)
        Mockito.`when`(now.timeInMillis).thenReturn(5)

        assertEquals(false, meetUp!!.canJoin(now))
    }

    @Test
    fun isStarted(){
        val now = Mockito.mock(Calendar::class.java)
        Mockito.`when`(now.timeInMillis).thenReturn(0)

        assertEquals( true, meetUp!!.isStarted(now))
    }

    @Test
    fun isNotStarted(){
        val now = Mockito.mock(Calendar::class.java)
        Mockito.`when`(now.timeInMillis).thenReturn(-1)

        assertEquals( false, meetUp!!.isStarted(now))
    }

    @Test
    fun join(){
        val now = Mockito.mock(Calendar::class.java)
        Mockito.`when`(now.timeInMillis).thenReturn(0)
        val user = ProfileUser("dummy1","dummy2","dummy", now)

        meetUp!!.join(now, user)
        assertEquals( true, meetUp!!.isParticipating(user))
    }

    @Test
    fun joinAndLeave(){
        val now = Mockito.mock(Calendar::class.java)
        Mockito.`when`(now.timeInMillis).thenReturn(0)
        val user = ProfileUser("dummy1","dummy2","dummy", now)

        meetUp!!.join(now, user)
        meetUp!!.leave(user)
        assertEquals( false, meetUp!!.isParticipating(user))
    }

    @Test
    fun `to firebase document conversion keeps right values`() {
        val firebaseDoc = meetUp?.toFirestoreData()
        assertThat(firebaseDoc, notNullValue())
        val fireBaseDocNN = firebaseDoc!!
        assertThat(fireBaseDocNN["name"], `is`(meetUp!!.name))
        assertThat(fireBaseDocNN["description"], `is`(meetUp!!.description))
        assertThat(fireBaseDocNN["tags"], `is`(meetUp!!.tags.toList().map { it.toString() }))
        assertThat(fireBaseDocNN["location"], `is`(meetUp!!.location.toGeoPoint()))
    }
}