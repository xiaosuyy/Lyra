@file:Suppress("UnstableApiUsage")

import com.android.build.api.variant.FilterConfiguration
import org.gradle.api.tasks.testing.Test
import java.util.UUID

plugins {
    id("build-logic.android.application")
    id("build-logic.android.compose")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
}

val isGithubPullRequest = providers.environmentVariable("GITHUB_EVENT_NAME").orNull == "pull_request"
val isIdeBuild = listOf("android.injected.invoked.from.ide", "idea.active").any { propertyName ->
    (project.findProperty(propertyName) as String?)?.toBoolean() == true ||
        providers.systemProperty(propertyName).orNull?.toBoolean() == true
}
val allowUnsignedRelease =
    (project.findProperty("allowUnsignedRelease") as String?)?.toBoolean() == true ||
        isGithubPullRequest ||
        isIdeBuild
val releaseKeystorePath = project.findProperty("KEYSTORE_FILE") as String? ?: "neri.jks"
val releaseKeystoreFile = project.file(releaseKeystorePath)
val releaseStorePassword = project.findProperty("KEYSTORE_PASSWORD") as String?
val releaseKeyAlias = project.findProperty("KEY_ALIAS") as String? ?: "key0"
val releaseKeyPassword = project.findProperty("KEY_PASSWORD") as String?
val releaseSigningReady = releaseKeystoreFile.exists() &&
    !releaseStorePassword.isNullOrBlank() &&
    releaseKeyAlias.isNotBlank() &&
    !releaseKeyPassword.isNullOrBlank()

// 自定义渠道搜索默认配置：仅保证开箱可用，推荐在 gradle.properties 或命令行覆盖。
val DEFAULT_CUSTOM_CHANNEL_TENCENT_AGGREGATE_KEY = "nvgKtvzw7CFSNFrDhA6"
val DEFAULT_CUSTOM_CHANNEL_MOBILE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

android {
    namespace = "com.lyra.player"
    val buildUUID = UUID.randomUUID()
    val buildAllReleaseAbis = (project.findProperty("buildAllReleaseAbis") as String?)?.toBoolean() == true
    val defaultReleaseAbiFilters = listOf("arm64-v8a")
    val allReleaseAbiFilters = listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")

    signingConfigs {
        create("release") {
            if (releaseSigningReady) {
                storeFile = releaseKeystoreFile
                storePassword = releaseStorePassword.orEmpty()
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword.orEmpty()
            }
        }
    }

    println(" __  __                     ____    ___                                     \n" +
            "/\\ \\/\\ \\                 __/\\  _`\\ /\\_ \\                                    \n" +
            "\\ \\ `\\\\ \\     __   _ __ /\\_\\ \\ \\L\\ \\//\\ \\      __     __  __     __   _ __  \n" +
            " \\ \\ , ` \\  /'__`\\/\\`'__\\/\\ \\ \\ ,__/ \\ \\ \\   /'__`\\  /\\ \\/\\ \\  /'__`\\/\\`'__\\\n" +
            "  \\ \\ \\`\\ \\/\\  __/\\ \\ \\/ \\ \\ \\ \\ \\/   \\_\\ \\_/\\ \\L\\.\\_\\ \\ \\_\\ \\/\\  __/\\ \\ \\/ \n" +
            "   \\ \\_\\ \\_\\ \\____\\\\ \\_\\  \\ \\_\\ \\_\\   /\\____\\ \\__/.\\_\\\\/`____ \\ \\____\\\\ \\_\\ \n" +
            "    \\/_/\\/_/\\/____/ \\/_/   \\/_/\\/_/   \\/____/\\/__/\\/_/ `/___/> \\/____/ \\/_/ \n" +
            "                                                          /\\___/            \n" +
            "                                                          \\/__/             ")
    println("buildUUID: $buildUUID")

    defaultConfig {
        applicationId = "com.lyra.player"

        buildConfigField("String", "BUILD_UUID", "\"${buildUUID}\"")
        buildConfigField("String", "TAG", "\"[NeriPlayer]\"")
        buildConfigField("long", "BUILD_TIMESTAMP", "${System.currentTimeMillis()}L")

        // 自定义渠道搜索的易失效 / 敏感配置，允许在构建时通过 Gradle 属性覆盖：
        //   -PcustomChannelTencentAggregateKey=xxxx
        //   -PcustomChannelMobileUserAgent="Mozilla/5.0 ..."
        // 默认值只保证开箱可用，不作为长期依赖。
        buildConfigField(
            "String",
            "CUSTOM_CHANNEL_TENCENT_AGGREGATE_KEY",
            "\"${(project.findProperty("customChannelTencentAggregateKey") as String?) ?: DEFAULT_CUSTOM_CHANNEL_TENCENT_AGGREGATE_KEY}\""
        )
        buildConfigField(
            "String",
            "CUSTOM_CHANNEL_MOBILE_USER_AGENT",
            "\"${(project.findProperty("customChannelMobileUserAgent") as String?) ?: DEFAULT_CUSTOM_CHANNEL_MOBILE_USER_AGENT}\""
        )


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        renderscriptTargetApi = 31
        renderscriptSupportModeEnabled = true

        externalNativeBuild {
            cmake {
                cppFlags += listOf(
                    "-fexceptions",
                    "-frtti"
                )
                arguments += listOf(
                    "-DANDROID_STL=c++_static",
                    "-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384"
                )
            }
        }
    }

    flavorDimensions += "edition"

    productFlavors {
        // 标准版：包名与文案保持原样
        create("standard") {
            dimension = "edition"
        }
        // 特别版：首页标题换成「晚秋&冬至」，包名独立，可以和标准版同时安装
        create("special") {
            dimension = "edition"
            applicationIdSuffix = ".special"
        }
    }

    buildTypes {
        val releaseSigningConfig = signingConfigs.getByName("release")

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (!buildAllReleaseAbis) {
                ndk {
                    // Regular release stays lean; manual release can opt into all ABI splits.
                    abiFilters += defaultReleaseAbiFilters
                }
            }
            if (releaseSigningReady) {
                signingConfig = releaseSigningConfig
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    sourceSets {
        getByName("androidTest") {
            assets.directories.add("schemas")
        }
    }

    packaging {
        dex {
            // minSdk 28 后 AGP 默认会把 dex 直接存储，恢复 legacy packaging 可显著降低 APK 体积
            useLegacyPackaging = true
        }
        jniLibs {
            // 压缩 APK 内的 native so，优先降低 release 下载体积
            useLegacyPackaging = true
        }
        resources {
            // Compose instrumentation 依赖 kotlinx.coroutines 的 ServiceLoader，
            // androidTest APK 需要合并同名 service 文件，避免只保留单个实现
            merges += "META-INF/services/*"
            excludes += setOf(
                "META-INF/*.version",
                "META-INF/**/LICENSE*",
                "META-INF/**/NOTICE*",
                "google/protobuf/*.proto",
                "org/mozilla/javascript/resources/Messages_*.properties",
                "DebugProbesKt.bin"
            )
        }
    }

    splits {
        abi {
            isEnable = buildAllReleaseAbis
            reset()
            include(*allReleaseAbiFilters.toTypedArray())
            // 打全架构时额外产出一个通用包：Windows 11 的安卓运行环境
            // （MuMu / WSA / 雷电 / BlueStacks）架构各不相同，装通用包不用挑。
            isUniversalApk = true
        }
    }

    bundle {
        language {
            enableSplit = false
        }
    }

}

gradle.taskGraph.whenReady {
    val releasePackagingRequested = allTasks.any { task ->
        task.project == project && (
            task.name == "assemble" ||
                task.name == "bundle" ||
                task.name.contains("Release", ignoreCase = false) &&
                listOf("assemble", "bundle", "package", "sign").any(task.name::startsWith)
            )
    }
    if (releasePackagingRequested && !allowUnsignedRelease && !releaseSigningReady) {
        val missingSigningParts = buildList {
            if (!releaseKeystoreFile.exists()) add("KEYSTORE_FILE=$releaseKeystorePath")
            if (releaseStorePassword.isNullOrBlank()) add("KEYSTORE_PASSWORD")
            if (releaseKeyAlias.isBlank()) add("KEY_ALIAS")
            if (releaseKeyPassword.isNullOrBlank()) add("KEY_PASSWORD")
        }.joinToString()

        throw GradleException(
            "Release signing material is required. Missing: $missingSigningParts. " +
                "Pass -PKEYSTORE_FILE, -PKEYSTORE_PASSWORD, -PKEY_ALIAS and -PKEY_PASSWORD " +
                "or use -PallowUnsignedRelease=true for PR validation builds."
        )
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    exclude("**/com/mocharealm/accompanist/lyrics/ui/utils/String.kt")
}

tasks.withType<Test>().configureEach {
    systemProperty(
        "runNeteaseSmoke",
        System.getProperty("runNeteaseSmoke") ?: "false"
    )
    systemProperty(
        "runYouTubePlaybackSmoke",
        System.getProperty("runYouTubePlaybackSmoke") ?: "false"
    )
    systemProperty(
        "youtubeSmokeVideoId",
        System.getProperty("youtubeSmokeVideoId") ?: ""
    )
    systemProperty(
        "youtubeSmokeForceRefresh",
        System.getProperty("youtubeSmokeForceRefresh") ?: "false"
    )
    systemProperty(
        "youtubeSmokeCookieFile",
        System.getProperty("youtubeSmokeCookieFile") ?: ""
    )
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

androidComponents {
    onVariants(selector().all()) { variant ->
        if (variant.buildType == "debug") return@onVariants

        variant.outputs.forEach { output ->
            val abiSuffix = output.filters
                .find { it.filterType == FilterConfiguration.FilterType.ABI }
                ?.identifier
                ?.let { "-$it" }
                ?: ""
            output.outputFileName.set(
                output.versionName.orElse("dev").map { versionName ->
                    "Lyra-${versionName}${abiSuffix}.apk"
                }
            )
        }
    }
}

dependencies {
    implementation(project(":ksp-annotations"))
    ksp(project(":ksp-processor"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.documentfile)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.foundation.layout)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    implementation(libs.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.icons)
    implementation(libs.androidx.foundation)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    testImplementation(libs.mockito.core)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.android)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.room.testing)
    implementation(libs.androidx.animation)
    implementation(libs.accompanist.navigation.animation)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.dec)
    implementation(libs.newpipe.extractor)
    implementation(libs.okhttp)
    implementation(libs.lyricon.provider)
    implementation(libs.zxing.core)
    implementation(libs.tiny.pinyin)

    implementation(project(":accompanist-lyrics-core"))
    implementation(project(":accompanist-lyrics-ui"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)

    // Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.datasource.okhttp)

    // 拖拽排序
    implementation(libs.reorderable)
    implementation(libs.gson)

    implementation(libs.androidx.media)

    implementation(libs.androidx.ui.graphics)

    implementation(libs.material.kolor)

    implementation(files("libs/lib-decoder-ffmpeg-media3-1.8.0-ffmpeg-6.0-api28-common-release.aar"))

    // Security - 加密存储
    implementation(libs.androidx.security.crypto)
    implementation(libs.taglib)

    // WorkManager - 后台同步
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.javascriptengine)

    implementation(libs.androidx.webkit)

    // 取主题色
    implementation(libs.androidx.palette.ktx)

    implementation(libs.superlyricapi)
}
