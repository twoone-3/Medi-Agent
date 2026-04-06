package com.project.medi_agent.data

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import java.util.Locale

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    @Volatile
    private var isTtsInitialized = false
    private var speechRecognizer: SpeechRecognizer? = null
    @Volatile
    private var isMuted: Boolean = false
    private var speechRate: Float = 1.0f
    private var speechPitch: Float = 1.0f

    init {
        // 使用 ApplicationContext 防止内存泄漏，并确保生命周期更稳健
        tts = TextToSpeech(context.applicationContext, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d("VoiceManager", "TTS 引擎初始化成功，正在设置语言...")
            
            // 尝试设置语言，如果系统本身就是中文，Locale.getDefault() 在国产机上最稳
            val result = tts?.setLanguage(Locale.CHINESE)
            
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("VoiceManager", "Locale.CHINESE 不支持，尝试默认语言")
                tts?.language = Locale.getDefault()
            }
            
            isTtsInitialized = true
            Log.d("VoiceManager", "TTS 准备就绪")
        } else {
            isTtsInitialized = false
            Log.e("VoiceManager", "TTS 初始化失败: $status")
        }
    }

    fun speak(text: String, onComplete: () -> Unit = {}) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            onComplete()
            return
        }

        // respect manual mute
        if (isMuted) {
            onComplete()
            return
        }

        if (isTtsInitialized && tts != null) {
            Log.d("VoiceManager", "正在播报: ${cleanText.take(10)}...")
            
            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MediAgentMsg")
            
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d("VoiceManager", "播放开始")
                }
                override fun onDone(utteranceId: String?) {
                    Log.d("VoiceManager", "播放完成")
                    onComplete()
                }
                override fun onError(utteranceId: String?) {
                    Log.e("VoiceManager", "播放出错")
                    onComplete()
                }
            })
            
            tts?.setSpeechRate(speechRate)
            tts?.setPitch(speechPitch)
            val result = tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "MediAgentMsg")
            if (result == TextToSpeech.ERROR) {
                Log.e("VoiceManager", "调用 speak 方法直接返回错误")
                onComplete()
            }
        } else {
            Log.e("VoiceManager", "播报失败：引擎未就绪 (isTtsInitialized=$isTtsInitialized)")
            Toast.makeText(context, "语音引擎还没准备好，请稍等片刻", Toast.LENGTH_SHORT).show()
            onComplete()
        }
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
        if (muted) stopSpeaking()
    }

    fun isMuted(): Boolean = isMuted

    fun setRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(speechRate)
    }

    fun setPitch(pitch: Float) {
        speechPitch = pitch.coerceIn(0.5f, 2.0f)
        tts?.setPitch(speechPitch)
    }

    fun getRate(): Float = speechRate

    fun getPitch(): Float = speechPitch

    // --- STT (语音转文字) ---
    fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit, onFinished: () -> Unit) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINESE.toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { Log.d("VoiceManager", "STT 就绪") }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                if (error != 5) Log.e("VoiceManager", "STT 错误: $error")
                onFinished()
            }
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onResult)
                onFinished()
            }
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onResult)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
