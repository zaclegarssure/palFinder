package com.github.palFinderTeam.palfinder.meetups.activities

import androidx.lifecycle.*
import com.github.palFinderTeam.palfinder.meetups.MeetUp
import com.github.palFinderTeam.palfinder.meetups.MeetUpRepository
import com.github.palFinderTeam.palfinder.profile.ProfileService
import com.github.palFinderTeam.palfinder.tag.Category
import com.github.palFinderTeam.palfinder.tag.TagsRepository
import com.github.palFinderTeam.palfinder.utils.Location
import com.github.palFinderTeam.palfinder.utils.Response
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.pow


@ExperimentalCoroutinesApi
@HiltViewModel
class MapListViewModel @Inject constructor(
    val meetUpRepository: MeetUpRepository,
    val profileService: ProfileService
) : ViewModel() {
    private val _listOfMeetUpResponse: MutableLiveData<Response<List<MeetUp>>> = MutableLiveData()
    val listOfMeetUpResponse: LiveData<Response<List<MeetUp>>> = _listOfMeetUpResponse
    lateinit var meetupList: List<MeetUp>
    private val _tags: MutableLiveData<Set<Category>> = MutableLiveData(setOf())
    val tags: LiveData<Set<Category>> = _tags
    var startingCameraPosition: LatLng = LatLng(46.31, 6.38)
    var startingZoom: Float = 15f
    var showOnlyJoined: Boolean = false


    lateinit var map: GoogleMap
    var mapReady = false
    private var markers = HashMap<String, Marker>()


    /**
     * get the Marker in this utils memory corresponding to this id
     * @param id: Unique identifier of the meetup
     * @return the marker corresponding to the id, null if non existent
     */
    fun getMarker(id: String):Marker?{
        return markers[id]
    }

    /**
     * set the map to which utils functions will be applied
     * @param map: GoogleMap
     */
    fun setGmap(map : GoogleMap){
        this.map = map
        this.mapReady = true
        setPositionAndZoom(startingCameraPosition, startingZoom)
    }

    fun update(){
        if(false){//getZoom() < 7f){
            //TODO get only the joined meetup

        } else{
            val earthCircumference = 40000.0
            // at zoom 0, the map is of size 256x256 pixels and for every zoom, the number of pixel is multiplied by 2
            val radiusAtZoom0 = earthCircumference/256
            val radius = radiusAtZoom0/2.0.pow(getZoom().toDouble())
            setGetMeetupAroundLocation(getCameraPosition(), radius)

        }
    }

    fun setGetMeetupAroundLocation(position:LatLng, radius:Double){
        viewModelScope.launch {
            meetUpRepository.getMeetUpsAroundLocation(
                Location(position.longitude, position.latitude),
                1450.0
            ).collect {
                _listOfMeetUpResponse.postValue(it)
            }
        }
    }

    /**
     * clear the map of all markers
     */
    fun clearMarkers(){
        val iterator = markers.iterator()
        while(iterator.hasNext()){
            val marker = iterator.next()
            marker.value.remove()
            iterator.remove()
        }

    }

    /**
     * set the camera of the map to a position,
     * if the map is not ready, set the starting location to this position
     * @param position: new position of the camera
     */
    fun setCameraPosition(position: LatLng){
        if(mapReady) {
            map.moveCamera(CameraUpdateFactory.newLatLng(position))
        }else startingCameraPosition = position
    }

    /**
     * get the current camera position
     * if map not ready, return the starting camera position
     * @return the camera position
     */
    fun getCameraPosition():LatLng{
        return if(mapReady) map.cameraPosition.target
        else startingCameraPosition
    }




    /**
     * set the zoom
     * if map not ready, set the starting zoom
     * @param zoom: new zoom of the camera
     */
    fun setZoom(zoom: Float){
        if(mapReady) {
            map.moveCamera(CameraUpdateFactory.zoomTo(zoom))
        }else{
            startingZoom = zoom
        }
    }

    /**
     * refresh the map to remove Marker that are not in the meetup list and
     * add those of the meetup list that are not in the map
     * if the map is not ready, do nothing
     */
    fun refresh() {
        if (!mapReady) return
        val response = listOfMeetUpResponse.value

        meetupList = if(response is Response.Success){
            response.data
        }else emptyList()
        clearMarkers()

        meetupList.forEach{ meetUp ->
            val position = LatLng(meetUp.location.latitude, meetUp.location.longitude)
            val marker = map.addMarker(MarkerOptions().position(position).title(meetUp.uuid))
                ?.let { markers[meetUp.uuid] = it }
        }
    }

    /**
     * set the position and the zoom
     * if the map is not ready, set both starting values
     * @param position : the new camera position of the camera
     * @param zoom : the new zoom of the camera
     */
    fun setPositionAndZoom(position: LatLng, zoom: Float){
        if(mapReady){
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoom))
        }else{
            startingCameraPosition = position
            startingZoom = zoom
        }
    }

    /**
     * get the current zoom
     * if map not ready, return the starting zoom
     * @return the zoom
     */
    fun getZoom(): Float{
        return if(mapReady) map.cameraPosition.zoom
        else startingZoom
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

    /**
     * Return the currently logged in user id
     */
    fun getUser():String?{
        return profileService.getLoggedInUserID()
    }
}