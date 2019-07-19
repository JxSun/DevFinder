package com.jxsun.devfinder.feature

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jxsun.devfinder.R
import com.jxsun.devfinder.model.GitHubUser
import com.jxsun.devfinder.util.GlideApp
import kotlinx.android.synthetic.main.item_devlist.view.*

class DevListRecyclerViewAdapter : RecyclerView.Adapter<DevListRecyclerViewAdapter.ViewHolder>() {

    private var keyword = ""
    private var nextPage = -1
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

    fun updateDevList(newKeyword: String, nextPage: Int, list: List<GitHubUser>) {
        if (this.keyword != newKeyword || this.nextPage > nextPage) {
            devList.clear()
        } else if (this.keyword == newKeyword && this.nextPage == nextPage) {
            // Data should be the same, just skip
            return
        }
        this.keyword = newKeyword
        this.nextPage = nextPage
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
            GlideApp.with(itemView.context)
                    .load(user.avatarUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(avatar)
        }
    }
}