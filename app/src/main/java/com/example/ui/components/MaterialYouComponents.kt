package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldPillHighlight
import com.example.ui.theme.MutedSecondaryText
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.UserRole

// --- 1. FLOATING PILL NAVIGATION BAR ---
@Composable
fun FloatingPillNavigationBar(
    userRole: UserRole,
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .wrapContentWidth()
            .height(56.dp)
            .testTag("floating_pill_nav_bar"),
        shape = CircleShape, // Full stadium capsule shape matching reference image
        color = Color(0xFF4A3B2C), // Warm dark earth brown container background
        border = BorderStroke(1.dp, Color(0xFF5A4938)),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (userRole == UserRole.TEACHER) {
                // TEACHER FLOATING NAV PILL (3 Tabs Only)
                NavPillTab(
                    title = "Home",
                    isSelected = currentScreen == AppScreen.TEACHER_HOME,
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    onClick = { onScreenSelected(AppScreen.TEACHER_HOME) },
                    testTag = "nav_teacher_home_tab"
                )

                NavPillTab(
                    title = "Start Class",
                    isSelected = currentScreen == AppScreen.TEACHER_START_CLASS,
                    selectedIcon = Icons.Filled.PlayArrow,
                    unselectedIcon = Icons.Outlined.PlayArrow,
                    onClick = { onScreenSelected(AppScreen.TEACHER_START_CLASS) },
                    testTag = "nav_teacher_start_class_tab"
                )

                NavPillTab(
                    title = "Live Roster",
                    isSelected = currentScreen == AppScreen.TEACHER_ROSTER,
                    selectedIcon = Icons.Filled.People,
                    unselectedIcon = Icons.Outlined.People,
                    onClick = { onScreenSelected(AppScreen.TEACHER_ROSTER) },
                    testTag = "nav_teacher_roster_tab"
                )
            } else {
                // STUDENT FLOATING NAV PILL (3 Tabs Only)
                NavPillTab(
                    title = "Home",
                    isSelected = currentScreen == AppScreen.STUDENT_HOME,
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    onClick = { onScreenSelected(AppScreen.STUDENT_HOME) },
                    testTag = "nav_student_home_tab"
                )

                NavPillTab(
                    title = "Scan",
                    isSelected = currentScreen == AppScreen.STUDENT_SCAN,
                    selectedIcon = Icons.Filled.QrCodeScanner,
                    unselectedIcon = Icons.Outlined.QrCodeScanner,
                    onClick = { onScreenSelected(AppScreen.STUDENT_SCAN) },
                    testTag = "nav_student_scan_tab"
                )

                NavPillTab(
                    title = "Timetable",
                    isSelected = currentScreen == AppScreen.STUDENT_CLASSES,
                    selectedIcon = Icons.Filled.CalendarMonth,
                    unselectedIcon = Icons.Outlined.CalendarMonth,
                    onClick = { onScreenSelected(AppScreen.STUDENT_CLASSES) },
                    testTag = "nav_student_classes_tab"
                )
            }
        }
    }
}

@Composable
private fun NavPillTab(
    title: String,
    isSelected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFE5C39E) else Color.Transparent, // Warm light sand/gold pill
        animationSpec = tween(durationMillis = 250),
        label = "PillBgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF2C1A0E) else Color(0xFFE4C7B0), // Dark espresso text or light cream icon
        animationSpec = tween(durationMillis = 250),
        label = "PillContentColor"
    )

    Box(
        modifier = Modifier
            .testTag(testTag)
            .clip(CircleShape) // Stadium pill shape for active tab highlight
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(
                horizontal = if (isSelected) 16.dp else 12.dp,
                vertical = 8.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) selectedIcon else unselectedIcon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = contentColor
                )
            }
        }
    }
}

// --- 2. MATERIAL YOU SECTION CHIPS / DAY BADGES ---
@Composable
fun SectionDayBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = GoldPillHighlight,
    contentColor: Color = Color(0xFF281A00)
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp), // filled pill badge 16.dp
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

// --- 3. MATERIAL YOU CONTENT CARDS ---
@Composable
fun MaterialYouContentCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
fun MaterialYouContentCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector = Icons.Default.ChevronRight,
    onClick: (() -> Unit)? = null,
    badgeText: String? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp), // subtle rounded corners 16dp
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (badgeText != null) {
                    SectionDayBadge(
                        text = badgeText,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSecondaryText
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// --- 4. SUPPORT & DANGER ZONE SECTION ---
@Composable
fun SupportAndDangerZoneSection(
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionDayBadge(
            text = "Support & Danger Zone",
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 1. Contact Support Button (Static Card - No Action)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("contact_support_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Headset,
                        contentDescription = "Contact Support",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Contact Support",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Having trouble with check-ins or attendance?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSecondaryText
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 2. Danger Zone / Log Out Button (High Contrast Error Surface)
        Card(
            onClick = { onLogOut() },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("danger_zone_logout_button"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF3B0909) // Dark Muted Red Surface
            ),
            border = BorderStroke(1.dp, Color(0xFF8C1D18))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF601410)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Log Out",
                        tint = Color(0xFFFFDAD6),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Log Out",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = Color(0xFFFFDAD6)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Sign out of your current session",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF2B8B5)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFFFFDAD6),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
