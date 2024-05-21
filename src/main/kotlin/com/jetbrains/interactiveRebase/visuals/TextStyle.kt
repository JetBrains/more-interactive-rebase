package com.jetbrains.interactiveRebase.visuals

enum class TextStyle {
    ITALIC,
    BOLD,
    CROSSED,
    ;

    companion object {
        /**
         * Gets the string representation of a style
         */
        fun getStyleTag(style: TextStyle): Pair<String, String> {
            return when (style) {
                ITALIC -> Pair("<i>", "</i>")
                BOLD -> Pair("<b>", "</b>")
                CROSSED -> Pair("<strike>", "</strike>")
            }
        }

        /**
         * Adds the styling tags to the text
         */
        fun addStyling(
            text: String,
            style: TextStyle,
        ): String {
            var actualText = text
            if (text.contains("<html>")) {
                actualText = text.removePrefix("<html>").removeSuffix("</html>")
            }
            val tags = TextStyle.getStyleTag(style)
            return "<html>${tags.first}$actualText${tags.second}</html>"
        }

        /**
         * Gets a string and removes any styling from it, ex: bold, italic
         */
        fun stripTextFromStyling(text: String): String {
            val pattern = Regex("<.*?>")
            return pattern.replace(text, "")
        }
    }
}
