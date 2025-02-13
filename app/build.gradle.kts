plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.chatbox_app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.chatbox_app"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        renderscriptTargetApi = 18
        renderscriptSupportModeEnabled = true

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
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        //noinspection DataBindingWithoutKapt
        dataBinding = true
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //noinspection UseTomlInstead,GradleDependency
    implementation ("com.google.android.gms:play-services-auth:20.7.0")
    //noinspection UseTomlInstead
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    //noinspection UseTomlInstead
    implementation ("com.google.firebase:firebase-auth:23.2.0")
    //noinspection UseTomlInstead
    implementation("com.google.firebase:firebase-auth-ktx")
    //noinspection UseTomlInstead
    implementation ("com.google.firebase:firebase-database:21.0.0")
    //noinspection UseTomlInstead
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    //noinspection UseTomlInstead
    implementation ("com.google.firebase:firebase-firestore:25.1.2")
    //noinspection UseTomlInstead
    implementation ("com.google.firebase:firebase-storage:21.0.1")
    //noinspection UseTomlInstead
    implementation ("de.hdodenhof:circleimageview:3.1.0")

//    implementation ("com.google.firebase:firebase-messaging:23.1.0")
//    debugImplementation ("com.squareup.leakcanary:leakcanary-android:2.14")

    val nav_version = "2.8.5"

    // Views/Fragments integration
    //noinspection KtxExtensionAvailable,UseTomlInstead,GradleDependency
    implementation("androidx.navigation:navigation-fragment:$nav_version")
    //noinspection GradleDependency,UseTomlInstead,KtxExtensionAvailable
    implementation("androidx.navigation:navigation-ui:$nav_version")
}