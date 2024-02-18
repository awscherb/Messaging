@file:OptIn(ExperimentalMaterial3Api::class)

package com.awscherb.messaging.ui.base

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import com.awscherb.messaging.ui.theme.MessagingTheme


@Composable
fun ScaffoldScreen(
    title: String,
    navOnClick: () -> Unit,
    navIcon: ImageVector = Icons.Default.Menu,
    topBarActions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = floatingActionButton,
        bottomBar = bottomBar,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = title)
                },
                actions = topBarActions,
                navigationIcon = {
                    IconButton(onClick = { navOnClick() }) {
                        Icon(
                            navIcon,
                            contentDescription = "Menu",
                        )
                    }
                },

                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        content = { padding ->
            content(padding)
        }
    )
}


@Preview
@Composable
fun ScaffoldScreenSimplePreview() {
    MessagingTheme {
        ScaffoldScreen(title = "Title", navOnClick = { }) {

        }
    }
}

@Preview
@Composable
fun ScaffoldScreenSimplePreviewCustomBack() {
    MessagingTheme {
        ScaffoldScreen(
            title = "Title", navOnClick = { },
            navIcon = Icons.Default.ArrowBack
        ) {

        }
    }
}

@Preview
@Composable
fun ScaffoldScreenSingleMenuItem() {
    MessagingTheme {
        ScaffoldScreen(title = "Title",
            topBarActions = {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Info, "Info")
                }
            },
            navOnClick = { }) {

        }
    }
}

@Preview
@Composable
fun ScaffoldScreenSingleDoubleItem() {
    MessagingTheme {
        ScaffoldScreen(title = "Title",
            topBarActions = {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Info, "Info")
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Settings, "Settings")
                }
            },
            navOnClick = { }) {

        }
    }
}
