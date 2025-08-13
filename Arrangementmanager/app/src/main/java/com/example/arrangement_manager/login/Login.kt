package com.example.arrangement_manager.login

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.arrangement_manager.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * A [Fragment] for user login and registration.
 *
 * This fragment provides a UI for users to enter their email and password,
 * and handles the login/registration process by interacting with the [LoginViewModel].
 * It also observes the ViewModel's state to update the UI, show error messages,
 * and navigate to the next screen upon successful login.
 */
class Login : Fragment() {

    // Instantiates the ViewModel using a custom factory
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(
            requireActivity().application
        )
    }

    // References to UI components
    private lateinit var emailInstance: EditText

    private lateinit var passwordInstance: EditText

    private lateinit var nextButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI components using their IDs from the layout
        emailInstance = view.findViewById(R.id.login_email)
        passwordInstance = view.findViewById(R.id.login_password)
        nextButton = view.findViewById(R.id.login_next)
        val navController = findNavController()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userSessionState.collectLatest { state ->
                // Enable/disable the button based on the loading state
                if (state.isLoading) {
                    // Button disabled during login
                    nextButton.isEnabled = false
                } else {
                    nextButton.isEnabled = true
                }

                // Error messages
                if (state.errorMessage != null) {
                    Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show()
                }

                // Navigate to the next screen upon successful login
                if (state.isLoggedIn) {
                    Toast.makeText(requireContext(), "Login successful for ${state.email}!", Toast.LENGTH_SHORT).show()
                    val action = LoginDirections.actionLoginToSelectionMode(state.email!!)
                    navController.navigate(action)
                }
            }
        }

        // Set up the click listener for the "Next" button
        nextButton.setOnClickListener {
            val email = emailInstance.text.toString().trim()
            val password = passwordInstance.text.toString().trim()

            if(email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all the fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Call the ViewModel's login method
            viewModel.loginOrRegister(email, password)
        }
    }
}