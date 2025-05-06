// Top-level plugins for Android application and Google services (Firebase)
plugins {
    id("com.android.application")             // Android application plugin
    id("com.google.gms.google-services")      // Google Services plugin (for Firebase)
}

android {
    namespace    = "com.silmedy.videocall"     // 앱의 패키지 네임스페이스
    compileSdk   = 35                         // 컴파일 대상 SDK 버전

    defaultConfig {
        applicationId = "com.silmedy.videocall"// 실제 설치될 패키지 ID
        minSdk        = 26                    // 지원 최소 Android SDK
        targetSdk     = 35                    // 최적화된 대상 SDK
        versionCode   = 1                     // 내부 버전 코드 (정수)
        versionName   = "1.0"                 // 사용자에게 보이는 버전명
        testInstrumentationRunner =          // 계측 테스트 실행기 지정
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true                    // ViewBinding 사용 설정
    }

    buildTypes {
        release {
            isMinifyEnabled = false           // 릴리즈 빌드에서 코드 난독화 비활성
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"          // 커스텀 ProGuard 규칙
            )
        }
    }

    compileOptions {
        sourceCompatibility =                // 자바 소스 호환성
            JavaVersion.VERSION_11
        targetCompatibility =                // 자바 타겟 호환성
            JavaVersion.VERSION_11
    }
}

dependencies {
    // AndroidX AppCompat 라이브러리 (기본 UI 컴포넌트 지원)
    implementation("androidx.appcompat:appcompat:1.6.1")
    // Material Design 컴포넌트
    implementation("com.google.android.material:material:1.9.0")
    // ConstraintLayout (레이아웃 라이브러리)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Firebase BoM(Bill of Materials) — 버전 통합 관리
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    // Firebase Realtime Database
    implementation("com.google.firebase:firebase-database")
    // Firebase 인증
    implementation("com.google.firebase:firebase-auth")
    // Firebase 클라우드 메시징(FCM)
    implementation("com.google.firebase:firebase-messaging")

    // WebRTC Android SDK (컴파일 SDK 35 필요)
    implementation("io.github.webrtc-sdk:android:125.6422.07")
    // AndroidX Activity (최신 Activity API)
    implementation("androidx.activity:activity:1.10.1")

    // OkHttp — 네트워킹 클라이언트
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    // OkHttp 로깅 인터셉터 (네트워크 통신 로깅)
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    // JUnit 4 — 단위 테스트
    testImplementation("junit:junit:4.13.2")
    // AndroidX Test Ext — Android용 JUnit 확장
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    // Espresso — UI 테스트 프레임워크
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // JWT 디코딩 라이브러리 (com.auth0.android.jwt 패키지 제공)
    implementation("com.auth0.android:jwtdecode:2.0.0")
}