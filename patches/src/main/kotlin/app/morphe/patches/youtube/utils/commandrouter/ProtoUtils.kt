/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3953
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.utils.commandrouter

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patches.shared.misc.fix.proto.ProtobufClassParseByteArrayWithRegistryFingerprint
import app.morphe.patches.shared.misc.fix.proto.protoDefaultInstanceFingerprint

/**
 * Smali that parses the byte array in [bytes] to a new [messageType] instance in [result],
 * using the app's own protobuf runtime. [registry] is overwritten.
 * All registers must be v0-v15.
 */
internal fun BytecodePatchContext.parseProtoInstructions(
    messageType: String,
    bytes: String,
    result: String,
    registry: String
): String {
    val defaultInstance = protoDefaultInstanceFingerprint(messageType)
        .instructionMatches.single().fieldReference

    // Generated messages share the static parseFrom of the protobuf runtime's message class.
    val parser = ProtobufClassParseByteArrayWithRegistryFingerprint.originalMethod

    return """
        sget-object $result, $defaultInstance
        invoke-static {}, Lcom/google/protobuf/ExtensionRegistryLite;->getGeneratedRegistry()Lcom/google/protobuf/ExtensionRegistryLite;
        move-result-object $registry
        invoke-static {$result, $bytes, $registry}, $parser
        move-result-object $result
    """
}
