plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    val signingProps = file("../signing.properties")
    fun getGitInfo(args: String): String = try {
        providers.exec {
            workingDir = rootDir
            commandLine = ("git " + args).split(" ")
        }.standardOutput.asText.get().trim().ifEmpty { "unknown" }
    } catch (e: Exception) {
        "unknown"
    }

    val commitHash = getGitInfo("rev-parse --short HEAD")

    namespace = "com.khaled.frais"
    compileSdk = 36

    val vName = "1.7"
    defaultConfig {
        applicationId = "com.khaled.frais"
        minSdk = 23
        targetSdk = 36
        versionCode = 9
        versionName = vName

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = "-g$commitHash"
        }
        create("beta") {
            initWith(getByName("release"))
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-BETA"
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (signingProps.exists()) {
                val props = `java.util`.Properties().apply { load(signingProps.reader()) }
                signingConfigs.create("release") {
                    storeFile = file(props.getProperty("storeFile"))
                    storePassword = props.getProperty("storePassword")
                    keyAlias = props.getProperty("keyAlias")
                    keyPassword = props.getProperty("keyPassword")
                }
            } else signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                output.outputFileName.set("Frais-v$vName.apk")
            }
        }
    }
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }
    kotlin {
        jvmToolchain(25)
    }
    androidResources {
        generateLocaleConfig = true
        localeFilters += "en"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.material)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.dhizuku.api)
    implementation(libs.appiconloader)
    implementation(libs.compose.preference)
    implementation(libs.commons.text)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hiddenapibypass)
    implementation(libs.backdrop)
    implementation(libs.work.runtime)
    implementation("androidx.palette:palette-ktx:1.0.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
