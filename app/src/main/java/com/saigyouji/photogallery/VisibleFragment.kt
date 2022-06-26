package com.saigyouji.photogallery

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.work.impl.utils.ForceStopRunnable

private const val TAG = "VisibleFragment"
abstract class VisibleFragment: Fragment(){
    private val onShowNotification = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            //if receive this, cancel the notification
            Log.i(TAG, "canceling notification")
            resultCode = Activity.RESULT_CANCELED
            abortBroadcast()
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(PollWorker.ACTION_SHOW_NOTIFICATION)
        requireActivity().registerReceiver(onShowNotification, filter, PollWorker.PREM_PRIVATE, null)
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(onShowNotification)
    }
}