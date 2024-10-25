plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("application")
}

group = "btpos.tools"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(shadow("commons-cli:commons-cli:1.9.0")!!)
    implementation(shadow("commons-io:commons-io:2.17.0")!!)
    implementation(shadow("net.coobird:thumbnailator:[0.4, 0.5)")!!)
    implementation(shadow("com.googlecode.json-simple:json-simple:1.1.1")!!)
    
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.jar.configure {
    manifest{
        attributes(mapOf(
            "Main-Class" to "btpos.tools.mclowrespackgenerator.Main"
        ))
    }
}

application {
    mainClass = "btpos.tools.mclowrespackgenerator.Main"
}


tasks.test {
    useJUnitPlatform()
}