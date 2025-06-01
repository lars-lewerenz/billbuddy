plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.billbuddy.android" // Choose your app's namespace
    compileSdk = project.property("android.compileSdk").toString().toInt()

    defaultConfig {
        applicationId = "com.billbuddy.android"
        minSdk = project.property("android.minSdk").toString().toInt()
        targetSdk = project.property("android.targetSdk").toString().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            // proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":shared")) // Dependency on the shared module
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat) // Basic app compatibility

    // Jetpack Compose dependencies
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose) // For setContent
    implementation(libs.androidx.lifecycle.viewmodel.compose) // For ViewModels with Compose

    // Koin for Android (if you plan to use it in androidApp specific parts)
    implementation(libs.koin.android)
    implementation(libs.google.play.services.auth)
}
