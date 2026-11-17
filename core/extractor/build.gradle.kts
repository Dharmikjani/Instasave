plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

android {
  namespace = "com.instasaver.core.extractor"
  compileSdk = 36

  defaultConfig {
    minSdk = 26
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  api(project(":core:model"))
  implementation(project(":core:network"))
  implementation(project(":core:database"))
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.jsoup)
  implementation(libs.okhttp)
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  testImplementation(libs.junit)
}
