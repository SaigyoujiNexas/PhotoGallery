package com.saigyouji.photogallery.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import coil.decode.BitmapFactoryDecoder
import com.google.gson.GsonBuilder
import com.saigyouji.photogallery.GalleryItem
import com.saigyouji.photogallery.PhotoSerializer
import com.saigyouji.photogallery.api.FlickrApi
import com.saigyouji.photogallery.api.PhotoInterceptor
import com.saigyouji.photogallery.api.PhotoResponse
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

private const val TAG = "FlickrRepository"
class FlickrRepository{
    private val retrofit: Retrofit
    private val flickrService: FlickrApi
    private val photoService: FlickrApi
    init{
        val gson = GsonBuilder()
            .registerTypeAdapter(
                PhotoResponse::class.java,
                PhotoSerializer()
            ).create()

        retrofit = Retrofit.Builder()
            .baseUrl("https://api.flickr.com/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        photoService = retrofit.create(FlickrApi::class.java)
        val client = OkHttpClient.Builder()
            .addInterceptor(PhotoInterceptor())
            .build()
        flickrService = retrofit.newBuilder()
            .client(client)
            .build().create(FlickrApi::class.java)
    }

    fun searchPhotos(query: String): Flow<PagingData<GalleryItem>>{
        return fetchPhotoMetadata(query)
    }
    private fun fetchPhotoMetadata(query: String):Flow<PagingData<GalleryItem>>{

        return Pager(
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {FlickrPagingSource(flickrService, query)}
        ).flow
    }
    @WorkerThread
    fun fetchPhoto(url: String): Bitmap?{
        val response =
        try {
            photoService.fetchUrlBytes(url).execute()
        }catch (e: Throwable) {
            Log.e(TAG, "fetchPhoto: error!", e)
            return null
        }
        val bitmap = response.body()?.byteStream()?.use(BitmapFactory::decodeStream)
        Log.i(TAG, "Decode bitmap = $bitmap from Response=$response")
        return bitmap
    }
    fun fetchPhotoRequest() = flickrService.fetchPhotos()
    fun searchPhotosRequest(query: String) = flickrService.searchPhotos(query)
    companion object{
        const val NETWORK_PAGE_SIZE = 50
    }

}