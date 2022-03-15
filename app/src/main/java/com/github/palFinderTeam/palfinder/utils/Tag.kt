package com.github.palFinderTeam.palfinder.utils

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.github.palFinderTeam.palfinder.tag.*

fun createTagFragmentModel(
    that: ViewModelStoreOwner,
    tagsViewModelFactory: TagsViewModelFactory<Category>
): TagsViewModel<Category> {
    return ViewModelProvider(
        that,
        tagsViewModelFactory
    ).get(TagsViewModel::class.java) as TagsViewModel<Category>
}

fun addToFragmentManager(supportFragmentManager: FragmentManager, id: Int){
    supportFragmentManager.commit {
        setReorderingAllowed(true)
        add<TagsDisplayFragment<Category>>(id)
    }
}