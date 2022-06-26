package com.saigyouji.photogallery

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.saigyouji.photogallery.api.PhotoResponse
import java.lang.reflect.Type

class PhotoSerializer: JsonDeserializer<PhotoResponse> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): PhotoResponse {
        val prim = json?.asJsonObject?.get("photos")
        return Gson().fromJson(prim, typeOfT)
    }
}