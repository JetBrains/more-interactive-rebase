package com.jetbrains.interactiveRebase

import com.intellij.util.ui.EditableModel
import javax.swing.table.AbstractTableModel

internal class IRCommitsTable<T : IRGitEntry>(private val initialEntries: List<T>) : AbstractTableModel(), EditableModel {
    companion object {
        const val COMMIT_ICON_COLUMN = 0
        const val SUBJECT_COLUMN = 1
    }

    var rebaseTodoModel = createRebaseTodoModel()
        private set


    private fun createRebaseTodoModel(): IRGitModel<T> = convertToModel(initialEntries)

    private val savedStates = SavedStates(rebaseTodoModel.elements)

    internal fun <T : IRGitEntry> convertToModel(entries: List<T>): IRGitModel<T> {
        val result = mutableListOf<IRGitModel.Element<T>>()
        // consider auto-squash
        entries.forEach { entry ->
            val index = result.size
            when (entry.action) {
                IRGitEntry.Action.PICK, IRGitEntry.Action.REWORD -> {
                    val type = IRGitModel.Type.NonUnite.KeepCommit.Pick
                    result.add(IRGitModel.Element.Simple(index, type, entry))
                }
                IRGitEntry.Action.EDIT -> {
                    val type = IRGitModel.Type.NonUnite.KeepCommit.Edit
                    result.add(IRGitModel.Element.Simple(index, type, entry))
                }
                IRGitEntry.Action.DROP -> {
                    // move them to the end
                }
                IRGitEntry.Action.FIXUP, IRGitEntry.Action.SQUASH -> {
                    val lastElement = result.lastOrNull() ?: throw IllegalArgumentException("Couldn't unite with non-existed commit")
                    val root = when (lastElement) {
                        is IRGitModel.Element.UniteChild<T> -> lastElement.root
                        is IRGitModel.Element.UniteRoot<T> -> lastElement
                        is IRGitModel.Element.Simple<T> -> {
                            when (val rootType = lastElement.type) {
                                is IRGitModel.Type.NonUnite.KeepCommit -> {
                                    val newRoot = IRGitModel.Element.UniteRoot(lastElement.index, rootType, lastElement.entry)
                                    result[newRoot.index] = newRoot
                                    newRoot
                                }
                                is IRGitModel.Type.NonUnite.Drop, is IRGitModel.Type.NonUnite.UpdateRef -> {
                                    throw IllegalStateException()
                                }
                            }
                        }
                    }
                    val element = IRGitModel.Element.UniteChild(index, entry, root)
                    root.addChild(element)
                    result.add(element)
                }
                IRGitEntry.Action.UPDATE_REF -> {
                    val type = IRGitModel.Type.NonUnite.UpdateRef
                    val element = IRGitModel.Element.Simple(index, type, entry)
                    result.add(element)
                }
                is IRGitEntry.Action.Other -> throw IllegalArgumentException("Couldn't convert unknown action to the model")
            }
        }
        entries.filter { it.action is IRGitEntry.Action.DROP }.forEach { entry ->
            val index = result.size
            result.add(IRGitModel.Element.Simple(index, IRGitModel.Type.NonUnite.Drop, entry))
        }
        return IRGitModel(result)
    }


    internal fun <T : IRGitEntry> IRGitModel<out T>.convertToEntries(): List<IRGitEntry> = elements.map { element ->
        val entry = element.entry
        IRGitEntry(element.type.command, entry.commit, entry.subject)
    }

    fun updateModel(f: (IRGitModel<T>) -> Unit) {
        f(rebaseTodoModel)
        savedStates.addState(rebaseTodoModel.elements)
        fireTableRowsUpdated(0, rowCount)
    }

    fun resetEntries() {
        rebaseTodoModel = createRebaseTodoModel()
        savedStates.addState(rebaseTodoModel.elements)
        fireTableRowsUpdated(0, rowCount)
    }

    override fun getRowCount() = rebaseTodoModel.elements.size

    override fun getColumnCount() = SUBJECT_COLUMN + 1

    override fun getValueAt(rowIndex: Int, columnIndex: Int): T = getEntry(rowIndex)

    override fun exchangeRows(oldIndex: Int, newIndex: Int) {
        updateModel { rebaseTodoModel ->
            rebaseTodoModel.exchangeIndices(oldIndex, newIndex)
        }
    }

    override fun canExchangeRows(oldIndex: Int, newIndex: Int) = true

    override fun removeRow(idx: Int) {
        throw UnsupportedOperationException()
    }

    override fun addRow() {
        throw UnsupportedOperationException()
    }

    override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
        if (aValue is String) {
            val commitMessage = getEntry(rowIndex).getFullCommitMessage() ?: throw IllegalStateException()
            if (aValue == commitMessage) {
                rebaseTodoModel.pick(listOf(rowIndex))
            }
            else {
                rebaseTodoModel.reword(rowIndex, aValue)
            }
        }
    }

    fun getEntry(row: Int): T = rebaseTodoModel.elements[row].entry

    override fun isCellEditable(rowIndex: Int, columnIndex: Int) = columnIndex == SUBJECT_COLUMN && rebaseTodoModel.canReword(rowIndex)

    fun getElement(row: Int): IRGitModel.Element<T> = rebaseTodoModel.elements[row]

    fun isFirstFixup(child: IRGitModel.Element.UniteChild<*>) = child === child.root.children.first()

    fun isLastFixup(child: IRGitModel.Element.UniteChild<*>) = child === child.root.children.last()

    fun getCommitMessage(row: Int): String {
        val elementType = getElement(row).type
        return if (elementType is IRGitModel.Type.NonUnite.KeepCommit.Reword) {
            elementType.newMessage
        }
        else {
            getEntry(row).getFullCommitMessage() ?: throw IllegalStateException()
        }
    }

    fun getPresentation(row: Int): String {
        val elementType = getElement(row).type
        return if (elementType is IRGitModel.Type.NonUnite.KeepCommit.Reword) {
            elementType.newMessage
        }
        else {
            val entry = getEntry(row)
            entry.getFullCommitMessage() ?: "${entry.action.command} ${entry.commit}"
        }
    }

    fun undo() {
        savedStates.prevState()?.let {
            rebaseTodoModel = IRGitModel(it)
        }
        fireTableRowsUpdated(0, rowCount)
    }

    fun redo() {
        savedStates.nextState()?.let {
            rebaseTodoModel = IRGitModel(it)
        }
        fireTableRowsUpdated(0, rowCount)
    }

    private class SavedStates<T : IRGitEntry>(initialState: List<IRGitModel.Element<T>>) {
        companion object {
            private const val MAX_SIZE = 10
        }

        private var currentState = 0
        private val states = mutableListOf(copyElements(initialState))

        private fun checkBoundsAndGetState(): List<IRGitModel.Element<T>>? {
            if (currentState !in states.indices) {
                currentState = currentState.coerceIn(states.indices)
                return null
            }
            return copyElements(states[currentState])
        }

        fun prevState(): List<IRGitModel.Element<T>>? {
            currentState--
            return checkBoundsAndGetState()
        }

        fun nextState(): List<IRGitModel.Element<T>>? {
            currentState++
            return checkBoundsAndGetState()
        }

        fun addState(newState: List<IRGitModel.Element<T>>) {
            currentState++
            if (currentState == MAX_SIZE) {
                currentState = MAX_SIZE - 1
                states.removeAt(0)
            }
            while (states.lastIndex != currentState - 1) {
                states.removeAt(states.lastIndex)
            }
            states.add(currentState, copyElements(newState))
        }

        private fun copyElements(elements: List<IRGitModel.Element<T>>): List<IRGitModel.Element<T>> {
            val result = mutableListOf<IRGitModel.Element<T>>()
            elements.forEach { elementToCopy ->
                when (elementToCopy) {
                    is IRGitModel.Element.Simple -> {
                        result.add(IRGitModel.Element.Simple(elementToCopy.index, elementToCopy.type, elementToCopy.entry))
                    }
                    is IRGitModel.Element.UniteRoot -> {
                        result.add(IRGitModel.Element.UniteRoot(elementToCopy.index, elementToCopy.type, elementToCopy.entry))
                    }
                    is IRGitModel.Element.UniteChild -> {
                        val rootIndex = elementToCopy.root.index
                        val root = result[rootIndex] as IRGitModel.Element.UniteRoot<T>
                        val child = IRGitModel.Element.UniteChild(elementToCopy.index, elementToCopy.entry, root)
                        root.addChild(child)
                        result.add(child)
                    }
                }
            }
            return result
        }
    }
}