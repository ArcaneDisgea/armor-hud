plugins {
    id("multiloader-platform")
    id("net.fabricmc.fabric-loom")
}

repositories {
    maven {
        url = uri("https://api.modrinth.com/maven")
        content {
            includeGroup("maven.modrinth")
        }
    }
    maven {
        name = "Nucleoid"
        url = uri("https://maven.nucleoid.xyz/releases")
        content {
            includeGroup("eu.pb4")
        }
    }
}

loom {
    runs.named("client") {
        client()

        displayName = "fabric - Client"
        runDirectory = file("../run")
        appendProjectPathToDisplayName = false
        generateRunConfig = true
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${BuildConfig.MINECRAFT_VERSION}")
    implementation("net.fabricmc:fabric-loader:${BuildConfig.FABRIC_LOADER_VERSION}")

    api("net.uku3lig:ukulib-fabric:${BuildConfig.UKULIB_VERSION}")

    compileOnly("maven.modrinth:bedrockify:${BuildConfig.BEDROCKIFY_VERSION}")
    compileOnly("eu.pb4:trinkets:${BuildConfig.TRINKETS_VERSION}") { isTransitive = false }
}

modrinth {
    loaders.add("quilt")
}