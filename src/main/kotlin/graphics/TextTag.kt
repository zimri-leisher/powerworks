package graphics

/**
 * Text tags are used to determine color, size and other things of rendered text. They are formatted strictly as follows:
 *
 * (Using the example of TextTagType.COLOR)
 *
 * val string = "this text here is not red. <color=red>this text here is red</color>
 *
 * Spaces before the beginning '<' and after the ending '>' are allowed, however, they are not allowed inside of the tag itself
 * Arguments have no quotation marks surrounding them
 * You may put tags inside of each other to combine effects
 */
enum class TextTagType(val identifier: String, val render: (Int, Int, String, List<TaggedText>) -> Unit) {
    DEFAULT("default", { xPixel, yPixel, argument, enclosingTags ->
    }),
    COLOR("color", { xPixel, yPixel, argument, enclosingTags ->
    }),
    SIZE("size", { _, _, _, _ ->

    });
}

data class TaggedText(val type: TextTagType, val arg: String, val enclosedText: String, val children: MutableList<TaggedText> = mutableListOf()) {
    companion object {
        const val TAG_BEGIN_CHAR = '<'
        const val TAG_CLOSE_CHAR = '>'
        const val TAG_END_CHAR = '/'
        const val TAG_ARG_CHAR = '='

        fun parse(text: String): TaggedText {
            return TaggedText(TextTagType.DEFAULT, "", text.replace(Regex("<\\S.*</\\S*>"), ""), splitIntoTags(text))
        }

        private fun splitIntoTags(text: String): MutableList<TaggedText> {
            val ret = mutableListOf<TaggedText>()
            var i = 0
            for(char in text) {
                // if this is the beginning of the tag
                if(i != text.lastIndex) {
                    if (char == TAG_BEGIN_CHAR && text[i + 1] != TAG_END_CHAR) {
                        try {
                            val fullTag = text.substring(i + 1, text.indexOf(TAG_CLOSE_CHAR, i + 1))
                            val tagName = fullTag.substring(0, if (fullTag.contains(TAG_ARG_CHAR)) fullTag.indexOf(TAG_ARG_CHAR) else fullTag.length)
                            val tagArg = fullTag.substring(fullTag.indexOf(TAG_ARG_CHAR) + 1)
                            val tagType = TextTagType.values().first { it.identifier == tagName }
                            // from the end of the opening tag to the beginning of the last closer for this tag type
                            val endIndex = text.indexOf("$TAG_BEGIN_CHAR$TAG_END_CHAR$tagName$TAG_CLOSE_CHAR")
                            val enclosedTextWithTags = text.substring(text.indexOf(TAG_CLOSE_CHAR, i) + 1, endIndex)
                            val children = splitIntoTags(enclosedTextWithTags)
                            val enclosedText = enclosedTextWithTags.replace(Regex("<\\S.*</\\S*>"), "")
                            val preTag = TaggedText(tagType, tagArg, enclosedText, children)
                            ret.add(preTag)
                            i = endIndex
                        } catch (e: StringIndexOutOfBoundsException) {
                            // malformed
                            i++
                        }
                    } else {
                        i++
                    }
                } else break
            }
            return ret
        }
    }
}