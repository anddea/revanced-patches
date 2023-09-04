package app.revanced.patches.youtube.layout.branding.name.patch

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import kotlin.io.path.exists

class RemoveElementsPatch : ResourcePatch {
    override fun execute(context: ResourceContext) {

        LANGUAGE_LIST.forEach { path ->
            val resDirectory = context["res"]
            val targetXmlPath = resDirectory.resolve(path).resolve("strings.xml").toPath()

            if (targetXmlPath.exists()) {
                val targetXml = context["res/$path/strings.xml"]

                targetXml.writeText(
                    targetXml.readText()
                        .replace(""".+"application_name".+""".toRegex(), "")
                )
            }
        }

    }

    companion object {
        val LANGUAGE_LIST = arrayOf(
            "values",
            "values-af",
            "values-am",
            "values-ar",
            "values-as",
            "values-az",
            "values-b+sr+Latn",
            "values-be",
            "values-bg",
            "values-bn",
            "values-bs",
            "values-ca",
            "values-cs",
            "values-da",
            "values-de",
            "values-el",
            "values-en-rGB",
            "values-en-rIN",
            "values-es",
            "values-es-rUS",
            "values-et",
            "values-eu",
            "values-fa",
            "values-fi",
            "values-fr",
            "values-fr-rCA",
            "values-gl",
            "values-gu",
            "values-hi",
            "values-hr",
            "values-hu",
            "values-hy",
            "values-in",
            "values-is",
            "values-it",
            "values-iw",
            "values-ja",
            "values-ka",
            "values-kk",
            "values-km",
            "values-kn",
            "values-ko",
            "values-ky",
            "values-lo",
            "values-lt",
            "values-lv",
            "values-mk",
            "values-ml",
            "values-mn",
            "values-mr",
            "values-ms",
            "values-my",
            "values-nb",
            "values-ne",
            "values-nl",
            "values-or",
            "values-pa",
            "values-pl",
            "values-pt",
            "values-pt-rBR",
            "values-pt-rPT",
            "values-ro",
            "values-ru",
            "values-si",
            "values-sk",
            "values-sl",
            "values-sq",
            "values-sr",
            "values-sv",
            "values-sw",
            "values-ta",
            "values-te",
            "values-th",
            "values-tl",
            "values-tr",
            "values-uk",
            "values-ur",
            "values-uz",
            "values-vi",
            "values-zh-rCN",
            "values-zh-rHK",
            "values-zh-rTW",
            "values-zu"
        )
    }
}
