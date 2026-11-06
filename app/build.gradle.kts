plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pedro.prisma"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pedro.prisma"
        minSdk = 31           // blur real (RenderEffect) a partir do Android 12
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }

    // cores libretro (.so em jniLibs) precisam ser extraídos p/ dlopen por caminho absoluto
    packaging { jniLibs { useLegacyPackaging = true } }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")


    // emulação embutida (cores libretro: NES/SNES/GB/GBA/Genesis)
    implementation("com.github.swordfish90:libretrodroid:0.13.2")

    // controle pelo celular (mesma stack do Lumen) + QR
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("dev.mobile:dadb:1.2.10")
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.documentfile:documentfile:1.0.1")
}
