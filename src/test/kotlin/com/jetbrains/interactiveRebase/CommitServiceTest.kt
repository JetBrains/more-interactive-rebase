package com.jetbrains.interactiveRebase

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.services.MyProjectService
import com.jetbrains.interactiveRebase.services.CommitService
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class CommitServiceTest : BasePlatformTestCase() {
    private lateinit var service : CommitService
    private lateinit var project : Project

    @Before
    fun setup() {
        project = mock(Project::class.java)
        service = CommitService(project)
    }

    @Test
    fun testGetOnlyDisplayable() {

    }

    @Test
    fun testRename() {
        myFixture.testRename("foo.xml", "foo_after.xml", "a2")
    }

    @Test
    fun testProjectService() {
        val projectService = project.service<MyProjectService>()

        Assert.assertNotSame(projectService.getRandomNumber(), projectService.getRandomNumber())
    }

    override fun getTestDataPath() = "src/test/testData/rename"
}
