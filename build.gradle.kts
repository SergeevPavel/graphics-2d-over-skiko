import sun.jvmstat.monitor.MonitoredVmUtil.mainClass

plugins {
    kotlin("jvm") version "2.3.0"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.skiko:skiko-awt-runtime-macos-arm64:0.9.43")
    implementation("org.jetbrains.skiko:skiko-awt:0.9.43")
}

application {
    mainClass.set("org.example.MainKt")
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerExecutionStrategy.set(org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy.IN_PROCESS)
}

tasks.withType<JavaExec> {
    jvmArgs(
        "--enable-preview",
        "--add-exports", "java.desktop/sun.awt=ALL-UNNAMED",
        "--add-exports", "java.desktop/sun.awt.image=ALL-UNNAMED",
        "--add-exports", "java.desktop/sun.java2d=ALL-UNNAMED",
        "--add-exports", "java.desktop/sun.font=ALL-UNNAMED",
        "--add-exports", "java.desktop/sun.swing=ALL-UNNAMED",
        "--add-exports", "java.desktop/java.awt.peer=ALL-UNNAMED",
        "--add-exports", "java.desktop/java.awt.dnd.peer=ALL-UNNAMED",
        "--add-exports", "java.desktop/sun.lwawt=ALL-UNNAMED",
        "--add-exports", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED",
        "--add-exports", "java.desktop/sun.java2d.metal=ALL-UNNAMED",
        "--add-opens", "java.desktop/java.awt=ALL-UNNAMED",
        "--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED"
    )
}

tasks.test {
    useJUnitPlatform()
}