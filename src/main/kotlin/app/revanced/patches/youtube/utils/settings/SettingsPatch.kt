package app.revanced.patches.youtube.utils.settings

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.patches.shared.patch.settings.AbstractSettingsResourcePatch
import app.revanced.patches.youtube.utils.integrations.IntegrationsPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addReVancedPreference
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusSettings
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import org.w3c.dom.Element
import java.io.Closeable
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
                "18.46.45",
                "18.48.39",
                "18.49.37",
                "19.01.34",
                "19.02.39",
                "19.03.36",
                "19.04.38",
                "19.05.36",
                "19.06.39",
                "19.07.40",
                "19.08.36",
                "19.09.38",
                "19.10.39",
                "19.11.38"
            ]
        )
    ],
    requiresIntegrations = true
)
@Suppress("unused")
object SettingsPatch : AbstractSettingsResourcePatch(
    "youtube/settings"
), Closeable {
    private const val DEFAULT_ELEMENT = "About"

    private val InsertPosition by stringPatchOption(
        key = "InsertPosition",
        default = DEFAULT_ELEMENT,
        title = "Insert position",
        description = "Specify the setting name before which the RVX setting should be inserted."
    )

    private val SETTINGS_ELEMENTS_MAP = mapOf(
        "parent settings" to "@string/parent_tools_key",
        "general" to "@string/general_key",
        "account" to "@string/account_switcher_key",
        "data saving" to "@string/data_saving_settings_key",
        "autoplay" to "@string/auto_play_key",
        "video quality preferences" to "@string/video_quality_settings_key",
        "background" to "@string/offline_key",
        "watch on tv" to "@string/pair_with_tv_key",
        "manage all history" to "@string/history_key",
        "your data in youtube" to "@string/your_data_key",
        "privacy" to "@string/privacy_key",
        "history & privacy" to "@string/privacy_key",
        "try experimental new features" to "@string/premium_early_access_browse_page_key",
        "purchases and memberships" to "@string/subscription_product_setting_key",
        "billing & payments" to "@string/billing_and_payment_key",
        "billing and payments" to "@string/billing_and_payment_key",
        "notifications" to "@string/notification_key",
        "connected apps" to "@string/connected_accounts_browse_page_key",
        "live chat" to "@string/live_chat_key",
        "captions" to "@string/captions_key",
        "accessibility" to "@string/accessibility_settings_key",
        "about" to "@string/about_key"
    )

    override fun execute(context: ResourceContext) {
        super.execute(context)
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

                        upward1828 = 232900000 <= playServicesVersion
                        upward1831 = 233200000 <= playServicesVersion
                        upward1834 = 233502000 <= playServicesVersion
                        upward1836 = 233700000 <= playServicesVersion
                        upward1839 = 234002000 <= playServicesVersion
                        upward1841 = 234200000 <= playServicesVersion
                        upward1843 = 234400000 <= playServicesVersion
                        upward1904 = 240502000 <= playServicesVersion

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
            ResourceGroup(
                "layout",
                "revanced_settings_with_toolbar.xml"
            ),
            ResourceGroup(
                "values-v21",
                "strings.xml"
            )
        ).forEach { resourceGroup ->
            context.copyResources("youtube/settings", resourceGroup)
        }


        /**
         * initialize ReVanced Extended Settings
         */
        val positionKey = InsertPosition?.lowercase()
        val elementKey = SETTINGS_ELEMENTS_MAP[positionKey] ?: SETTINGS_ELEMENTS_MAP[DEFAULT_ELEMENT.lowercase()]
        elementKey?.let { addReVancedPreference("extended_settings", it) }

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

    }

    private val THREAD_COUNT = Runtime.getRuntime().availableProcessors()
    private val threadPoolExecutor = Executors.newFixedThreadPool(THREAD_COUNT)

    internal lateinit var contexts: ResourceContext
    internal var upward1828: Boolean = false
    internal var upward1831: Boolean = false
    internal var upward1834: Boolean = false
    internal var upward1836: Boolean = false
    internal var upward1839: Boolean = false
    internal var upward1841: Boolean = false
    internal var upward1843: Boolean = false
    internal var upward1904: Boolean = false

    internal fun addPreference(settingArray: Array<String>) {
        contexts.addPreference(settingArray)
    }

    internal fun addReVancedPreference(key: String, insertKey: String = "misc") {
        contexts.addReVancedPreference(key, insertKey)
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