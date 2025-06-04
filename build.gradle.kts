plugins {
    id("java")
}

repositories {
    mavenCentral()
}

val versions = mapOf(
    "montoyaApi" to "2025.5",
    "flatlaf" to "3.2.5",
    "gson" to "2.10.1",
    "junit" to "5.10.0",
    "mockito" to "5.5.0",
    "assertj" to "3.24.2"
)

dependencies {
    compileOnly("net.portswigger.burp.extensions:montoya-api:${versions["montoyaApi"]}")
    compileOnly("com.formdev:flatlaf:${versions["flatlaf"]}")
    compileOnly("com.formdev:flatlaf-extras:${versions["flatlaf"]}")
    implementation("com.google.code.gson:gson:${versions["gson"]}")
    
    testImplementation("org.junit.jupiter:junit-jupiter:${versions["junit"]}")
    testImplementation("org.mockito:mockito-core:${versions["mockito"]}")
    testImplementation("org.mockito:mockito-junit-jupiter:${versions["mockito"]}")
    testImplementation("org.assertj:assertj-core:${versions["assertj"]}")
    testImplementation("net.portswigger.burp.extensions:montoya-api:${versions["montoyaApi"]}")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
    options.encoding = "UTF-8"
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().filter { it.isDirectory })
    from(configurations.runtimeClasspath.get().filterNot { it.isDirectory }.map { zipTree(it) })
}