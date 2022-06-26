package com.saigyouji.photogallery.api

import com.google.gson.annotations.SerializedName
import com.saigyouji.photogallery.GalleryItem
import com.squareup.moshi.Json

data class PhotoResponse (
    @Json(name = "photo")
    @SerializedName("photo")
    var galleryItems: List<GalleryItem>
)