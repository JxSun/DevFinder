package com.jxsun.devfinder.feature

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jxsun.devfinder.R
import com.jxsun.devfinder.model.GitHubUser
import kotlinx.android.synthetic.main.item_devlist.view.*

class DevListRecyclerViewAdapter : RecyclerView.Adapter<DevListRecyclerViewAdapter.ViewHolder>() {

    private var keyword = ""
    private val devList = mutableListOf<GitHubUser>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_devlist, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return devList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(devList[position])
    }

    fun addDevList(newKeyword: String, list: List<GitHubUser>) {
        if (keyword != newKeyword) {
            keyword = newKeyword
            devList.clear()
        }
        devList.addAll(list)
        notifyDataSetChanged()
    }

    class ViewHolder(
            itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        private val avatar: ImageView = itemView.userAvatar
        private val name: TextView = itemView.userName

        fun bind(user: GitHubUser) {
            name.text = user.loginName
            Glide.with(itemView.context)
                    .load(user.avatarUrl)
                    .into(avatar)
        }
    }
}