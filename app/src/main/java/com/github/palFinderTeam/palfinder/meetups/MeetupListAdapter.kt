package com.github.palFinderTeam.palfinder.meetups

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.palFinderTeam.palfinder.R
import com.github.palFinderTeam.palfinder.utils.Location
import com.github.palFinderTeam.palfinder.utils.PrettyDate
import com.github.palFinderTeam.palfinder.utils.SearchedFilter
import com.github.palFinderTeam.palfinder.utils.image.ImageInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MeetupListAdapter(private val dataSet: List<MeetUp>, val currentDataSet: MutableList<MeetUp>,
                        private var filter: SearchedFilter<MeetUp>, private var currentLocation: Location,
                        private val onItemClicked: (position: Int) -> Unit) :
    RecyclerView.Adapter<MeetupListAdapter.ViewHolder>(), Filterable {

    companion object {
        const val PARTICIPANTS_RATIO: String = "%d / %d"
    }

    class ViewHolder(view: View, private val onItemClicked: (position: Int) -> Unit) :
        RecyclerView.ViewHolder(view), View.OnClickListener {
        //TODO - add some remaining fields to display
        val meetupTitle: TextView = view.findViewById(R.id.meetup_title)
        val meetupDate: TextView = view.findViewById(R.id.meetup_date)
        val meetupDescription: TextView = view.findViewById(R.id.meetup_description)
        val meetupNumberParticipants: TextView = view.findViewById(R.id.meetup_participant)
        val meetupDistance: TextView = view.findViewById(R.id.meetup_dist)
        val meetupImage: ImageView = view.findViewById(R.id.meetup_pic)

        val parContext: Context = view.context

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val position = adapterPosition
            onItemClicked(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder {
        //create a new view for each meetup
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.meetup_listview_new, parent, false)
        ) {
            val item = currentDataSet[it]
            val originalItemPos = dataSet.indexOf(item)

            onItemClicked(originalItemPos)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //update displayed elements with the content of the current dataset
        val meetupTitle = holder.meetupTitle
        meetupTitle.text = currentDataSet[position].name
        val meetupDate = holder.meetupDate
        val prettyDate = PrettyDate()

        meetupDate.text = prettyDate.timeDiff(currentDataSet[position].startDate)
        val meetupDescription = holder.meetupDescription
        meetupDescription.text = currentDataSet[position].description

        val meetupDistance = holder.meetupDistance
        meetupDistance.text =
            currentDataSet[position].location.prettyDistanceTo(holder.parContext, currentLocation)

        val meetupNumberParticipants = holder.meetupNumberParticipants
        meetupNumberParticipants.text = String.format(
            PARTICIPANTS_RATIO,
            currentDataSet[position].numberOfParticipants(),
            currentDataSet[position].capacity
        )

        val meetupPicture = holder.meetupImage

        CoroutineScope(Dispatchers.Main).launch {
            currentDataSet[position].iconId?.let {
                ImageInstance(it).loadImageInto(meetupPicture)
            }
        }
    }

    override fun getItemCount(): Int = currentDataSet.size

    override fun getFilter(): Filter = filter

}



