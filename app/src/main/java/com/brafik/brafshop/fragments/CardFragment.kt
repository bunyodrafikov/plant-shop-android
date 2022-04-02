package com.brafik.brafshop.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.brafik.brafshop.R
import com.brafik.brafshop.constants.Functions
import com.brafik.brafshop.databinding.FragmentCardBinding
import com.brafik.brafshop.entities.Plant
import com.brafik.brafshop.viewmodels.CardViewModel
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.wallet.PaymentData
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.json.JSONException
import org.json.JSONObject

class CardFragment : Fragment(R.layout.fragment_card) {
    private var _binding: FragmentCardBinding? = null
    private val binding get() = _binding!!
    private val plants = Firebase.firestore.collection("plants")
    private lateinit var plant: Plant
    private lateinit var googlePayButton: RelativeLayout

    private val viewModel: CardViewModel by viewModels {
        CardViewModel.CardViewModelFactory(activity?.application)
    }

    private val resolvePaymentForResult =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
            when (result.resultCode) {
                AppCompatActivity.RESULT_OK ->
                    result.data?.let { intent ->
                        PaymentData.getFromIntent(intent)?.let(::handlePaymentSuccess)
                    }

                AppCompatActivity.RESULT_CANCELED -> {
                    // The user cancelled the payment attempt
                    Toast.makeText(requireContext(), "Something got wrong", Toast.LENGTH_LONG)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val id = arguments?.getLong("id") ?: 0
        plants
            .whereEqualTo("id", id)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val plantRef: MutableMap<String, Any> = mutableMapOf()
                querySnapshot.documents.first().data?.let { plantRef.putAll(it) }
                plant = Plant(
                    plantRef["price"].toString().toLong(),
                    plantRef["name"].toString(),
                    plantRef["description"].toString(),
                    plantRef["pots"] as List<Long>,
                    plantRef["id"].toString().toLong(),
                    plantRef["category"].toString(),
                )
                Log.e("Plants", plant.toString())
                binding.apply {
                    plantTitle.text = plant.name
                    plantPrice.text = plant.price.toString().plus("$")
                    plantDescription.text = plant.description

                    backButton.setOnClickListener { view ->
                        view.findNavController().navigateUp()
                    }
                    Functions.countOnClickSetter(plusCount, minusCount, count)
                    Functions.getImage(plantImage, plant.id.toInt(), requireContext())
                    googlePayButton = buyButton
                    buyButton.setOnClickListener {
                        requestPayment(plant.id)
                        Log.d("pay", "click")
                    }

                    // Check whether Google Pay can be used to complete a payment
                    viewModel.canUseGooglePay.observe(
                        requireActivity(),
                        Observer(::setGooglePayAvailable)
                    )
                }
            }
    }

    /**
     * If isReadyToPay returned `true`, show the button and hide the "checking" text. Otherwise,
     * notify the user that Google Pay is not available. Please adjust to fit in with your current
     * user flow. You are not required to explicitly let the user know if isReadyToPay returns `false`.
     *
     * @param available isReadyToPay API response.
     */
    private fun setGooglePayAvailable(available: Boolean) {
        if (available) {
            googlePayButton.visibility = View.VISIBLE
        } else {
            Toast.makeText(
                requireContext(), R.string.googlepay_status_unavailable,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun requestPayment(price: Long) {

        // Disables the button to prevent multiple clicks.
        googlePayButton.isClickable = false

        // The price provided to the API should include taxes and shipping.
        // This price is not displayed to the user.
        val dummyPriceCents = 100L
        val shippingCostCents = 100L
        val task = viewModel.getLoadPaymentDataTask(price + dummyPriceCents + shippingCostCents)

        task.addOnCompleteListener { completedTask ->
            if (completedTask.isSuccessful) {
                completedTask.result.let(::handlePaymentSuccess)
            } else {
                when (val exception = completedTask.exception) {
                    is ResolvableApiException -> {
                        resolvePaymentForResult.launch(
                            IntentSenderRequest.Builder(exception.resolution).build()
                        )
                    }
                    is ApiException -> {
                        handleError(exception.statusCode, exception.message)
                    }
                    else -> {
                        handleError(
                            CommonStatusCodes.INTERNAL_ERROR, "Unexpected non API" +
                                    " exception when trying to deliver the task result to an activity!"
                        )
                    }
                }
            }

            // Re-enables the Google Pay payment button.
            googlePayButton.isClickable = true
        }
    }

    /**
     * PaymentData response object contains the payment information, as well as any additional
     * requested information, such as billing and shipping address.
     *
     * @param paymentData A response object returned by Google after a payer approves payment.
     * @see [Payment
     * Data](https://developers.google.com/pay/api/android/reference/object.PaymentData)
     */
    private fun handlePaymentSuccess(paymentData: PaymentData) {
        val paymentInformation = paymentData.toJson()

        try {
            // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
            val paymentMethodData =
                JSONObject(paymentInformation).getJSONObject("paymentMethodData")
            val billingName = paymentMethodData.getJSONObject("info")
                .getJSONObject("billingAddress").getString("name")
            Log.d("BillingName", billingName)

            Toast.makeText(
                context,
                getString(R.string.payments_show_name, billingName),
                Toast.LENGTH_LONG
            ).show()

            // Logging token string.
            Log.d(
                "Google Pay token", paymentMethodData
                    .getJSONObject("tokenizationData")
                    .getString("token")
            )

        } catch (error: JSONException) {
            Log.e("handlePaymentSuccess", "Error: $error")
        }

    }

    /**
     * At this stage, the user has already seen a popup informing them an error occurred. Normally,
     * only logging is required.
     *
     * @param statusCode will hold the value of any constant from CommonStatusCode or one of the
     * WalletConstants.ERROR_CODE_* constants.
     * @see [
     * Wallet Constants Library](https://developers.google.com/android/reference/com/google/android/gms/wallet/WalletConstants.constant-summary)
     */
    private fun handleError(statusCode: Int, message: String?) {
        Log.w("loadPaymentData failed", "Error code: $statusCode, Message: $message")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
