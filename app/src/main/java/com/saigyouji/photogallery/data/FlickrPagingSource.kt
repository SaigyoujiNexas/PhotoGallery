package com.saigyouji.photogallery.data

import android.text.TextUtils
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.saigyouji.photogallery.GalleryItem
import com.saigyouji.photogallery.api.FlickrApi
import com.saigyouji.photogallery.api.PhotoResponse
import com.saigyouji.photogallery.data.FlickrRepository.Companion.NETWORK_PAGE_SIZE
import okio.IOException
import retrofit2.Call
import retrofit2.await

private const val FLICKR_STARTING_PAGE_INDEX = 1
private const val TAG = "FlickrPagingSource"
class FlickrPagingSource(
    private val flickrService: FlickrApi,
    private val params: String
): PagingSource<Int, GalleryItem>() {
    override fun getRefreshKey(state: PagingState<Int, GalleryItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?:state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GalleryItem> {
        val position = params.key ?: FLICKR_STARTING_PAGE_INDEX
        return try{
            val resp =
                if(TextUtils.isEmpty(this.params.trim())) {
                    Log.d(TAG, "invoke empty fetch.")
                    flickrService.fetchPhotos(page = position, perPage = params.loadSize).await()
                }
                else
                    flickrService.searchPhotos(this.params, position, params.loadSize).await()
            Log.d(TAG, "load: ${resp.galleryItems.toString()}")
            val nextKey = if(resp.galleryItems.isEmpty()){
                null
            }else{
                position + (params.loadSize / NETWORK_PAGE_SIZE)
            }
            LoadResult.Page(
                data = resp.galleryItems,
                prevKey = if(position == FLICKR_STARTING_PAGE_INDEX) null else position - 1,
                nextKey = nextKey
            )
        }catch (e: IOException){
            return LoadResult.Error(e)
        }
    }
}