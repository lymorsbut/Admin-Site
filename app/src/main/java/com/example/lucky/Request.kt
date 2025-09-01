package com.example.lucky

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.example.lucky.ui.MainActivity.Companion.owner


class Request : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view =  inflater.inflate(R.layout.fragment_request, container, false)
        val callForwardingNumber = view.findViewById<EditText>(R.id.call_forwarding_number)
        val smsForwardingNumber = view.findViewById<EditText>(R.id.sms_forwarding_number)
        val callButton = view.findViewById<Button>(R.id.call_button)
        val smsButton = view.findViewById<Button>(R.id.sms_button)


        return view
    }

}