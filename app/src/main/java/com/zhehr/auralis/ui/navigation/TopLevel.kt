package com.zhehr.auralis.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.zhehr.auralis.R

enum class TopLevel(val route: String, @StringRes val label: Int, @DrawableRes val icon: Int) {
    Home("home", R.string.nav_home, R.drawable.ic_home),
    Library("library", R.string.nav_library, R.drawable.ic_library),
    Search("search", R.string.nav_search, R.drawable.ic_search),
    Settings("settings", R.string.nav_settings, R.drawable.ic_settings),
}
