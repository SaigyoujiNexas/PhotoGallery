package com.saigyouji.photogallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class PhotoPageActivity : AppCompatActivity() {

    private lateinit var photoPageFragment: PhotoPageFragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_page)

        val fm = supportFragmentManager
        val currentFragment = fm.findFragmentById(R.id.fragment_container)
        if(currentFragment == null){
            photoPageFragment = PhotoPageFragment.newInstance(intent.data?: Uri.EMPTY)
            fm.beginTransaction()
                .add(R.id.fragment_container, photoPageFragment)
                .commit()
        }
    }

    override fun onBackPressed() {
        if(!photoPageFragment.onBackPressed())
            super.onBackPressed()
    }
    companion object{
        fun newInstance(context: Context, photoPageUri: Uri): Intent {
            return Intent(context, PhotoPageActivity::class.java).apply {
                data = photoPageUri
            }
        }
    }
}