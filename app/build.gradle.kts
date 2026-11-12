plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "ar.flon.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "ar.flon.tv"
        // API 23 cubre Fire TV Stick viejos (Fire OS 6 = API 25) y cualquier Android TV actual.
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"

        // URL del canales.json remoto. Cambiala por la tuya (GitHub raw, tu servidor, etc.).
        // Si falla, la app usa la última copia descargada o la que viene dentro del APK.
        buildConfigField(
            "String",
            "CANALES_URL",
            "\"https://raw.githubusercontent.com/LucasAimar/tv-flon/main/app/src/main/assets/canales.json\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Para uso personal alcanza con firmar el release con la clave de debug.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.tv.material)

    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.coil.compose)
}
