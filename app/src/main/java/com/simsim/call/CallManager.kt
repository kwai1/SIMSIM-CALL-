package com.simsim.call

import android.content.Context
import io.livekit.android.LiveKit
import io.livekit.android.e2ee.BaseKeyProvider
import io.livekit.android.e2ee.E2EEOptions
import io.livekit.android.room.Room
import io.livekit.android.RoomOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Audio-only LiveKit room wrapper. The media encryption key is supplied by the
 * caller through the app's separately encrypted call document; it is never sent
 * to the token service.
 */
class CallManager(context: Context) {
    interface Callback {
        fun onSuccess()
        fun onError(message: String)
    }

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var room: Room? = null

    fun connect(serverUrl: String, token: String, sharedKey: String, callback: Callback) {
        scope.launch {
            try {
                disconnectCurrent()
                require(serverUrl.startsWith("wss://") || serverUrl.startsWith("ws://")) {
                    "عنوان LiveKit غير صالح"
                }
                require(token.isNotBlank() && sharedKey.isNotBlank()) {
                    "بيانات الاتصال المشفر ناقصة"
                }

                val keyProvider = BaseKeyProvider()
                check(keyProvider.setSharedKey(sharedKey)) {
                    "تعذر تهيئة مفتاح تشفير المكالمة"
                }
                val roomOptions = RoomOptions(
                    e2eeOptions = E2EEOptions(keyProvider = keyProvider)
                )
                val newRoom = LiveKit.create(appContext, roomOptions)
                room = newRoom
                newRoom.connect(serverUrl, token)
                newRoom.localParticipant.setMicrophoneEnabled(true)
                callback.onSuccess()
            } catch (error: Exception) {
                disconnectCurrent()
                callback.onError(error.message ?: "تعذر بدء المكالمة")
            }
        }
    }

    fun disconnect(callback: Callback? = null) {
        scope.launch {
            try {
                disconnectCurrent()
                callback?.onSuccess()
            } catch (error: Exception) {
                callback?.onError(error.message ?: "تعذر إنهاء المكالمة")
            }
        }
    }

    private fun disconnectCurrent() {
        room?.disconnect()
        room = null
    }

    fun release() {
        disconnectCurrent()
        scope.cancel()
    }
}
