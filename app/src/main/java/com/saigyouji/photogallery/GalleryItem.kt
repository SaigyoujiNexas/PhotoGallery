package com.saigyouji.photogallery

import android.net.Uri
import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json

data class GalleryItem(
    var title: String = "",
    var id: String="",
    @Json(name = "url_s")
    @SerializedName("url_s")
    var url: String="",
    @SerializedName("owner") var owner: String = ""
){
    val photoPageUri: Uri
    get(){
        return Uri.parse("https://www.flickr.com/photos/")
            .buildUpon()
            .appendPath(owner)
            .appendPath(id)
            .build()
    }
}