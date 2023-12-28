package com.leadrdrk.umapatcher.patcher

import com.leadrdrk.umapatcher.data.TextBlock
import com.leadrdrk.umapatcher.unity.file.SerializedFile
import kotlin.math.max
import kotlin.math.roundToInt

abstract class BaseStoryPatcher(
    skipMachineTl: Boolean = false,
    nThreads: Int = 8,
    forcePatch: Boolean = false,
    makeBackup: Boolean = false,
    restoreMode: Boolean = false,
    private val cps: Int = 28,
    private val fps: Int = 30
): AssetsPatcher(skipMachineTl, nThreads, forcePatch, makeBackup, restoreMode) {
    @Suppress("UNCHECKED_CAST")
    override fun patch(assets: SerializedFile, rootValues: MutableMap<String, Any>, i: Int, textBlock: TextBlock): PatchResult {
        // Skip untranslated blocks
        if (textBlock.enText == null && textBlock.enName == null)
            return PatchResult.Skipped

        val pathId = textBlock.pathId!!
        val obj = assets.objects[pathId] ?: return PatchResult.ObjectNotFound
        val values = obj.readTypeTree() ?: return PatchResult.ObjectReadFailed

        if (textBlock.enText != null) values["Text"] = textBlock.enText
        if (textBlock.enName != null) values["Name"] = textBlock.enName

        // Adjust clip/anim length
        val blockIdx = textBlock.blockIdx!!
        if (textBlock.origClipLength != null && textBlock.enText != null) {
            val newTxtLength = (textBlock.enText.length.toFloat() / cps * fps).roundToInt()
            val newClipLength = textBlock.newClipLength ?:
            (values["WaitFrame"] as Int + max(newTxtLength, values["VoiceLength"] as Int))

            if (newClipLength > textBlock.origClipLength) {
                val newBlockLength = newClipLength + values["StartFrame"] as Int + 1
                values["ClipLength"] = newClipLength

                val blockList = rootValues["BlockList"] as List<MutableMap<String, Any>>
                blockList[blockIdx]["BlockLength"] = newBlockLength

                if (textBlock.animData != null) {
                    textBlock.animData.forEach { animGroup ->
                        val newAnimLength = animGroup.origLen + newClipLength - textBlock.origClipLength
                        if (newAnimLength > animGroup.origLen) {
                            val animObject = assets.objects[animGroup.pathId]
                            val animValues = animObject?.readTypeTree()
                            if (animValues != null) {
                                animValues["ClipLength"] = newAnimLength
                                animObject.saveTypeTree(animValues)
                            }
                        }
                    }
                }
            }
        }

        // Translate choices
        if (textBlock.choices != null) {
            val choices = textBlock.choices
            val origChoices = values["ChoiceDataList"] as List<MutableMap<String, Any>>
            if (choices.size == origChoices.size) {
                choices.forEachIndexed { j, choice ->
                    if (choice.enText != null)
                        origChoices[j]["Text"] = choice.enText
                }
            }
        }

        // Translate colored text
        if (textBlock.coloredText != null) {
            val colored = textBlock.coloredText
            val origColored = values["ColorTextInfoList"] as List<MutableMap<String, Any>>
            if (colored.size == origColored.size) {
                colored.forEachIndexed { j, text ->
                    if (text.enText != null)
                        origColored[j]["Text"] = text.enText
                }
            }
        }

        obj.saveTypeTree(values)
        return PatchResult.Success
    }
}