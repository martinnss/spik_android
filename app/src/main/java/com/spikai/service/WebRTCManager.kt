package com.spikai.service

import android.content.Context
import android.media.AudioManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spikai.model.ConnectionStatus
import com.spikai.model.ConversationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.webrtc.*
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * WebRTCManager - Critical service for real-time AI conversation using WebRTC
 * This is a direct translation of the Swift WebRTCManager with perfect feature parity
 */
class WebRTCManager(private val context: Context) : ViewModel() {
    
    // UI State - Published properties from Swift
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    private val _eventTypeStr = MutableStateFlow("")
    val eventTypeStr: StateFlow<String> = _eventTypeStr.asStateFlow()
    
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()
    
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()
    
    // Private state for feedback prevention
    private var isTemporarilyMutedForFeedbackPrevention: Boolean = false
    
    // Basic conversation text
    private val _conversation = MutableStateFlow<List<ConversationItem>>(emptyList())
    val conversation: StateFlow<List<ConversationItem>> = _conversation.asStateFlow()
    
    private val _outgoingMessage = MutableStateFlow("")
    val outgoingMessage: StateFlow<String> = _outgoingMessage.asStateFlow()
    
    // Store items by item_id for easy updates
    private val conversationMap: MutableMap<String, ConversationItem> = mutableMapOf()
    
    // Model & session config - settable for ephemeral tokens
    var apiKey: String = ""
    var modelName: String = "gpt-4o-mini-realtime-preview-2024-12-17"
    var systemInstructions: String = ""
    var voice: String = "alloy"
    
    // Backend configuration
    private var backendURL: String = NetworkConfig.getBackendURL(context)
    
    // WebRTC references
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null
    private var audioTrack: AudioTrack? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    
    // Audio management
    private var audioManager: AudioManager? = null
    
    // JSON parser
    private val json = Json { ignoreUnknownKeys = true }
    
    // HTTP client for network requests
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // Connection timeout job
    private var connectionTimeoutJob: Job? = null
    
    init {
        println("🎯 [WebRTCManager] Service initialized")
        println("🌐 [WebRTCManager] Backend URL: $backendURL")
        
        // Initialize WebRTC
        initializeWebRTC()
        
        // Initialize audio manager
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    
    companion object {
        @Volatile
        private var INSTANCE: WebRTCManager? = null
        
        fun getInstance(context: Context): WebRTCManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebRTCManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // MARK: - Initialization
    
    private fun initializeWebRTC() {
        println("🔧 [WebRTCManager] Initializing WebRTC...")
        
        try {
            // Initialize WebRTC
            val initializeOptions = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(initializeOptions)
            
            // Create PeerConnectionFactory
            val options = PeerConnectionFactory.Options()
            val encoderFactory = DefaultVideoEncoderFactory(null, true, true)
            val decoderFactory = DefaultVideoDecoderFactory(null)
            
            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(createAudioDeviceModule())
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory()
            
            println("✅ [WebRTCManager] WebRTC initialized successfully")
            
        } catch (e: Exception) {
            println("❌ [WebRTCManager] Failed to initialize WebRTC: ${e.message}")
            viewModelScope.launch {
                _connectionStatus.value = ConnectionStatus.FAILED
                _errorMessage.value = "WebRTC initialization failed: ${e.message}"
            }
        }
    }
    
    private fun createAudioDeviceModule(): AudioDeviceModule {
        return JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()
    }
    
    // MARK: - Public Methods
    
    /**
     * Fetch session configuration from backend and start WebRTC connection
     * Direct translation of Swift fetchSessionConfigAndStartConnection
     */
    fun fetchSessionConfigAndStartConnection(levelId: Int? = null, speed: Double = 1.0) {
        println("🚀 [WebRTCManager] fetchSessionConfigAndStartConnection called - levelId: $levelId, speed: $speed")
        
        viewModelScope.launch {
            _connectionStatus.value = ConnectionStatus.CONNECTING
            
            // Add a connection timeout to prevent infinite hanging
            connectionTimeoutJob?.cancel()
            connectionTimeoutJob = viewModelScope.launch {
                delay(30000) // 30 seconds
                if (_connectionStatus.value == ConnectionStatus.CONNECTING) {
                    println("⏰ [WebRTCManager] Connection timeout - forcing disconnect")
                    _connectionStatus.value = ConnectionStatus.FAILED
                    _errorMessage.value = "Connection timeout. Please try again."
                    stopConnection()
                }
            }
            
            try {
                val sessionConfig = fetchSessionConfig(levelId, speed)
                
                // Update configuration with backend response
                apiKey = sessionConfig.clientSecret
                systemInstructions = sessionConfig.instructions
                voice = sessionConfig.voice
                modelName = sessionConfig.model
                _errorMessage.value = "" // Clear any previous errors
                
                println("✅ [WebRTCManager] Session config loaded - Model: ${sessionConfig.model}, Voice: ${sessionConfig.voice}, Speed: $speed")
                
                // Add a small delay before starting connection to prevent race conditions
                delay(200)
                startConnectionWithConfig()
                
            } catch (e: Exception) {
                println("❌ [WebRTCManager] Failed to fetch session config: ${e.message}")
                _connectionStatus.value = ConnectionStatus.FAILED
                _errorMessage.value = "Failed to fetch session configuration: ${e.message}"
            }
        }
    }
    
    /**
     * Start a WebRTC connection using current configuration
     * Direct translation of Swift startConnectionWithConfig
     */
    private fun startConnectionWithConfig() {
        println("🔗 [WebRTCManager] startConnectionWithConfig called")
        
        if (apiKey.isEmpty()) {
            println("❌ [WebRTCManager] API key is empty. Cannot start connection.")
            viewModelScope.launch {
                _connectionStatus.value = ConnectionStatus.FAILED
                _errorMessage.value = "No API key available. Please check your backend configuration."
            }
            return
        }
        
        viewModelScope.launch {
            // Clear conversation
            _conversation.value = emptyList()
            conversationMap.clear()
            
            setupPeerConnection()
            setupLocalAudio()
            configureAudioSession()
            
            val pc = peerConnection
            if (pc == null) {
                println("❌ [WebRTCManager] PeerConnection is null after setup")
                return@launch
            }
            
            // Create a Data Channel for sending/receiving events
            val config = DataChannel.Init().apply {
                ordered = true
            }
            
            dataChannel = pc.createDataChannel("oai-events", config)
            dataChannel?.registerObserver(object : DataChannel.Observer {
                override fun onBufferedAmountChange(amount: Long) {}
                override fun onStateChange() {
                    val state = dataChannel?.state()
                    println("📡 [WebRTCManager] Data channel state changed: $state")
                    
                    if (state == DataChannel.State.OPEN) {
                        // Enforce speaker output when data channel opens
                        enforceSpeakerOutput()
                        sendSessionUpdate()
                    }
                }
                override fun onMessage(buffer: DataChannel.Buffer?) {
                    buffer?.data?.let { data ->
                        val bytes = ByteArray(data.remaining())
                        data.get(bytes)
                        val message = String(bytes, Charsets.UTF_8)
                        
                        viewModelScope.launch {
                            handleIncomingJSON(message)
                        }
                    }
                }
            })
            
            // Create an SDP offer
            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("levelControl", "true"))
            }
            
            pc.createOffer(object : SdpObserver {
                override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                    sessionDescription?.let { sdp ->
                        println("✅ [WebRTCManager] SDP offer created successfully")
                        
                        // Set local description
                        pc.setLocalDescription(object : SdpObserver {
                            override fun onSetSuccess() {
                                println("✅ [WebRTCManager] Local description set successfully")
                                
                                viewModelScope.launch {
                                    try {
                                        val remoteSdp = fetchRemoteSDP(sdp.description)
                                        
                                        val answerSdp = SessionDescription(SessionDescription.Type.ANSWER, remoteSdp)
                                        pc.setRemoteDescription(object : SdpObserver {
                                            override fun onSetSuccess() {
                                                println("✅ [WebRTCManager] Remote description set successfully")
                                                _connectionStatus.value = ConnectionStatus.CONNECTED
                                                connectionTimeoutJob?.cancel()
                                            }
                                            override fun onSetFailure(error: String?) {
                                                println("❌ [WebRTCManager] Failed to set remote description: $error")
                                                _connectionStatus.value = ConnectionStatus.FAILED
                                                _errorMessage.value = "Failed to set remote description: $error"
                                            }
                                            override fun onCreateSuccess(p0: SessionDescription?) {}
                                            override fun onCreateFailure(p0: String?) {}
                                        }, answerSdp)
                                        
                                    } catch (e: Exception) {
                                        println("❌ [WebRTCManager] Failed to fetch remote SDP: ${e.message}")
                                        _connectionStatus.value = ConnectionStatus.FAILED
                                        _errorMessage.value = "Failed to connect to OpenAI: ${e.message}"
                                    }
                                }
                            }
                            override fun onSetFailure(error: String?) {
                                println("❌ [WebRTCManager] Failed to set local description: $error")
                                _connectionStatus.value = ConnectionStatus.FAILED
                                _errorMessage.value = "Failed to set local description: $error"
                            }
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onCreateFailure(p0: String?) {}
                        }, sdp)
                    }
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(error: String?) {
                    println("❌ [WebRTCManager] Failed to create offer: $error")
                    viewModelScope.launch {
                        _connectionStatus.value = ConnectionStatus.FAILED
                        _errorMessage.value = "Failed to create offer: $error"
                    }
                }
                override fun onSetFailure(error: String?) {}
            }, constraints)
        }
    }
    
    fun stopConnection() {
        println("🛑 [WebRTCManager] stopConnection called")
        
        connectionTimeoutJob?.cancel()
        
        peerConnection?.close()
        peerConnection = null
        dataChannel = null
        audioTrack = null
        
        viewModelScope.launch {
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
        }
        
        println("✅ [WebRTCManager] Connection stopped")
    }
    
    /**
     * Set outgoing message text
     */
    fun setOutgoingMessage(message: String) {
        viewModelScope.launch {
            _outgoingMessage.value = message
        }
    }
    
    /**
     * Sends a custom "conversation.item.create" event
     * Direct translation of Swift sendMessage
     */
    fun sendMessage() {
        println("📤 [WebRTCManager] sendMessage called")
        
        val dc = dataChannel
        val message = _outgoingMessage.value.trim()
        
        if (dc == null || message.isEmpty()) {
            println("⚠️ [WebRTCManager] Cannot send message - dataChannel: ${dc != null}, message empty: ${message.isEmpty()}")
            return
        }
        
        val realtimeEvent = mapOf(
            "type" to "conversation.item.create",
            "item" to mapOf(
                "type" to "message",
                "role" to "user",
                "content" to listOf(
                    mapOf(
                        "type" to "input_text",
                        "text" to message
                    )
                )
            )
        )
        
        try {
            val jsonString = json.encodeToString(realtimeEvent)
            val buffer = DataChannel.Buffer(
                java.nio.ByteBuffer.wrap(jsonString.toByteArray(Charsets.UTF_8)),
                false
            )
            
            if (dc.send(buffer)) {
                println("✅ [WebRTCManager] Message sent successfully")
                viewModelScope.launch {
                    _outgoingMessage.value = ""
                }
                createResponse()
            } else {
                println("❌ [WebRTCManager] Failed to send message")
            }
            
        } catch (e: Exception) {
            println("❌ [WebRTCManager] Error sending message: ${e.message}")
        }
    }
    
    /**
     * Sends a "response.create" event
     * Direct translation of Swift createResponse
     */
    fun createResponse() {
        println("🎬 [WebRTCManager] createResponse called")
        
        val dc = dataChannel ?: return
        
        val realtimeEvent = mapOf("type" to "response.create")
        
        try {
            val jsonString = json.encodeToString(realtimeEvent)
            val buffer = DataChannel.Buffer(
                java.nio.ByteBuffer.wrap(jsonString.toByteArray(Charsets.UTF_8)),
                false
            )
            
            if (dc.send(buffer)) {
                println("✅ [WebRTCManager] Response creation event sent")
            } else {
                println("❌ [WebRTCManager] Failed to send response creation event")
            }
            
        } catch (e: Exception) {
            println("❌ [WebRTCManager] Error sending response create: ${e.message}")
        }
    }
    
    /**
     * Updates session configuration with the latest instructions and voice
     * Direct translation of Swift sendSessionUpdate
     */
    fun sendSessionUpdate() {
        println("⚙️ [WebRTCManager] sendSessionUpdate called")
        
        val dc = dataChannel
        if (dc == null || dc.state() != DataChannel.State.OPEN) {
            println("⚠️ [WebRTCManager] Data channel is not open. Cannot send session.update.")
            return
        }
        
        val sessionUpdate = mapOf(
            "type" to "session.update",
            "session" to mapOf(
                "modalities" to listOf("text", "audio"),
                "instructions" to systemInstructions,
                "voice" to voice,
                "input_audio_format" to "pcm16",
                "output_audio_format" to "pcm16",
                "input_audio_transcription" to mapOf(
                    "model" to "whisper-1",
                    "language" to "en"  // Force English transcription
                ),
                "turn_detection" to mapOf(
                    "create_response" to true
                ),
                "max_response_output_tokens" to "inf"
            )
        )
        
        try {
            val jsonString = json.encodeToString(sessionUpdate)
            val buffer = DataChannel.Buffer(
                java.nio.ByteBuffer.wrap(jsonString.toByteArray(Charsets.UTF_8)),
                false
            )
            
            if (dc.send(buffer)) {
                println("✅ [WebRTCManager] session.update event sent.")
                
                // After session update, trigger the AI to start talking
                viewModelScope.launch {
                    delay(500)
                    triggerInitialAIResponse()
                }
            } else {
                println("❌ [WebRTCManager] Failed to send session update")
            }
            
        } catch (e: Exception) {
            println("❌ [WebRTCManager] Failed to serialize session.update JSON: ${e.message}")
        }
    }
    
    /**
     * Triggers the AI agent to start the conversation immediately
     * Direct translation of Swift triggerInitialAIResponse
     */
    private fun triggerInitialAIResponse() {
        println("🎭 [WebRTCManager] triggerInitialAIResponse called")
        
        val dc = dataChannel
        if (dc == null || dc.state() != DataChannel.State.OPEN) {
            println("⚠️ [WebRTCManager] Data channel is not open. Cannot trigger initial AI response.")
            return
        }
        
        // Send a response.create event to make the AI start talking
        val responseEvent = mapOf("type" to "response.create")
        
        try {
            val jsonString = json.encodeToString(responseEvent)
            val buffer = DataChannel.Buffer(
                java.nio.ByteBuffer.wrap(jsonString.toByteArray(Charsets.UTF_8)),
                false
            )
            
            if (dc.send(buffer)) {
                println("✅ [WebRTCManager] Initial AI response triggered - AI should start talking now")
            } else {
                println("❌ [WebRTCManager] Failed to trigger initial AI response")
            }
            
        } catch (e: Exception) {
            println("❌ [WebRTCManager] Failed to trigger initial AI response: ${e.message}")
        }
    }
    
    // MARK: - Session Configuration
    
    /**
     * Data structure for session configuration from backend
     * Direct translation of Swift SessionConfig
     */
    private data class SessionConfig(
        val clientSecret: String,
        val instructions: String,
        val voice: String,
        val model: String,
        val modalities: List<String>
    )
    
    /**
     * Fetch session configuration from backend
     * Direct translation of Swift fetchSessionConfig
     */
    private suspend fun fetchSessionConfig(levelId: Int? = null, speed: Double = 1.0): SessionConfig = withContext(Dispatchers.IO) {
        println("🌐 [WebRTCManager] Fetching session config from: $backendURL/get-ephemeral-token")
        
        val url = "$backendURL/get-ephemeral-token"
        
        // Create request body with level ID and speed
        val requestBody = mutableMapOf<String, Any>()
        levelId?.let { 
            requestBody["code"] = it
            println("📤 [WebRTCManager] Sending level ID: $it")
        }
        
        // Always send speed parameter
        requestBody["speed"] = speed
        println("📤 [WebRTCManager] Sending speed: $speed")
        
        val jsonString = json.encodeToString(requestBody)
        val body = jsonString.toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()
        
        try {
            val response = httpClient.newCall(request).execute()
            
            println("📡 [WebRTCManager] Backend response status: ${response.code}")
            
            if (!response.isSuccessful) {
                val errorMessage = response.message
                throw IOException("Backend error (${response.code}): $errorMessage")
            }
            
            val responseBody = response.body?.string() ?: throw IOException("Empty response body")
            val jsonResponse = json.parseToJsonElement(responseBody).jsonObject
            
            println("📦 [WebRTCManager] Received session config: ${jsonResponse.keys.joinToString(", ")}")
            
            // Extract session configuration
            val clientSecretObj = jsonResponse["client_secret"]?.jsonObject
            val clientSecret = clientSecretObj?.get("value")?.jsonPrimitive?.content ?: ""
            val instructions = jsonResponse["instructions"]?.jsonPrimitive?.content ?: ""
            val voice = jsonResponse["voice"]?.jsonPrimitive?.content ?: "alloy"
            val model = jsonResponse["model"]?.jsonPrimitive?.content ?: "gpt-4o-mini-realtime-preview-2024-12-17"
            val modalities = jsonResponse["modalities"]?.jsonArray?.map { 
                it.jsonPrimitive.content 
            } ?: listOf("text", "audio")
            
            println("🔧 [WebRTCManager] Parsed session config:")
            println("   Client secret length: ${clientSecret.length} characters")
            println("   Instructions length: ${instructions.length} characters")
            println("   Voice: $voice")
            println("   Model: $model")
            println("   Modalities: $modalities")
            
            if (clientSecret.isEmpty()) {
                throw IOException("Empty client secret in response")
            }
            
            SessionConfig(
                clientSecret = clientSecret,
                instructions = instructions,
                voice = voice,
                model = model,
                modalities = modalities
            )
            
        } catch (e: UnknownHostException) {
            throw IOException("No internet connection")
        } catch (e: SocketTimeoutException) {
            throw IOException("Request timed out. Please check your backend server.")
        } catch (e: IOException) {
            if (e.message?.contains("Cannot connect") == true) {
                throw IOException("Cannot connect to backend server. Please check the URL.")
            }
            throw e
        } catch (e: Exception) {
            throw IOException("Failed to fetch session config: ${e.message}")
        }
    }
    
    // MARK: - Private Methods
    
    private fun setupPeerConnection() {
        println("🔧 [WebRTCManager] setupPeerConnection called")
        
        try {
            val config = PeerConnection.RTCConfiguration(emptyList()).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            }
            
            val observer = object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                    println("📡 [WebRTCManager] Signaling state changed: $state")
                }
                
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    println("🧊 [WebRTCManager] ICE Connection State changed to: $state")
                }
                
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidate(candidate: IceCandidate?) {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onAddStream(stream: MediaStream?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(dataChannel: DataChannel?) {
                    dataChannel?.registerObserver(object : DataChannel.Observer {
                        override fun onBufferedAmountChange(amount: Long) {}
                        override fun onStateChange() {
                            println("📡 [WebRTCManager] Server data channel state: ${dataChannel.state()}")
                        }
                        override fun onMessage(buffer: DataChannel.Buffer?) {
                            buffer?.data?.let { data ->
                                val bytes = ByteArray(data.remaining())
                                data.get(bytes)
                                val message = String(bytes, Charsets.UTF_8)
                                
                                viewModelScope.launch {
                                    handleIncomingJSON(message)
                                }
                            }
                        }
                    })
                }
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
            }
            
            peerConnection = peerConnectionFactory?.createPeerConnection(config, observer)
            
            if (peerConnection != null) {
                println("✅ [WebRTCManager] RTCPeerConnection created successfully")
            } else {
                throw Exception("Failed to create PeerConnection")
            }
            
        } catch (e: Exception) {
            println("❌ [WebRTCManager] Error setting up peer connection: ${e.message}")
            viewModelScope.launch {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                _errorMessage.value = "WebRTC setup failed: ${e.message}"
            }
        }
    }
    
    private fun configureAudioSession() {
        println("🎵 [WebRTCManager] configureAudioSession called")
        
        // TODO: Request microphone permission - this should be handled by the UI/Activity
        // For now, assume permission is granted
        println("✅ [WebRTCManager] Microphone permission assumed granted")
        
        setupAudioSessionWithPermission()
    }
    
    private fun setupAudioSessionWithPermission() {
        println("🔊 [WebRTCManager] setupAudioSessionWithPermission called")
        
        try {
            audioManager?.let { am ->
                // Configure audio for voice call
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                
                // Enable speaker phone for loud output
                am.isSpeakerphoneOn = true
                
                // Set stream volume to maximum for voice calls
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVolume, 0)
                
                println("✅ [WebRTCManager] Audio session configured successfully with speaker output")
                
                // Log current audio configuration
                println("🔊 [WebRTCManager] Audio configuration:")
                println("   Mode: ${am.mode}")
                println("   Speaker on: ${am.isSpeakerphoneOn}")
                println("   Voice call volume: ${am.getStreamVolume(AudioManager.STREAM_VOICE_CALL)}/$maxVolume")
            }
            
        } catch (e: Exception) {
            println("❌ [WebRTCManager] Failed to configure audio session: ${e.message}")
            
            // Try fallback configuration
            try {
                audioManager?.let { am ->
                    am.mode = AudioManager.MODE_NORMAL
                    am.isSpeakerphoneOn = true
                    println("✅ [WebRTCManager] Fallback audio session configured")
                }
            } catch (fallbackError: Exception) {
                println("❌ [WebRTCManager] Fallback audio session also failed: ${fallbackError.message}")
                viewModelScope.launch {
                    _errorMessage.value = "Audio setup failed: ${fallbackError.message}"
                }
            }
        }
    }
    
    private fun setupLocalAudio() {
        println("🎤 [WebRTCManager] setupLocalAudio called")
        
        val pc = peerConnection
        if (pc == null) {
            println("❌ [WebRTCManager] Cannot setup local audio - peerConnection is null")
            return
        }
        
        try {
            val factory = peerConnectionFactory ?: throw Exception("PeerConnectionFactory is null")
            
            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("googNoiseReduction", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            }
            
            val audioSource = factory.createAudioSource(constraints)
            val localAudioTrack = factory.createAudioTrack("local_audio", audioSource)
            
            pc.addTrack(localAudioTrack, listOf("local_stream"))
            audioTrack = localAudioTrack
            
            println("✅ [WebRTCManager] Local audio configured successfully")
            
        } catch (e: Exception) {
            println("❌ [WebRTCManager] Error setting up local audio: ${e.message}")
            viewModelScope.launch {
                _errorMessage.value = "Failed to setup audio: ${e.message}"
            }
        }
    }
    
    /**
     * Posts our SDP offer to the Realtime API, returns the answer SDP
     * Direct translation of Swift fetchRemoteSDP
     */
    private suspend fun fetchRemoteSDP(localSdp: String): String = withContext(Dispatchers.IO) {
        val baseUrl = "https://api.openai.com/v1/realtime"
        val url = "$baseUrl?model=$modelName"
        
        println("🌐 [WebRTCManager] Posting SDP to OpenAI Realtime API:")
        println("   URL: $url")
        println("   Model: $modelName")
        println("   API Key length: ${apiKey.length} characters")
        println("   SDP length: ${localSdp.length} characters")
        
        val body = localSdp.toRequestBody("application/sdp".toMediaType())
        
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/sdp")
            .addHeader("Authorization", "Bearer $apiKey")
            .build()
        
        val response = httpClient.newCall(request).execute()
        
        if (!response.isSuccessful) {
            val code = response.code
            val errorBody = response.body?.string() ?: ""
            
            println("❌ [WebRTCManager] OpenAI API Error Response: $errorBody")
            
            val errorMessage = "OpenAI API error ($code): $errorBody"
            throw IOException(errorMessage)
        }
        
        val answerSdp = response.body?.string() 
            ?: throw IOException("Unable to decode SDP")
        
        println("✅ [WebRTCManager] Received SDP answer from OpenAI")
        answerSdp
    }
    
    private suspend fun handleIncomingJSON(jsonString: String) {
        println("📨 [WebRTCManager] Received JSON:\n$jsonString\n")
        
        try {
            val eventDict = json.parseToJsonElement(jsonString).jsonObject
            val eventType = eventDict["type"]?.jsonPrimitive?.content ?: return
            
            _eventTypeStr.value = eventType
            
            when (eventType) {
                "conversation.item.created" -> {
                    val item = eventDict["item"]?.jsonObject
                    val itemId = item?.get("id")?.jsonPrimitive?.content
                    val role = item?.get("role")?.jsonPrimitive?.content
                    
                    if (itemId != null && role != null) {
                        // If item contains "content", extract the text
                        val content = item["content"]?.jsonArray
                        val text = content?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                        
                        val newItem = ConversationItem(id = itemId, role = role, text = text)
                        conversationMap[itemId] = newItem
                        
                        if (role == "assistant" || role == "user") {
                            val currentList = _conversation.value.toMutableList()
                            currentList.add(newItem)
                            _conversation.value = currentList
                        }
                    }
                }
                
                "response.audio_transcript.delta" -> {
                    // partial transcript for assistant's message
                    val itemId = eventDict["item_id"]?.jsonPrimitive?.content
                    val delta = eventDict["delta"]?.jsonPrimitive?.content
                    
                    if (itemId != null && delta != null) {
                        conversationMap[itemId]?.let { convItem ->
                            val updatedItem = convItem.copy(text = convItem.text + delta)
                            conversationMap[itemId] = updatedItem
                            
                            // Update in conversation list
                            val currentList = _conversation.value.toMutableList()
                            val index = currentList.indexOfFirst { it.id == itemId }
                            if (index >= 0) {
                                currentList[index] = updatedItem
                                _conversation.value = currentList
                            }
                        }
                    }
                }
                
                "response.audio_transcript.done" -> {
                    // final transcript for assistant's message
                    val itemId = eventDict["item_id"]?.jsonPrimitive?.content
                    val transcript = eventDict["transcript"]?.jsonPrimitive?.content
                    
                    if (itemId != null && transcript != null) {
                        conversationMap[itemId]?.let { convItem ->
                            val updatedItem = convItem.copy(text = transcript)
                            conversationMap[itemId] = updatedItem
                            
                            // Update in conversation list
                            val currentList = _conversation.value.toMutableList()
                            val index = currentList.indexOfFirst { it.id == itemId }
                            if (index >= 0) {
                                currentList[index] = updatedItem
                                _conversation.value = currentList
                            }
                        }
                    }
                }
                
                "conversation.item.input_audio_transcription.completed" -> {
                    // final transcript for user's audio input
                    val itemId = eventDict["item_id"]?.jsonPrimitive?.content
                    val transcript = eventDict["transcript"]?.jsonPrimitive?.content
                    
                    if (itemId != null && transcript != null) {
                        conversationMap[itemId]?.let { convItem ->
                            val updatedItem = convItem.copy(text = transcript)
                            conversationMap[itemId] = updatedItem
                            
                            // Update in conversation list
                            val currentList = _conversation.value.toMutableList()
                            val index = currentList.indexOfFirst { it.id == itemId }
                            if (index >= 0) {
                                currentList[index] = updatedItem
                                _conversation.value = currentList
                            }
                        }
                    }
                }
                
                "output_audio_buffer.started" -> {
                    // AI is speaking - temporarily mute microphone to prevent feedback
                    muteLocalAudio(true)
                }
                
                "output_audio_buffer.stopped" -> {
                    // AI finished speaking - safe to unmute with delay
                    viewModelScope.launch {
                        delay(100)
                        muteLocalAudio(false)
                    }
                }
            }
            
        } catch (e: Exception) {
            println("❌ [WebRTCManager] Error handling incoming JSON: ${e.message}")
        }
    }
    
    /**
     * Configure backend URL for ephemeral token requests
     * Direct translation of Swift setBackendURL
     */
    fun setBackendURL(url: String) {
        this.backendURL = url
        println("🌐 [WebRTCManager] Backend URL updated to: $url")
    }
    
    // MARK: - Audio Management for Feedback Prevention
    
    /**
     * Temporarily mute the microphone to prevent feedback during AI speech
     * Direct translation of Swift muteLocalAudio
     */
    private fun muteLocalAudio(mute: Boolean) {
        isTemporarilyMutedForFeedbackPrevention = mute
        updateAudioTrackState()
        
        if (mute) {
            println("🔇 [WebRTCManager] Microphone muted to prevent feedback")
        } else {
            println("🎤 [WebRTCManager] Microphone unmuted (feedback prevention off)")
        }
    }
    
    /**
     * Update the actual audio track state based on user mute and feedback prevention
     * Direct translation of Swift updateAudioTrackState
     */
    private fun updateAudioTrackState() {
        val shouldMute = _isMuted.value || isTemporarilyMutedForFeedbackPrevention
        audioTrack?.setEnabled(!shouldMute)
    }
    
    /**
     * Public method to toggle mute state for user control
     * Direct translation of Swift toggleMute
     */
    fun toggleMute() {
        viewModelScope.launch {
            _isMuted.value = !_isMuted.value
            updateAudioTrackState()
            // Ensure speaker output is maintained when toggling mute
            enforceSpeakerOutput()
            
            if (_isMuted.value) {
                println("🔇 [WebRTCManager] User muted microphone")
            } else {
                println("🎤 [WebRTCManager] User unmuted microphone")
            }
        }
    }
    
    /**
     * Public method to set mute state
     * Direct translation of Swift setMuted
     */
    fun setMuted(muted: Boolean) {
        viewModelScope.launch {
            _isMuted.value = muted
            updateAudioTrackState()
            // Ensure speaker output is maintained when setting mute state
            enforceSpeakerOutput()
            
            if (muted) {
                println("🔇 [WebRTCManager] Microphone muted")
            } else {
                println("🎤 [WebRTCManager] Microphone unmuted")
            }
        }
    }
    
    // MARK: - Audio Route Management
    
    /**
     * Enforces speaker output to ensure audio plays loudly through both speakers
     * Direct translation of Swift enforceSpeakerOutput
     */
    private fun enforceSpeakerOutput() {
        try {
            audioManager?.let { am ->
                // Force speaker phone on
                am.isSpeakerphoneOn = true
                
                // Set to communication mode for better voice handling
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                
                println("🔊 [WebRTCManager] Audio route enforced to speaker:")
                println("   Speaker phone: ${am.isSpeakerphoneOn}")
                println("   Audio mode: ${am.mode}")
            }
            
        } catch (e: Exception) {
            println("⚠️ [WebRTCManager] Failed to enforce speaker output: ${e.message}")
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopConnection()
        peerConnectionFactory?.dispose()
        println("🗑️ [WebRTCManager] ViewModel cleared and resources disposed")
    }
}
