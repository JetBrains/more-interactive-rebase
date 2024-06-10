package com.jetbrains.interactiveRebase.visuals

enum class TextStyle {
    ITALIC,
    BOLD,
    CROSSED,
    LEFT_ALIGNMENT,
    RIGHT_ALIGNMENT,
    ;

    companion object {
        /**
         * Gets the string representation of a style
         */
        fun getStyleTag(style: TextStyle): Pair<String, String> {
            return when (style) {
                ITALIC -> Pair("<span style='white-space: nowrap; overflow: hidden; text-overflow: ellipsis;'><i>", "</i></span>")
                BOLD -> Pair("<span style='white-space: nowrap; overflow: hidden; text-overflow: ellipsis;'><b>", "</b></span>")
                CROSSED ->
                    Pair(
                        "<span style='white-space: nowrap; overflow: hidden; text-overflow: ellipsis;'><strike>",
                        "</strike></span>",
                    )
                LEFT_ALIGNMENT ->
                    Pair(
                        "<span style='white-space: nowrap; overflow: hidden; text-overflow: ellipsis; text-align: left;'><body>",
                        "</body></span>",
                    )
                RIGHT_ALIGNMENT ->
                    Pair(
                        "<span style='white-space: nowrap; overflow: hidden; text-overflow: ellipsis; text-align: right;'><body>",
                        "</body></span>",
                    )
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
