package com.brafik.brafshop.fragments

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.forEach
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.brafik.brafshop.R
import com.brafik.brafshop.constants.Functions
import com.brafik.brafshop.databinding.FragmentLoginBinding
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginFragment : Fragment(R.layout.fragment_login) {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val listOfProviders = listOf("Email", "Google", "Facebook")
    private var providerState = listOfProviders[0]

    //Auth reference
    private lateinit var auth: FirebaseAuth

    //Google client and request
    private lateinit var oneTapClient: SignInClient
    private lateinit var googleSignUpRequest: BeginSignInRequest

    //Facebook
    private lateinit var callbackManager: CallbackManager
    private lateinit var buttonFacebookLogin: LoginButton

    private lateinit var navView: NavigationView
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.errorLayout.visibility = View.GONE
        binding.progressBar.visibility = View.GONE

        oneTapClient = Identity.getSignInClient(requireActivity())

        googleSignUpRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.google_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()

        //Firebase Auth
        auth = Firebase.auth

        navView = activity?.findViewById<View>(R.id.nav_view) as NavigationView
        drawerLayout = activity?.findViewById<View>(R.id.drawerLayout) as DrawerLayout

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val currentUser = auth.currentUser
        updateUI(currentUser)

        Functions.hideItem(currentUser != null, navView)
        activity?.invalidateOptionsMenu()

        binding.apply {
            menuButtonInLogin.setOnClickListener {
                requireActivity().findViewById<ImageView>(R.id.menuButton).performClick()
            }

            registerButtonInLogin.setOnClickListener {
                findNavController().navigate(LoginFragmentDirections.loginToRegister())
            }
            //Mail Sign In
            loginButton.setOnClickListener {
                progressBar.visibility = View.VISIBLE
                errorLayout.visibility = View.GONE
                val email = loginField.text.toString()
                val password = passwordField.text.toString()
                Log.d("EMAILS", email.plus(password))
                signIn(email, password)
            }
            //Google Sign In
            googleSignIn.setOnClickListener {
                progressBar.visibility = View.VISIBLE
                errorLayout.visibility = View.GONE
                providerState = listOfProviders[1]
                oneTapClient.beginSignIn(googleSignUpRequest)
                    .addOnSuccessListener(requireActivity()) { result ->
                        try {

                            startIntentSenderForResult(
                                result.pendingIntent.intentSender, REQ_ONE_TAP,
                                null, 0, 0, 0, null
                            )
                        } catch (e: IntentSender.SendIntentException) {
                            Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                        }
                    }
                    .addOnFailureListener(requireActivity()) { e ->
                        // No Google Accounts found. Just continue presenting the signed-out UI.
                        Log.d(TAG, e.localizedMessage!!)
                    }
            }

            //Facebook Login button
            callbackManager = CallbackManager.Factory.create()
            buttonFacebookLogin = binding.facebookButton
            buttonFacebookLogin.registerCallback(callbackManager, object :
                FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    Log.d(TAG, "facebook:onSuccess:$result")
                    handleFacebookAccessToken(result.accessToken)
                }

                override fun onCancel() {
                    Log.d(TAG, "facebook:onCancel")
                }

                override fun onError(error: FacebookException) {
                    Log.d(TAG, "facebook:onError", error)
                }
            })
            facebookSignInView.setOnClickListener {
                progressBar.visibility = View.VISIBLE
                errorLayout.visibility = View.GONE
                facebookButton.performClick()
            }
        }
        navView.menu.forEach { menuItem ->
            menuItem.setOnMenuItemClickListener {
                Functions.menuItemSelected(it, drawerLayout, navView, findNavController())
            }
        }
    }

    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInMessage: Success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInMessage: Failure", task.exception)
                    Toast.makeText(
                        requireContext(), "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI(null)
                    binding.progressBar.visibility = View.GONE
                    binding.errorText.text = task.exception?.message.toString()
                    binding.errorLayout.visibility = View.VISIBLE
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (providerState) {
            listOfProviders[1] -> {
                when (requestCode) {
                    REQ_ONE_TAP -> {
                        try {
                            val credential = oneTapClient.getSignInCredentialFromIntent(data)
                            val idToken = credential.googleIdToken
                            val password = credential.password
                            when {
                                idToken != null -> {
                                    // Got an ID token from Google. Use it to authenticate
                                    // with your backend.
                                    firebaseAuthWithGoogle(idToken)
                                    Log.d(TAG, "Got ID token.")
                                }
                                password != null -> {
                                    // Got a saved username and password. Use them to authenticate
                                    // with your backend.
                                    Log.d(TAG, "Got password.")
                                }
                                else -> {
                                    // Shouldn't happen.
                                    Log.d(TAG, "No ID token or password!")
                                }
                            }
                        } catch (e: ApiException) {
                            when (e.statusCode) {
                                CommonStatusCodes.CANCELED -> {
                                    Log.d(TAG, "One-tap dialog was closed.")
                                }
                                CommonStatusCodes.NETWORK_ERROR -> {
                                    Log.d(TAG, "One-tap encountered a network error.")
                                    // Try again or just ignore.
                                }
                                else -> {
                                    Log.d(
                                        TAG, "Couldn't get credential from result." +
                                                " (${e.localizedMessage})"
                                    )
                                }
                            }
                        }
                    }
                }
            }
            listOfProviders[2] -> {
                callbackManager.onActivityResult(requestCode, resultCode, data)
            }
            else -> {
                Log.d(
                    TAG, "Set the onActivityResult"
                )
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    binding.progressBar.visibility = View.GONE
                    binding.errorText.text = task.exception?.message.toString()
                    binding.errorLayout.visibility = View.VISIBLE
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    updateUI(null)
                }
            }
    }

    // [START auth_with_facebook]
    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d(TAG, "handleFacebookAccessToken:$token")

        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    updateUI(null)
                    binding.progressBar.visibility = View.GONE
                    binding.errorText.text = task.exception?.message.toString()
                    binding.errorLayout.visibility = View.VISIBLE
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            findNavController().navigate(LoginFragmentDirections.loginToMain())
            binding.progressBar.visibility = View.GONE
        } else {
            Log.w(TAG, "UpdateUI is NULL")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "LoginFragment"
        private const val REQ_ONE_TAP = 9001
    }
}