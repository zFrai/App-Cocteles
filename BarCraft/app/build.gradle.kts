import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Firebase se activa solo cuando existe app/google-services.json.
// Sin ese archivo la app compila igual y funciona en "modo local" (solo recetas).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Claves de APIs externas: se leen de secrets.properties (no se sube a Git).
val secrets = Properties().apply {
    val f = rootProject.file("secrets.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.barcraft.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.barcraft.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "3.0"

        buildConfigField("String", "GIPHY_API_KEY", "\"${secrets.getProperty("GIPHY_API_KEY", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Persistencia local (offline)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Imágenes y GIFs
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    // Bloqueo con huella / PIN
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.process)

    // Corrutinas
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Firebase: cuentas, chat y comunidad
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // QR del perfil: zxing genera el código; el escáner de Google Play services lo lee sin pedir permiso de cámara
    implementation(libs.zxing.core)
    implementation(libs.play.services.code.scanner)
}
