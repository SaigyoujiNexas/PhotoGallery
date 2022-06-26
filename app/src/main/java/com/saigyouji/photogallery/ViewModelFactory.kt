package com.saigyouji.photogallery

import android.content.Context
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner

class ViewModelFactory(
    owner: SavedStateRegistryOwner
): AbstractSavedStateViewModelFactory(owner, null){
    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        TODO("Not yet implemented")
    }
}