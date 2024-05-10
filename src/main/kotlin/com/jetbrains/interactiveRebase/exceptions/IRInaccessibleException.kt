package com.jetbrains.interactiveRebase.exceptions

import com.intellij.openapi.vcs.VcsException

class IRInaccessibleException(s: String) : VcsException(s)
