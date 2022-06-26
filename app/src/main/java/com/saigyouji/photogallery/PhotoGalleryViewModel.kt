package com.saigyouji.photogallery

import android.app.Application
import android.text.TextUtils
import android.widget.Toast
import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.saigyouji.photogallery.data.FlickrRepository
import com.saigyouji.photogallery.data.QueryPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoGalleryViewModel(
    private val app: Application
    ): AndroidViewModel(app){
    private val repository: FlickrRepository = FlickrRepository()
    val galleryItemFlow: Flow<PagingData<GalleryItem>>
    private val mutableSearchTerm = MutableLiveData<String>()
    val searchTerm: String
    get() = mutableSearchTerm.value ?: ""
    private val searchTextFlow = mutableSearchTerm.asFlow()
    init {
        mutableSearchTerm.value = QueryPreferences.getStoredQuery(app.applicationContext)
        galleryItemFlow = searchTextFlow
            .flatMapLatest { str ->
                repository.searchPhotos(str)
                    .map { pagingData ->
                        pagingData.filter {
                            return@filter it.url.isNotBlank()
                        }
                    }
            }.catch {
                Toast.makeText(app, it.localizedMessage?:it.message, Toast.LENGTH_LONG).show()
            }
            .cachedIn(viewModelScope)
    }
    fun fetchPhotos(query: String){
        QueryPreferences.setStoredQuery(app.applicationContext, query)
        mutableSearchTerm.value = query
    }
}