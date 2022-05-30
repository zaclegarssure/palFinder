package com.github.palFinderTeam.palfinder.utils

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.github.palFinderTeam.palfinder.tag.Category
import com.github.palFinderTeam.palfinder.tag.TagsDisplayFragment
import com.github.palFinderTeam.palfinder.tag.TagsViewModel
import com.github.palFinderTeam.palfinder.tag.TagsViewModelFactory

/**
 * Create a [TagsViewModel] for the given [that] and [tagsViewModelFactory].
 */
fun createTagFragmentModel(
    that: ViewModelStoreOwner,
    tagsViewModelFactory: TagsViewModelFactory<Category>
): TagsViewModel<Category> {
    return ViewModelProvider(
        that,
        tagsViewModelFactory
    )[TagsViewModel::class.java] as TagsViewModel<Category>
}

/**
 * Adds supportFragmentManager to the [FragmentManager]
 */
fun addTagsToFragmentManager(supportFragmentManager: FragmentManager, id: Int){
    supportFragmentManager.commit {
        setReorderingAllowed(true)
        add<TagsDisplayFragment<Category>>(id)
    }
}