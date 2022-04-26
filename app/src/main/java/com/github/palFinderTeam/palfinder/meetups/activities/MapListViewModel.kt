package com.github.palFinderTeam.palfinder.meetups.activities

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.palFinderTeam.palfinder.meetups.MeetUp
import com.github.palFinderTeam.palfinder.meetups.MeetUpRepository
import com.github.palFinderTeam.palfinder.profile.ProfileService
import com.github.palFinderTeam.palfinder.tag.Category
import com.github.palFinderTeam.palfinder.tag.TagsRepository
import com.github.palFinderTeam.palfinder.utils.Location
import com.github.palFinderTeam.palfinder.utils.Response
import com.github.palFinderTeam.palfinder.utils.time.RealTimeService
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import javax.inject.Inject


@ExperimentalCoroutinesApi
@HiltViewModel
class MapListViewModel @Inject constructor(
    val meetUpRepository: MeetUpRepository,
    val profileService: ProfileService,
    val timeService: RealTimeService,
    application: Application
) : AndroidViewModel(application) {
    private val _listOfMeetUpResponse: MutableLiveData<Response<List<MeetUp>>> = MutableLiveData()
    val listOfMeetUpResponse: LiveData<Response<List<MeetUp>>> = _listOfMeetUpResponse

    private val _tags: MutableLiveData<Set<Category>> = MutableLiveData(setOf())
    val tags: LiveData<Set<Category>> = _tags

    private val locationClient =
        LocationServices.getFusedLocationProviderClient(getApplication<Application>().applicationContext)

    private val _userLocation: MutableLiveData<Location> = MutableLiveData()
    val userLocation: LiveData<Location> = _userLocation

    private val _requestPermissions = MutableLiveData(false)
    val requestPermissions: LiveData<Boolean> = _requestPermissions

    // Search params.
    private val _searchRadius = MutableLiveData(INITIAL_RADIUS)
    private val _searchLocation = MutableLiveData(START_LOCATION)
    val searchLocation: LiveData<Location> = _searchLocation
    val searchRadius: LiveData<Double> = _searchRadius
    private var showOnlyJoined = false
    private var showOnlyAvailableInTime = true

    init {
        if (ActivityCompat.checkSelfPermission(
                getApplication<Application>().applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                getApplication<Application>().applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _requestPermissions.value = true
        } else {
            locationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    _userLocation.postValue(Location(location.longitude, location.latitude))
                }
            }
        }
    }

    /**
     * Set search parameters that you want to apply.
     *
     * @param location Location around which to search.
     * @param radiusInKm Radius of the search.
     * @param showOnlyJoined If true, only show joined meetups, this will ignore the radius.
     * @param showOnlyAvailable If true, only show meetup available at the current time.
     */
    fun setSearchParameters(
        location: Location? = null,
        radiusInKm: Double? = null,
        showOnlyJoined: Boolean? = null,
        showOnlyAvailable: Boolean? = null
    ) {
        location?.let {
            _searchLocation.value = it
        }
        radiusInKm?.let {
            _searchRadius.value = it
        }
        showOnlyJoined?.let {
            this.showOnlyJoined = it
        }
        showOnlyAvailable?.let {
            showOnlyAvailableInTime = it
        }
    }

    /**
     * Same as [setSearchParameters] but will also fetch meetups if needed.
     *
     * @param location Location around which to search.
     * @param radiusInKm Radius of the search.
     * @param showOnlyJoined If true, only show joined meetups, this will ignore the radius.
     * @param forceFetch If true, always fetch after assigning the params.
     */
    fun setSearchParamAndFetch(
        location: Location? = null,
        radiusInKm: Double? = null,
        showOnlyJoined: Boolean? = null,
        showOnlyAvailable: Boolean? = null,
        forceFetch: Boolean = false
    ) {
        // In case the search params are the same we don't fetch again.
        if (
            (!forceFetch)
            && (location == null || searchLocation.value == location)
            && (radiusInKm == null || searchRadius.value == radiusInKm)
            && (showOnlyJoined == null || this.showOnlyJoined == showOnlyJoined)
            && (showOnlyAvailable == null || showOnlyAvailableInTime == showOnlyAvailable)
        ) {
            return
        }

        setSearchParameters(location, radiusInKm, showOnlyJoined, showOnlyAvailable)
        fetchMeetUps()
    }

    /**
     * Fetch asynchronously the meetups with the different parameters value set in the viewModel.
     * This is not triggered automatically when a parameter (like radius) is changed, views have to
     * call it by themself.
     */
    fun fetchMeetUps() {
        if (showOnlyJoined) {
            fetchUserMeetUps()
        } else {
            getMeetupAroundLocation(searchLocation.value!!, searchRadius.value ?: INITIAL_RADIUS)
            //getMeetupAroundLocation(_searchLocation.value!!, 500.0)
        }
    }

    /**
     * Retrieves meetups around a certain location, and post them to the
     * meetUpResponse liveData.
     *
     * @param position Location around which it will fetch.
     * @param radiusInKm Radius of the search, in Km.
     */
    private fun getMeetupAroundLocation(
        position: Location,
        radiusInKm: Double
    ) {
        val date = if (showOnlyAvailableInTime) timeService.now() else null
        viewModelScope.launch {
            meetUpRepository.getMeetUpsAroundLocation(
                position,
                radiusInKm,
                currentDate = date
            ).collect {
                _listOfMeetUpResponse.postValue(it)
            }
        }
    }

    /**
     * Fetch all meetUp the user is taking part to.
     */
    private fun fetchUserMeetUps() {
        val userId = profileService.getLoggedInUserID()
        val date = if (showOnlyAvailableInTime) timeService.now() else null
        if (userId != null) {
            viewModelScope.launch {
                meetUpRepository.getAllMeetUps(loggedUser = userId, currentDate = date)
                    .collect {
                        _listOfMeetUpResponse.postValue(Response.Success(it))
                    }
            }
        } else {
            _listOfMeetUpResponse.value = Response.Failure("No login users.")
        }
    }

    /**
     *  Provides the tagContainer with the necessary tags and allows it to edit them.
     */
    val tagRepository = object : TagsRepository<Category> {
        override val tags: Set<Category>
            get() = _tags.value ?: setOf()

        override val isEditable = true
        override val allTags = Category.values().toSet()

        override fun removeTag(tag: Category): Boolean {
            val tags = _tags.value
            return if (tags == null || !tags.contains(tag)) {
                false
            } else {
                _tags.value = tags.minus(tag)
                true
            }
        }

        override fun addTag(tag: Category): Boolean {
            val tags = _tags.value
            return if (tags == null || tags.contains(tag)) {
                false
            } else {
                _tags.value = tags.plus(tag)
                true
            }
        }
    }


    companion object {
        const val INITIAL_RADIUS: Double = 400.0
        val START_LOCATION = Location(45.0, 45.0)
    }
}