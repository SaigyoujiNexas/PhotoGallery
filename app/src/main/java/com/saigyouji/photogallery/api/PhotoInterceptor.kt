package com.saigyouji.photogallery.api

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response


private const val API_KEY = "71d58d0fee9ea2b26e03c001ea8374b1"
class PhotoInterceptor: Interceptor{
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()

        val newUrl : HttpUrl = originalRequest
            .url.newBuilder()
            .addQueryParameter("api_key", API_KEY)
            .addQueryParameter("format", "json")
            .addQueryParameter("nojsoncallback", "1")
            .addQueryParameter("extras", "url_s")
            .addQueryParameter("safesearch", "1")
            .addQueryParameter("iscommons", "true")
            .addQueryParameter("media", "photos")
            .build()
        val newRequest: Request = originalRequest.newBuilder()
            .url(newUrl)
            .build()
        return chain.proceed(newRequest)

    }
}