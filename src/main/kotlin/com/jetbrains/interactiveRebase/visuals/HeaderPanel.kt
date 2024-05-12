
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout
import java.awt.Graphics
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent

class HeaderPanel( private val mainPanel: JComponent) : JBPanel<JBPanel<*>>() {
    val gitActionsPanel = JBPanel<JBPanel<*>>()
    val changeActionsPanel = JBPanel<JBPanel<*>>()

    init {
        gitActionsPanel.layout = BoxLayout(gitActionsPanel, BoxLayout.X_AXIS)
        addGitButtons(gitActionsPanel)

        changeActionsPanel.layout = BoxLayout(changeActionsPanel, BoxLayout.X_AXIS)
        addChangeButtons(changeActionsPanel)
    }

    override fun paintComponent(g: Graphics?) {
        this.layout = BorderLayout()
        super.paintComponent(g)
        this.add(gitActionsPanel, BorderLayout.WEST)
        this.add(changeActionsPanel, BorderLayout.EAST)
    }

    /**
     * Add git action buttons to the header panel.
     * At the moment, the buttons are hardcoded, but we will replace them with icons and listeners later.
     */
    private fun addGitButtons(buttonPanel: JBPanel<JBPanel<*>>) {
        val squashButton = JButton("Squash")
        val fixupButton = JButton("Stop to edit")
        val rewordButton = JButton("Reword")
        val dropButton = JButton("Drop")

        buttonPanel.add(squashButton)
        buttonPanel.add(fixupButton)
        buttonPanel.add(rewordButton)
        buttonPanel.add(dropButton)

       // rewordButton.addActionListener{ IRGitRebaseUtils(project).interactivelyRebaseUsingLog()}

    }

    /**
     * Add change action buttons to the header panel.
     * At the moment, the buttons are hardcoded, but we will replace them with icons and listeners later.
     */
    private fun addChangeButtons(buttonPanel: JBPanel<JBPanel<*>>) {
        val pickButton = JButton("Pick")
        val resetButton = JButton("Reset")

        buttonPanel.add(pickButton)
        buttonPanel.add(resetButton)
    }
}
