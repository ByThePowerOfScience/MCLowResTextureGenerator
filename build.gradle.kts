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
    implementation(shadow("systems.manifold:manifold-rt:2024.1.37")!!)
    // Add manifold to -processorpath for javac
    annotationProcessor (group= "systems.manifold", name= "manifold-exceptions", version= "2024.1.37")
    testAnnotationProcessor(group= "systems.manifold", name= "manifold-exceptions", version= "2024.1.37")

    
    implementation(shadow("commons-cli:commons-cli:1.9.0")!!)
    implementation(shadow("commons-io:commons-io:2.17.0")!!)
    implementation(shadow("net.coobird:thumbnailator:[0.4, 0.5)")!!)
    
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
    mainModule = "btpos.tools.mclowrespackgenerator"
    mainClass = "btpos.tools.mclowrespackgenerator.Main"
}


if (JavaVersion.current() != JavaVersion.VERSION_1_8 &&
    sourceSets.main.get().allJava.files.any {it.name == "module-info.java"}) {
    tasks.withType<JavaCompile> {
        // if you DO define a module-info.java file:
        options.compilerArgs.addAll(listOf("-Xplugin:Manifold", "--module-path", classpath.asPath))
    }
} else {
    tasks.withType<JavaCompile> {
        // If you DO NOT define a module-info.java file:
        options.compilerArgs.add("-Xplugin:Manifold")
    }
}

tasks.test {
    useJUnitPlatform()
}
