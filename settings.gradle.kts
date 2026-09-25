pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "Planovo Расписание"

include(":app")

// Automatically restore debug.keystore if missing (e.g. after cloning or downloading ZIP)
val root = rootDir
val keystore = java.io.File(root, "debug.keystore")
val keystoreBase64 = java.io.File(root, "debug.keystore.base64")
if (!keystore.exists() && keystoreBase64.exists()) {
  try {
    val decoded = java.util.Base64.getDecoder().decode(keystoreBase64.readText().trim())
    keystore.writeBytes(decoded)
  } catch (_: Exception) {}
}
if (!keystore.exists()) {
  val defaultKeystore = java.io.File(System.getProperty("user.home"), ".android/debug.keystore")
  if (defaultKeystore.exists()) {
    try {
      defaultKeystore.copyTo(keystore, overwrite = false)
    } catch (_: Exception) {}
  }
}
