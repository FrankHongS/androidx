/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.build.testConfiguration

import androidx.build.dependencyTracker.ProjectSubset
import androidx.build.renameApkForTesting
import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Writes a configuration file in
 * <a href=https://source.android.com/devices/tech/test_infra/tradefed/testing/through-suite/android-test-structure>AndroidTest.xml</a>
 * format that gets zipped alongside the APKs to be tested.
 * This config gets ingested by Tradefed.
 */
abstract class GenerateTestConfigurationTask : DefaultTask() {

    @get:InputFiles
    @get:Optional
    abstract val appFolder: DirectoryProperty

    @get:Internal
    abstract val appLoader: Property<BuiltArtifactsLoader>

    @get:Input
    @get:Optional
    abstract val appProjectPath: Property<String>

    @get:InputFiles
    abstract val testFolder: DirectoryProperty

    @get:Internal
    abstract val testLoader: Property<BuiltArtifactsLoader>

    @get:Input
    abstract val testProjectPath: Property<String>

    @get:Input
    abstract val minSdk: Property<Int>

    @get:Input
    abstract val hasBenchmarkPlugin: Property<Boolean>

    @get:Input
    abstract val testRunner: Property<String>

    @get:Input
    abstract val affectedModuleDetectorSubset: Property<ProjectSubset>

    @get:OutputFile
    abstract val outputXml: RegularFileProperty

    @TaskAction
    fun generateAndroidTestZip() {
        writeConfigFileContent()
    }

    private fun writeConfigFileContent() {
        /*
        Testing an Android Application project involves 2 APKS: an application to be instrumented,
        and a test APK. Testing an Android Library project involves only 1 APK, since the library
        is bundled inside the test APK, meaning it is self instrumenting. We add extra data to
        configurations testing Android Application projects, so that both APKs get installed.
         */
        val configBuilder = ConfigBuilder()
        if (appLoader.isPresent) {
            val appApk = appLoader.get().load(appFolder.get())
                ?: throw RuntimeException("Cannot load required APK for task: $name")
            // We don't need to check hasBenchmarkPlugin because benchmarks shouldn't have test apps
            val appName = appApk.elements.single().outputFile.substringAfterLast("/")
                .renameApkForTesting(appProjectPath.get(), hasBenchmarkPlugin = false)
            // TODO(b/178776319)
            if (appProjectPath.get().contains("macrobenchmark-target")) {
                configBuilder.appApkName(appName.replace("debug-androidTest", "release"))
            } else {
                configBuilder.appApkName(appName)
            }
        }
        val isPostsubmit: Boolean = when (affectedModuleDetectorSubset.get()) {
            ProjectSubset.CHANGED_PROJECTS, ProjectSubset.ALL_AFFECTED_PROJECTS -> {
                true
            }
            ProjectSubset.DEPENDENT_PROJECTS -> {
                false
            }
            else -> {
                throw IllegalStateException(
                    "$name should not be running if the AffectedModuleDetector is returning " +
                        "${affectedModuleDetectorSubset.get()} for this project."
                )
            }
        }
        configBuilder.isPostsubmit(isPostsubmit)
        if (hasBenchmarkPlugin.get()) {
            configBuilder.isBenchmark(true)
            if (isPostsubmit) {
                configBuilder.tag("microbenchmarks")
            } else {
                configBuilder.tag("microbenchmarks_presubmit")
            }
        } else if (testProjectPath.get().endsWith("macrobenchmark")) {
            configBuilder.tag("macrobenchmarks")
        }
        val testApk = testLoader.get().load(testFolder.get())
            ?: throw RuntimeException("Cannot load required APK for task: $name")
        val testName = testApk.elements.single().outputFile
            .substringAfterLast("/")
            .renameApkForTesting(testProjectPath.get(), hasBenchmarkPlugin.get())
        configBuilder.testApkName(testName)
            .applicationId(testApk.applicationId)
            .minSdk(minSdk.get().toString())
            .testRunner(testRunner.get())

        val resolvedOutputFile: File = outputXml.asFile.get()
        if (!resolvedOutputFile.exists()) {
            if (!resolvedOutputFile.createNewFile()) {
                throw RuntimeException(
                    "Failed to create test configuration file: $outputXml"
                )
            }
        }
        resolvedOutputFile.writeText(configBuilder.build())
    }
}
