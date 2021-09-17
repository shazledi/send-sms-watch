package com.jambit.alpha.sendsmswatch

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.jambit.alpha.sendsmswatch.databinding.ActivityMainBinding
import java.util.*

class MainActivity : Activity() {


    private lateinit var binding: ActivityMainBinding

    companion object {
        const val EMERGENCY_DESTINATION = "" // TODO: put a phone number here e.g +491234678
        const val SENT = "SMS_SENT"
        const val TAG = "SendSmsWatch"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

    override fun onResume() {
        super.onResume()
        getPermissions()
        sendSms()
    }

    private fun getPermissions() {
        val permissions = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
        )

        if (doesNotHavePermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, 1)
        }
    }

    private fun doesNotHavePermissions(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return true;
            }
        }
        return false;
    }

    fun sendSms() {
        listSubscriptions(this)
        val smsSubscription = SmsManager.getDefaultSmsSubscriptionId()
        Log.i(TAG, "Default SMS subscription: $smsSubscription")
        val smsManager = SmsManager.getSmsManagerForSubscriptionId(smsSubscription)
        //noinspection ConstantConditions
        if (EMERGENCY_DESTINATION.isEmpty()) {
            Log.w(TAG, "Not sending emergency SMS because no configuration exists.")
            return
        }
        // Debug
        val sentIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(SENT),
            PendingIntent.FLAG_IMMUTABLE
        )
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (resultCode) {
                    RESULT_OK -> Log.i(TAG, "OK")
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                        val extras = getResultExtras(true)
                        Log.i(TAG, "GENERIC FAILURE: $extras")
                    }
                    SmsManager.RESULT_ERROR_NO_SERVICE -> Log.i( TAG, "NO SERVICE" )
                    SmsManager.RESULT_ERROR_NULL_PDU -> Log.i( TAG, "NULL PDU" )
                    SmsManager.RESULT_ERROR_RADIO_OFF -> Log.i( TAG, "RADIO OFF")
                }
                unregisterReceiver(this)
            }
        }, IntentFilter(SENT))

        // Actual send
        val json = """{"hello": "world"}"""
        smsManager.sendTextMessage(
            EMERGENCY_DESTINATION,
            null,
            json,
            sentIntent,
            null
        )
    }

    @SuppressLint("MissingPermission")
    private fun listSubscriptions(activity: Activity) {
        val manager = activity.getSystemService(
            SubscriptionManager::class.java
        )
        if (manager == null) {
            Log.w(TAG, "Got empty subscriptions")
            return
        }
        val subscriptions = manager.activeSubscriptionInfoList
        for (info in subscriptions) {
            val msg = String.format(
                Locale.ENGLISH,
                "Subscription id %d for %s as %s",
                info.subscriptionId, info.carrierName, info.displayName
            )
            Log.i(TAG, msg)
        }
    }
}