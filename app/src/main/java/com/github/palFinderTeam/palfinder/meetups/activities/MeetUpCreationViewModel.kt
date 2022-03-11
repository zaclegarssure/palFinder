package com.github.palFinderTeam.palfinder.meetups.activities

import android.icu.util.Calendar
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.palFinderTeam.palfinder.meetups.FirebaseMeetUpService
import com.github.palFinderTeam.palfinder.meetups.MeetUp
import com.github.palFinderTeam.palfinder.meetups.TempUser
import com.github.palFinderTeam.palfinder.utils.Location
import com.github.palFinderTeam.palfinder.utils.isDeltaBefore
import kotlinx.coroutines.launch

class MeetUpCreationViewModel: ViewModel() {
    private var uuid: String? = null

    private val _startDate: MutableLiveData<Calendar> = MutableLiveData()
    private val _endDate: MutableLiveData<Calendar> = MutableLiveData()
    private val _capacity: MutableLiveData<Int> = MutableLiveData(1)
    private val _hasMaxCapacity: MutableLiveData<Boolean> = MutableLiveData(false)
    private val _name: MutableLiveData<String> = MutableLiveData("")
    private val _description: MutableLiveData<String> = MutableLiveData("")

    private val _sendSuccess: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val startDate: LiveData<Calendar> = _startDate
    val endDate: LiveData<Calendar> = _endDate
    val capacity: LiveData<Int> = _capacity
    val hasMaxCapacity: LiveData<Boolean> = _hasMaxCapacity
    val name: LiveData<String> = _name
    val description: LiveData<String> = _description
    val sendSuccess: LiveData<Boolean> = _sendSuccess

    fun setStartDate(date: Calendar) {
        _startDate.value = date
        checkDateIntegrity()
    }

    fun setEndDate(date: Calendar) {
        _endDate.value = date
        checkDateIntegrity()
    }

    fun setCapacity(capacity: Int) {
        _capacity.value = capacity
    }

    fun setHasMaxCapacity(hasMaxCapacity: Boolean) {
        _hasMaxCapacity.value = hasMaxCapacity
    }

    fun getMeetUpId() = uuid

    fun loadMeetUp(meetUpId: String) {
        viewModelScope.launch {
            val meetUp = FirebaseMeetUpService.getMeetUpData(meetUpId)
            if (meetUp != null) {
                uuid = meetUp.uuid
                _name.value = meetUp.name
                _description.value = meetUp.description
                _startDate.value = meetUp.startDate
                _endDate.value = meetUp.endDate
                _hasMaxCapacity.value = meetUp.hasMaxCapacity
                _capacity.value = meetUp.capacity
            }

        }
    }


    fun sendMeetUp() {
        if (uuid == null) {
            val meetUp = MeetUp(
                "",
                TempUser("d", "michel"),
                "wathever",
                name.value!!,
                description.value!!,
                startDate.value!!,
                endDate.value!!,
                Location(1.0, 2.0),
                listOf(),
                hasMaxCapacity.value!!,
                capacity.value!!,
                mutableListOf()
            )
            // create new meetup
            viewModelScope.launch {
                val meetUpId = FirebaseMeetUpService.createMeetUp(meetUp)
                _sendSuccess.value = (meetUpId != null)
            }
        } else {
            // edit existing meetup
        }
    }

    /**
     * Enforce that End Date is After Start Date
     */
    private fun checkDateIntegrity(){
        if (startDate.value == null || endDate.value == null) {
            return
        }
        // Check if at least defaultTimeDelta between start and end
        if (!startDate.value!!.isDeltaBefore(endDate.value!!, defaultTimeDelta)){
            val newCalendar = Calendar.getInstance()
            newCalendar.timeInMillis = startDate.value!!.timeInMillis
            newCalendar.add(Calendar.MILLISECOND, defaultTimeDelta)
            _endDate.value = newCalendar
        }
    }
}