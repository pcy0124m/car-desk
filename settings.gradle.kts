pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // 高德地图 SDK 官方仓库（3dmap/定位/搜索等）
        maven { url = uri("https://maven.amap.com/repository/maven-public") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 高德地图 SDK 官方仓库
        maven { url = uri("https://maven.amap.com/repository/maven-public") }
    }
}

rootProject.name = "CarDesk"
include(":app")