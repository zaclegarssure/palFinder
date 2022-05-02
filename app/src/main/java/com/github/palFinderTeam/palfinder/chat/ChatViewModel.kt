package com.github.palFinderTeam.palfinder.chat

import android.app.Application
import android.icu.util.Calendar
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.palFinderTeam.palfinder.PalFinderApplication
import com.github.palFinderTeam.palfinder.R
import com.github.palFinderTeam.palfinder.profile.ProfileService
import com.github.palFinderTeam.palfinder.profile.ProfileUser
import com.github.palFinderTeam.palfinder.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    var chatService: ChatService,
    val profileService: ProfileService,
    application: Application
    ): AndroidViewModel(application) {

    val listOfMessage: MutableLiveData<MutableList<ChatMessage>> = MutableLiveData()
    val profilesData = HashMap<String, ProfileUser>()

    private lateinit var chatID: String

    /**
     * Send content to the connected Chat
     *
     * @param content message to send
     */
    fun sendMessage(content: String){
        val user = profileService.getLoggedInUserID()
        if (user != null) {
            val msg = ChatMessage(Calendar.getInstance(), user, content, false)

            viewModelScope.launch {
                chatService.postMessage(chatID, msg)
            }
        }
    }

    /**
     * Update List of Message
     *
     * @param chatID meetup id of the chat
     */
    fun connectToChat(chatID: String){
        this.chatID = chatID
        listOfMessage.value = mutableListOf()
        viewModelScope.launch {
            chatService.getAllMessageFromChat(chatID).collect {
                listOfMessage.postValue(it.sortedBy { it.sentAt.time }.toMutableList())
            }
        }
    }

    /**
     * Load Profile data into the view
     *
     * @param userId  Id of the user to load
     * @param pictureBox ImageView where to put the profile picture
     * @param nameBox TextView where to put the user name
     */
    suspend fun loadProfileData(userId: String, pictureBox: ImageView, nameBox: TextView){
        if (!profilesData.containsKey(userId)){
            viewModelScope.launch {
                profileService.fetchProfileFlow(userId).collect {
                    when(it) {
                        is Response.Success -> {
                            profilesData[userId] = it.data
                            loadCachedUser(userId, pictureBox, nameBox)
                        }
                        else -> {
                            nameBox.text = nameBox.context.getString(R.string.placeholder_name)
                        }
                    }
                }
            }
        }
        else{
            loadCachedUser(userId, pictureBox, nameBox)
        }
    }

    private suspend fun loadCachedUser(userId: String, pictureBox: ImageView, nameBox: TextView){
        nameBox.text = profilesData[userId]!!.username
        profilesData[userId]!!.pfp.loadImageInto(pictureBox, getApplication<Application>().applicationContext)
    }
}