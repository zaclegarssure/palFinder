package com.github.palFinderTeam.palfinder.tag

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TagsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: TagsViewModel<TestTags>
    private lateinit var fakeRepository: TestRepository


    @Before
    fun setup() {
        fakeRepository = TestRepository()
        viewModel = TagsViewModel(fakeRepository)
    }

    @Test
    fun `when adding and removing tags make sure it is indeed visible`() {
        viewModel.addTag(TestTags.TEST_1)
        viewModel.addTag(TestTags.TEST_2)
        assertThat(viewModel.tagContainer.value, `hasItems`(TestTags.TEST_1, TestTags.TEST_2))
        viewModel.removeTag(TestTags.TEST_1)
        assertThat(viewModel.tagContainer.value, not(`hasItems`(TestTags.TEST_1)))
    }

    @Test
    fun `cannot add tag when immutable`() {
        fakeRepository._isEditable = false
        viewModel.addTag(TestTags.TEST_1)
        assertThat(viewModel.tagContainer.value, not(hasItem(TestTags.TEST_1)))
    }

    @Test
    fun `cannot remove tag when immutable`() {
        viewModel.addTag(TestTags.TEST_1)
        fakeRepository._isEditable = false
        viewModel.removeTag(TestTags.TEST_1)
        assertThat(viewModel.tagContainer.value, hasItem(TestTags.TEST_1))
    }
}

enum class TestTags(override val tagName: String) : Tag {
    TEST_0("test_0"),
    TEST_1("test_1"),
    TEST_2("test_2"),
    TEST_3("test_3"),
    TEST_4("test_4"),
    TEST_5("test_5"),
}

class TestRepository : TagsRepository<TestTags> {
    private var _tags = mutableSetOf<TestTags>()

    var _isEditable = true

    override val tags = _tags
    override val isEditable
        get() = _isEditable
    override val allTags = TestTags.values().toSet()

    override fun removeTag(tag: TestTags): Boolean {
        return _tags.remove(tag)
    }

    override fun addTag(tag: TestTags): Boolean {
        return _tags.add(tag)
    }

}