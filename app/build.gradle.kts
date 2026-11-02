plugins { id("com.android.application"); id("org.jetbrains.kotlin.plugin.compose") }
android {
    namespace = "pt.gametym"
    compileSdk = 37
    defaultConfig {
        applicationId = "pt.gametym"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui-android:1.10.4")
    implementation("androidx.compose.foundation:foundation-android:1.10.4")
    implementation("androidx.compose.material3:material3-android:1.4.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    // Compose Test traz Espresso antigo; 3.7 evita reflexão incompatível com Android 17.
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4-android:1.10.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.10.4")
}
