package com.ciclismo.portugal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.ciclismo.portugal.presentation.navigation.CiclismoNavGraph
import com.ciclismo.portugal.presentation.theme.CiclismoPortugalTheme
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var consentInformation: ConsentInformation
    private var consentForm: ConsentForm? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()
        setupGDPRConsentForm()

        setContent {
            CiclismoPortugalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CiclismoNavGraph()
                }
            }
        }
    }

    private fun setupGDPRConsentForm() {
        // Configurar parâmetros de consentimento
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)

        // Solicitar atualização das informações de consentimento
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                // Consentimento atualizado com sucesso
                Log.d("MainActivity", "GDPR consent information updated")

                // Verificar se é necessário mostrar o formulário
                if (consentInformation.isConsentFormAvailable &&
                    consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    loadConsentForm()
                }
            },
            { error ->
                // Erro ao obter informação de consentimento
                Log.e("MainActivity", "Error updating GDPR consent: ${error.message}")
            }
        )
    }

    private fun loadConsentForm() {
        UserMessagingPlatform.loadConsentForm(
            this,
            { form ->
                consentForm = form
                // Mostrar formulário se o consentimento ainda é necessário
                if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    form.show(this) { error ->
                        if (error != null) {
                            Log.e("MainActivity", "Consent form error: ${error.message}")
                        }
                        // Após fechar o formulário, carregar novamente se necessário
                        setupGDPRConsentForm()
                    }
                }
            },
            { error ->
                Log.e("MainActivity", "Error loading consent form: ${error.message}")
            }
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}
