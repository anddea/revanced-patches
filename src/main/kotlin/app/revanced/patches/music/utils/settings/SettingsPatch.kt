package app.revanced.patches.music.utils.settings

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.fix.accessibility.AccessibilityNodeInfoPatch
import app.revanced.patches.music.utils.settings.ResourceUtils.addPreferenceCategory
import app.revanced.patches.music.utils.settings.ResourceUtils.addPreferenceWithIntent
import app.revanced.patches.music.utils.settings.ResourceUtils.addRVXSettingsPreference
import app.revanced.patches.music.utils.settings.ResourceUtils.addSwitchPreference
import app.revanced.patches.music.utils.settings.ResourceUtils.sortPreferenceCategory
import app.revanced.util.copyXmlNode
import app.revanced.util.patch.BaseResourcePatch
import org.w3c.dom.Element
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION", "unused")
object SettingsPatch : BaseResourcePatch(
    name = "Settings",
    description = "Adds ReVanced Extended settings to YouTube Music.",
    dependencies = setOf(
        AccessibilityNodeInfoPatch::class,
        SettingsBytecodePatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    requiresIntegrations = true
), Closeable {
    private val THREAD_COUNT = Runtime.getRuntime().availableProcessors()
    private val threadPoolExecutor = Executors.newFixedThreadPool(THREAD_COUNT)

    lateinit var contexts: ResourceContext
    internal var upward0636 = false
    internal var upward0642 = false

    override fun execute(context: ResourceContext) {
        contexts = context

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

                        upward0636 = 240399000 <= playServicesVersion
                        upward0642 = 240999000 <= playServicesVersion

                        break
                    }
                }
            }
        }

        threadPoolExecutor
            .also { it.shutdown() }
            .awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)

        /**
         * copy strings
         */
        context.copyXmlNode("music/settings/host", "values/strings.xml", "resources")

        /**
         * hide divider
         */
        val styleFile = context["res/values/styles.xml"]

        styleFile.writeText(
            styleFile.readText()
                .replace(
                    "allowDividerAbove\">true",
                    "allowDividerAbove\">false"
                ).replace(
                    "allowDividerBelow\">true",
                    "allowDividerBelow\">false"
                )
        )


        /**
         * Copy colors
         */
        context.xmlEditor["res/values/colors.xml"].use { editor ->
            val resourcesNode = editor.file.getElementsByTagName("resources").item(0) as Element

            for (i in 0 until resourcesNode.childNodes.length) {
                val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                node.textContent = when (node.getAttribute("name")) {
                    "material_deep_teal_500" -> "@android:color/white"

                    else -> continue
                }
            }
        }

        context.addRVXSettingsPreference()
    }

    internal fun addSwitchPreference(
        category: CategoryType,
        key: String,
        defaultValue: String
    ) = addSwitchPreference(category, key, defaultValue, "")

    internal fun addSwitchPreference(
        category: CategoryType,
        key: String,
        defaultValue: String,
        setSummary: Boolean
    ) = addSwitchPreference(category, key, defaultValue, "", setSummary)

    internal fun addSwitchPreference(
        category: CategoryType,
        key: String,
        defaultValue: String,
        dependencyKey: String
    ) = addSwitchPreference(category, key, defaultValue, dependencyKey, true)

    internal fun addSwitchPreference(
        category: CategoryType,
        key: String,
        defaultValue: String,
        dependencyKey: String,
        setSummary: Boolean
    ) {
        val categoryValue = category.value
        contexts.addPreferenceCategory(categoryValue)
        contexts.addSwitchPreference(categoryValue, key, defaultValue, dependencyKey, setSummary)
    }

    internal fun addPreferenceWithIntent(
        category: CategoryType,
        key: String
    ) = addPreferenceWithIntent(category, key, "")

    internal fun addPreferenceWithIntent(
        category: CategoryType,
        key: String,
        dependencyKey: String
    ) {
        val categoryValue = category.value
        contexts.addPreferenceCategory(categoryValue)
        contexts.addPreferenceWithIntent(categoryValue, key, dependencyKey)
    }

    override fun close() {
        /**
         * Copy arrays
         */
        contexts.copyXmlNode("music/settings/host", "values/arrays.xml", "resources")

        addPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_extended_settings_import_export"
        )

        CategoryType.entries.sorted().forEach {
            contexts.sortPreferenceCategory(it.value)
        }
    }
}
