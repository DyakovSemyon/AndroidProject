import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    id("com.google.gms.google-services")
}

// ---------------------------------------------------------------------------
// Секреты (Doppler) → файлы
//
// В репозитории НЕТ google-services.json и keystore. Они материализуются из
// переменных окружения, которые кладёт Doppler:
//   локально:         doppler run -- ./gradlew ...   (или ./scripts/sync-secrets.sh)
//   GitHub Actions:   dopplerhq/cli-action + DOPPLER_TOKEN
//
// Правило: любое значение секрета — single-line. Файлы передаём как base64.
// ---------------------------------------------------------------------------
// Типизированный таск обязателен: замыкание `doLast` в build-скрипте держит ссылку
// на script-объект, а это несовместимо с configuration cache (а он тут включён).
abstract class MaterializeSecretsTask : DefaultTask() {
    @get:Input @get:Optional abstract val googleServicesJson: Property<String>
    @get:Input @get:Optional abstract val googleServicesJsonB64: Property<String>
    @get:Input @get:Optional abstract val keystoreB64: Property<String>
    @get:Internal abstract val googleServicesJsonFile: RegularFileProperty
    @get:Internal abstract val keystoreFile: RegularFileProperty

    @TaskAction
    fun materialize() {
        val gsj = googleServicesJsonFile.get().asFile
        when {
            googleServicesJson.isPresent ->
                gsj.writeText(googleServicesJson.get())
            googleServicesJsonB64.isPresent ->
                gsj.writeBytes(Base64.getMimeDecoder().decode(googleServicesJsonB64.get()))
            gsj.exists() ->
                logger.lifecycle("google-services.json уже на месте — пропускаю")
            else -> throw GradleException(
                """
                |google-services.json отсутствует, а переменные Doppler не заданы.
                |
                |Локально:
                |  brew install dopplerhq/cli/doppler && doppler login && doppler setup
                |  doppler run -- ./gradlew assembleDebug
                |  либо: ./scripts/sync-secrets.sh   (создаст файл для Android Studio)
                |
                |CI:
                |  добавь DOPPLER_TOKEN в GitHub -> Settings -> Secrets and variables -> Actions
                """.trimMargin()
            )
        }

        if (keystoreB64.isPresent) {
            keystoreFile.get().asFile.writeBytes(Base64.getMimeDecoder().decode(keystoreB64.get()))
        }
    }
}

val releaseKeystoreFile = layout.projectDirectory.file(
    providers.environmentVariable("KEYSTORE_PATH").getOrElse("release.keystore")
)

val materializeSecrets = tasks.register<MaterializeSecretsTask>("materializeSecrets") {
    group = "secrets"
    description = "Создаёт app/google-services.json (и release.keystore) из переменных окружения Doppler."

    googleServicesJson.set(providers.environmentVariable("GOOGLE_SERVICES_JSON"))
    googleServicesJsonB64.set(providers.environmentVariable("GOOGLE_SERVICES_JSON_B64"))
    keystoreB64.set(providers.environmentVariable("KEYSTORE_B64"))
    googleServicesJsonFile.set(layout.projectDirectory.file("google-services.json"))
    keystoreFile.set(releaseKeystoreFile)
}

// Google Services плагин ищет файл на этапе выполнения своих process*GoogleServices.
tasks.matching { it.name.startsWith("process") && it.name.endsWith("GoogleServices") }
    .configureEach { dependsOn(materializeSecrets) }
// lazy-matching, потому что AGP регистрирует preBuild в afterEvaluate
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(materializeSecrets) }

val signingReady = providers.environmentVariable("KEYSTORE_B64").isPresent ||
    releaseKeystoreFile.asFile.exists()
val storePasswordValue = providers.environmentVariable("KEYSTORE_PASSWORD").orNull
val keyPasswordValue = providers.environmentVariable("KEY_PASSWORD").orNull
val keyAliasValue = providers.environmentVariable("KEY_ALIAS").getOrElse("coflow")

android {
    namespace = "com.example.coflow"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.coflow"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (signingReady) {
            create("release") {
                storeFile = releaseKeystoreFile.asFile
                storePassword = storePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue ?: storePasswordValue
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
            if (signingReady) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // 1. Firebase BoM (управляет версиями всех библиотек Firebase, чтобы они были совместимы)
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))

    // 2. Firebase Authentication
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // 3. Firebase Firestore (база данных)
    implementation("com.google.firebase:firebase-firestore-ktx")

    // 4. Lifecycle ViewModel Compose (КРИТИЧЕСКИ ВАЖНО: это та самая библиотека, которая позволяет использовать viewModel() и чинит баг со стиранием текста!)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
}
