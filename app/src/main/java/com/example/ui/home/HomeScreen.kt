package com.example.ui.home

import androidx.compose.foundation.background
import com.example.ui.components.SupportAndDangerZoneSection
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SectionDayBadge
import com.example.ui.theme.GoldPillHighlight
import com.example.ui.theme.MutedSecondaryText
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel
import com.example.util.SubjectMapping
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeProfile by viewModel.activeStudentProfile.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val allAttendanceRecords by viewModel.allAttendanceRecords.collectAsState()

    val studentName = activeProfile?.fullName?.ifBlank { null }
        ?: viewModel.studentFullName.collectAsState().value.ifBlank { "Piyush Kumar" }
    val rollNumber = activeProfile?.rollNumber?.ifBlank { null }
        ?: viewModel.studentRollNumber.collectAsState().value.ifBlank { "2506028" }
    val phoneNumber = activeProfile?.phoneNumber?.ifBlank { null }
        ?: viewModel.studentPhoneNumber.collectAsState().value.ifBlank { "8210777536" }

    // Refresh history when viewing Student Home Screen
    LaunchedEffect(Unit) {
        viewModel.refreshAllHistoryRecords()
    }

    // Helper function for case-insensitive subject code prefix extraction
    fun parseSubjectCode(sessionId: String): String {
        return sessionId.split("_", "-", " ").firstOrNull()?.trim()?.uppercase()?.ifBlank { "CS030601" } ?: "CS030601"
    }

    // 1. Strict equality on Roll Number for logged-in user (DO NOT count by deviceId if rollNumber differs)
    val currentRoll = rollNumber.trim().uppercase()
    val studentRecords = allAttendanceRecords.filter { rec ->
        rec.studentRollNumber.trim().uppercase() == currentRoll
    }

    // 2. Group attendance records by Subject Code prefix case-insensitively
    val recordsGroupedBySubject = studentRecords.groupBy { rec ->
        parseSubjectCode(rec.sessionId)
    }

    // Default subject catalog covering ONLY official CSE Core courses, filtering out legacy codes
    val detectedSubjects = recordsGroupedBySubject.keys.filter { !SubjectMapping.isLegacyCode(it) }
    val subjectCatalog = (SubjectMapping.OFFICIAL_SUBJECT_CODES + detectedSubjects).distinct().sorted()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldPillHighlight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF281A00),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Student Hub",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("home_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Day Badge Header
            val currentDay = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                SectionDayBadge(text = currentDay)
            }

            // ==========================================
            // SECTION 1: MINIMAL PROFILE HEADER CARD
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_header_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Avatar Icon Box
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(GoldPillHighlight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = studentName.take(1).uppercase(Locale.getDefault()),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    ),
                                    color = Color(0xFF281A00)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = studentName,
                                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 20.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Roll Number Badge
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = GoldPillHighlight.copy(alpha = 0.9f),
                                        modifier = Modifier.testTag("roll_number_badge")
                                    ) {
                                        Text(
                                            text = "Roll: $rollNumber",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            ),
                                            color = Color(0xFF281A00),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Phone Number
                                    Text(
                                        text = if (phoneNumber.startsWith("+")) phoneNumber else "+91 $phoneNumber",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MutedSecondaryText,
                                        modifier = Modifier.testTag("phone_number_display")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 2: TOP QUICK ACTION CARD
            // "In Class Right Now? Tap to Scan"
            // ==========================================
            Card(
                onClick = { viewModel.navigateToScreen(AppScreen.STUDENT_SCAN) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("top_quick_action_scan_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan QR",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "In Class Right Now?",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Scan live classroom QR code for instant check-in",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.navigateToScreen(AppScreen.STUDENT_SCAN) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("tap_to_scan_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tap to Scan Now",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                    }
                }
            }

            // ==========================================
            // SECTION 3: SUBJECT BREAKDOWN & STATS
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionDayBadge(text = "Subject Attendance Breakdown")

                subjectCatalog.forEach { subCode ->
                    val targetSubCode = subCode.trim().uppercase()
                    val attendedCount = studentRecords.count { parseSubjectCode(it.sessionId) == targetSubCode }
                    val title = SubjectMapping.getSubjectTitle(subCode)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subject_card_$subCode"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Subject Code Badge
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = GoldPillHighlight,
                                    modifier = Modifier.testTag("subject_badge_$subCode")
                                ) {
                                    Text(
                                        text = subCode,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF281A00),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            lineHeight = 18.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        softWrap = true,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (attendedCount == 1) "1 Session Attended" else "$attendedCount Sessions Attended",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedSecondaryText
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Attendance Status Chip
                            val isSafe = attendedCount >= 2
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSafe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.testTag("status_chip_$subCode")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isSafe) MaterialTheme.colorScheme.primary else MutedSecondaryText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSafe) "Safe" else "75% Goal",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isSafe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 4: TIMETABLE & SCHEDULE CARD
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionDayBadge(text = "Class Routine")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schedule_container_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = "Calendar",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "Class Timetable",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Automated timetable & schedule sync",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedSecondaryText
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.testTag("coming_soon_tag")
                        ) {
                            Text(
                                text = "Coming Soon",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 5: ACCOUNT SUPPORT & DANGER ZONE
            // ==========================================
            SupportAndDangerZoneSection(
                onLogOut = { viewModel.performLogOut() }
            )

            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}
