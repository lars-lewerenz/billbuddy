import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.native.cocoapods)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    // Define Android target
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    // Define iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.contentnegotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.napier)
                implementation(libs.multiplatformSettings.noArg)
                implementation(libs.multiplatformSettings.coroutines)
                implementation(libs.koin.core)
                implementation(libs.sqldelight.driver.sqlite) // For common logic if applicable, or remove if only platform specific
                implementation(libs.sqldelight.coroutines.extensions)
                implementation(libs.uuid)
                implementation(libs.kotlinx.datetime)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.test.junit) // For running tests with JUnit
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio) // JVM engine for Ktor
                implementation(libs.sqldelight.driver.android)
                implementation(libs.koin.android) // For Android specific Koin features
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                // implementation(libs.androidx.test.junit) // If you need AndroidX test utilities
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation(libs.ktor.client.darwin) // Darwin engine for Ktor
                implementation(libs.sqldelight.driver.native)
            }
        }
        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
            dependencies {
                // Dependencies for iOS tests
            }
        }
    }
    // Cocoapods configuration
    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "14.1" // Choose an appropriate deployment target
        framework {
            baseName = "shared"
            isStatic = true
        }
        //pod("FirebaseAuth") // Example if you were to use Firebase Auth directly in iOS through cocoapods
    }
}

android {
    namespace = "com.billbuddy.shared" // Replace with your desired namespace
    compileSdk = project.property("android.compileSdk").toString().toInt()
    defaultConfig {
        minSdk = project.property("android.minSdk").toString().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    // Explicitly set the packaging options for SQLDelight
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            pickFirsts += "META-INF/io.netty.versions.properties" // Example if Ktor CIO pulls in Netty and causes conflicts
        }
    }
}

sqldelight {
  databases {
    create("BillBuddyDatabase") { // Database name
      packageName = "com.billbuddy.shared.db" // Package name for generated Kotlin sources
      // srcDirs = listOf("src/commonMain/sqldelight") // Specify .sq files location if not default
    }
  }
}
