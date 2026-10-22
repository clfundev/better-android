import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

kotlin {
    compilerOptions {
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_3
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

val configPropertiesFile = rootProject.file("config.properties")
val configProperties = Properties()
configProperties.load(FileInputStream(configPropertiesFile))

android {
    namespace = "io.github.clfundev.better"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "io.github.clfundev.better"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"${configProperties["baseUrl"] as String}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(configProperties["storeFile"] as String)
            storePassword = configProperties["storePassword"] as String
            keyAlias = configProperties["keyAlias"] as String
            keyPassword = configProperties["keyPassword"] as String
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.service)

    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Room
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx) // Kotlin Extensions and Coroutines support for Room

    // Settings
    implementation(libs.preference)

    // Paging
    implementation(libs.paging.runtime)

    // Jetpack annotations, such as @NonNull
    implementation(libs.annotation)

    // kotlinx-serialization-json
    implementation(libs.kotlin.serialization.json)
}