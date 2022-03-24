package com.github.palFinderTeam.palfinder.map

import android.content.Intent
import android.icu.util.Calendar
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.github.palFinderTeam.palfinder.UIMockMeetUpRepositoryModule
import com.github.palFinderTeam.palfinder.meetups.MeetUp
import com.github.palFinderTeam.palfinder.meetups.MeetUpRepository
import com.github.palFinderTeam.palfinder.meetups.activities.MEETUP_SHOWN
import com.github.palFinderTeam.palfinder.utils.Location
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class MapsActivityTest {


    @Inject
    lateinit var meetUpRepository: MeetUpRepository


    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    var fineLocationPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)

    @get:Rule
    var coarseLocationPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.ACCESS_COARSE_LOCATION)


    lateinit var utils: MapsActivityViewModel

    @Before
    fun init() {
        hiltRule.inject()
        utils = MapsActivityViewModel(meetUpRepository)
    }

    /*
    @Test
    fun testMarkerClick() = runTest() {

        val intent = Intent(ApplicationProvider.getApplicationContext(), MapsActivity::class.java)
        val scenario = ActivityScenario.launch<MapsActivity>(intent)


        val id = "id"
        val lat = 15.0
        val long = -15.0

        val date1 = Calendar.getInstance()
        date1!!.set(2022, 2, 1, 0, 0, 0)
        val date2 = Calendar.getInstance()
        date2!!.set(2022, 2, 1, 1, 0, 0)

        val meetup = MeetUp(
            id,
            "user4",
            "",
            "meetUp4Name",
            "meetUp4Description",
            date1,
            date2,
            Location(long, lat),
            emptySet(),
            false,
            42,
            listOf("user2")
        )

        (meetUpRepository as UIMockMeetUpRepositoryModule.UIMockRepository).db[meetup.uuid] = meetup
        utils.refresh()

        scenario.use {

            Intents.init()

            utils.setCameraPosition(LatLng(lat, long))
            utils.updateFetcherLocation(LatLng(lat, long))


            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            val marker = device.findObject(
                UiSelector().descriptionContains("Google Map")
                    .childSelector(UiSelector().descriptionContains(id))
            )
            marker.waitForExists(1000)
            marker.click()
            intended(IntentMatchers.hasExtra(MEETUP_SHOWN, id))
            Intents.release()
        }


    }*/


}
