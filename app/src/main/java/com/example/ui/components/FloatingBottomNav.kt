package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    isKm: Boolean = true,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem(ScreenTab.HOME, Icons.Default.Home, if (isKm) "ទំព័រដើម" else "Home", "nav_home"),
        NavItem(ScreenTab.WORKOUT, Icons.Default.FitnessCenter, if (isKm) "ហាត់ប្រាណ" else "Workout", "nav_workout"),
        NavItem(ScreenTab.PLANS, Icons.AutoMirrored.Filled.EventNote, if (isKm) "គម្រោង" else "Plans", "nav_plans"),
        NavItem(ScreenTab.PROGRESS, Icons.Default.Timeline, if (isKm) "វឌ្ឍនភាព" else "Progress", "nav_progress"),
        NavItem(ScreenTab.PROFILE, Icons.Default.Person, if (isKm) "គណនី" else "Profile", "nav_profile")
    )

    val isDark = MaterialTheme.colorScheme.background == CharcoalBackground

    val navContainerColor = if (isDark) {
        Color(0xF0181A20)
    } else {
        Color(0xF8FFFFFF)
    }

    val navBorderColor = if (isDark) {
        Color(0x33D4F23A)
    } else {
        Color(0x1F000000)
    }

    val unselectedIconColor = if (isDark) {
        Color(0xFF8E95A5)
    } else {
        Color(0xFF64748B)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(28.dp)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(navContainerColor)
                .border(
                    width = 1.dp,
                    color = navBorderColor,
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            items.forEach { item ->
                val isSelected = item.tab == selectedTab

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(item.testTag)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onTabSelected(item.tab)
                        }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp, 28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) LimeAccent else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) CharcoalBackground else unselectedIconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) (if (isDark) LimeAccent else Color(0xFF1E293B)) else unselectedIconColor,
                            fontSize = 10.sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
