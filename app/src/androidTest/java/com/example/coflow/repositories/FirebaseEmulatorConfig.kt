package com.example.coflow.repositories

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings

/**
 * Подключает Firebase Auth и Firestore к эмуляторам, которые запускает
 * `firebase emulators:exec` в CI.
 *
 * `10.0.2.2` — адрес хоста (машины с эмуляторами) с точки зрения Android-эмулятора.
 * Настройки Firestore можно менять только до первого запроса, поэтому [connect]
 * вызывается один раз на процесс инструментальных тестов.
 */
object FirebaseEmulatorConfig {
    private const val EMULATOR_HOST = "10.0.2.2"
    private const val FIRESTORE_PORT = 8080
    private const val AUTH_PORT = 9099

    private var connected = false

    fun connect() {
        if (connected) return
        connected = true

        val firestore = Firebase.firestore
        // Сначала настройки кеша, потом useEmulator: useEmulator дописывает
        // адрес хоста в текущие настройки, поэтому обратный порядок сбросил бы адрес.
        firestore.firestoreSettings =
            firestoreSettings {
                setLocalCacheSettings(memoryCacheSettings {})
            }
        firestore.useEmulator(EMULATOR_HOST, FIRESTORE_PORT)

        FirebaseAuth.getInstance().useEmulator(EMULATOR_HOST, AUTH_PORT)
    }
}
