plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.nt118_englishvocabapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.nt118_englishvocabapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
// Gson Converter (Để Retrofit dùng Gson)
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
// OkHttp (Thư viện nền tảng của Retrofit, cần để tạo Interceptor)
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
// (Bạn cũng có thể cần thêm logging-interceptor để debug)
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}