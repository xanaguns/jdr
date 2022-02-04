package com.jeongdaeri.myrecyclerview

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.layout_recycler_item.view.*

// 커스텀 뷰홀더
class MyViewHolder(itemView: View,
                   recyclerviewInterface: MyRecyclerviewInterface):
                    RecyclerView.ViewHolder(itemView),
                    View.OnClickListener
{

    val TAG: String = "로그"

    private val usernameTextView = itemView.user_name_txt
    private val profileImageView = itemView.profile_img

    private var myRecyclerviewInterface : MyRecyclerviewInterface? = null

    // 기본 생성자
    init {
        Log.d(TAG, "MyViewHolder - init() called")

        itemView.setOnClickListener(this)
        this.myRecyclerviewInterface = recyclerviewInterface

    }


    // 데이터와 뷰를 묶는다.
    fun bind(myModel: MyModel){
        Log.d(TAG, "MyViewHolder - bind() called")

        // 텍스트뷰 와 실제 텍스트 데이터를 묶는다.
        usernameTextView.text = myModel.name

        // 이미지뷰와 실제 이미지 데이터를 묶는다 .
        Glide
            .with(App.instance)
            .load(myModel.profileImage)
//            .centerCrop()
            .placeholder(R.mipmap.ic_launcher)
            .into(profileImageView)

    }

    override fun onClick(p0: View?) {
        Log.d(TAG, "MyViewHolder - onClick() called")

        this.myRecyclerviewInterface?.onItemClicked(adapterPosition)

    }


}
