plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.2"
}


group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    implementation("org.agrona:agrona:2.3.1")
    implementation("org.hdrhistogram:HdrHistogram:2.2.2")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.gradleup.shadow:shadow-gradle-plugin:9.2.2")
    }
}
// `apply plugin` stuff are used with `buildscript`.
apply(plugin = "java")
apply(plugin = "com.gradleup.shadow")



tasks.test {
    useJUnitPlatform()
}
