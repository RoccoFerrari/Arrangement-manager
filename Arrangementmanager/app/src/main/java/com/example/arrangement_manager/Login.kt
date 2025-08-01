package com.example.arrangement_manager

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Login.newInstance] factory method to
 * create an instance of this fragment.
 */
class Login : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(
            requireActivity().application, // Passa l'istanza di Application
            (requireActivity().application as YourApplicationClass).arrangementDAO
        )
    }

    // Dichiarazione dell'istanza per l'email
    private lateinit var emailInstance: EditText

    // Dichiarazione dell'istanza per la password
    private lateinit var passwordInstance: EditText

    // Dichiarazione button next
    private lateinit var nextButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

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
                    nextButton.isEnabled = false // Disabilita il pulsante durante il caricamento
                } else {
                    nextButton.isEnabled = true // Riabilita il pulsante
                }

                // Gestisci i messaggi di errore
                if (state.errorMessage != null) {
                    Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show()
                }

                if (state.isLoggedIn) {
                    Toast.makeText(requireContext(), "Accesso riuscito per ${state.email}!", Toast.LENGTH_SHORT).show()
                    val action = LoginDirections.actionLoginToSelectionMode(state.email!!)
                    findNavController().navigate(action)
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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Login.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Login().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}