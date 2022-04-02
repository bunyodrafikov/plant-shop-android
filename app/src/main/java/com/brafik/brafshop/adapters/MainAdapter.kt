package com.brafik.brafshop.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.brafik.brafshop.R
import com.brafik.brafshop.constants.Functions
import com.brafik.brafshop.constants.IMAGES
import com.brafik.brafshop.fragments.MainFragmentDirections

private const val TAG = "MainAdapter"
val urls = IMAGES

class MainAdapter :
    RecyclerView.Adapter<MainAdapter.MainViewHolder>() {

    class MainViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgButton: ImageButton = view.findViewById(R.id.imgButton)
        val context = view.context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val layout = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.main_recommendations_item, parent, false)
        return MainViewHolder(layout)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        try {
            Functions.getImage(holder.imgButton, position, holder.context)
            holder.imgButton.setOnClickListener {
                it.findNavController().navigate(
                    MainFragmentDirections.mainToCard(position.toLong())
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "$e")
        }
    }

    override fun getItemCount(): Int {
        return urls.size
    }
}
