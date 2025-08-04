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

class Login : Fragment() {

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(
            requireActivity().application
        )
    }

    // Dichiarazione dell'istanza per l'email
    private lateinit var emailInstance: EditText

    // Dichiarazione dell'istanza per la password
    private lateinit var passwordInstance: EditText

    // Dichiarazione button next
    private lateinit var nextButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inizializzazione dell'istanza per l'email
        emailInstance = view.findViewById(R.id.login_email)
        // Inizializzazione dell'istanza per la password
        passwordInstance = view.findViewById(R.id.login_password)
        // Inizializzazione del button next
        nextButton = view.findViewById(R.id.login_next)
        val navController = findNavController()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userSessionState.collectLatest { state ->
                if (state.isLoading) {
                    // Pulsante disabilitato durante il login
                    nextButton.isEnabled = false
                } else {
                    nextButton.isEnabled = true
                }

                // Messaggi di errore
                if (state.errorMessage != null) {
                    Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show()
                }

                if (state.isLoggedIn) {
                    Toast.makeText(requireContext(), "Accesso riuscito per ${state.email}!", Toast.LENGTH_SHORT).show()
                    val action = LoginDirections.actionLoginToSelectionMode(state.email!!)
                    navController.navigate(action)
                }
            }
        }

        // Aggiunta di un listener al button next
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

            viewModel.loginOrRegister(email, password)
        }
    }
}