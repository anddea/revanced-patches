package app.revanced.patches.youtube.utils.settings

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.patches.shared.patch.settings.AbstractSettingsResourcePatch
import app.revanced.patches.youtube.utils.integrations.IntegrationsPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.util.resources.IconHelper.YOUTUBE_LAUNCHER_ICON_ARRAY
import app.revanced.util.resources.IconHelper.copyFiles
import app.revanced.util.resources.IconHelper.makeDirectoryAndCopyFiles
import app.revanced.util.resources.ResourceHelper.addPreference
import app.revanced.util.resources.ResourceHelper.addReVancedPreference
import app.revanced.util.resources.ResourceHelper.updatePatchStatus
import app.revanced.util.resources.ResourceHelper.updatePatchStatusSettings
import app.revanced.util.resources.ResourceUtils
import app.revanced.util.resources.ResourceUtils.copyResources
import org.w3c.dom.Element
import java.io.Closeable
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Patch(
    name = "Settings",
    description = "Applies mandatory patches to implement ReVanced Extended settings into the application.",
    dependencies = [
        IntegrationsPatch::class,
        ResourceMappingPatch::class,
        SharedResourceIdPatch::class,
        SettingsBytecodePatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43",
                "18.46.43"
            ]
        )
    ],
    requiresIntegrations = true
)
@Suppress("unused")
object SettingsPatch : AbstractSettingsResourcePatch(
    "youtube/settings",
    "youtube/settings/host",
    true
), Closeable {
    override fun execute(context: ResourceContext) {
        super.execute(context)
        contexts = context

        /**
         * Check if YouTube version is higher than v18.28.xx
         */
        val resourceXmlFile = context["res/values/integers.xml"].readBytes()

        for (threadIndex in 0 until THREAD_COUNT) {
            threadPoolExecutor.execute thread@{
                context.xmlEditor[resourceXmlFile.inputStream()].use { editor ->
                    val resources = editor.file.documentElement.childNodes
                    val resourcesLength = resources.length
                    val jobSize = resourcesLength / THREAD_COUNT

                    val batchStart = jobSize * threadIndex
                    val batchEnd = jobSize * (threadIndex + 1)
                    element@ for (i in batchStart until batchEnd) {
                        if (i >= resourcesLength) return@thread

                        val node = resources.item(i)
                        if (node !is Element) continue

                        if (node.nodeName != "integer" || !node.getAttribute("name")
                                .startsWith("google_play_services_version")
                        ) continue

                        val playServicesVersion = node.textContent.toInt()

                        is1836 = playServicesVersion in 233700000..233801999
                        upward1828 = 232900000 <= playServicesVersion
                        upward1834 = 233502000 <= playServicesVersion
                        upward1839 = 234002000 <= playServicesVersion
                        upward1841 = 234200000 <= playServicesVersion

                        break
                    }
                }
            }
        }

        threadPoolExecutor
            .also { it.shutdown() }
            .awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)

        /**
         * create directory for the untranslated language resources
         */
        context["res/values-v21"].mkdirs()

        arrayOf(
            ResourceUtils.ResourceGroup(
                "layout",
                "revanced_settings_with_toolbar.xml"
            ),
            ResourceUtils.ResourceGroup(
                "values-v21",
                "strings.xml"
            )
        ).forEach { resourceGroup ->
            context.copyResources("youtube/settings", resourceGroup)
        }

        /**
         * initialize ReVanced Extended Settings
         */
        addReVancedPreference("extended_settings")

        /**
         * remove ReVanced Extended Settings divider
         */
        arrayOf("Theme.YouTube.Settings", "Theme.YouTube.Settings.Dark").forEach { themeName ->
            context.xmlEditor["res/values/styles.xml"].use { editor ->
                with(editor.file) {
                    val resourcesNode = getElementsByTagName("resources").item(0) as Element

                    val newElement: Element = createElement("item")
                    newElement.setAttribute("name", "android:listDivider")

                    for (i in 0 until resourcesNode.childNodes.length) {
                        val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                        if (node.getAttribute("name") == themeName) {
                            newElement.appendChild(createTextNode("@null"))

                            node.appendChild(newElement)
                        }
                    }
                }
            }
        }

        /**
         * If a custom branding icon path exists, merge it
         */
        val iconPath = "branding"
        val targetDirectory = Paths.get("").toAbsolutePath().toString() + "/$iconPath"

        if (File(targetDirectory).exists()) {
            fun copyResources(resourceGroups: List<ResourceUtils.ResourceGroup>) {
                try {
                    context.copyFiles(resourceGroups, iconPath)
                } catch (_: Exception) {
                    context.makeDirectoryAndCopyFiles(resourceGroups, iconPath)
                }
            }

            val iconResourceFileNames =
                YOUTUBE_LAUNCHER_ICON_ARRAY
                    .map { "$it.png" }
                    .toTypedArray()

            fun createGroup(directory: String) = ResourceUtils.ResourceGroup(
                directory, *iconResourceFileNames
            )

            arrayOf("xxxhdpi", "xxhdpi", "xhdpi", "hdpi", "mdpi")
                .map { "mipmap-$it" }
                .map(::createGroup)
                .let(::copyResources)
        }

    }

    private val THREAD_COUNT = Runtime.getRuntime().availableProcessors()
    private val threadPoolExecutor = Executors.newFixedThreadPool(THREAD_COUNT)

    internal lateinit var contexts: ResourceContext
    internal var is1836: Boolean = false
    internal var upward1828: Boolean = false
    internal var upward1834: Boolean = false
    internal var upward1839: Boolean = false
    internal var upward1841: Boolean = false

    internal fun addPreference(settingArray: Array<String>) {
        contexts.addPreference(settingArray)
    }

    internal fun addReVancedPreference(key: String) {
        contexts.addReVancedPreference(key)
    }

    internal fun updatePatchStatus(patchTitle: String) {
        contexts.updatePatchStatus(patchTitle)
    }

    override fun close() {
        SettingsBytecodePatch.contexts.classes.forEach { classDef ->
            if (classDef.sourceFile != "BuildConfig.java")
                return@forEach

            classDef.fields.forEach { field ->
                if (field.name == "VERSION_NAME") {
                    contexts.updatePatchStatusSettings(
                        "ReVanced Integrations",
                        field.initialValue.toString().trim()
                    )
                }
            }
        }

        contexts["res/xml/revanced_prefs.xml"].apply {
            writeText(
                readText()
                    .replace(
                        "&quot;",
                        ""
                    )
            )
        }
    }
}