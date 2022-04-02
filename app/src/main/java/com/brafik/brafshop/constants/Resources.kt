package com.brafik.brafshop.constants

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.brafik.brafshop.R
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.facebook.login.LoginManager
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


val IMAGES = listOf(
    "https://firebasestorage.googleapis.com/v0/b/brafplantshop.appspot.com/o/plants%2Fplant-1.png?alt=media&token=ff4e5bd0-cb38-44dd-a254-9d4fc77e933a".toUri(),
    "https://firebasestorage.googleapis.com/v0/b/brafplantshop.appspot.com/o/plants%2Fplant-2.jpg?alt=media&token=26404ff4-39cf-4c73-9e89-4d6384913b51".toUri()
)

object Functions {
    fun getImage(img: ImageView, id: Int, context: Context) {
        val circleProgressBar = CircularProgressDrawable(context)
        circleProgressBar.strokeWidth = 5f
        circleProgressBar.centerRadius = 30f
        circleProgressBar.start()

        Glide.with(img)
            .load(IMAGES[id])
            .placeholder(circleProgressBar)
            .transition(DrawableTransitionOptions.withCrossFade(50))
            .priority(Priority.HIGH)
            .skipMemoryCache(true)
            .fitCenter()
            .into(img)
    }

    fun countOnClickSetter(plusCount: ImageButton, minusCount: ImageButton, count: TextView) {
        plusCount.setOnClickListener {
            var plus = count.text.toString().toInt()
            if (plus < 15) {
                plus++
            }
            count.text = plus.toString()
        }
        minusCount.setOnClickListener {
            var minus = count.text.toString().toInt()
            if (minus != 0) {
                minus--
            }
            count.text = minus.toString()
        }
    }

    fun onEnterListener(editText: EditText, fragment: Fragment, action: NavDirections) {
        editText.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                // if the event is a key down event on the enter button
                if (event.action == KeyEvent.ACTION_DOWN &&
                    keyCode == KeyEvent.KEYCODE_ENTER
                ) {
                    // perform action on key pres
                    fragment.findNavController().navigate(action)
                    // hide soft keyboard programmatically
                    hideKeyboard(fragment.requireActivity())
                    // clear focus and hide cursor from edit text
                    editText.clearFocus()
                    editText.isCursorVisible = false

                    return true
                }
                return false
            }

        })
    }

    fun hideKeyboard(activity: Activity) {
        val imm: InputMethodManager =
            activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view: View? = activity.currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0)
    }

    fun menuItemSelected(
        item: MenuItem,
        drawerLayout: DrawerLayout,
        navView: NavigationView,
        navController: NavController
    ): Boolean {
        return when (item.itemId) {
            R.id.nav_login_button -> {
                navController.navigate(R.id.loginFragment)
                drawerLayout.closeDrawer(navView)
                Log.d("Main", "nav_login_button")
                true
            }
            R.id.nav_signup_button -> {
                navController.navigate(R.id.registerFragment)
                drawerLayout.closeDrawer(navView)
                Log.d("Main", "nav_signup_button")
                true
            }
            R.id.nav_main_button -> {
                navController.navigate(R.id.mainFragment)
                drawerLayout.closeDrawer(navView)
                Log.d("Main", "nav_main_button")
                true
            }
            R.id.nav_search_button -> {
                navController.navigate(R.id.searchFragment)
                drawerLayout.closeDrawer(navView)
                Log.d("Main", "nav_main_button")
                true
            }
            R.id.nav_signout_button -> {
                Firebase.auth.signOut()
                LoginManager.getInstance().logOut()
                navController.navigateUp()
                drawerLayout.closeDrawer(navView)
                true
            }
            R.id.nav_leaf_button -> {
                navController.navigate(
                    R.id.categoryPageFragment,
                    bundleOf(Pair("category", "leaf"))
                )
                drawerLayout.closeDrawer(navView)
                Log.d("Main", "nav_leaf_button")
                true
            }
            R.id.nav_succulents_button -> {
                navController.navigate(
                    R.id.categoryPageFragment,
                    bundleOf(Pair("category", "succulents"))
                )
                drawerLayout.closeDrawer(navView)
                Log.d("Main", "nav_succulents_button")
                true
            }
            R.id.nav_flowers_button -> {
                navController.navigate(
                    R.id.categoryPageFragment,
                    bundleOf(Pair("category", "flowers"))
                )
                drawerLayout.closeDrawer(navView)
                Log.d("Main", "nav_flowers_button")
                true
            }
            else -> {
                Log.d("Main", "ERROR")
                false
            }
        }
    }

    fun hideItem(isUser: Boolean, navView: NavigationView) {
        navView.menu.setGroupVisible(R.id.unauthorized, !isUser)
        navView.menu.setGroupVisible(R.id.authorized, isUser)
        navView.menu.setGroupVisible(R.id.categories, isUser)
        navView.menu.findItem(R.id.nav_signout_button).isVisible = isUser
    }
}