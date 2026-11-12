plugins { id("com.android.application") }
android {
    namespace = "com.siu.local"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.siu.local"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "com.siu.local.SmokeInstrumentation"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies { implementation("com.google.zxing:core:3.5.3") }
