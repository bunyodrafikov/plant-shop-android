package com.brafik.brafshop.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.brafik.brafshop.R
import com.brafik.brafshop.constants.Functions
import com.brafik.brafshop.entities.Plant
import com.brafik.brafshop.fragments.CategoryPageFragmentDirections

class CategoryPageAdapter(private val items: List<Plant>) :
    RecyclerView.Adapter<CategoryPageAdapter.CategoryViewHolder>() {
    class CategoryViewHolder(item: View) : RecyclerView.ViewHolder(item) {
        val title: TextView = item.findViewById(R.id.title)
        val price: TextView = item.findViewById(R.id.price)
        val img: ImageView = item.findViewById(R.id.image)
        val button: ConstraintLayout = item.findViewById(R.id.button)
        val context: Context = item.context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val layout = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.category_list_item, parent, false)
        return CategoryViewHolder(layout)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.title.text = items[position].name
        holder.price.text = items[position].price.toString().plus("$")
        holder.button.setOnClickListener {
            it.findNavController().navigate(
                CategoryPageFragmentDirections.categoryToCard(items[position].id)
            )
        }
        Functions.getImage(holder.img, items[position].id.toInt(), holder.context)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}