package com.saigyouji.photogallery.data

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import android.util.LruCache
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "ThumbnailDownloader"
private const val MESSAGE_DOWNLOAD = 0
class ThumbnailDownloader<in T> (
    private val responseHandler: Handler,
    private val onThumbnailDownloaded: (T, Bitmap) -> Unit
        ): HandlerThread(TAG) {

    private val cache: LruCache<String, Bitmap> = LruCache(200)

    val fragmentLifecycleObserver: LifecycleObserver =
        LifecycleEventObserver { _, event ->
            when(event){
                Lifecycle.Event.ON_CREATE -> {
                    Log.i(TAG, "Starting background thread")
                    start()
                    looper
                }
                Lifecycle.Event.ON_DESTROY->{
                    Log.i(TAG, "Destorying background thread")
                    quit()
                }
                else ->{

                }
            }
        }
    val viewLifecycleObserver: LifecycleObserver =
        LifecycleEventObserver { _, event ->
            if(event == Lifecycle.Event.ON_DESTROY){
                Log.i(TAG, "Clearing all requests from queue")
                requestHandler.removeMessages(MESSAGE_DOWNLOAD)
                requestMap.clear()
            }
        }

    private var hasQuit = false
    private lateinit var requestHandler: Handler
    private val requestMap = ConcurrentHashMap<T, String>()
    private val flickrRepository =  FlickrRepository()
    @Suppress("UNCHECKED_CAST")
    override fun onLooperPrepared() {
        requestHandler = object : Handler(looper) {
            override fun handleMessage(msg: Message){
                if(msg.what == MESSAGE_DOWNLOAD){
                    val target = msg.obj as T
                    Log.i(TAG, "Got a request for URL: ${requestMap[target]}")
                    handleRequest(target)
                }
            }
        }
    }
    override fun quit(): Boolean {
        hasQuit = true
        return super.quit()
    }

    fun queueThumbnail(target: T, url: String){
        requestMap[target] = url
        requestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
            .sendToTarget()
    }


    private fun handleRequest(target: T){
        val url = requestMap[target] ?: return
        val bitmap= cache.get(url)
            ?:flickrRepository.fetchPhoto(url) ?:return
        cache.put(url, bitmap)
        responseHandler.post {
            if(requestMap[target] != url || hasQuit){
                return@post
            }
            requestMap.remove(target)
            onThumbnailDownloaded(target, bitmap)
        }
    }
}