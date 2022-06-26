package com.saigyouji.photogallery

import android.app.Activity
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat

private const val TAG = "NotificationReceiver"
class NotificationReceiver: BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(TAG, "onReceive: received result:  $resultCode")
        if(resultCode != Activity.RESULT_OK){
            return
        }
        intent?.let { _intent ->
            val requestCode = _intent.getIntExtra(PollWorker.REQUEST_CODE, 0)
            val notification = _intent.getParcelableExtra<Notification>(PollWorker.NOTIFICATION)!!
            val notificationManager = NotificationManagerCompat.from(context!!)
            notificationManager.notify(requestCode, notification)
        }
    }
}