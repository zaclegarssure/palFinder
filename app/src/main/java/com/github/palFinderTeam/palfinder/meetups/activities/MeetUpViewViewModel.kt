package com.github.palFinderTeam.palfinder.meetups.activities

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.palFinderTeam.palfinder.R
import com.github.palFinderTeam.palfinder.meetups.MeetUp
import com.github.palFinderTeam.palfinder.meetups.MeetUpRepository
import com.github.palFinderTeam.palfinder.profile.ProfileService
import com.github.palFinderTeam.palfinder.tag.Category
import com.github.palFinderTeam.palfinder.tag.TagsRepository
import com.github.palFinderTeam.palfinder.utils.Response
import com.github.palFinderTeam.palfinder.utils.time.TimeService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MeetUpViewViewModel @Inject constructor(
    private val meetUpRepository: MeetUpRepository,
    private val profileService: ProfileService,
    private val timeService: TimeService
) : ViewModel() {
    private var _meetUp: MutableLiveData<MeetUp> = MutableLiveData<MeetUp>()
    val meetUp: LiveData<MeetUp> = _meetUp

    /**
     * Fetch given meetup and update corresponding livedata.
     *
     * @param meetUpId Id of the meetup to be fetched.
     */
    fun loadMeetUp(meetUpId: String) {
        viewModelScope.launch {
            val fetchedMeetUp = meetUpRepository.getMeetUpData(meetUpId)
            // TODO do something on error
            fetchedMeetUp?.let { _meetUp.value = it }
        }
    }

    fun getMeetupID(): String{
        return meetUp.value!!.uuid
    }

    fun getUsernameOf(uuid: String, callback: (String?)->Unit){
        viewModelScope.launch {
            val user = profileService.fetchUserProfile(uuid)?.username
            callback(user)
        }
    }

    fun hasJoin(): Boolean {
        val uuid = profileService.getLoggedInUserID()
        return if (uuid != null){
            meetUp.value!!.isParticipating(uuid)
        }
        else{
            false
        }
    }
    fun isCreator(): Boolean {
        val uuid = profileService.getLoggedInUserID()
        return if (uuid != null){
            meetUp.value!!.creatorId == uuid
        }
        else{
            false
        }
    }

    fun joinOrLeave(context: Context){
        val uuid = profileService.getLoggedInUserID()
        if (uuid != null){
            viewModelScope.launch {
                if (hasJoin()) {
                    when(val ret = meetUpRepository.leaveMeetUp(meetUp.value!!.uuid, uuid)){
                        is Response.Failure -> Toast.makeText(context, ret.errorMessage, Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(context, R.string.meetup_view_left, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    when(val ret = meetUpRepository.joinMeetUp(meetUp.value!!.uuid, uuid, timeService.now())){
                        is Response.Failure -> Toast.makeText(context, ret.errorMessage, Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(context, R.string.meetup_view_joined, Toast.LENGTH_SHORT).show()
                    }
                }
                loadMeetUp(meetUp.value!!.uuid)
            }
        }
    }

    /**
     * Describe how tags should be transferred from this viewModel to the tag viewModel.
     */
    val tagRepository = object : TagsRepository<Category> {
        override val tags: Set<Category>
            get() = meetUp.value?.tags ?: setOf()

        override val isEditable = false
        override val allTags = Category.values().toSet()

        override fun removeTag(tag: Category): Boolean = false

        override fun addTag(tag: Category): Boolean = false
    }
}