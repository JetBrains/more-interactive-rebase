package com.jetbrains.interactiveRebase.visuals.multipleBranches

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.jetbrains.interactiveRebase.visuals.Palette
import com.jetbrains.interactiveRebase.visuals.RoundedButton
import java.awt.Color
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JLabel

class SideBranchPanel(val branchName: String): RoundedPanel(), Disposable {
    var isSelected: Boolean = false
    lateinit var label: JLabel
    lateinit var button: RoundedButton

    init {
        backgroundColor = background
        cornerRadius = 15
        createSideBranchPanel()
    }

    internal fun createSideBranchPanel() {
        layout = GridBagLayout()
        addSideBranchLabel()
        addRemoveBranchButton()
    }

    internal fun addSideBranchLabel(){
        label = JBLabel(branchName)

        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.9
        gbc.anchor = GridBagConstraints.LINE_START
        gbc.insets = JBUI.insets(5, 20, 5, 5)

        add(label, gbc)
    }

    internal fun addRemoveBranchButton(){
        button = RoundedButton("", Color(0,0,0,0), Color(0,0,0,0))
//        button.action = RemoveBranchAction()
        button.arcHeight = 5
        button.arcWidth = 5
        button.icon = AllIcons.General.Remove
        val iconSize = button.icon?.let { Dimension((1.4 * it.iconWidth).toInt(), (1.4 * it.iconHeight).toInt()) }

        val gbc = GridBagConstraints()
        gbc.gridx = 1
        gbc.weightx = 0.1
        gbc.anchor = GridBagConstraints.LINE_END
        gbc.insets = JBUI.insetsRight(10)
        button.border = null
        button.preferredSize = iconSize
        button.isOpaque = false
        button.setContentAreaFilled(false)

        button.isVisible = false
        add(button, gbc)
    }

    internal fun onHover(){
        this.isOpaque = true
        backgroundColor = Palette.GRAYBUTTON
        this.repaint()
        this.revalidate()
    }

    internal fun resetSideBranchPanelVisually(){
        this.isOpaque = false
        this.isSelected = false
        this.button.isVisible = false
        this.label.foreground = JBColor.BLACK
        backgroundColor = Color(0,0,0,0)
        this.repaint()
        this.revalidate()
    }

    internal fun selectBranch(){
        this.isOpaque = true
        backgroundColor = Palette.DARKSHADOW
        this.isSelected = true
        this.button.isVisible = true
        this.repaint()
        this.revalidate()
    }

    internal fun grayOutText(){
        this.label.foreground = Palette.GRAYBUTTON
    }

    internal fun buttonOnHover(){
//       button.isOpaque = true
        button.backgroundColor = Palette.DARKSHADOW.darker()
        button.repaint()
        button.revalidate()
    }
    internal fun buttonOnHoverExit(){
        button.isOpaque = false
        button.border = null
        button.isOpaque = false
        button.setContentAreaFilled(false)
        button.backgroundColor = Color(0,0,0,0)
        button.repaint()
        button.revalidate()
    }

    override fun dispose() {
    }


}