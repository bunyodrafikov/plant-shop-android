package com.brafik.brafshop.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.brafik.brafshop.R
import com.brafik.brafshop.adapters.SearchPageAdapter
import com.brafik.brafshop.databinding.FragmentSearchBinding
import com.brafik.brafshop.entities.Plant
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class SearchViewModel(private val application: Application) : ViewModel() {
    private val db = Firebase.firestore
    private val plants = db.collection("plants")

    fun search(keyword: String, binding: FragmentSearchBinding) {
        val keys = listOf("name", "category")
        val keywords = listOf(keyword, keyword.plus(" "))
        val listOfPlants = mutableListOf<DocumentSnapshot>()
        for (key in keys) {
            plants
                .whereIn(key, keywords)
                .get()
                .addOnSuccessListener {
                    Log.d("Plants", it.documents.toString())
                    listOfPlants.addAll(it.documents)
                    displayByCategory(listOfPlants, binding)
                }
        }
    }

    private fun displayByCategory(docs: List<DocumentSnapshot>, binding: FragmentSearchBinding) {
        if (docs.isNotEmpty()) {
            binding.textView.text = application.getString(R.string.found_plants)
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
                binding.mainRecycleView.adapter = SearchPageAdapter(listOfPlants)
            }
        } else {
            binding.mainRecycleView.adapter = SearchPageAdapter(listOf())
            binding.textView.text = application.getString(R.string.nothing_found)
        }
    }

    class SearchViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
                SearchViewModel(application) as T
            } else {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}