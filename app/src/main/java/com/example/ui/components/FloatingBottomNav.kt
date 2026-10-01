package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.LimeAccent
import com.example.ui.viewmodel.ScreenTab

data class NavItem(
    val tab: ScreenTab,
    val icon: ImageVector,
    val label: String,
    val testTag: String
)

@Composable
fun FloatingBottomNav(
    selectedTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem(ScreenTab.HOME, Icons.Default.Home, "Home", "nav_home"),
        NavItem(ScreenTab.WORKOUT, Icons.Default.FitnessCenter, "Workout", "nav_workout"),
        NavItem(ScreenTab.PLANS, Icons.Default.EventNote, "Plans", "nav_plans"),
        NavItem(ScreenTab.PROGRESS, Icons.Default.Timeline, "Progress", "nav_progress"),
        NavItem(ScreenTab.PROFILE, Icons.Default.Person, "Profile", "nav_profile")
    )

    val isDark = MaterialTheme.colorScheme.background == CharcoalBackground

    // Glassy floating island container styling
    val navContainerColor = if (isDark) {
        Color(0xF0181A20) // Deep sleek glassy charcoal
    } else {
        Color(0xF8FFFFFF) // Crisp luminous floating white
    }

    val navBorderColor = if (isDark) {
        Color(0x33D4F23A) // Subtle neon lime border glow
    } else {
        Color(0x1F000000) // Crisp light border
    }

    val unselectedIconColor = if (isDark) {
        Color(0xFF9EA3AE)
    } else {
        Color(0xFF64748B)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(36.dp)
                )
                .clip(RoundedCornerShape(36.dp))
                .background(navContainerColor)
                .border(
                    width = 1.dp,
                    color = navBorderColor,
                    shape = RoundedCornerShape(36.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                val isSelected = item.tab == selectedTab
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.02f else 1.0f,
                    animationSpec = androidx.compose.animation.core.tween(100),
                    label = "tab_scale"
                )

                Row(
                    modifier = Modifier
                        .testTag(item.testTag)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(if (isSelected) LimeAccent else Color.Transparent)
                        .clickable {
                            onTabSelected(item.tab)
                        }
                        .padding(horizontal = if (isSelected) 14.dp else 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) CharcoalBackground else unselectedIconColor,
                        modifier = Modifier.size(22.dp)
                    )

                    AnimatedVisibility(
                        visible = isSelected,
                        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                expandHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        Row {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalBackground,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}
