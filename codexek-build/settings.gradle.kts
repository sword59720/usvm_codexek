rootProject.name = "cx-usvm-jvm-build"
include("usvm-util", "usvm-core", "usvm-dataflow", "usvm-jvm-dataflow", "usvm-jvm",
    "usvm-jvm:usvm-jvm-api", "usvm-jvm:usvm-jvm-test-api", "usvm-jvm:usvm-jvm-util")

rootProject.projectDir = file(".")
