package com.github.palFinderTeam.palfinder.map

import android.icu.util.Calendar
import com.github.palFinderTeam.palfinder.meetups.MeetUp
import com.github.palFinderTeam.palfinder.profile.ProfileUser
import com.github.palFinderTeam.palfinder.utils.Location
import com.github.palFinderTeam.palfinder.utils.image.ImageInstance
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.mock

class MapsUtilsTest {


    private val utils = MapsUtils()

    private lateinit var meetup1: MeetUp
    private lateinit var meetup2: MeetUp
    private lateinit var meetup3: MeetUp
    private lateinit var meetup4: MeetUp

    private lateinit var position: LatLng


    private val mockMap = mock(GoogleMap::class.java)
    private fun createMockMarker(options: MarkerOptions): Marker{
        val marker = mock(Marker::class.java)
        marker.position = options.position
        marker.title = options.title
        return marker
    }

    @Before
    fun init() {

        val date1 = mock(Calendar::class.java)
        Mockito.`when`(date1.timeInMillis).thenReturn(0)

        val date2 = mock(Calendar::class.java)
        Mockito.`when`(date2.timeInMillis).thenReturn(1)

        Mockito.`when`(mockMap.addMarker(any(MarkerOptions::class.java))).thenAnswer { invocation ->
            val options = invocation.getArgument<MarkerOptions>(0)
            val marker = createMockMarker(options)
            marker
        }


        meetup1 = MeetUp(
            "1",
            ProfileUser("", "tempUser1", "tempUser1", date1, ImageInstance("icons/pfp_demo.jpg")),
            "",
            "meetUp1Name",
            "meetUp1Description",
            date1,
            date2,
            Location(0.0, 0.0),
            emptySet(),
            false,
            2,
            mutableListOf(ProfileUser("", "tempUser2", "tempUser2", date2, ImageInstance("icons/pfp_demo.jpg")))
        )

        meetup2 = MeetUp(
            "2",
            ProfileUser("", "tempUser2", "tempUser2", date2, ImageInstance("icons/pfp_demo.jpg")),
            "",
            "meetUp2Name",
            "meetUp2Description",
            date1,
            date2,
            Location(15.0, -15.0),
            emptySet(),
            false,
            2,
            mutableListOf(ProfileUser("", "tempUser2", "tempUser2", date2, ImageInstance("icons/pfp_demo.jpg")))
        )

        meetup3 = MeetUp(
            "3",
            ProfileUser("", "tempUser3", "user3", date2, ImageInstance("icons/pfp_demo.jpg")),
            "",
            "meetUp3Name",
            "meetUp3Description",
            date1,
            date2,
            Location(-30.0, 45.0),
            emptySet(),
            false,
            4,
            mutableListOf(ProfileUser("", "tempUser2", "tempUser2", date2, ImageInstance("icons/pfp_demo.jpg")))
        )

        meetup4 = MeetUp(
            "4",
            ProfileUser("", "tempUser4", "user4", date2, ImageInstance("icons/pfp_demo.jpg")),
            "",
            "meetUp4Name",
            "meetUp4Description",
            date1,
            date2,
            Location(69.0, 42.0),
            emptySet(),
            false,
            1337,
            mutableListOf(ProfileUser("", "tempUser2", "tempUser2", date2, ImageInstance("icons/pfp_demo.jpg")))
        )
    }


    @Test
    fun testSetMap(){
        utils.setMap(mockMap)
        Assert.assertEquals(true, utils.mapReady)
    }


    @Test
    fun testAddMeetupMarkerMapNotReady(){
        utils.mapReady = false
        utils.addMeetupMarker(meetup1)
        Assert.assertEquals(meetup1, utils.getMeetup(meetup1.uuid))
        Assert.assertEquals(null, utils.getMarker(meetup1.uuid))
    }

    @Test
    fun testAddMeetupMarkerMapReady(){
        utils.setMap(mockMap)

        utils.addMeetupMarker(meetup2)
        val expMarker2 = mockMap.addMarker(MarkerOptions().position(LatLng(meetup2.location.latitude, meetup2.location.longitude)).title(meetup2.uuid))
        Assert.assertEquals(meetup2, utils.getMeetup(meetup2.uuid))
        val actMarker = utils.getMarker(meetup2.uuid)
        Assert.assertEquals(expMarker2?.title, actMarker?.title)
        Assert.assertEquals(expMarker2?.position, actMarker?.position)

    }

    @Test
    fun testRemoveMarkerMapNotReady(){
        utils.mapReady = false
        utils.addMeetupMarker(meetup3)
        utils.removeMarker(meetup3.uuid)
        Assert.assertEquals(null, utils.getMeetup(meetup3.uuid))
    }

    @Test
    fun testRemoveMarkerMapReady(){
        utils.setMap(mockMap)
        utils.addMeetupMarker(meetup3)
        utils.removeMarker(meetup3.uuid)
        Assert.assertEquals(null, utils.getMeetup(meetup3.uuid))
        Assert.assertEquals(null, utils.getMarker(meetup3.uuid))

    }

    @Test
    fun clearMarkers(){
        utils.setMap(mockMap)
        utils.addMeetupMarker(meetup3)
        utils.addMeetupMarker(meetup4)
        utils.clearMarkers()
        Assert.assertEquals(meetup3, utils.getMeetup(meetup3.uuid))
        Assert.assertEquals(meetup4, utils.getMeetup(meetup4.uuid))
        Assert.assertEquals(null, utils.getMarker(meetup3.uuid))
        Assert.assertEquals(null, utils.getMarker(meetup4.uuid))
    }

    @Test
    fun clearMap(){
        utils.setMap(mockMap)
        utils.addMeetupMarker(meetup3)
        utils.addMeetupMarker(meetup4)
        utils.clearMap()
        Assert.assertEquals(null, utils.getMeetup(meetup3.uuid))
        Assert.assertEquals(null, utils.getMeetup(meetup4.uuid))
        Assert.assertEquals(null, utils.getMarker(meetup3.uuid))
        Assert.assertEquals(null, utils.getMarker(meetup4.uuid))
    }

    @Test
    fun testRefresh(){
        utils.setMap(mockMap)
        utils.mapReady = false

        utils.addMeetupMarker(meetup1)
        utils.addMeetupMarker(meetup2)

        val expMarker1 = mockMap.addMarker(MarkerOptions().position(LatLng(meetup1.location.latitude, meetup1.location.longitude)).title(meetup1.uuid))
        val expMarker2 = mockMap.addMarker(MarkerOptions().position(LatLng(meetup2.location.latitude, meetup2.location.longitude)).title(meetup2.uuid))
        utils.refresh()
        Assert.assertEquals(meetup1, utils.getMeetup(meetup1.uuid))
        Assert.assertEquals(meetup2, utils.getMeetup(meetup2.uuid))
        Assert.assertEquals(null, utils.getMarker(meetup1.uuid))
        Assert.assertEquals(null, utils.getMarker(meetup2.uuid))
        utils.mapReady = true
        utils.refresh()
        Assert.assertEquals(meetup1, utils.getMeetup(meetup1.uuid))
        Assert.assertEquals(meetup2, utils.getMeetup(meetup2.uuid))
        val actMarker1 = utils.getMarker(meetup1.uuid)
        val actMarker2 = utils.getMarker(meetup2.uuid)
        Assert.assertEquals(expMarker1?.position, actMarker1?.position)
        Assert.assertEquals(expMarker1?.title, actMarker1?.title)
        Assert.assertEquals(expMarker2?.position, actMarker2?.position)
        Assert.assertEquals(expMarker2?.title, actMarker2?.title)

    }

    @Test
    fun testCameraStartingPosition(){
        utils.mapReady = false
        val pos = LatLng(77.0, 52.0)
        utils.setCameraPosition(pos)
        Assert.assertEquals(pos, utils.getStartingCameraPosition())
        Assert.assertEquals(pos, utils.getCameraPosition())
    }


}