package com.brafik.brafshop.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.brafik.brafshop.R
import com.brafik.brafshop.adapters.MainAdapter
import com.brafik.brafshop.constants.Functions
import com.brafik.brafshop.databinding.FragmentMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private var user: FirebaseUser? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var navView: NavigationView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        val view = binding.root
        user = Firebase.auth.currentUser
        auth = Firebase.auth
        navView = requireActivity().findViewById(R.id.nav_view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (user == null) {
            reload()
        }
        Functions.hideItem(auth.currentUser != null, navView)
        changeMenu(navView, user!!)
        activity?.invalidateOptionsMenu()
        val editText = binding.search.editText!!
        binding.apply {
            mainRecycleView.adapter = MainAdapter()
            leafButton.setOnClickListener {
                findNavController().navigate(MainFragmentDirections.mainToCategory("leaf"))
            }
            succulentsButton.setOnClickListener {
                findNavController().navigate(MainFragmentDirections.mainToCategory("succulents"))
            }
            flowersButton.setOnClickListener {
                findNavController().navigate(MainFragmentDirections.mainToCategory("flowers"))
            }
            search.setEndIconOnClickListener {
                findNavController().navigate(MainFragmentDirections.mainToSearch(search.editText?.text.toString()))
            }
            menuButtonInMain.setOnClickListener {
                requireActivity().findViewById<ImageView>(R.id.menuButton).performClick()
            }
            val action = MainFragmentDirections.mainToSearch(search.editText?.text.toString())
            Functions.onEnterListener(editText, this@MainFragment, action)
        }
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                binding.textView.text =
                    getString(R.string.hello).plus(" ${Firebase.auth.currentUser?.displayName}")
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun reload() {
        findNavController().navigate(MainFragmentDirections.mainToRegister())
    }

    private fun changeMenu(navView: NavigationView, user: FirebaseUser) {
        val navHeader = navView.getHeaderView(0)
        val name = navHeader.findViewById<TextView>(R.id.nameInMenuDrawer)
        val mail = navHeader.findViewById<TextView>(R.id.mailInMenuDrawer)
        name.text = user.displayName
        mail.text = user.email
        if (mail.text.isNotEmpty()) {
            mail.visibility = View.VISIBLE
        }
    }

}