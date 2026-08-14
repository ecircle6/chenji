// 仓库策略：CI（GitHub Actions 等海外网络）走官方源——阿里云镜像在海外返回 502 会导致解析失败；
// 本地（国内网络）用阿里云镜像加速，官方源兜底。CI=true 由 GitHub Actions 自动设置。
pluginManagement {
    repositories {
        if (System.getenv("CI") != "true") {
            // 国内镜像源优先，加速下载
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/central")
            maven("https://maven.aliyun.com/repository/gradle-plugin")
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        if (System.getenv("CI") != "true") {
            // 国内镜像源优先，加速下载
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/central")
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "BirthApp"
include(":app")
