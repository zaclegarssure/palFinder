package com.github.palFinderTeam.palfinder

import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.test.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.github.palFinderTeam.palfinder.meetups.MeetUp
import com.github.palFinderTeam.palfinder.meetups.activities.RecyclerViewMatcher
import com.github.palFinderTeam.palfinder.profile.*
import com.github.palFinderTeam.palfinder.utils.EspressoIdlingResource
import com.github.palFinderTeam.palfinder.utils.image.ImageInstance
import com.github.palFinderTeam.palfinder.utils.launchFragmentInHiltContainer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matchers.notNullValue
import org.junit.*
import javax.inject.Inject

@ExperimentalCoroutinesApi
@HiltAndroidTest
class ProfileActivityTest {

    private lateinit var userLouca: ProfileUser
    private lateinit var userCat: ProfileUser
    private lateinit var userLongBio: ProfileUser
    private lateinit var userNoBio: ProfileUser

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var profileService: ProfileService

    @Before
    fun getProfile() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        hiltRule.inject()

        userLouca = ProfileUser(
            "1234",
            "gerussi",
            "Louca",
            "Gerussi",
            Calendar.getInstance(),
            ImageInstance("icons/cat.png")
        )
        userCat = ProfileUser(
            "12345",
            "gerussi",
            "Louca",
            "Gerussi",
            Calendar.getInstance(),
            ImageInstance("https://fail"),
            "I am cat"
        )
        userLongBio = ProfileUser(
            "123456",
            "vlong",
            "Very",
            "Long",
            Calendar.getInstance(),
            ImageInstance(""),
            "Hello I am still a cat but now with a longer description. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam."
        )
        userNoBio = ProfileUser(
            "123",
            "no",
            "No",
            "Bio",
            Calendar.getInstance(),
            ImageInstance(""),
            ""
        )
    }

    @After
    fun cleanUp() {
        (profileService as UIMockProfileServiceModule.UIMockProfileService).clearDB()
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Test
    fun fullNameIsCorrectlyDisplayed() = runTest {
        val id = profileService.create(userLouca)
        // Create intent with data to inject
        val intent =
            Intent(ApplicationProvider.getApplicationContext(), ProfileActivity::class.java)
                .apply {
                    putExtra(USER_ID, id)
                }
        // Launch activity
        val scenario = ActivityScenario.launch<ProfileActivity>(intent)
        scenario.use {
            onView(withId(R.id.userProfileName)).check(
                matches(
                    withText(userLouca.fullName())
                )
            )
        }
    }

    @Test
    fun userHasNoBioDisplaysTitleDifferently() = runTest {
        val id = profileService.create(userLouca)
        val intent =
            Intent(ApplicationProvider.getApplicationContext(), ProfileActivity::class.java)
                .apply { putExtra(USER_ID, id) }
        // Launch activity
        val scenario = ActivityScenario.launch<ProfileActivity>(intent)
        scenario.use {
            onView(withId(R.id.userProfileAboutTitle)).check(
                matches(
                    withText(getResourceString(R.string.no_desc))
                )
            )
        }
    }

    @Test
    fun userWithBioDisplaysShortBioEntirely() = runTest {
        val id = profileService.create(userCat)
        val intent =
            Intent(ApplicationProvider.getApplicationContext(), ProfileActivity::class.java)
                .apply { putExtra(USER_ID, id) }

        // Launch activity
        val scenario = ActivityScenario.launch<ProfileActivity>(intent)
        scenario.use {
            onView(withId(R.id.userProfileDescription)).check(
                matches(
                    withText(userCat.description)
                )
            )
        }
    }

    @Test
    fun noBioHasNoReadMore() = runTest {
        val id = profileService.create(userNoBio)
        val intent =
            Intent(ApplicationProvider.getApplicationContext(), ProfileActivity::class.java)
                .apply { putExtra(USER_ID, id) }

        // Launch activity
        val scenario = ActivityScenario.launch<ProfileActivity>(intent)
        scenario.use {
            onView(withId(R.id.userProfileDescOverflow)).check(
                matches(
                    withEffectiveVisibility(Visibility.GONE)
                )
            )
        }
    }

    @Test
    fun userExpandsBioCorrectly() = runTest {
        val id = profileService.create(userLongBio)
        val intent =
            Intent(ApplicationProvider.getApplicationContext(), ProfileActivity::class.java)
                .apply { putExtra(USER_ID, id) }

        val scenario = ActivityScenario.launch<ProfileActivity>(intent)

        scenario.use {
            onView(withId(R.id.userProfileDescOverflow)).perform(scrollTo(), ViewActions.click())
            onView(withId(R.id.userProfileDescription)).check(matches(withText(userLongBio.description)))
        }
    }

    @Test
    fun httpLoadImageAndClearCache() = runTest {
        val id = profileService.create(userLouca)
        val intent =
            Intent(ApplicationProvider.getApplicationContext(), ProfileActivity::class.java)
                .apply { putExtra(USER_ID, id) }

        // Launch activity
        val scenario = ActivityScenario.launch<ProfileActivity>(intent)
        scenario.use {
            onView(withId(R.id.userProfileName)).check(
                matches(
                    withText(userLouca.fullName())
                )
            )
            // Check status of image after cache clear
            userCat.pfp.clearImageCache()
            Assert.assertEquals(ImageInstance.NOT_LOADED, userCat.pfp.imgStatus)
        }
    }

    private fun getResourceString(id: Int): String? {
        val targetContext: Context = InstrumentationRegistry.getTargetContext()
        return targetContext.getResources().getString(id)
    }


    @Test
    fun followWorksOnProfileView() = runTest {
        val userid = profileService.create(userLouca)
        val id2 = profileService.create(userCat)

        (profileService as UIMockProfileServiceModule.UIMockProfileService).setLoggedInUserID(userid!!)

        assertThat(userid, notNullValue())

        val intent =
            Intent(ApplicationProvider.getApplicationContext(), ProfileActivity::class.java)
                .apply { putExtra(USER_ID, id2) }
        val scenario =
            ActivityScenario.launch<ProfileActivity>(intent)
        scenario!!.use {

            assert(!profileService.fetch(userid)!!.following.contains(id2))
            assert(!profileService.fetch(id2!!)!!.followed.contains(userid))
            onView(
                withId(R.id.button_follow_profile)
            ).perform(ViewActions.click())
            assert(profileService.fetch(userid)!!.following.contains(id2))
            assert(profileService.fetch(id2)!!.followed.contains(userid))
            onView(
                withId(R.id.button_follow_profile)
            ).perform(ViewActions.click())
            assert(!profileService.fetch(userid)!!.following.contains(id2))
            assert(!profileService.fetch(id2)!!.followed.contains(userid))
        }
    }
}

