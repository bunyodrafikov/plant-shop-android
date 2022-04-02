package com.brafik.brafshop.fragments

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.brafik.brafshop.R
import com.brafik.brafshop.constants.Functions
import com.brafik.brafshop.databinding.FragmentRegisterBinding
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
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.errorLayout.visibility = View.GONE
        binding.progressBar.visibility = View.GONE

        auth = Firebase.auth

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

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Functions.hideItem(
            auth.currentUser != null,
            requireActivity().findViewById(R.id.nav_view)
        )

        binding.apply {
            registerButton.setOnClickListener {
                val email = loginField.text.toString()
                val password = passwordField.text.toString()
                val name = nameField.text.toString()
                Log.d("EMAILS", email.plus(password))
                signUp(email, password, name)
            }
            loginButtonInRegister.setOnClickListener {
                findNavController().navigate(RegisterFragmentDirections.registerToLogin())
            }
            googleSignUp.setOnClickListener {
                providerState = listOfProviders[1]
                oneTapClient.beginSignIn(googleSignUpRequest)
                    .addOnSuccessListener(requireActivity()) { result ->
                        binding.progressBar.visibility = View.VISIBLE
                        errorLayout.visibility = View.GONE
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
                        binding.progressBar.visibility = View.GONE
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
            menuButtonInRegister.setOnClickListener {
                requireActivity().findViewById<ImageView>(R.id.menuButton).performClick()
            }
        }
    }

    private fun signUp(email: String, password: String, name: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail")
                    val user = auth.currentUser

                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name).build()

                    user!!.updateProfile(profileUpdates)
                        .addOnCompleteListener {
                            user.reload()
                                .addOnSuccessListener {
                                    Log.w(TAG, auth.currentUser?.displayName.toString())
                                    updateUI(user)
                                }
                            if (it.isSuccessful) {
                                Log.d(TAG, "User profile updated.")
                            }
                        }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail", task.exception)
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
            findNavController().navigate(RegisterFragmentDirections.registerToMain())
        } else {
            Log.w(TAG, "UpdateUI is NULL")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "RegisterFragment"
        private const val REQ_ONE_TAP: Int = 9002
    }
}