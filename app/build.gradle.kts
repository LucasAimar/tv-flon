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
        // El número de compilación de GitHub Actions define la versión. Así la app puede
        // comparar contra la última release (tag build-N) y actualizarse sola.
        // Se suma 100 para que siempre sea mayor que las versiones manuales anteriores (1 a 5).
        val build = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0
        versionCode = 100 + build
        versionName = "0.10.0 (build $build)"
        buildConfigField("int", "BUILD", "$build")
        buildConfigField("String", "REPO", "\"LucasAimar/tv-flon\"")

        // URL del canales.json remoto. Cambiala por la tuya (GitHub raw, tu servidor, etc.).
        // Si falla, la app usa la última copia descargada o la que viene dentro del APK.
        buildConfigField(
            "String",
            "CANALES_URL",
            "\"https://raw.githubusercontent.com/LucasAimar/tv-flon/main/app/src/main/assets/canales.json\""
        )
    }

    // Clave de firma FIJA. Android solo acepta una actualización si está firmada con la
    // misma clave que la versión instalada. En GitHub Actions el archivo flon.jks se arma
    // desde el secreto KEYSTORE_BASE64 (ver .github/workflows/build.yml).
    // Sin ese archivo (por ej. compilando en otra PC) se usa la clave de debug.
    val claveFija = file("flon.jks")
    signingConfigs {
        create("flon") {
            storeFile = claveFija
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "tvflon"
            keyPassword = System.getenv("KEYSTORE_PASSWORD")
        }
    }

    buildTypes {
        debug {
            if (claveFija.exists()) signingConfig = signingConfigs.getByName("flon")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(if (claveFija.exists()) "flon" else "debug")
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
    implementation(libs.androidx.webkit)
}
