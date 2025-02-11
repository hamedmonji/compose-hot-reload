/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package builds

import builds.conventions.CommitStatusPublisher
import builds.conventions.HostRequirement
import builds.conventions.PublishLocallyConvention
import builds.utils.Host
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.buildFeatures.buildCache
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.sequential
import jetbrains.buildServer.configs.kotlin.triggers.vcs


object Tests : Project({
    name = "Tests"
    description = "Compose Hot Reload Tests"

    buildType(AllTests)

    val linuxTest = Test(Host.Linux)
    val macOsTest = Test(Host.MacOS)
    val windowsTest = Test(Host.Windows)

    buildType(linuxTest)
    buildType(macOsTest)
    buildType(windowsTest)

    sequential {
        buildType(PublishLocally)
        parallel {
            buildType(linuxTest)
            buildType(macOsTest)
            buildType(windowsTest)
        }
        buildType(AllTests)
    }
})

object AllTests : BuildType({
    name = "All Tests"
    description = "All tests"

    triggers {
        vcs {
        }
    }
})

class Test(
    override val requiredHost: Host
) : BuildType({
    name = "Tests ($requiredHost)"
    id("Tests_$requiredHost")

    artifactRules = """
        **/*-actual*
        **/build/reports/**
    """.trimIndent()


    features {
        buildCache {
            publish = true
            name = "Functional Test Gradle Cache"
            rules = """
                tests/build/gradleHome/**
                tests/build/reloadFunctionalTestWarmup/**
            """.trimIndent()
        }
    }

    steps {
        gradle {
            name = "Test"
            tasks = "check"
        }
    }
}), PublishLocallyConvention,
    CommitStatusPublisher,
    HostRequirement.Dynamic
