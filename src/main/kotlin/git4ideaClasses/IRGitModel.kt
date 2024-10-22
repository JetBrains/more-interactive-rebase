// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package git4ideaClasses

import kotlin.math.max
import kotlin.math.min

/**
 * The model that handles interactive rebasing actions and changes
 */
class IRGitModel<T : IRGitEntry>(initialState: List<Element<T>>) {
    internal val rows = ElementList(initialState)

    val elements: List<Element<T>>
        get() = rows.elements

    /**
     * Converts the model to a list of entries
     */
    internal fun convertToEntries(): List<IRGitEntry> =
        elements.map { element ->
            val entry = element.entry
            IRGitEntry(element.type.command, entry.commit, entry.subject)
        }

    /**
     * Checks if the Pick action is allowed
     */
    fun canPick(indices: List<Int>) = anyOfType(indices) { it !is Type.NonUnite.KeepCommit.Pick && it !is Type.NonUnite.UpdateRef }

    /**
     * Adds pick action for the indicated commits
     */
    fun pick(indices: List<Int>) {
        keepCommitAction(indices, Type.NonUnite.KeepCommit.Pick)
    }

    /**
     * Checks if editing is possible
     */
    fun canEdit(indices: List<Int>) = anyOfType(indices) { it !is Type.NonUnite.KeepCommit.Edit && it !is Type.NonUnite.UpdateRef }

    /**
     * Adds editing action for the selected commits
     */
    fun edit(indices: List<Int>) {
        keepCommitAction(indices, Type.NonUnite.KeepCommit.Edit)
    }

    /**
     * Checks if the reword action is possible
     */
    fun canReword(index: Int): Boolean = rows[index] !is Element.UniteChild && rows[index].type !is Type.NonUnite.UpdateRef

    /**
     * Adds rewording action for the selected commit with the given message
     */
    fun reword(
        index: Int,
        message: String,
    ) {
        keepCommitAction(listOf(index), Type.NonUnite.KeepCommit.Reword(message))
    }

    /**
     * Checks if the drop action is possible
     */
    fun canDrop(indices: List<Int>) = anyOfType(indices) { it !is Type.NonUnite.Drop && it !is Type.NonUnite.UpdateRef }

    /**
     * Adds dropping action for the selected commits
     */
    fun drop(indices: List<Int>) {
        val elements = indices.map { rows[it] }.filter { it.type != Type.NonUnite.UpdateRef }
        elements.filterIsInstance<Element.Simple<T>>().forEach { element ->
            element.type = Type.NonUnite.Drop
        }
        rows.modifyList {
            elements.filterIsInstance<Element.UniteRoot<T>>().forEach { root ->
                convertUniteGroupToSimple(root, Type.NonUnite.Drop)
            }
        }

        val uniteChildren = indices.sortedDescending().map { rows[it] }.filterIsInstance<Element.UniteChild<T>>()
        rows.modifyList {
            uniteChildren.forEach { child ->
                removeAndMoveUniteChild(child, Type.NonUnite.Drop)
            }
        }
    }

    /**
     * Possibly can be deleted
     */
    fun canUnite(indices: List<Int>): Boolean {
        if (indices.size < 2) {
            return false
        }
        if (indices.any { rows[it].type == Type.NonUnite.UpdateRef }) return false
        val root =
            when (val element = rows[indices.first()]) {
                is Element.Simple -> return true
                is Element.UniteRoot -> element
                is Element.UniteChild -> element.root
            }
        indices.drop(1).forEach { index ->
            val element = rows[index] as? Element.UniteChild ?: return true
            if (element.root !== root) {
                return true
            }
        }
        return false
    }

    /**
     * Squashing and fixup
     */
    fun unite(indices: List<Int>): Element.UniteRoot<T> {
        lateinit var root: Element.UniteRoot<T>
        rows.modifyList {
            root = convertToRoot(indices.first())
            val newChildren =
                indices.asSequence().drop(1).map { rows[it] }
                    .filter { it !is Element.UniteChild || it.root !== root }
                    .map { if (it is Element.UniteRoot) it.uniteGroup else listOf(it) }
                    .flatten()
                    .distinct()
                    .toList()
            moveElements(newChildren, root.newChildIndex())
            newChildren.forEach { newChild ->
                addToUniteGroup(newChild.index, root)
            }
        }
        return root
    }

    /**
     * Reorder action
     */
    fun exchangeIndices(
        oldIndex: Int,
        newIndex: Int,
    ) {
        rows.modifyList {
            val elementsToMove: List<Element<T>> =
                when (val element = rows[oldIndex]) {
                    is Element.UniteRoot -> element.uniteGroup
                    is Element.Simple -> listOf(element)
                    is Element.UniteChild -> {
                        val newElement = removeAndMoveUniteChild(element, Type.NonUnite.KeepCommit.Pick)
                        if (newElement.index == newIndex) {
                            // moved to the last position of current UniteGroup
                            addToUniteGroup(newElement.index, element.root)
                            listOf()
                        } else {
                            listOf(newElement)
                        }
                    }
                }
            moveElements(elementsToMove, newIndex)
            addToUniteGroupIfNeeded(elementsToMove)
        }
    }

    /**
     * Used for reordering
     */
    private fun MutableElementList<T>.addToUniteGroupIfNeeded(elements: List<Element<T>>) {
        if (elements.isEmpty()) {
            return
        }
        val next = getNextElement(elements.last())
        if (next is Element.UniteChild) {
            val newRoot = next.root
            elements.forEach {
                addToUniteGroup(it.index, newRoot)
            }
        }
    }

    /**
     * Used for drop
     */
    private fun MutableElementList<T>.convertUniteGroupToSimple(
        root: Element.UniteRoot<T>,
        newType: Type.NonUnite,
    ) {
        root.uniteGroup.forEach { element ->
            forceChangeElement(element, Element.Simple(element.index, newType, element.entry))
        }
    }

    private fun MutableElementList<T>.changeUniteChild(
        child: Element.UniteChild<T>,
        newElement: Element<T>,
    ) {
        val root = child.root
        root.removeChild(child)
        if (root.children.isEmpty()) {
            changeUniteRoot(root, Element.Simple(root.index, root.type, root.entry))
        }
        forceChangeElement(child, newElement)
    }

    private fun MutableElementList<T>.changeUniteRoot(
        element: Element.UniteRoot<T>,
        newElement: Element<T>,
    ) {
        convertUniteGroupToSimple(element, Type.NonUnite.KeepCommit.Pick)
        forceChangeElement(element, newElement)
    }

    /**
     * Retrieves the next element
     */
    private fun getNextElement(element: Element<T>) = (element.index + 1).takeIf { it < rows.size }?.let { rows[it] }

    private fun MutableElementList<T>.addToUniteGroup(
        index: Int,
        root: Element.UniteRoot<T>,
    ) {
        val element = elements[index]
        val newChildElement = Element.UniteChild(index, element.entry, root)
        root.addChild(newChildElement)
        when (element) {
            is Element.Simple -> changeSimple(element, newChildElement)
            is Element.UniteRoot -> changeUniteRoot(element, newChildElement)
            is Element.UniteChild -> changeUniteChild(element, newChildElement)
        }
    }

    internal fun convertElementToRoot(indices: List<Int>): Element.UniteRoot<T> {
        lateinit var root: Element.UniteRoot<T>
        rows.modifyList {
            root = convertToRoot(indices.last())
        }
        return root
    }

    internal fun MutableElementList<T>.convertToRoot(rootIndex: Int): Element.UniteRoot<T> =
        when (val element = rows[rootIndex]) {
            is Element.UniteRoot -> element
            is Element.UniteChild -> element.root
            is Element.Simple -> {
                val currentType = element.type
                val newType =
                    if (currentType is Type.NonUnite.KeepCommit) {
                        currentType
                    } else {
                        Type.NonUnite.KeepCommit.Pick
                    }
                Element.UniteRoot(rootIndex, newType, element.entry).also { root ->
                    changeSimple(element, root)
                }
            }
        }

    /**
     * Used for every interactive rebase action
     */
    private fun keepCommitAction(
        indices: List<Int>,
        type: Type.NonUnite.KeepCommit,
    ) {
        rows.modifyList {
            indices.sortedDescending().forEach { index ->
                when (val element = rows[index]) {
                    is Element.Simple -> {
                        element.type = type
                    }
                    is Element.UniteRoot -> {
                        element.type = type
                    }
                    is Element.UniteChild -> removeAndMoveUniteChild(element, type)
                }
            }
        }
    }

    private fun MutableElementList<T>.removeAndMoveUniteChild(
        child: Element.UniteChild<T>,
        newType: Type.NonUnite,
    ): Element<T> {
        val root = child.root
        val newIndex = root.lastChildIndex()
        val element = Element.Simple(child.index, newType, child.entry)
        changeUniteChild(child, element)
        moveElements(listOf(element), newIndex)
        return element
    }

    private fun anyOfType(
        indices: List<Int>,
        condition: (Type) -> Boolean,
    ): Boolean = indices.any { condition(rows[it].type) }

    /**
     * List for the initial states
     */
    internal class ElementList<T : IRGitEntry>(initialState: List<Element<T>>) {
        private val mutableElementList = MutableElementList(initialState)

        val size = initialState.size

        val elements: List<Element<T>>
            get() = mutableElementList.elements

        operator fun get(index: Int): Element<T> = mutableElementList[index]

        fun modifyList(updateFunction: MutableElementList<T>.() -> Unit) {
            mutableElementList.updateFunction()
            validateElements()
        }

        private fun validateElements() {
            elements.forEachIndexed { index, element ->
                check(element.index == index)
                when (element) {
                    is Element.UniteRoot -> {
                        validateUniteGroup(element)
                    }
                    is Element.UniteChild -> {
                        check(element.root == elements[element.root.index])
                    }
                    is Element.Simple -> {
                    }
                }
            }
        }

        private fun validateUniteGroup(root: Element.UniteRoot<T>) {
            check(root.children.isNotEmpty())
            for (index in root.index + 1 until root.index + root.children.size) {
                val child = elements[index]
                check(child is Element.UniteChild<T>)
                check(child.root == root)
            }
        }
    }

    internal class MutableElementList<T : IRGitEntry>(initialState: List<Element<T>>) {
        private val _elements: MutableList<Element<T>> = initialState.toMutableList()

        val elements: List<Element<T>>
            get() = _elements

        operator fun get(index: Int): Element<T> = _elements[index]

        fun forceChangeElement(
            element: Element<T>,
            newElement: Element<T>,
        ) {
            check(element.index == newElement.index)
            _elements[element.index] = newElement
        }

        fun changeSimple(
            element: Element.Simple<T>,
            newElement: Element<T>,
        ) {
            forceChangeElement(element, newElement)
        }

        fun moveElements(
            moveGroup: List<Element<T>>,
            position: Int,
        ) {
            if (moveGroup.isEmpty()) {
                return
            }
            var moveGroupNewFirstIndex = position.coerceIn(0, _elements.size - moveGroup.size)
            val elementAtNewPosition = _elements[moveGroupNewFirstIndex]
            moveGroupNewFirstIndex = shiftIndexIfNeeded(moveGroup, position, elementAtNewPosition, moveGroupNewFirstIndex)
            val minIndex = min(moveGroup.first().index, moveGroupNewFirstIndex)
            val maxIndex = max(moveGroup.last().index, (moveGroupNewFirstIndex + moveGroup.size).coerceAtMost(_elements.size - 1))

            val moveGroupIndices = moveGroup.map { it.index }.toSet()

            val saveElements = (minIndex..maxIndex).filter { it !in moveGroupIndices }.map { _elements[it] }
            val saveElementsBeforeCount = moveGroupNewFirstIndex - minIndex
            val saveElementsBeforeMoveGroup = saveElements.take(saveElementsBeforeCount)
            val saveElementsAfterMoveGroup = saveElements.drop(saveElementsBeforeCount)

            val changedElementsInterval = saveElementsBeforeMoveGroup + moveGroup + saveElementsAfterMoveGroup

            changedElementsInterval.forEachIndexed { i, element ->
                val newIndex = minIndex + i
                _elements[newIndex] = element
                element.index = newIndex
            }

            changedElementsInterval
                .map { element -> if (element is Element.UniteChild) element.root else element }
                .filterIsInstance<Element.UniteRoot<T>>()
                .distinct()
                .forEach { root ->
                    root.childrenIndicesChanged()
                }
        }

        // Shift index for "update-ref" entries, to not to add them into squashed groups
        private fun shiftIndexIfNeeded(
            moveGroup: List<Element<T>>,
            position: Int,
            elementAtNewPosition: Element<T>,
            moveGroupNewFirstIndex: Int,
        ): Int {
            val isGroupContainsUpdateRef = moveGroup.any { it.type == Type.NonUnite.UpdateRef }
            if (isGroupContainsUpdateRef) {
                val isMovingUp = moveGroup.first().index > position
                if (elementAtNewPosition is Element.UniteChild && isMovingUp) {
                    return elementAtNewPosition.root.index
                } else if (elementAtNewPosition is Element.UniteRoot && !isMovingUp) {
                    return elementAtNewPosition.lastChildIndex()
                }
            }
            return moveGroupNewFirstIndex
        }
    }

    /**
     * Type of action
     */
    sealed class Type(val command: IRGitEntry.KnownAction) {
        sealed class NonUnite(command: IRGitEntry.KnownAction) : Type(command) {
            sealed class KeepCommit(command: IRGitEntry.KnownAction) : NonUnite(command) {
                object Pick : KeepCommit(IRGitEntry.Action.PICK)

                object Edit : KeepCommit(IRGitEntry.Action.EDIT)

                class Reword(val newMessage: String) : KeepCommit(IRGitEntry.Action.REWORD)
            }

            object UpdateRef : NonUnite(IRGitEntry.Action.UPDATEREF)

            object Drop : NonUnite(IRGitEntry.Action.DROP)
        }

        object Unite : Type(IRGitEntry.Action.FIXUP)
    }

    sealed class Element<out T : IRGitEntry>(var index: Int, open val type: Type, val entry: T) {
        class Simple<T : IRGitEntry>(index: Int, override var type: Type.NonUnite, entry: T) : Element<T>(index, type, entry)

        class UniteRoot<T : IRGitEntry>(
            index: Int,
            override var type: Type.NonUnite.KeepCommit,
            entry: T,
        ) : Element<T>(index, type, entry) {
            private val _children = mutableListOf<UniteChild<T>>()
            val children: List<UniteChild<T>>
                get() = _children

            val uniteGroup
                get() = listOf(this) + _children

            fun addChild(child: UniteChild<T>) {
                check(child.index in this.index + 1..newChildIndex())
                if (child.index == newChildIndex()) {
                    _children.add(child)
                } else {
                    _children.add(child.index - index - 1, child)
                }
            }

            fun lastChildIndex() = _children.lastOrNull()?.index ?: index

            fun newChildIndex() = lastChildIndex() + 1

            fun removeChild(child: UniteChild<T>) {
                _children.remove(child)
            }

            fun childrenIndicesChanged() {
                _children.sortBy { it.index }
            }

            fun getUnitedCommitMessage(singleCommitMessageGetter: (T) -> String): String {
                return uniteGroup.map { element -> singleCommitMessageGetter(element.entry) }.toSet().joinToString("\n".repeat(3))
            }
        }

        class UniteChild<T : IRGitEntry>(index: Int, entry: T, val root: UniteRoot<T>) : Element<T>(index, Type.Unite, entry)
    }
}
