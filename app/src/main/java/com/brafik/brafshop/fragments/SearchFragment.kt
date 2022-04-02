package com.brafik.brafshop.fragments

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.brafik.brafshop.R
import com.brafik.brafshop.constants.Functions.hideKeyboard
import com.brafik.brafshop.databinding.FragmentSearchBinding
import com.brafik.brafshop.viewmodels.SearchViewModel

class SearchFragment : Fragment(R.layout.fragment_search) {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var keyword: String

    private val viewModel: SearchViewModel by viewModels {
        SearchViewModel.SearchViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        val view = binding.root
        keyword = arguments?.getString("keyword")!!
        viewModel.search(keyword, binding)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.backButton.setOnClickListener {
            it.findNavController().navigateUp()
        }
        val editText = binding.search.editText!!
        editText.setText(keyword)
        binding.search.setEndIconOnClickListener {
            binding.mainRecycleView.removeAllViewsInLayout()
            viewModel.search(binding.search.editText?.text.toString(), binding)
        }
        editText.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                // if the event is a key down event on the enter button
                if (event.action == KeyEvent.ACTION_DOWN &&
                    keyCode == KeyEvent.KEYCODE_ENTER
                ) {
                    // perform action on key pres
                    viewModel.search(binding.search.editText?.text.toString(), binding)
                    // hide soft keyboard programmatically
                    hideKeyboard(requireActivity())
                    // clear focus and hide cursor from edit text
                    editText.clearFocus()
                    editText.isCursorVisible = false

                    return true
                }
                return false
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}