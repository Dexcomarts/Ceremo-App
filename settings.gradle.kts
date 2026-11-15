import org.gradle.api.artifacts.verification.DependencyVerificationMode

check(gradle.startParameter.dependencyVerificationMode == DependencyVerificationMode.STRICT) {
    "ceremo builds require strict dependency verification."
}
check(file("gradle/verification-metadata.xml").isFile ||
    gradle.startParameter.writeDependencyVerifications.isNotEmpty()) {
    "Missing dependency verification metadata. Restore the reviewed baseline before building."
}

rootProject.name = "ceremo_backend"
