package app.morphe.patches.all.misc.hex

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.patch.stringsOption
import app.morphe.util.Utils.trimIndentMultiline

@Suppress("unused")
val hexResourcePatch = rawResourcePatch(
    description = "Replaces a hexadecimal patterns of bytes of files in an APK.",
    use = false,
) {
    val replacements by stringsOption(
        key = "replacements",
        title = "Replacements",
        description = """
            Hexadecimal patterns to search for and replace with another in a target file.

            A pattern is a sequence of case insensitive strings, each representing hexadecimal bytes, separated by spaces.
            An example pattern is 'aa 01 02 FF'.

            Every pattern must be followed by a pipe ('|'), the replacement pattern,
            another pipe ('|'), and the path to the file to make the changes in relative to the APK root. 
            The replacement pattern must be shorter or equal in length to the pattern.

            Full example of a valid replacement:
            '01 02 aa FF|03 04|path/to/file'
        """.trimIndentMultiline(),
        required = true,
    )

    dependsOn(
        hexPatch(
            block = fun HexPatchBuilder.() {
                replacements!!.forEach { replacement ->
                    try {
                        val (pattern, replacementPattern, targetFilePath) = replacement.split("|", limit = 3)
                        pattern asPatternTo replacementPattern inFile targetFilePath
                    } catch (e: Exception) {
                        throw PatchException(
                            "Invalid replacement: $replacement.\n" +
                                    "Every pattern must be followed by a pipe ('|'), " +
                                    "the replacement pattern, another pipe ('|'), " +
                                    "and the path to the file to make the changes in relative to the APK root. ",
                        )
                    }
                }
            },
        )
    )
}
