import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
	`java-library`

	id("com.github.johnrengelman.shadow") version "8.1.1"

	// auto update dependencies with 'useLatestVersions' task
	id("se.patrikerdes.use-latest-versions") version "0.2.18"
	id("com.github.ben-manes.versions") version "0.51.0"
	id("com.diffplug.spotless") version "6.25.0"
}

dependencies {
	val jadxVersion = "1.5.1-SNAPSHOT"
	val isJadxSnapshot = jadxVersion.endsWith("-SNAPSHOT")

	// use compile only scope to exclude jadx-core and its dependencies from result jar
	compileOnly("io.github.skylot:jadx-core:$jadxVersion") {
		isChanging = isJadxSnapshot
	}

	testImplementation("ch.qos.logback:logback-classic:1.5.9")
	testImplementation("org.assertj:assertj-core:3.26.3")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.2")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.2")

	implementation("org.slf4j:slf4j-api:2.0.13")
}

repositories {
	mavenLocal()
	mavenCentral()
	maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
	google()
}

java {
	sourceCompatibility = JavaVersion.VERSION_11
	targetCompatibility = JavaVersion.VERSION_11
}

version = System.getenv("VERSION") ?: "dev"
apply(plugin = "com.diffplug.spotless")
apply(plugin = "checkstyle")

configure<SpotlessExtension> {
	java {
		importOrderFile("$rootDir/config/code-formatter/eclipse.importorder")
		eclipse().configFile("$rootDir/config/code-formatter/eclipse.xml")
		removeUnusedImports()
		commonFormatOptions()
	}
	kotlin {
		ktlint().editorConfigOverride(mapOf("indent_style" to "tab"))
		commonFormatOptions()
	}
	kotlinGradle {
		ktlint()
		commonFormatOptions()
	}
	format("misc") {
		target("**/*.gradle", "**/*.xml", "**/.gitignore", "**/.properties")
		targetExclude(".gradle/**", ".idea/**", "*/build/**")
		commonFormatOptions()
	}
}
tasks {
	withType(Test::class) {
		useJUnitPlatform()
	}
	val shadowJar =
		withType(ShadowJar::class) {
			archiveClassifier.set("") // remove '-all' suffix
		}

	// copy result jar into "build/dist" directory
	register<Copy>("dist") {
		group = "jadx-plugin"
		dependsOn(shadowJar)
		dependsOn(withType(Jar::class))

		from(shadowJar)
		into(layout.buildDirectory.dir("dist"))
	}
}

fun FormatExtension.commonFormatOptions() {
	lineEndings = LineEnding.UNIX
	encoding = Charsets.UTF_8
	trimTrailingWhitespace()
	endWithNewline()
}
