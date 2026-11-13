# codeXek JVM backend build

Standalone JVM-only Gradle build, independent of upstream Python/TS settings.
Run the consumer workspace scripts/macos/build-usvm-engine.sh (or Windows .bat)
to verify the pinned source and publish all modules to ../build/repository.
The Maven version includes the complete source commit; POM/module metadata
preserve transitive dependencies. Use Temurin 21.0.9+10. Dependency locks and
verification metadata are maintained here with this build.
