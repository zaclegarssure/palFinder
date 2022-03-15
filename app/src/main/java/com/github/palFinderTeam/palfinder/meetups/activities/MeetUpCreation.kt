package com.github.palFinderTeam.palfinder.meetups.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import com.github.palFinderTeam.palfinder.R
import com.github.palFinderTeam.palfinder.tag.Category
import com.github.palFinderTeam.palfinder.tag.TagsViewModel
import com.github.palFinderTeam.palfinder.tag.TagsViewModelFactory
import com.github.palFinderTeam.palfinder.utils.LiveDataExtension.observeOnce
import com.github.palFinderTeam.palfinder.utils.addToFragmentManager
import com.github.palFinderTeam.palfinder.utils.askTime
import com.github.palFinderTeam.palfinder.utils.createTagFragmentModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

const val MEETUP_EDIT = "com.github.palFinderTeam.palFinder.meetup_view.MEETUP_EDIT"
const val defaultTimeDelta = 1000 * 60 * 60

@SuppressLint("SimpleDateFormat") // Apps Crash with the alternative to SimpleDateFormat
@AndroidEntryPoint
class MeetUpCreation : AppCompatActivity() {
    private val viewModel: MeetUpCreationViewModel by viewModels()
    private lateinit var tagsViewModelFactory: TagsViewModelFactory<Category>
    private lateinit var tagsViewModel: TagsViewModel<Category>

    private var dateFormat = SimpleDateFormat()

    private lateinit var hasLimitCheckBox: CheckBox
    private lateinit var limitEditText: EditText
    private lateinit var nameEditText: EditText
    private lateinit var descriptionEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meet_up_creation)

        dateFormat = SimpleDateFormat(getString(R.string.date_long_format))

        hasLimitCheckBox = findViewById(R.id.hasCapacityButton)
        limitEditText = findViewById(R.id.et_Capacity)
        nameEditText = findViewById(R.id.et_EventName)
        descriptionEditText = findViewById(R.id.et_Description)

        bindUI()

        // Create tag fragment
        tagsViewModelFactory = TagsViewModelFactory(viewModel.tagRepository)
        tagsViewModel = createTagFragmentModel(this, tagsViewModelFactory)

        if (savedInstanceState == null) {
            addToFragmentManager(supportFragmentManager, R.id.fc_tags)
        }

        // Load meetup or start from scratch
        if (intent.hasExtra(MEETUP_EDIT)) {
            val meetupId = intent.getStringExtra(MEETUP_EDIT)
            if (meetupId != null) {
                viewModel.loadMeetUp(meetupId)
            }
        } else {
            viewModel.fillWithDefaultValues()
        }

        // Make sure tags are refreshed once when fetching from DB
        viewModel.tags.observeOnce(this) {
            tagsViewModel.refreshTags()
        }
    }

    private fun bindUI() {
        viewModel.startDate.observe(this) { newDate ->
            setTextView(R.id.tv_StartDate, dateFormat.format(newDate))
        }
        viewModel.endDate.observe(this) { newDate ->
            setTextView(R.id.tv_EndDate, dateFormat.format(newDate))
        }
        viewModel.name.observeOnce(this) {
            setTextView(R.id.et_EventName, it)
        }
        nameEditText.doAfterTextChanged { text ->
            viewModel.setName(text.toString())
        }
        viewModel.description.observeOnce(this) {
            setTextView(R.id.et_Description, it)
        }
        descriptionEditText.doAfterTextChanged { text ->
            viewModel.setDescription(text.toString())
        }
        viewModel.hasMaxCapacity.observeOnce(this) { hasMaxCapacity ->
            hasLimitCheckBox.isChecked = hasMaxCapacity
            limitEditText.isEnabled = hasMaxCapacity
        }
        viewModel.capacity.observeOnce(this) {
            setTextView(R.id.et_Capacity, it.toString())
        }
        hasLimitCheckBox.setOnCheckedChangeListener { _, isChecked ->
            setCapacityField(isChecked)
            viewModel.setHasMaxCapacity(isChecked)
        }
        limitEditText.doAfterTextChanged { text ->
            val parsed = text.toString().toIntOrNull()
            if (parsed != null) {
                viewModel.setCapacity(parsed)
            } else {
                // TODO find something meaningful to do
                viewModel.setCapacity(1)
            }
        }
        limitEditText.isEnabled = hasLimitCheckBox.isChecked
    }

    private fun setCapacityField(isEditable: Boolean) {
        if (isEditable) {
            limitEditText.isEnabled = true
        } else {
            limitEditText.isEnabled = false
            limitEditText.text.clear()
        }
    }

    private fun setTextView(id: Int, value: String) {
        findViewById<TextView>(id).apply { this.text = value }
    }

    fun onStartTimeSelectButton(v: View) {
        askTime(supportFragmentManager).thenAccept {
            viewModel.setStartDate(it)
        }
    }

    fun onEndTimeSelectButton(v: View) {
        askTime(supportFragmentManager).thenAccept {
            viewModel.setEndDate(it)
        }
    }


    /**
     * Check Name and Description are present
     */
    private fun checkFieldValid(name: String, description: String): Boolean {
        if (name == "" || description == "") {
            showMessage(
                R.string.meetup_creation_missing_name_desc,
                R.string.meetup_creation_missing_name_desc_title
            )
            return false
        }
        return true
    }

    fun onDone(v: View) {

        // Check field validity
        val name = nameEditText.text.toString()
        val description = descriptionEditText.text.toString()
        if (!checkFieldValid(name, description)) return

        // Listen on DB response to move forward.
        viewModel.sendSuccess.observe(this) { isSuccessFull ->
            if (isSuccessFull) {
                val intent = Intent(this, MeetUpView::class.java).apply {
                    putExtra(MEETUP_SHOWN, viewModel.getMeetUpId())
                }
                startActivity(intent)
            } else {
                Snackbar.make(v, getString(R.string.DB_error_msg), 4).show()
            }
        }

        viewModel.sendMeetUp()
    }

    private fun showMessage(message: Int, title: Int) {
        val dlgAlert = AlertDialog.Builder(this)
        dlgAlert.setMessage(message)
        dlgAlert.setTitle(title)
        dlgAlert.setPositiveButton(R.string.ok, null)
        dlgAlert.setCancelable(true)
        dlgAlert.create().show()
    }
}