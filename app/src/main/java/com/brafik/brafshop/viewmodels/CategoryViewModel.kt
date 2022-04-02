package com.brafik.brafshop.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.brafik.brafshop.adapters.CategoryPageAdapter
import com.brafik.brafshop.databinding.FragmentCategoryBinding
import com.brafik.brafshop.entities.Plant
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CategoryViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val plants = db.collection("plants")

    fun findByCategory(category: String, binding: FragmentCategoryBinding) {
        plants
            .whereEqualTo("category", category)
            .get()
            .addOnSuccessListener {
                Log.d("Plants", it.documents.toString())
                displayByCategory(it.documents, binding)
            }
    }

    private fun displayByCategory(docs: List<DocumentSnapshot>, binding: FragmentCategoryBinding) {
        val listOfPlants = mutableListOf<Plant>()
        for (doc in docs) {
            listOfPlants.add(
                Plant(
                    doc["price"].toString().toLong(),
                    doc["name"].toString(),
                    doc["description"].toString(),
                    doc["pots"] as List<Long>,
                    doc["id"].toString().toLong(),
                    doc["category"].toString(),
                )
            )
            binding.mainRecycleView.adapter = CategoryPageAdapter(listOfPlants)
        }
    }

    class CategoryViewModelFactory :
        ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(CategoryViewModel::class.java)) {
                CategoryViewModel() as T
            } else {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}