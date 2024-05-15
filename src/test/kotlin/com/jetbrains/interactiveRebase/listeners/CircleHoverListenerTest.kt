package com.jetbrains.interactiveRebase.listeners

<<<<<<< HEAD
import CirclePanel
import com.intellij.mock.MockVirtualFile
=======
>>>>>>> efcefae (Rebase connection established)
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.vcs.log.Hash
import com.intellij.vcs.log.VcsUser
import com.intellij.vcs.log.VcsUserRegistry
import com.intellij.vcs.log.impl.VcsUserImpl
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.visuals.CirclePanel
import git4idea.GitCommit
import git4idea.history.GitCommitRequirements
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D

class CircleHoverListenerTest : BasePlatformTestCase() {
    private lateinit var circlePanel: CirclePanel
    private lateinit var listener: CircleHoverListener
    private lateinit var commit1: CommitInfo

    override fun setUp() {
        super.setUp()
        circlePanel = mock(CirclePanel::class.java)
        listener = CircleHoverListener(circlePanel)
        commit1 = CommitInfo(createCommit("my commit"), project, null)
    }

    fun testMouseEnteredInsideCircle() {
        val event = mock(MouseEvent::class.java)
        `when`(event.x).thenReturn(10)
        `when`(event.y).thenReturn(10)
        `when`(circlePanel.circle).thenReturn(Ellipse2D.Double(0.0, 0.0, 20.0, 20.0))
        `when`(circlePanel.commit).thenReturn(commit1)

        listener.mouseEntered(event)

        verify(circlePanel).repaint()
        assertThat(circlePanel.commit.isHovered).isTrue()
    }

    fun testMouseEnteredOutsideCircle() {
        val event = mock(MouseEvent::class.java)
        `when`(event.x).thenReturn(10)
        `when`(event.y).thenReturn(10)
        `when`(circlePanel.circle).thenReturn(Ellipse2D.Double(10.0, 10.0, 20.0, 20.0))

        listener.mouseEntered(event)
        verify(circlePanel, never()).repaint()
    }

    fun testMouseEnteredNullEvent() {
        val event = mock(MouseEvent::class.java)
        `when`(event.x).thenReturn(10)
        `when`(event.y).thenReturn(10)
        `when`(circlePanel.circle).thenReturn(Ellipse2D.Double(10.0, 10.0, 20.0, 20.0))

        listener.mouseEntered(null)
        verify(circlePanel, never()).repaint()
    }

    fun testMouseExitedOutsideCircle() {
        val event = mock(MouseEvent::class.java)
        `when`(event.x).thenReturn(10)
        `when`(event.y).thenReturn(10)
        `when`(circlePanel.circle).thenReturn(Ellipse2D.Double(10.0, 10.0, 20.0, 20.0))
        `when`(circlePanel.commit).thenReturn(commit1)

        listener.mouseExited(event)

        assertThat(circlePanel.commit.isHovered).isFalse()
        verify(circlePanel).repaint()
    }

    fun testMouseExitedInsideCircle() {
        val event = mock(MouseEvent::class.java)
        `when`(event.x).thenReturn(10)
        `when`(event.y).thenReturn(10)
        `when`(circlePanel.circle).thenReturn(Ellipse2D.Double(0.0, 0.0, 20.0, 20.0))
        `when`(circlePanel.commit).thenReturn(commit1)
        listener.mouseExited(event)
        verify(circlePanel, never()).repaint()
    }

    fun testMouseExitedNullEvent() {
        val event = mock(MouseEvent::class.java)
        `when`(event.x).thenReturn(10)
        `when`(event.y).thenReturn(10)
        `when`(circlePanel.circle).thenReturn(Ellipse2D.Double(0.0, 0.0, 20.0, 20.0))
        listener.mouseExited(null)
        verify(circlePanel, never()).repaint()
    }

    fun testMouseClicked() {
        `when`(circlePanel.commit).thenReturn(commit1)
        listener.mouseClicked(null)
        assertThat(circlePanel.commit.isSelected).isTrue()
        verify(circlePanel).repaint()
    }

    fun testMouseMovedInsideCircle() {
        val event = mock(MouseEvent::class.java)
        `when`(event.x).thenReturn(10)
        `when`(event.y).thenReturn(10)
        `when`(circlePanel.circle).thenReturn(Ellipse2D.Double(0.0, 0.0, 20.0, 20.0))
        `when`(circlePanel.commit).thenReturn(commit1)
        listener.mouseMoved(event)

        assertThat(circlePanel.commit.isHovered).isTrue()
        verify(circlePanel).repaint()
    }

    fun testMouseMovedOutsideCircle() {
        val event = mock(MouseEvent::class.java)
        `when`(event.x).thenReturn(100)
        `when`(event.y).thenReturn(100)
        `when`(circlePanel.circle).thenReturn(Ellipse2D.Double(10.0, 10.0, 20.0, 20.0))
        `when`(circlePanel.commit).thenReturn(commit1)
        listener.mouseMoved(event)

        assertThat(circlePanel.commit.isHovered).isFalse()
        verify(circlePanel).repaint()
    }

    fun testUnsupportedOperations() {
        val event = mock(MouseEvent::class.java)

        listOf(
            { listener.mousePressed(event) },
            { listener.mouseReleased(event) },
            { listener.mouseDragged(event) },
        ).forEach { testOperation ->
            try {
                testOperation.invoke()
                Assert.fail("Expected UnsupportedOperationException was not thrown")
            } catch (e: UnsupportedOperationException) {
                // The expected behavior of these dummy methods is to do nothing other than throw an exception.
            }
        }
    }

    private fun createCommit(subject: String): GitCommit {
        val author = MockVcsUserRegistry().users.first()
        val hash = MockHash()
        val root = MockVirtualFile("mock name")
        val message = "example long commit message"
        val commitRequirements = GitCommitRequirements()
        return GitCommit(project, hash, listOf(), 1000L, root, subject, author, message, author, 1000L, listOf(), commitRequirements)
    }

    private class MockVcsUserRegistry : VcsUserRegistry {
        override fun getUsers(): MutableSet<VcsUser> {
            return mutableSetOf(
                createUser("abc", "abc@goodmail.com"),
                createUser("aaa", "aaa@badmail.com"),
            )
        }

        override fun createUser(
            name: String,
            email: String,
        ): VcsUser {
            return VcsUserImpl(name, email)
        }
    }

    private class MockHash : Hash {
        override fun asString(): String {
            return "exampleHash"
        }

        override fun toShortString(): String {
            return "exampleShortHash"
        }
    }
}
