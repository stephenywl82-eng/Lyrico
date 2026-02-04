package com.lonx.lyrico.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.lonx.lyrico.BuildConfig
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.ItemArrowType
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.icons.ArrowBack
import com.moriafly.salt.ui.icons.SaltIcons
import com.moriafly.salt.ui.rememberScrollState
import com.moriafly.salt.ui.verticalScroll
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Destination<RootGraph>(route = "about")
fun AboutScreen(
    navigator: DestinationsNavigator
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(SaltTheme.colors.background),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarColors(
                    containerColor = SaltTheme.colors.background,
                    scrolledContainerColor = SaltTheme.colors.background,
                    navigationIconContentColor = SaltTheme.colors.text,
                    titleContentColor = SaltTheme.colors.text,
                    actionIconContentColor = SaltTheme.colors.text,
                    subtitleContentColor = SaltTheme.colors.subText
                ),
                title = {
                    Text(
                        text = "关于",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(imageVector = SaltIcons.ArrowBack, contentDescription = null)
                    }
                },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SaltTheme.colors.background)
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            RoundedColumn {
                Item(
                    text = "应用版本",
                    sub = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})",
                    onClick = {},
                    arrowType = ItemArrowType.None
                )
                Item(
                    text = "项目地址",
                    sub = "在 GitHub 上查看项目源码",
                    onClick = {
                        openBrowser(context, "https://github.com/replica0110/Lyrico")
                    },
                    arrowType = ItemArrowType.Link
                )
            }
            Spacer(modifier = Modifier.height(SaltTheme.dimens.padding))
        }
    }
}
private fun openBrowser(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}