package com.saigyouji.photogallery

import android.animation.TypeEvaluator
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.saigyouji.photogallery.api.FlickrApi
import com.saigyouji.photogallery.api.PhotoResponse
import com.saigyouji.photogallery.data.FlickrRepository
import com.saigyouji.photogallery.data.QueryPreferences
import com.saigyouji.photogallery.data.ThumbnailDownloader
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okio.IOException
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Calendar.MINUTE
import java.util.concurrent.TimeUnit

private const val TAG = "PhotoGalleryFragment"
private const val POLL_WORK = "POLL_WORK"
/**
 * A simple [Fragment] subclass.
 * Use the [fragment_photo_gallery.newInstance] factory method to
 * create an instance of this fragment.
 */
class PhotoGalleryFragment : VisibleFragment() {

    private lateinit var photoGalleryViewModel: PhotoGalleryViewModel
    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var thumbnailDownloader: ThumbnailDownloader<PhotoHolder>
    private lateinit var photoLoadingProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)

        photoGalleryViewModel = ViewModelProvider(this.requireActivity())[PhotoGalleryViewModel::class.java]
        thumbnailDownloader = ThumbnailDownloader(Handler(Looper.getMainLooper())){ holder, bitmap->
            val drawable = BitmapDrawable(resources, bitmap)
            holder.bindDrawable(drawable)
        }
        lifecycle.addObserver(thumbnailDownloader.fragmentLifecycleObserver)

        viewLifecycleOwnerLiveData.observe(this){
            it?.lifecycle?.apply {
                if(currentState == Lifecycle.State.CREATED)
                    addObserver(thumbnailDownloader.viewLifecycleObserver)
                else if(currentState == Lifecycle.State.DESTROYED)
                    removeObserver(thumbnailDownloader.viewLifecycleObserver)
            }
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_photo_gallery, container, false)
        photoLoadingProgressBar  = v.findViewById(R.id.progress_bar_photo_loading)
        photoRecyclerView = v.findViewById(R.id.photo_recycler_view)
        var  itemCnt = 3
        photoRecyclerView.layoutManager =object: GridLayoutManager(context, itemCnt){
            override fun calculateExtraLayoutSpace(
                state: RecyclerView.State,
                extraLayoutSpace: IntArray
            ) {
                super.calculateExtraLayoutSpace(state, extraLayoutSpace)
                extraLayoutSpace[1] = 480
                Log.d(TAG, "extra layout space: ${extraLayoutSpace[1]}")
            }
        }
        photoRecyclerView.viewTreeObserver.addOnGlobalLayoutListener {
            val width = photoRecyclerView.width
            if(width / 300 != itemCnt) {
                itemCnt = width / 300;
                Log.d(TAG, "itemCnt: $itemCnt")
                photoRecyclerView.layoutManager = GridLayoutManager(context, itemCnt)
            }
        }
      //  viewLifecycleOwner.lifecycle.addObserver(thumbnailDownloader.viewLifecycleObserver)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        PhotoAdapter().let{adapter->
            photoRecyclerView.adapter = adapter
            lifecycleScope.launch {
                photoGalleryViewModel.galleryItemFlow.collectLatest(adapter::submitData)
            }
            lifecycleScope.launch{
                adapter.loadStateFlow.collect{ loadState ->
                    photoLoadingProgressBar.isVisible = loadState.source.refresh is LoadState.Loading
                    if(loadState.source.refresh is LoadState.Loading)
                        photoRecyclerView.visibility = View.INVISIBLE
                    else{
                        photoRecyclerView.visibility = View.VISIBLE
                    }
                  //  photoRecyclerView.isVisible = loadState.source.refresh !is LoadState.Loading
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
      //  viewLifecycleOwner.lifecycle.removeObserver(thumbnailDownloader.viewLifecycleObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(thumbnailDownloader.fragmentLifecycleObserver)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_photo_gallery, menu)
        val searchItem: MenuItem = menu.findItem(R.id.menu_item_search)
        val searchView = searchItem.actionView as androidx.appcompat.widget.SearchView
        searchView.apply {
            setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    Log.d(TAG, "QueryTextSubmit: $query")
                    photoGalleryViewModel.fetchPhotos(query?:"")
                    clearFocus()
                    onActionViewCollapsed()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    Log.d(TAG, "onQueryTextChange: $newText")
                    return false
                }
            })
            setOnSearchClickListener {
                searchView.setQuery(photoGalleryViewModel.searchTerm, false)

            }
        }
        val toggleItem = menu.findItem(R.id.menu_item_toggle_polling)
        val isPolling = QueryPreferences.isPolling(requireContext())
        val toggleItemTitle = if(isPolling){
            R.string.stop_polling
        }
        else{
            R.string.start_polling
        }
        toggleItem.setTitle(toggleItemTitle)


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.menu_item_clear -> {
                photoGalleryViewModel.fetchPhotos("")
                true
            }
            R.id.menu_item_toggle_polling ->{
                val isPolling = QueryPreferences.isPolling(requireContext())
                if(isPolling){
                    WorkManager.getInstance(requireContext()).cancelUniqueWork(POLL_WORK)
                    QueryPreferences.setPolling(requireContext(), false)
                }
                else{
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .build()
                    val periodicRequest = PeriodicWorkRequest
                        .Builder(PollWorker::class.java, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build()

                    WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(POLL_WORK,
                    ExistingPeriodicWorkPolicy.KEEP, periodicRequest)
                    QueryPreferences.setPolling(requireContext(), true)
                }
                activity?.invalidateOptionsMenu()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private inner class PhotoHolder(private val itemImageView: ImageView): RecyclerView.ViewHolder(itemImageView)
    , View.OnClickListener{
        private lateinit var galleryItem: GalleryItem
        init{
            itemView.setOnClickListener(this)
        }

        val bindDrawable :(Drawable) -> Unit = itemImageView::setImageDrawable
        fun bindGalleryItem(item: GalleryItem){
            galleryItem = item
        }

        override fun onClick(v: View?) {
            val intent = PhotoPageActivity
                .newInstance(requireContext(), galleryItem.photoPageUri)
            startActivity(intent)
        }
    }
    private inner class PhotoAdapter: PagingDataAdapter<GalleryItem,PhotoHolder>(PhotoDiffCallback()){

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
            val view = layoutInflater.inflate(
                R.layout.list_item_gallery,
                parent,
                false
            ) as ImageView
            return PhotoHolder(view)
        }

        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            val item = getItem(position)
            item?.let {
                holder.bindGalleryItem(it)
                val placeHolder: Drawable = ContextCompat.getDrawable(
                    requireActivity(),
                    R.drawable.bill_up_close
                )?:ColorDrawable()
                Log.d(TAG, "item url: ${it.url}")
                holder.bindDrawable(placeHolder)
                try {
                    thumbnailDownloader.queueThumbnail(holder, it.url)
                }catch (e: IOException)
                {
                    Log.e(TAG, "onBindViewHolder: error!", e)
                }catch (e: HttpException)
                {
                    Log.e(TAG, "onBindViewHolder: error!", e)
                }
            }
        }

    }
    private class PhotoDiffCallback: DiffUtil.ItemCallback<GalleryItem>(){
        override fun areItemsTheSame(oldItem: GalleryItem, newItem: GalleryItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GalleryItem, newItem: GalleryItem) = newItem == oldItem
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment fragment_photo_gallery.
         */
        @JvmStatic
        fun newInstance() = PhotoGalleryFragment()
    }
}