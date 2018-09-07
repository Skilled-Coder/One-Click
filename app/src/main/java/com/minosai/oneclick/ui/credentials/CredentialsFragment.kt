package com.minosai.oneclick.ui.credentials

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.minosai.oneclick.R
import com.minosai.oneclick.di.Injectable
import kotlinx.android.synthetic.main.fragment_credentials.*
import javax.inject.Inject

class CredentialsFragment : Fragment(), Injectable {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    lateinit var credentialsViewModel: CredentialsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_credentials, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        credentialsViewModel = ViewModelProviders.of(this, viewModelFactory).get(CredentialsViewModel::class.java)

        button_cred_next.setOnClickListener {

            val displayName = text_creds_displayname.text.toString()
            val userName = text_creds_username.text.toString()
            val password = text_creds_password.text.toString()

            with(credentialsViewModel) {
                addAccount(userName, password, true)
                changeFirstOpenBoolean()
                saveDisplayNamøe(displayName)
            }

            findNavController().popBackStack()
        }

    }

}