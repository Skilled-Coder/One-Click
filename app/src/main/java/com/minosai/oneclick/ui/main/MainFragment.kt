package com.minosai.oneclick.ui.main

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.work.WorkManager
import com.minosai.oneclick.R
import com.minosai.oneclick.di.Injectable
import com.minosai.oneclick.model.AccountInfo
import com.minosai.oneclick.util.service.WebService
import com.minosai.oneclick.util.Constants
import com.minosai.oneclick.util.receiver.LoginLogoutReceiver
import com.minosai.oneclick.util.receiver.WifiReceiver
import com.minosai.oneclick.util.receiver.listener.LoginLogoutListener
import com.minosai.oneclick.util.receiver.listener.WifiConnectivityListener
import com.treebo.internetavailabilitychecker.InternetAvailabilityChecker
import com.treebo.internetavailabilitychecker.InternetConnectivityListener
import kotlinx.android.synthetic.main.fragment_main.*
import javax.inject.Inject

class MainFragment : Fragment(),
        Injectable,
        InternetConnectivityListener,
        WifiConnectivityListener,
        LoginLogoutListener {

    val TAG = javaClass.simpleName ?: Constants.PACKAGE_NAME

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var preferences: SharedPreferences
    @Inject
    lateinit var webService: WebService

    private lateinit var wifiReceiver: WifiReceiver
    private lateinit var loginLogoutReceiver: LoginLogoutReceiver
    private lateinit var mInternetAvailabilityChecker: InternetAvailabilityChecker
    private var activeAccount: AccountInfo? = null
    private var isLoading = false

    lateinit var mainViewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wifiReceiver = WifiReceiver(this)
        registerWifiReceiver()
        try {
            mInternetAvailabilityChecker = InternetAvailabilityChecker.getInstance()
            mInternetAvailabilityChecker.addInternetConnectivityListener(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mainViewModel = ViewModelProviders.of(this, viewModelFactory).get(MainViewModel::class.java)
        text_home_displayname.text = "Hello, ${mainViewModel.displayName}"

        addObservers()
        setClicks()
    }

    override fun onDestroyView() {
        try {
            mInternetAvailabilityChecker.removeInternetConnectivityChangeListener(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        unregisterWifiReceiver()
        super.onDestroyView()
    }

    private fun addObservers() {

        mainViewModel.getAllAccounts().observe(this, Observer {
            activeAccount = mainViewModel.getActiveAccount()
            updateUi()
        })

        WorkManager.getInstance().getStatusById(WebService.usageWork.id)
                .observe(this, Observer { workStatus ->
                    if (workStatus != null && workStatus.state.isFinished) {
                        stopLoading()
                    }
                })
    }

    private fun setClicks() {

        button_login.setOnClickListener {
            val userName = input_userid.text.toString()
            val password = input_password.text.toString()
            webService.login(this, activeAccount)
            saveUser(userName,  password)
            startLoading()
        }

        button_logout.setOnClickListener {
            webService.logout(this)
            startLoading()
        }
    }

    private fun registerWifiReceiver() {
        try {
            val intentFilter = IntentFilter()
            with(intentFilter) {
                addAction("android.net.wifi.WIFI_SATE_CHANGED")
                addAction("android.net.conn.CONNECTIVITY_CHANGE")
                addAction("android.net.wifi.supplicant.CONNECTION_CHANGE")
                addAction("android.net.wifi.STATE_CHANGE")
            }
            context?.registerReceiver(wifiReceiver, intentFilter)
            Log.d(TAG, "WifiReceiver registered")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "WifiReceiver already registered")
        }
    }

    private fun unregisterWifiReceiver() {
        try {
            context?.unregisterReceiver(wifiReceiver)
        } catch (e: Exception) {
            // already unregistered
        }
    }

    private fun updateUi() {

        activeAccount?.let {
            text_home_username.text = it.username
            text_home_usage.text = it.usage
            text_home_usage.text = it.usage
        }
    }

    private fun saveUser(userName: String, password: String) {
        preferences.edit()
                .putString(Constants.PREF_USERNAME, userName)
                .putString(Constants.PREF_PASSWORD, password)
                .apply()
    }

    override fun onInternetConnectivityChanged(isConnected: Boolean) {
        mainViewModel.isOnline = isConnected
        updateState()
    }

    override fun onWifiStateChanged(isConnectedToWifi: Boolean, ssid: String) {
        mainViewModel.isWifiConnected = isConnectedToWifi
        mainViewModel.ssid = ssid
        updateState()
    }

    override fun onLoggedListener(requestType: WebService.Companion.RequestType, isLogged: Boolean) {
        stopLoading()
        mainViewModel.isOnline = isLogged
        when(requestType) {
            WebService.Companion.RequestType.LOGIN -> {
                mainViewModel.isOnline = isLogged
                if (isLogged) {
                    if (mainViewModel.isAutoUpdateUsage()) {
                        startLoading()
                        webService.getUsage {  usage ->
                            mainViewModel.updateUsage(usage)
                        }
                    }
                }
            }
            WebService.Companion.RequestType.LOGOUT -> {
                if (mainViewModel.isOnline && isLogged) {
                    mainViewModel.isOnline = false
                }
            }
        }
        updateState()
    }

    private fun updateState() {
        if (mainViewModel.isWifiConnected) {
            if (mainViewModel.isOnline) {
                text_home_state.text = "${mainViewModel.ssid } • Logged in"
            } else {
                text_home_state.text = "${mainViewModel.ssid } • Not logged in"
            }
        } else {
            text_home_state.text = "Not connected to prontonetwork"
        }
    }

    private fun toggleLoading() {
        if (isLoading) {
            stopLoading()
        } else {
            startLoading()
        }
    }

    private fun startLoading() {
        if (!isLoading) {
            isLoading = true
            //TODO: Start loading animation
        }
    }

    private fun stopLoading() {
        if (isLoading) {
            isLoading = false
            //TODO: Stop loading animation
        }
    }

}