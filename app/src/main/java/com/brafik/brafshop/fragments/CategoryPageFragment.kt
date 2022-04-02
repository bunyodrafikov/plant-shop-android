package com.brafik.brafshop.fragments

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.brafik.brafshop.R
import com.brafik.brafshop.databinding.FragmentCategoryBinding
import com.brafik.brafshop.viewmodels.CategoryViewModel

class CategoryPageFragment : Fragment(R.layout.fragment_category) {
    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoryViewModel by viewModels {
        CategoryViewModel.CategoryViewModelFactory()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        val view = binding.root
        val category = arguments?.getString("category")!!
        viewModel.findByCategory(category, binding)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.backButton.setOnClickListener {
            it.findNavController().navigateUp()
        }
        val category = arguments?.getString("category")!!
        val resourceId = context?.resIdByName(category, "string")!!
        binding.textView.text = getString(resourceId).plus(" Category")
    }

    private fun Context.resIdByName(resIdName: String?, resType: String): Int {
        resIdName?.let {
            return resources.getIdentifier(it, resType, packageName)
        }
        throw Resources.NotFoundException()
    }
}