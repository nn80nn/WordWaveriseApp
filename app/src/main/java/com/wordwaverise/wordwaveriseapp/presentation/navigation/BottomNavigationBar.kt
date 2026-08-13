package com.wordwaverise.wordwaveriseapp.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.wordwaverise.wordwaveriseapp.ui.theme.*

@Composable
fun BottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val screens = listOf(
        Screen.Search,
        Screen.Saved,
        Screen.Tasks,
        Screen.Profile
    )

    val colors = WaveTheme.colors

    // A hairline instead of a drop shadow: the bar is a rule the content
    // stops at, not a slab floating over it.
    Column(modifier = modifier.fillMaxWidth().background(colors.surface)) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.hairline)
    )
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = colors.surface,
        tonalElevation = 0.dp
    ) {
        screens.forEach { screen ->
            val isSelected = currentRoute == screen.route

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Search.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // The source PNGs carry the old brand cyan, so they are
                        // re-tinted rather than shown as-is — otherwise the bar
                        // keeps the previous palette in both themes.
                        Icon(
                            painter = painterResource(id = screen.icon),
                            contentDescription = screen.title,
                            tint = if (isSelected) colors.secondary else colors.textMuted,
                            modifier = Modifier.size(23.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = screen.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = colors.secondary,
                    unselectedTextColor = colors.textMuted,
                    indicatorColor = colors.tag
                )
            )
        }
    }
    }
}
