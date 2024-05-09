package com.jetbrains.interactiveRebase.visuals

/**
 * Branch model class
 */

class Branch(
    public val isCheckedOut: Boolean,
    public val name: String,
    public val commits: List<String>,
)
