package com.spikai.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

// MARK: - User-Facing Error Types
enum class SpikError(val errorDescription: String, val recoverySuggestion: String, val isRetryable: Boolean, val shouldShowToUser: Boolean) {
    // Network Errors
    NO_INTERNET_CONNECTION(
        "Sin conexión a internet",
        "Verifica tu conexión a internet e intenta nuevamente.",
        true,
        true
    ),
    SERVER_UNAVAILABLE(
        "El servidor no está disponible",
        "Por favor intenta más tarde. Si el problema persiste, contacta soporte.",
        true,
        true
    ),
    REQUEST_TIMEOUT(
        "La solicitud tardó demasiado tiempo",
        "Verifica tu conexión e intenta nuevamente.",
        true,
        false
    ),
    NETWORK_ERROR(
        "Error de conexión",
        "Verifica tu conexión a internet e intenta nuevamente.",
        true,
        false
    ),
    
    // Authentication Errors
    AUTHENTICATION_FAILED(
        "Error de autenticación",
        "Por favor inicia sesión nuevamente.",
        false,
        true
    ),
    USER_NOT_FOUND(
        "Usuario no encontrado",
        "Crea una cuenta nueva o verifica tus credenciales.",
        false,
        false
    ),
    INVALID_CREDENTIALS(
        "Credenciales inválidas",
        "Verifica tu email y contraseña.",
        false,
        false
    ),
    ACCOUNT_DISABLED(
        "Cuenta deshabilitada",
        "Contacta soporte para reactivar tu cuenta.",
        false,
        true
    ),
    AUTH_TOKEN_EXPIRED(
        "Sesión expirada",
        "Por favor inicia sesión nuevamente.",
        true,
        true
    ),
    
    // Data Errors
    INVALID_DATA(
        "Datos inválidos",
        "Verifica que la información esté completa y correcta.",
        false,
        true
    ),
    DATA_CORRUPTED(
        "Los datos están corruptos",
        "Intenta cerrar y abrir la app nuevamente.",
        false,
        true
    ),
    DATA_NOT_FOUND(
        "Datos no encontrados",
        "La información solicitada no está disponible.",
        false,
        false
    ),
    SYNC_FAILED(
        "Error de sincronización",
        "Verifica tu conexión e intenta sincronizar nuevamente.",
        true,
        false
    ),
    
    // App Errors
    FEATURE_UNAVAILABLE(
        "Función no disponible",
        "Esta función estará disponible pronto.",
        false,
        false
    ),
    PERMISSION_DENIED(
        "Permisos insuficientes",
        "Ve a Configuración para otorgar los permisos necesarios.",
        false,
        true
    ),
    SYSTEM_ERROR(
        "Error del sistema",
        "Intenta cerrar y abrir la app nuevamente.",
        false,
        false
    ),
    UNKNOWN_ERROR(
        "Error desconocido",
        "Por favor intenta nuevamente. Si el problema persiste, contacta soporte.",
        false,
        false
    ),
    NOTIFICATION_SETUP_FAILED(
        "Error al configurar notificaciones",
        "Verifica los permisos de notificaciones en Configuración.",
        false,
        false
    );
}

// MARK: - Error Handling Service
class ErrorHandlingService {
    
    private val _currentError = MutableStateFlow<SpikError?>(null)
    val currentError: StateFlow<SpikError?> = _currentError.asStateFlow()
    
    private val _isShowingError = MutableStateFlow(false)
    val isShowingError: StateFlow<Boolean> = _isShowingError.asStateFlow()
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    companion object {
        val shared = ErrorHandlingService()
    }
    
    // Convert common errors to user-friendly SpikError
    fun handleError(error: Throwable): SpikError {
        println("🔍 [ErrorHandlingService] Converting error to SpikError:")
        println("🔍 [ErrorHandlingService] Original error: $error")
        println("🔍 [ErrorHandlingService] Error type: ${error::class.java}")
        
        if (error is SpikErrorException) {
            println("🔍 [ErrorHandlingService] Already a SpikError: ${error.spikError}")
            return error.spikError
        }
        
        // Network errors
        when (error) {
            is UnknownHostException -> {
                println("🔍 [ErrorHandlingService] UnknownHostException detected")
                return SpikError.NO_INTERNET_CONNECTION
            }
            is SocketTimeoutException -> {
                println("🔍 [ErrorHandlingService] SocketTimeoutException detected")
                return SpikError.REQUEST_TIMEOUT
            }
            is IOException -> {
                println("🔍 [ErrorHandlingService] IOException detected")
                return SpikError.NETWORK_ERROR
            }
        }
        
        // TODO: Add specific Firebase Auth error handling when Firebase is integrated
        // TODO: Add specific HTTP error handling based on response codes
        
        println("🔍 [ErrorHandlingService] Converting to unknownError - no specific handler found")
        return SpikError.UNKNOWN_ERROR
    }
    
    // Show error to user - only for critical errors that affect user experience
    fun showError(error: Throwable, file: String = "", function: String = "", line: Int = 0) {
        val spikError = handleError(error)
        
        // Only show critical errors to user
        if (!spikError.shouldShowToUser) {
            println("📝 [ErrorHandlingService] Non-critical error logged (not showing to user): ${spikError.errorDescription}")
            return
        }
        
        println("🚨 [ErrorHandlingService] === CRITICAL ERROR - SHOWING TO USER ===")
        println("🚨 [ErrorHandlingService] Location: $file:$line in $function")
        println("🚨 [ErrorHandlingService] Original error: $error")
        println("🚨 [ErrorHandlingService] Error type: ${error::class.java}")
        println("🚨 [ErrorHandlingService] Error description: ${error.message}")
        println("🚨 [ErrorHandlingService] === END CRITICAL ERROR INFO ===")
        
        coroutineScope.launch {
            _currentError.value = spikError
            _isShowingError.value = true
        }
    }
    
    // Overload for SpikError directly
    fun showError(spikError: SpikError, file: String = "", function: String = "", line: Int = 0) {
        // Only show critical errors to user
        if (!spikError.shouldShowToUser) {
            println("📝 [ErrorHandlingService] Non-critical error logged (not showing to user): ${spikError.errorDescription}")
            return
        }
        
        println("🚨 [ErrorHandlingService] === CRITICAL ERROR - SHOWING TO USER ===")
        println("🚨 [ErrorHandlingService] Location: $file:$line in $function")
        println("🚨 [ErrorHandlingService] SpikError: $spikError")
        println("🚨 [ErrorHandlingService] Error description: ${spikError.errorDescription}")
        println("🚨 [ErrorHandlingService] === END CRITICAL ERROR INFO ===")
        
        coroutineScope.launch {
            _currentError.value = spikError
            _isShowingError.value = true
        }
    }
    
    // Log error without showing to user - for non-critical errors
    fun logError(error: Throwable, file: String = "", function: String = "", line: Int = 0) {
        println("📝 [ErrorHandlingService] Logging non-critical error:")
        println("📝 [ErrorHandlingService] Location: $file:$line in $function")
        println("📝 [ErrorHandlingService] Error: ${error.message}")
    }
    
    // Clear current error
    fun clearError() {
        coroutineScope.launch {
            _currentError.value = null
            _isShowingError.value = false
        }
    }
}

// Helper exception class to wrap SpikError
class SpikErrorException(val spikError: SpikError) : Exception(spikError.errorDescription)
