plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Function to load .env file
fun loadEnvFile(): Map<String, String> {
    val envFile = rootProject.file(".env")
    val envMap = mutableMapOf<String, String>()
    
    if (envFile.exists()) {
        envFile.readLines().forEach { line ->
            if (line.isNotBlank() && !line.startsWith("#") && "=" in line) {
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    envMap[parts[0].trim()] = parts[1].trim()
                }
            }
        }
    }
    return envMap
}

val envVariables = loadEnvFile()

android {
    namespace = "com.example.timsales"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.timsales"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Add BuildConfig fields from .env
        buildConfigField("String", "GRIST_BASE_URL", "\"${envVariables["GRIST_BASE_URL"] ?: "https://docs.getgrist.com/api/"}\"")
        buildConfigField("String", "GRIST_DOC_ID", "\"${envVariables["GRIST_DOC_ID"] ?: ""}\"")
        buildConfigField("String", "GRIST_API_KEY", "\"${envVariables["GRIST_API_KEY"] ?: ""}\"")
        buildConfigField("String", "GRIST_TABLE_ID", "\"${envVariables["GRIST_TABLE_ID"] ?: "TimSales"}\"")
        buildConfigField("String", "TIMYEAR_TABLE_ID", "\"${envVariables["TIMYEAR_TABLE_ID"] ?: "TimYear"}\"")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Core Android dependencies - ใช้ version ที่เหมาะสมกับ compileSdk 34
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // Material Design and UI
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    
    // Network dependencies - Retrofit & Gson
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    
    // Coroutines support
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    
    // Fragment KTX (for lifecycleScope)
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.activity:activity-ktx:1.8.2")
    
    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}