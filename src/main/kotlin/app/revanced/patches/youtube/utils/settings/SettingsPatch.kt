package app.revanced.patches.youtube.utils.settings

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.shared.elements.StringsElementsUtils.removeStringsElements
import app.revanced.patches.shared.mapping.ResourceMappingPatch
import app.revanced.patches.youtube.misc.translations.LANGUAGES
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.IntegrationsPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreferenceFragment
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusSettings
import app.revanced.util.ResourceGroup
import app.revanced.util.classLoader
import app.revanced.util.copyResources
import app.revanced.util.copyXmlNode
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.patch.BaseResourcePatch
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.Closeable
import java.io.FileNotFoundException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.jar.Manifest

@Suppress("DEPRECATION", "unused")
object SettingsPatch : BaseResourcePatch(
    name = "Settings",
    description = "Applies mandatory patches to implement ReVanced Extended settings into the application.",
    dependencies = setOf(
        IntegrationsPatch::class,
        ResourceMappingPatch::class,
        SharedResourceIdPatch::class,
        SettingsBytecodePatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    requiresIntegrations = true
), Closeable {

    private const val DEFAULT_ELEMENT = "About"
    private const val DEFAULT_NAME = "ReVanced Extended"

    private val SETTINGS_ELEMENTS_MAP = mapOf(
        "Parent settings" to "@string/parent_tools_key",
        "General" to "@string/general_key",
        "Account" to "@string/account_switcher_key",
        "Data saving" to "@string/data_saving_settings_key",
        "Autoplay" to "@string/auto_play_key",
        "Video quality preferences" to "@string/video_quality_settings_key",
        "Background" to "@string/offline_key",
        "Watch on TV" to "@string/pair_with_tv_key",
        "Manage all history" to "@string/history_key",
        "Your data in YouTube" to "@string/your_data_key",
        "Privacy" to "@string/privacy_key",
        "History & privacy" to "@string/privacy_key",
        "Try experimental new features" to "@string/premium_early_access_browse_page_key",
        "Purchases and memberships" to "@string/subscription_product_setting_key",
        "Billing & payments" to "@string/billing_and_payment_key",
        "Billing and payments" to "@string/billing_and_payment_key",
        "Notifications" to "@string/notification_key",
        "Connected apps" to "@string/connected_accounts_browse_page_key",
        "Live chat" to "@string/live_chat_key",
        "Captions" to "@string/captions_key",
        "Accessibility" to "@string/accessibility_settings_key",
        DEFAULT_ELEMENT to "@string/about_key"
    )

    private val InsertPosition by stringPatchOption(
        key = "InsertPosition",
        default = DEFAULT_ELEMENT,
        values = SETTINGS_ELEMENTS_MAP,
        title = "Insert position",
        description = "Specify the setting name before which the RVX setting should be inserted."
    )

    private val CustomName by stringPatchOption(
        key = "CustomName",
        default = DEFAULT_NAME,
        title = "Setting Name",
        description = "Specify a custom name for the Extended preference."
    )

    private val THREAD_COUNT = Runtime.getRuntime().availableProcessors()
    private val threadPoolExecutor = Executors.newFixedThreadPool(THREAD_COUNT)

    internal lateinit var contexts: ResourceContext
    internal var upward1831 = false
    internal var upward1834 = false
    internal var upward1839 = false
    internal var upward1842 = false
    internal var upward1849 = false
    internal var upward1902 = false
    internal var upward1912 = false

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

                        upward1831 = 233200000 <= playServicesVersion
                        upward1834 = 233500000 <= playServicesVersion
                        upward1839 = 234000000 <= playServicesVersion
                        upward1842 = 234302000 <= playServicesVersion
                        upward1849 = 235000000 <= playServicesVersion
                        upward1902 = 240204000 < playServicesVersion
                        upward1912 = 241302000 <= playServicesVersion

                        break
                    }
                }
            }
        }

        threadPoolExecutor
            .also { it.shutdown() }
            .awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)

        /**
         * remove strings duplicated with RVX resources
         *
         * YouTube does not provide translations for these strings.
         * That's why it's been added to RVX resources.
         * This string also exists in RVX resources, so it must be removed to avoid being duplicated.
         */
        context.removeStringsElements(
            arrayOf("values"),
            arrayOf(
                "accessibility_settings_edu_opt_in_text",
                "accessibility_settings_edu_opt_out_text"
            )
        )

        /**
         * copy arrays, strings and preference
         */
        arrayOf(
            "arrays.xml",
            "dimens.xml",
            "strings.xml",
            "styles.xml"
        ).forEach { xmlFile ->
            context.copyXmlNode("youtube/settings/host", "values/$xmlFile", "resources")
        }

        arrayOf(
            ResourceGroup(
                "layout",
                "revanced_settings_preferences_category.xml",
                "revanced_settings_with_toolbar.xml",
            ),
            ResourceGroup(
                "xml",
                "revanced_prefs.xml",
            )
        ).forEach { resourceGroup ->
            context.copyResources("youtube/settings", resourceGroup)
        }

        /**
         * initialize ReVanced Extended Settings
         */
        val elementKey =
            SETTINGS_ELEMENTS_MAP[InsertPosition] ?: InsertPosition ?: SETTINGS_ELEMENTS_MAP[DEFAULT_ELEMENT]

        elementKey?.let { insertKey ->
            context.addPreferenceFragment(
                "revanced_extended_settings",
                insertKey
            )
        }

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

    internal fun addPreference(settingArray: Array<String>) {
        contexts.addPreference(settingArray)
    }

    internal fun updatePatchStatus(patch: BaseResourcePatch) {
        updatePatchStatus(patch.name!!)
    }

    internal fun updatePatchStatus(patch: BaseBytecodePatch) {
        updatePatchStatus(patch.name!!)
    }

    internal fun updatePatchStatus(patchTitle: String) {
        contexts.updatePatchStatus(patchTitle)
    }

    override fun close() {

        // region set ReVanced Patches Version

        val jarManifest = classLoader.getResources("META-INF/MANIFEST.MF")
        while (jarManifest.hasMoreElements())
            contexts.updatePatchStatusSettings(
                "ReVanced Patches",
                Manifest(jarManifest.nextElement().openStream())
                    .mainAttributes
                    .getValue("Version") + ""
            )

        // endregion

        // region set ReVanced Integrations Version

        val buildConfigMutableClass = SettingsBytecodePatch.contexts.findClass { it.sourceFile == "BuildConfig.java" }!!.mutableClass
        val versionNameField = buildConfigMutableClass.fields.single { it.name == "VERSION_NAME" }
        val versionName = versionNameField.initialValue.toString().trim().replace("\"","").replace("&quot;", "")

        contexts.updatePatchStatusSettings(
            "ReVanced Integrations",
            versionName
        )

        // endregion

        /**
         * change ReVanced Extended title:
         * everything points to `revanced_extended_settings_title` in the values files
         */
        setOf(
            "", // used for the default values-v21
            *LANGUAGES
        ).forEach {
            val valueFilePath: String = if (it != "") "res/values-$it-v21/strings.xml" else "res/values/strings.xml"

            try {
                contexts.xmlEditor[valueFilePath].use { editor ->
                    with(editor.file) {
                        val nodeList = getElementsByTagName("string")

                        for (i in 0 until nodeList.length) {
                            val node: Node = nodeList.item(i)
                            if (node.attributes.getNamedItem("name").nodeValue != "revanced_extended_settings_title")
                                continue
                            node.textContent = CustomName
                            break
                        }
                    }
                }
            }
            catch (_: FileNotFoundException) { /* ignore missing files */ }
        }

    }
}