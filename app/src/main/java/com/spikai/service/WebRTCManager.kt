package com.spikai.service

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spikai.model.ConnectionStatus
import com.spikai.model.ConversationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.webrtc.*
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

/**
 * WebRTCManager - Real-time AI conversation using WebRTC with OpenAI Realtime API
 * Complete makeover based on working reference implementation
 */
class WebRTCManager(private val context: Context) : ViewModel() {
    
    // ==================================================
    // MARK: - STATE MANAGEMENT (Business Logic - Preserved)
    // ==================================================
    
    // UI State
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    private val _eventTypeStr = MutableStateFlow("")
    val eventTypeStr: StateFlow<String> = _eventTypeStr.asStateFlow()
    
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()
    
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()
    
    // Conversation Management
    private val _conversation = MutableStateFlow<List<ConversationItem>>(emptyList())
    val conversation: StateFlow<List<ConversationItem>> = _conversation.asStateFlow()
    
    private val _outgoingMessage = MutableStateFlow("")
    val outgoingMessage: StateFlow<String> = _outgoingMessage.asStateFlow()
    
    private val conversationMap: MutableMap<String, ConversationItem> = mutableMapOf()
    
    // Session Configuration (Business Logic)
    var apiKey: String = ""
    var modelName: String = "gpt-4o-mini-realtime-preview-2024-12-17"
    var systemInstructions: String = ""
    var voice: String = "alloy"
    private var backendURL: String = NetworkConfig.getBackendURL(context)
    
    // ==================================================
    // MARK: - WEBRTC CORE (New Implementation)
    // ==================================================
    
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null
    private var localAudioTrack: AudioTrack? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var audioManager: AudioManager? = null
    
    // ==================================================
    // MARK: - UTILITIES
    // ==================================================
    
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private var connectionTimeoutJob: Job? = null
    private var isTemporarilyMutedForFeedbackPrevention = false
    
    // ==================================================
    // MARK: - INITIALIZATION
    // ==================================================
    
    init {
        println("🎯 [WebRTCManager] Service initialized")
        println("🌐 [WebRTCManager] Backend URL: $backendURL")
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        initializeWebRTC()
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
    
    private fun initializeWebRTC() {
        println("🔧 [WebRTCManager] Initializing WebRTC...")
        try {
            val initOptions = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(initOptions)
            
            val options = PeerConnectionFactory.Options()
            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(createAudioDeviceModule())
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
        println("🎙️ [WebRTCManager] Creating audio device module for OpenAI")
        return JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(false)
            .setUseHardwareNoiseSuppressor(false)
            .setAudioRecordErrorCallback(object : JavaAudioDeviceModule.AudioRecordErrorCallback {
                override fun onWebRtcAudioRecordInitError(errorMessage: String) {
                    println("❌ [Audio] Record init error: $errorMessage")
                }
                override fun onWebRtcAudioRecordStartError(
                    errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode,
                    errorMessage: String
                ) {
                    println("❌ [Audio] Record start error: $errorMessage")
                }
                override fun onWebRtcAudioRecordError(errorMessage: String) {
                    println("❌ [Audio] Record error: $errorMessage")
                }
            })
            .setAudioTrackErrorCallback(object : JavaAudioDeviceModule.AudioTrackErrorCallback {
                override fun onWebRtcAudioTrackInitError(errorMessage: String) {
                    println("❌ [Audio] Track init error: $errorMessage")
                }
                override fun onWebRtcAudioTrackStartError(
                    errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode,
                    errorMessage: String
                ) {
                    println("❌ [Audio] Track start error: $errorMessage")
                }
                override fun onWebRtcAudioTrackError(errorMessage: String) {
                    println("❌ [Audio] Track error: $errorMessage")
                }
            })
            .createAudioDeviceModule()
    }
    
    // ==================================================
    // MARK: - PUBLIC API (Business Logic Entry Points)
    // ==================================================
    
    /**
     * Main entry point - Fetch session config from backend and start connection
     */
    fun fetchSessionConfigAndStartConnection(levelId: Int? = null, speed: Double = 1.0) {
        println("🚀 [WebRTCManager] Starting session - levelId: $levelId, speed: $speed")
        
        viewModelScope.launch {
            _connectionStatus.value = ConnectionStatus.CONNECTING
            
            connectionTimeoutJob?.cancel()
            connectionTimeoutJob = viewModelScope.launch {
                delay(30000)
                if (_connectionStatus.value == ConnectionStatus.CONNECTING) {
                    println("⏰ [WebRTCManager] Connection timeout")
                    _connectionStatus.value = ConnectionStatus.FAILED
                    _errorMessage.value = "Connection timeout. Please try again."
                    stopConnection()
                }
            }
            
            try {
                // Fetch ephemeral token from backend
                val ephemeralToken = getEphemeralToken(levelId, speed)
                
                println("✅ [WebRTCManager] Got ephemeral token - length: ${ephemeralToken.length}")
                
                // Clear conversation
                _conversation.value = emptyList()
                conversationMap.clear()
                _errorMessage.value = ""
                
                // Start WebRTC session
                startSession(ephemeralToken, modelName, voice)
                
            } catch (e: Exception) {
                println("❌ [WebRTCManager] Failed to start session: ${e.message}")
                _connectionStatus.value = ConnectionStatus.FAILED
                _errorMessage.value = "Failed to start session: ${e.message}"
            }
        }
    }
    
    fun stopConnection() {
        println("🛑 [WebRTCManager] Stopping connection")
        connectionTimeoutJob?.cancel()
        localAudioTrack?.dispose()
        dataChannel?.dispose()
        peerConnection?.dispose()
        
        localAudioTrack = null
        dataChannel = null
        peerConnection = null
        
        audioManager?.mode = AudioManager.MODE_NORMAL
        
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        println("✅ [WebRTCManager] Connection stopped")
    }
    
    fun setBackendURL(url: String) {
        this.backendURL = url
        println("🌐 [WebRTCManager] Backend URL updated to: $url")
    }
    
    fun toggleMute() {
        viewModelScope.launch {
            _isMuted.value = !_isMuted.value
            updateAudioTrackState()
            if (_isMuted.value) {
                println("🔇 [WebRTCManager] User muted microphone")
            } else {
                println("🎤 [WebRTCManager] User unmuted microphone")
            }
        }
    }
    
    fun setMuted(muted: Boolean) {
        viewModelScope.launch {
            _isMuted.value = muted
            updateAudioTrackState()
        }
    }
    
    fun setOutgoingMessage(message: String) {
        _outgoingMessage.value = message
    }
    
    fun sendMessage() {
        val message = _outgoingMessage.value
        if (message.isEmpty()) return
        
        println("📤 [WebRTCManager] Sending text message: $message")
        // Implementation for sending text messages via data channel
        _outgoingMessage.value = ""
    }
    
    fun createResponse() {
        println("🎬 [WebRTCManager] Creating response")
        val event = mapOf("type" to "response.create")
        sendEvent(event)
    }
    
    // ==================================================
    // MARK: - BACKEND INTEGRATION (Business Logic)
    // ==================================================
    
    private suspend fun getEphemeralToken(levelId: Int?, speed: Double): String = withContext(Dispatchers.IO) {
        println("🌐 [WebRTCManager] Fetching ephemeral token from: $backendURL/get-ephemeral-token")
        
        val url = "$backendURL/get-ephemeral-token"
        val jsonBody = buildString {
            append("{")
            val parts = mutableListOf<String>()
            levelId?.let { 
                parts.add("\"code\":$it")
                println("📤 [WebRTCManager] Sending level ID: $it")
            }
            parts.add("\"speed\":$speed")
            println("📤 [WebRTCManager] Sending speed: $speed")
            append(parts.joinToString(","))
            append("}")
        }
        
        val body = jsonBody.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()
        
        try {
            val response = httpClient.newCall(request).execute()
            
            println("📡 [WebRTCManager] Backend response status: ${response.code}")
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                println("❌ [WebRTCManager] Backend error response: $errorBody")
                throw IOException("Backend error (${response.code}): ${response.message}")
            }
            
            val responseBody = response.body?.string() ?: throw IOException("Empty response")
            val jsonResponse = json.parseToJsonElement(responseBody).jsonObject
            
            println("📦 [WebRTCManager] Received session config: ${jsonResponse.keys.joinToString(", ")}")
            
            val clientSecretObj = jsonResponse["client_secret"]?.jsonObject
            val clientSecret = clientSecretObj?.get("value")?.jsonPrimitive?.content ?: ""
            val instructions = jsonResponse["instructions"]?.jsonPrimitive?.content ?: ""
            val voiceValue = jsonResponse["voice"]?.jsonPrimitive?.content ?: "alloy"
            val model = jsonResponse["model"]?.jsonPrimitive?.content ?: "gpt-4o-mini-realtime-preview-2024-12-17"
            
            // Update configuration
            apiKey = clientSecret
            systemInstructions = instructions
            voice = voiceValue
            modelName = model
            
            println("🔧 [WebRTCManager] Parsed session config:")
            println("   Client secret length: ${clientSecret.length} characters")
            println("   Instructions length: ${instructions.length} characters")
            println("   Voice: $voice")
            println("   Model: $model")
            
            if (clientSecret.isEmpty()) {
                throw IOException("Empty ephemeral token")
            }
            
            clientSecret
            
        } catch (e: UnknownHostException) {
            throw IOException("No internet connection")
        } catch (e: SocketTimeoutException) {
            throw IOException("Request timed out")
        } catch (e: Exception) {
            throw IOException("Failed to fetch token: ${e.message}")
        }
    }
    
    // ==================================================
    // MARK: - WEBRTC SESSION (New Implementation)
    // ==================================================
    
    private suspend fun startSession(ephemeralToken: String, model: String, voice: String) {
        withContext(Dispatchers.Main) {
            try {
                // Step 1: Create peer connection
                createPeerConnection()
                
                // Step 2: Setup audio tracks
                setupAudioTracks()
                
                // Step 3: Setup data channel BEFORE creating offer
                setupDataChannel()
                
                // Step 4: Configure audio session
                configureAudioSession()
                
                // Step 5: Create and send offer (will include data channel)
                createAndSendOffer(ephemeralToken, model)
                
            } catch (e: Exception) {
                println("❌ [WebRTCManager] Failed to start session: ${e.message}")
                _connectionStatus.value = ConnectionStatus.FAILED
                _errorMessage.value = "Failed to start session: ${e.message}"
            }
        }
    }
    
    private fun createPeerConnection() {
        println("🔧 [WebRTCManager] Creating peer connection")
        
        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
                    .createIceServer()
            )
        ).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        
        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                    println("📡 [WebRTCManager] Signaling state: $state")
                }
                
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    println("🧊 [WebRTCManager] ICE connection state: $state")
                    when (state) {
                        PeerConnection.IceConnectionState.CONNECTED -> {
                            println("✅ [WebRTCManager] ICE Connected")
                        }
                        PeerConnection.IceConnectionState.FAILED -> {
                            println("❌ [WebRTCManager] ICE Connection Failed")
                            viewModelScope.launch {
                                _connectionStatus.value = ConnectionStatus.FAILED
                                _errorMessage.value = "ICE connection failed"
                            }
                        }
                        else -> {}
                    }
                }
                
                override fun onIceConnectionReceivingChange(receiving: Boolean) {
                    println("🧊 [WebRTCManager] ICE receiving: $receiving")
                }
                
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                    println("🧊 [WebRTCManager] ICE gathering: $state")
                }
                
                override fun onIceCandidate(candidate: IceCandidate?) {
                    println("🧊 [WebRTCManager] ICE candidate: ${candidate?.sdp}")
                }
                
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                
                override fun onAddStream(stream: MediaStream?) {
                    println("🎵 [WebRTCManager] Remote stream added")
                    stream?.audioTracks?.firstOrNull()?.let { audioTrack ->
                        handleRemoteAudioTrack(audioTrack)
                    }
                }
                
                override fun onRemoveStream(stream: MediaStream?) {
                    println("🎵 [WebRTCManager] Remote stream removed")
                }
                
                override fun onDataChannel(dc: DataChannel?) {
                    println("📡 [WebRTCManager] Data channel received: ${dc?.label()}")
                }
                
                override fun onRenegotiationNeeded() {
                    println("🔄 [WebRTCManager] Renegotiation needed")
                }
                
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                    println("🎵 [WebRTCManager] Track added")
                }
            }
        )
        
        if (peerConnection == null) {
            throw Exception("Failed to create PeerConnection")
        }
        
        println("✅ [WebRTCManager] Peer connection created")
    }
    
    private fun setupAudioTracks() {
        println("🎤 [WebRTCManager] Setting up audio tracks")
        
        val factory = peerConnectionFactory ?: throw Exception("PeerConnectionFactory is null")
        val pc = peerConnection ?: throw Exception("PeerConnection is null")
        
        // Create audio constraints - Reduced sensitivity for less noise pickup
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "false"))  // Desactivado para reducir sensibilidad
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googTypingNoiseDetection", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAudioMirroring", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseReduction", "true"))
            
            optional.add(MediaConstraints.KeyValuePair("googEchoCancellation2", "true"))
        }
        
        val audioSource = factory.createAudioSource(audioConstraints)
        localAudioTrack = factory.createAudioTrack("local_audio", audioSource)
        localAudioTrack?.setEnabled(true)
        
        val sender = pc.addTrack(localAudioTrack, listOf("local_stream"))
        
        println("✅ [WebRTCManager] Audio track added with reduced sensitivity:")
        println("   Track ID: ${localAudioTrack?.id()}")
        println("   Track enabled: ${localAudioTrack?.enabled()}")
        println("   Sender ID: ${sender?.id()}")
        println("   Echo cancellation: enabled")
        println("   Noise suppression: enabled")
        println("   Auto gain control: DISABLED (reduced sensitivity)")
        println("   Total senders in PC: ${pc.senders.size}")
        println("   Total receivers in PC: ${pc.receivers.size}")
    }
    
    private fun configureAudioSession() {
        println("🔊 [WebRTCManager] Configuring audio session")
        
        try {
            audioManager?.let { am ->
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                am.isSpeakerphoneOn = true
                
                // Reducir volumen del micrófono para menos sensibilidad
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                val reducedVolume = (maxVolume * 0.7).toInt()  // 70% del volumen máximo
                am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, reducedVolume, 0)
                
                println("✅ [WebRTCManager] Audio session configured with reduced microphone sensitivity")
                println("   Max volume: $maxVolume, Set to: $reducedVolume (70%)")
            }
        } catch (e: Exception) {
            println("⚠️ [WebRTCManager] Failed to configure audio: ${e.message}")
        }
    }
    
    private fun createAndSendOffer(ephemeralToken: String, model: String) {
        println("📝 [WebRTCManager] Creating SDP offer")
        
        val mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
        
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let { offer ->
                    println("✅ [WebRTCManager] SDP offer created")
                    println("📝 [SDP OFFER] Full SDP:\n${offer.description}")
                    setLocalDescriptionAndSendOffer(offer, ephemeralToken, model)
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
        }, mediaConstraints)
    }
    
    private fun setLocalDescriptionAndSendOffer(
        offer: SessionDescription,
        ephemeralToken: String,
        model: String
    ) {
        peerConnection?.setLocalDescription(object : SdpObserver {
            override fun onSetSuccess() {
                println("✅ [WebRTCManager] Local description set")
                viewModelScope.launch {
                    sendOfferToOpenAI(offer, ephemeralToken, model)
                }
            }
            
            override fun onSetFailure(error: String?) {
                println("❌ [WebRTCManager] Failed to set local description: $error")
                viewModelScope.launch {
                    _connectionStatus.value = ConnectionStatus.FAILED
                    _errorMessage.value = "Failed to set local description: $error"
                }
            }
            
            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
        }, offer)
    }
    
    private suspend fun sendOfferToOpenAI(
        offer: SessionDescription,
        ephemeralToken: String,
        model: String
    ) {
        withContext(Dispatchers.IO) {
            val baseUrl = "https://api.openai.com/v1/realtime"
            val url = "$baseUrl?model=$model"
            
            println("🌐 [WebRTCManager] Sending offer to OpenAI")
            println("📝 [SDP] Offer length: ${offer.description.length}")
            
            val request = Request.Builder()
                .url(url)
                .post(offer.description.toRequestBody("application/sdp".toMediaType()))
                .addHeader("Authorization", "Bearer $ephemeralToken")
                .addHeader("Content-Type", "application/sdp")
                .build()
            
            try {
                val response = httpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val answerSdp = response.body?.string()
                    answerSdp?.let { sdp ->
                        println("✅ [WebRTCManager] Received SDP answer")
                        println("📝 [SDP ANSWER] Full SDP:\n$sdp")
                        
                        val answer = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                        setRemoteDescription(answer)
                    }
                } else {
                    val errorBody = response.body?.string() ?: ""
                    println("❌ [WebRTCManager] OpenAI rejected offer: ${response.code}")
                    println("❌ [Error] $errorBody")
                    
                    withContext(Dispatchers.Main) {
                        _connectionStatus.value = ConnectionStatus.FAILED
                        _errorMessage.value = "OpenAI API error: ${response.code}"
                    }
                }
            } catch (e: Exception) {
                println("❌ [WebRTCManager] Failed to send offer: ${e.message}")
                withContext(Dispatchers.Main) {
                    _connectionStatus.value = ConnectionStatus.FAILED
                    _errorMessage.value = "Failed to send offer: ${e.message}"
                }
            }
        }
    }
    
    private fun setRemoteDescription(answer: SessionDescription) {
        viewModelScope.launch(Dispatchers.Main) {
            peerConnection?.setRemoteDescription(object : SdpObserver {
                override fun onSetSuccess() {
                    println("✅ [WebRTCManager] Remote description set - Connection established!")
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    connectionTimeoutJob?.cancel()
                    
                    // Data channel was already created before offer, should be open now
                    sendSessionUpdate()
                }
                
                override fun onSetFailure(error: String?) {
                    println("❌ [WebRTCManager] Failed to set remote description: $error")
                    _connectionStatus.value = ConnectionStatus.FAILED
                    _errorMessage.value = "Failed to set remote description: $error"
                }
                
                override fun onCreateSuccess(sdp: SessionDescription?) {}
                override fun onCreateFailure(error: String?) {}
            }, answer)
        }
    }
    
    private fun setupDataChannel() {
        println("📡 [WebRTCManager] Setting up data channel")
        
        val dataChannelInit = DataChannel.Init().apply {
            ordered = true
        }
        
        dataChannel = peerConnection?.createDataChannel("oai-events", dataChannelInit)
        
        dataChannel?.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(amount: Long) {}
            
            override fun onStateChange() {
                val state = dataChannel?.state()
                println("📡 [WebRTCManager] Data channel state: $state")
                
                if (state == DataChannel.State.OPEN) {
                    onDataChannelOpen()
                }
            }
            
            override fun onMessage(buffer: DataChannel.Buffer?) {
                buffer?.let { handleDataChannelMessage(it) }
            }
        })
    }
    
    private fun onDataChannelOpen() {
        println("✅ [WebRTCManager] Data channel opened")
        
        // Send session configuration
        sendSessionUpdate()
        
        // Trigger an initial AI greeting to test audio output
        viewModelScope.launch {
            delay(500) // Wait for session update to complete
            println("🎬 [WebRTCManager] Requesting initial AI greeting")
            createResponse()
        }
    }
    
    fun sendSessionUpdate() {
        println("📤 [WebRTCManager] Sending session update")
        
        val dc = dataChannel
        if (dc == null || dc.state() != DataChannel.State.OPEN) {
            println("⚠️ [WebRTCManager] Data channel not ready")
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
                "turn_detection" to mapOf(
                    "type" to "server_vad",
                    "threshold" to 0.7,  // Aumentado de 0.5 a 0.7 para reducir sensibilidad
                    "prefix_padding_ms" to 200,  // Reducido de 300 a 200ms
                    "silence_duration_ms" to 1500,  // Reducido de 2100 a 1500ms para respuesta más rápida
                    "create_response" to true
                ),
                "input_audio_transcription" to mapOf(
                    "model" to "whisper-1",
                    "language" to "en"
                ),
                "max_response_output_tokens" to "inf"
            )
        )
        
        sendEvent(sessionUpdate)
        println("✅ [WebRTCManager] Session update sent with server_vad configuration")
    }
    
    private fun sendEvent(event: Map<String, Any>) {
        try {
            // Convert map to JSON string manually using kotlinx.serialization's JSON builder
            val jsonObject = buildJsonObject {
                event.forEach { (key, value) ->
                    put(key, convertToJsonElement(value))
                }
            }
            val eventJson = jsonObject.toString()
            
            println("📤 [WebRTCManager] Sending event: ${event["type"]}")
            println("📦 [JSON] $eventJson")
            
            val buffer = DataChannel.Buffer(
                ByteBuffer.wrap(eventJson.toByteArray()),
                false
            )
            
            val sent = dataChannel?.send(buffer) ?: false
            if (sent) {
                println("✅ [WebRTCManager] Event sent successfully")
            } else {
                println("❌ [WebRTCManager] Failed to send event - buffer full or channel closed")
            }
        } catch (e: Exception) {
            println("❌ [WebRTCManager] Failed to send event: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun convertToJsonElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> {
                buildJsonObject {
                    @Suppress("UNCHECKED_CAST")
                    (value as Map<String, Any?>).forEach { (k, v) ->
                        put(k, convertToJsonElement(v))
                    }
                }
            }
            is List<*> -> {
                buildJsonArray {
                    value.forEach { add(convertToJsonElement(it)) }
                }
            }
            else -> JsonPrimitive(value.toString())
        }
    }
    
    private fun handleDataChannelMessage(buffer: DataChannel.Buffer) {
        val data = ByteArray(buffer.data.remaining())
        buffer.data.get(data)
        val message = String(data, Charsets.UTF_8)
        
        println("📨 [WebRTCManager] Received data channel message - length: ${message.length}")
        println("📨 [Raw] ${message.take(200)}${if (message.length > 200) "..." else ""}")
        
        viewModelScope.launch {
            handleIncomingJSON(message)
        }
    }
    
    private fun handleRemoteAudioTrack(audioTrack: AudioTrack) {
        println("🎵 [WebRTCManager] Handling remote audio track")
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager?.isSpeakerphoneOn = true
    }
    
    // ==================================================
    // MARK: - EVENT HANDLING (Business Logic - Preserved)
    // ==================================================
    
    private suspend fun handleIncomingJSON(jsonString: String) {
        try {
            val eventDict = json.parseToJsonElement(jsonString).jsonObject
            val eventType = eventDict["type"]?.jsonPrimitive?.content ?: return
            
            println("🔔 [Event] Type: $eventType")
            _eventTypeStr.value = eventType
            
            when (eventType) {
                "session.created" -> {
                    println("✅ [Event] Session created")
                }
                "session.updated" -> {
                    println("✅ [Event] Session updated")
                }
            "conversation.item.created" -> {
                val item = eventDict["item"]?.jsonObject
                val itemId = item?.get("id")?.jsonPrimitive?.content
                val role = item?.get("role")?.jsonPrimitive?.content
                
                if (itemId != null && role != null) {
                    val content = item["content"]?.jsonArray
                    val text = content?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                    
                    val newItem = ConversationItem(id = itemId, role = role, text = text)
                    conversationMap[itemId] = newItem
                    
                    if (role == "assistant" || role == "user") {
                        updateConversationList()
                    }
                }
            }
            "response.audio_transcript.delta" -> {
                val itemId = eventDict["item_id"]?.jsonPrimitive?.content
                val delta = eventDict["delta"]?.jsonPrimitive?.content
                
                if (itemId != null && delta != null) {
                    conversationMap[itemId]?.let { convItem ->
                        conversationMap[itemId] = convItem.copy(text = convItem.text + delta)
                        updateConversationList()
                    }
                }
            }
            "response.audio_transcript.done" -> {
                val itemId = eventDict["item_id"]?.jsonPrimitive?.content
                val transcript = eventDict["transcript"]?.jsonPrimitive?.content
                
                if (itemId != null && transcript != null) {
                    conversationMap[itemId]?.let { convItem ->
                        conversationMap[itemId] = convItem.copy(text = transcript)
                        updateConversationList()
                    }
                }
            }
            "conversation.item.input_audio_transcription.completed" -> {
                val itemId = eventDict["item_id"]?.jsonPrimitive?.content
                val transcript = eventDict["transcript"]?.jsonPrimitive?.content
                
                if (itemId != null && transcript != null) {
                    conversationMap[itemId]?.let { convItem ->
                        conversationMap[itemId] = convItem.copy(text = transcript)
                        updateConversationList()
                    }
                }
            }
            "input_audio_buffer.speech_started" -> {
                println("🟢 [Event] User started speaking")
            }
            "input_audio_buffer.speech_stopped" -> {
                println("🔴 [Event] User stopped speaking")
            }
            "input_audio_buffer.committed" -> {
                println("✅ [Event] User audio buffer committed")
            }
            "output_audio_buffer.started" -> {
                println("🔇 [WebRTCManager] AI speaking - muting mic")
                muteLocalAudio(true)
            }
            "output_audio_buffer.stopped" -> {
                println("🔊 [WebRTCManager] AI stopped - unmuting mic")
                viewModelScope.launch {
                    delay(200)
                    muteLocalAudio(false)
                }
            }
            "error" -> {
                println("❌ [Event] Error: $jsonString")
            }
            else -> {
                println("⚠️ [Event] Unhandled event type: $eventType")
                println("⚠️ [Event] Full JSON: $jsonString")
            }
        }
        } catch (e: Exception) {
            println("❌ [Event] Failed to parse event: ${e.message}")
            println("❌ [Event] Raw data: $jsonString")
        }
    }
    
    private fun updateConversationList() {
        _conversation.value = conversationMap.values
            .filter { it.role == "assistant" || it.role == "user" }
            .sortedBy { it.id }
    }
    
    private fun muteLocalAudio(mute: Boolean) {
        isTemporarilyMutedForFeedbackPrevention = mute
        updateAudioTrackState()
    }
    
    private fun updateAudioTrackState() {
        val shouldMute = _isMuted.value || isTemporarilyMutedForFeedbackPrevention
        localAudioTrack?.setEnabled(!shouldMute)
    }
    
    // ==================================================
    // MARK: - CLEANUP
    // ==================================================
    
    override fun onCleared() {
        super.onCleared()
        println("🧹 [WebRTCManager] Cleaning up")
        stopConnection()
    }
}
