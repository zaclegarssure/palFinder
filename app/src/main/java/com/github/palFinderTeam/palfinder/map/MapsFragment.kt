package com.github.palFinderTeam.palfinder.map

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.palFinderTeam.palfinder.R
import com.github.palFinderTeam.palfinder.meetups.MeetUp
import com.github.palFinderTeam.palfinder.meetups.activities.LOCATION_RESULT
import com.github.palFinderTeam.palfinder.meetups.activities.MEETUP_SHOWN
import com.github.palFinderTeam.palfinder.meetups.activities.MapListViewModel
import com.github.palFinderTeam.palfinder.meetups.activities.MeetUpView
import com.github.palFinderTeam.palfinder.utils.Location
import com.github.palFinderTeam.palfinder.utils.Location.Companion.toLocation
import com.github.palFinderTeam.palfinder.utils.Response
import com.github.palFinderTeam.palfinder.utils.setNavigationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.IOException
import kotlin.math.pow


const val LOCATION_SELECT = "com.github.palFinderTeam.palFinder.MAP.LOCATION_SELECT"
const val LOCATION_SELECTED = "com.github.palFinderTeam.palFinder.MAP.LOCATION_SELECTED"
const val CONTEXT = "com.github.palFinderTeam.palFinder.MAP.CONTEXT"

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
    SearchView.OnQueryTextListener {

    private lateinit var selectLocationButton: FloatingActionButton
    private lateinit var selectMapTypeButton: FloatingActionButton
    private lateinit var context: Context
    private lateinit var map: GoogleMap

    val viewModel: MapListViewModel by activityViewModels()
    private val args: MapsFragmentArgs by navArgs()

    private val markers = HashMap<String, Marker>()
    private var meetUpForMarkers: Set<MeetUp> = emptySet()
    private var mapReady = false

    enum class Context {
        MARKER,
        SELECT_LOCATION
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_maps, container, false).rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context = args.context

        selectLocationButton = view.findViewById(R.id.bt_locationSelection)
        selectMapTypeButton = view.findViewById(R.id.bt_changeMapType)

        viewModel.setSearchParameters(showOnlyJoined = args.showOnlyJoined)
        if (args.startOnUserLocation) {
            viewModel.userLocation.observe(requireActivity()) { location ->
                viewModel.setSearchParameters(location = location)
            }
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map_tab) as SupportMapFragment

        mapFragment.getMapAsync(this)

        when (context) {
            Context.MARKER -> {

                viewModel.listOfMeetUpResponse.observe(viewLifecycleOwner) {
                    if (it is Response.Success) {
                        refreshMarkers(it.data)
                    }
                }
            }
            Context.SELECT_LOCATION -> {
            }
        }

        val searchLocation = view.findViewById<SearchView>(R.id.search_on_map)
        searchLocation.imeOptions = EditorInfo.IME_ACTION_DONE
        searchLocation.setOnQueryTextListener(this)
    }


    /**
     * When a meetUp marker is clicked, open the marker description
     */
    override fun onMarkerClick(marker: Marker): Boolean {
        when (context) {
            Context.MARKER -> {
                val id = marker.title
                if (id != null) {
                    val intent = Intent(requireActivity(), MeetUpView::class.java).apply {
                        putExtra(MEETUP_SHOWN, id)
                    }
                    startActivity(intent)
                    return true
                }
                return false
            }
            else -> return false
        }
    }

    private fun onMapClick(p0: LatLng) {
        // Add a marker if the map is used to select a location
        setSelectionMarker(p0.toLocation())

    }

    /**
     * Add or Update the Position Selection Marker
     */
    private fun setSelectionMarker(location: Location) {
        map.clear()
        map.addMarker(
            MarkerOptions().position(location.toLatLng()).title("Here").draggable(true)
        )
        selectLocationButton.apply { this.show() }
        selectLocationButton.setOnClickListener {
            onConfirm(location)
        }
    }

    /**
     * Return the selected Location to the previous activity through the viewModel.
     */
    private fun onConfirm(location: Location) {
        setNavigationResult(location, LOCATION_RESULT)
        findNavController().navigateUp()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true

        // If we don't have requestPermissions that mean we have the right permissions.
        if (viewModel.requestPermissions.value == false) {
            map.isMyLocationEnabled = true
        }

        mapReady = true

        viewModel.searchLocation.value?.let {
            map.moveCamera(CameraUpdateFactory.newLatLng(it.toLatLng()))
        }

        when (context) {
            Context.MARKER -> {
                map.setOnMarkerClickListener(this)
                // Only update markers when camera is still
                map.setOnCameraIdleListener {
                    fetchMeetUpsInView()
                }
                fetchMeetUpsInView()
            }
            Context.SELECT_LOCATION -> {
                map.setOnMapClickListener { onMapClick(it) }
                args.startSelection?.let {
                    onMapClick(it.toLatLng())
                }
            }
        }

        selectMapTypeButton.setOnClickListener {
            changeMapType()
        }
    }

    override fun onQueryTextSubmit(p0: String?): Boolean {
        var addressList: List<Address>? = null

        if (p0 == null || p0 == "") {
            Toast.makeText(
                requireContext(),
                getString(R.string.search_no_location),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val geoCoder = Geocoder(requireContext())
            try {
                addressList = geoCoder.getFromLocationName(p0, 1)

            } catch (e: IOException) {
                e.printStackTrace()
            }
            if (addressList == null || addressList.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.search_location_not_found),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val address = addressList[0]
                val location = Location(address.longitude, address.latitude)
                viewModel.setSearchParameters(location = location)
                viewModel.fetchMeetUps()
                map.animateCamera(CameraUpdateFactory.newLatLng(location.toLatLng()))
            }
        }
        return false
    }

    override fun onQueryTextChange(p0: String?): Boolean {
        return false
    }

    private fun changeMapType() {
        if (map.mapType == GoogleMap.MAP_TYPE_NORMAL) {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
        } else {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
        }
    }

    /**
     * refresh the map to remove Marker that are not in the meetup list and
     * add those of the meetup list that are not in the map
     * if the map is not ready, do nothing
     */
    private fun refreshMarkers(meetUpList: List<MeetUp>) {
        if (!mapReady) {
            return
        }

        val deletedMarkers = meetUpForMarkers.minus(meetUpList)
        val addedMarkers = meetUpList.minus(meetUpForMarkers)
        meetUpForMarkers = meetUpList.toSet()
        deletedMarkers.forEach { markers[it.uuid]?.remove() }
        addedMarkers.forEach { meetUp ->
            val position = LatLng(meetUp.location.latitude, meetUp.location.longitude)
            map.addMarker(MarkerOptions().position(position).title(meetUp.uuid))
                ?.let { markers[meetUp.uuid] = it }
        }
    }

    /**
     * Fetch meetups that are inside the visible area of the map.
     */
    private fun fetchMeetUpsInView() {
        if (!mapReady) {
            return
        }

        val earthCircumference = 40000.0
        // at zoom 0, the map is of size 256x256 pixels and for every zoom, the number of pixel is multiplied by 2
        val radiusAtZoom0 = earthCircumference / 256
        val radius = radiusAtZoom0 / 2.0.pow(map.cameraPosition.zoom.toDouble())
        val position: Location = map.cameraPosition.target.toLocation()

        viewModel.setSearchParameters(location = position, radiusInKm = radius)
        viewModel.fetchMeetUps()
    }

    fun getMapLocation(): Location {
        return if (!mapReady) {
            MapListViewModel.START_LOCATION
        } else {
            map.cameraPosition.target.toLocation()
        }
    }

    fun getMapType(): Int {
        return if (!mapReady) {
            GoogleMap.MAP_TYPE_NORMAL
        } else {
            map.mapType
        }
    }

    fun setMapLocation(location: Location) {
        if (mapReady) {
            map.animateCamera(CameraUpdateFactory.newLatLng(location.toLatLng()))
        }
    }
}