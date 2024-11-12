package com.example.childreneducationapplication.stamp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.childreneducationapplication.R

class GiftStampAdapter(
    private val context: Context,
    private val userStampCount: Int,
    private val targetStampCount: Int,
) :
    RecyclerView.Adapter<GiftStampAdapter.GiftStampViewHolder>() {

    // Define how many stamps per page
    private val stampsPerPage = 10

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GiftStampViewHolder {
        val view =
            LayoutInflater.from(context).inflate(R.layout.fragment_stamp_layout, parent, false)

        return GiftStampViewHolder(view)
    }

    override fun onBindViewHolder(holder: GiftStampViewHolder, position: Int) {
        val startIndex = position * stampsPerPage
        val endIndex = minOf(
            startIndex + stampsPerPage,
            userStampCount
        ) // Make sure to not exceed userStampCount
        val leftStampCount =
            if (targetStampCount - userStampCount <= 0) 0 else targetStampCount - userStampCount
        holder.setTotalStampCount(userStampCount, leftStampCount)
        holder.bind(startIndex, endIndex, targetStampCount)
    }

    override fun getItemCount(): Int {
        val displayStampCount = if(targetStampCount > userStampCount) targetStampCount else userStampCount

        return (displayStampCount + stampsPerPage - 1) / stampsPerPage // Equivalent to ceil(userStampCount / stampsPerPage)
    }

    class GiftStampViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val accumulatedStampTextView: TextView =
            itemView.findViewById(R.id.accumulated_stamp)
        private val leftStampCount: TextView =
            itemView.findViewById(R.id.left_stamp)
        private val giftStampBoxes: List<LinearLayout> = listOf(
            itemView.findViewById(R.id.gift_stamp_box0),
            itemView.findViewById(R.id.gift_stamp_box1),
            itemView.findViewById(R.id.gift_stamp_box2),
            itemView.findViewById(R.id.gift_stamp_box3),
            itemView.findViewById(R.id.gift_stamp_box4),
            itemView.findViewById(R.id.gift_stamp_box5),
            itemView.findViewById(R.id.gift_stamp_box6),
            itemView.findViewById(R.id.gift_stamp_box7),
            itemView.findViewById(R.id.gift_stamp_box8),
            itemView.findViewById(R.id.gift_stamp_box9)
        )

        fun setTotalStampCount(count: Int, leftCount: Int) {
            accumulatedStampTextView.text = count.toString()
            leftStampCount.text = leftCount.toString()
        }

        fun bind(
            startIndex: Int,
            endIndex: Int,
            targetStampCount: Int
        ) {
            // Clear existing stamps
            giftStampBoxes.forEach { box ->
                box.removeAllViews()
            }

            if (startIndex <= (targetStampCount - 1) && (targetStampCount - 1) <= (startIndex + 9)) {
                val targetBoxIndex = (targetStampCount - 1) % giftStampBoxes.size
                val stampTargetLayout = R.layout.stamp_item_target_layout
                val stampView = LayoutInflater.from(itemView.context)
                    .inflate(stampTargetLayout, giftStampBoxes[targetBoxIndex], false)
                giftStampBoxes[targetBoxIndex].addView(stampView)
            }

            // Add stamps to the boxes based on the calculated start and end index
            for (i in startIndex until endIndex) {
                val boxIndex = i % giftStampBoxes.size // Use modulo to wrap around
                println("Target Stamp : $targetStampCount, i : $i")
                val stampLayoutRes = R.layout.stamp_item_layout
                val stampView = LayoutInflater.from(itemView.context)
                    .inflate(stampLayoutRes, giftStampBoxes[boxIndex], false)
                giftStampBoxes[boxIndex].removeAllViews()
                giftStampBoxes[boxIndex].addView(stampView)
            }
        }
    }
}