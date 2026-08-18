plugins {
    id("com.android.application") version "9.3.1"
    id("org.jetbrains.kotlin.android") version "2.2.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
}

/**
 * Версия приложения — та же логика, что и у веб-CRM (обсуждали 19.08):
 * первый коммит = 1.00, каждый следующий = +0.01. У приложения нет
 * сервера, который может спросить git на лету при каждом открытии
 * (как это делает веб-CRM) — здесь версия считается один раз, во время
 * СБОРКИ (Gradle читает git прямо тут, в конфигурации). Раз пересборка
 * и так обязательна после каждого git pull — версия обновится ровно
 * тогда, когда нужно.
 */
fun gitCommitCount(): Int {
    return try {
        val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
        process.waitFor()
        process.inputStream.bufferedReader().readText().trim().toIntOrNull() ?: 1
    } catch (e: Exception) {
        1
    }
}

val gitCommits = gitCommitCount()
val appVersionName = String.format("%.2f", 1.00 + (gitCommits - 1) * 0.01)

android {
    namespace = "com.example.aviorcms"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.aviorcms"
        minSdk = 24
        targetSdk = 34
        versionCode = gitCommits
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // --- Compose (через BOM — версии всех compose-библиотек согласованы автоматически) ---
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.5")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // --- Ядро Android/Compose ---
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // --- Хранилище токена ---
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // --- Сеть: Retrofit + Gson + логирование запросов ---
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // --- Тесты (стандартный минимум Android Studio) ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
