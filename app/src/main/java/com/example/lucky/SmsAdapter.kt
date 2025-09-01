package com.example.lucky

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.lucky.Util.toDate

class SmsAdapter (val songs: List<MessageDetail>, var context: Context): RecyclerView.Adapter<SmsAdapter.MyViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view : View = inflater.inflate(R.layout.item_view,parent,false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Log.i("TAG", "onBindViewHolder: position $position")
        holder.txtTitle.text = if (songs[position].number?.isNotEmpty()==true) songs[position].number else songs[position].b
        holder.txtDiscription.text = if ( songs[position].timeStamp.toDate().isNotEmpty()==true) songs[position].timeStamp.toDate() else { songs[position].c.toLong().toDate() }
        holder.body.text = if (songs[position].message.isNotEmpty()==true) songs[position].message   else{ songs[position].e }
        holder.container.setOnClickListener {
            if (holder.body.isVisible){
                holder.body.visibility = View.GONE
            }else{
                holder.body.visibility = View.VISIBLE
            }
        }
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var txtTitle = itemView.findViewById<TextView>(R.id.address)
        var txtDiscription = itemView.findViewById<TextView>(R.id.date)
        var body = itemView.findViewById<TextView>(R.id.body)
        var container = itemView.findViewById<LinearLayout>(R.id.container)
    }
}