import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

// Room schema 产物目录：与实体编译结果做一致性校验（checkSchema），产物入库。
// 放 src/test/assets 根下（MigrationTestHelper 从 assets 读 <类名>/<版本>.json，
// 路径不带 schemas/ 前缀），Robolectric 单测直接能读到
room {
    schemaDirectory("$projectDir/src/test/assets")
}

// 签名凭据放在 keystore.properties（已在 .gitignore 里忽略，不进版本库）。
// 文件存在才启用正式签名；别人拉下代码时没有这个文件，release 会退回未签名（不报错）。
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.birthapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.birthapp"
        minSdk = 26
        targetSdk = 35
        // versionCode 给系统判断新旧版用，每次发版 +1；
        // versionName 是给人看的版本号，与发布说明保持一致
        versionCode = 11
        versionName = "2.1.8"
    }

    // 安装包文件名改成「辰记_v版本号.apk」，
    // 发给手机安装时一眼能认出是什么软件、什么版本
    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl)
                .outputFileName = "辰记_v${versionName}.apk"
        }
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // 有签名配置就用它签 release，否则保持未签名（方便无密钥的环境也能编译）
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            // R8 混淆+压缩：增加反编译难度，剔除无用代码和资源
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    // Robolectric 单测需要 Android 资源环境（迁移测试要读 assets 里的 schema）
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    // IDE 里 @Preview 的实时渲染工具（只在 debug 变体里）
    debugImplementation("androidx.compose.ui:ui-tooling")
    // 预览用的 ComponentActivity 声明（Compose UI 测试的 createComposeRule 需要）
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Activity
    implementation("androidx.activity:activity-compose:1.9.3")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Material
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.core:core-ktx:1.15.0")

    // Glance：桌面小组件。写法与 Compose 同源，但运行在系统的 RemoteViews 里，
    // 所以只能用 Glance 自己的组件，不能直接搬 Compose UI 过去
    implementation("androidx.glance:glance-appwidget:1.1.1")

    // 单元测试：农历换算这种纯算法靠已知日期对照验证，不依赖模拟器
    testImplementation("junit:junit:4.13.2")
    // 备份编解码用的 org.json 在本地单测里是空壳（Android 框架类），
    // 补一份真实现只给测试用，不会打进安装包
    testImplementation("org.json:json:20231013")

    // 迁移测试：Robolectric 在 JVM 上提供 SQLite，MigrationTestHelper 校验迁移正确性
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test:monitor:1.7.1")
    // Compose UI 测试（Robolectric 本地跑，语义树断言，不需要模拟器）
    testImplementation("androidx.compose.ui:ui-test-junit4")
}
