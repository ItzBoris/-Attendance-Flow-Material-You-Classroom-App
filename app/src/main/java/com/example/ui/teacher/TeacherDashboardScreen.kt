package com.example.ui.teacher

import com.example.ui.components.SupportAndDangerZoneSection

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AttendanceRecord
import com.example.ui.components.MaterialYouContentCard
import com.example.ui.components.SectionDayBadge
import com.example.ui.theme.GoldPillHighlight
import com.example.ui.theme.MutedSecondaryText
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel
import com.example.util.SubjectMapping
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =========================================================================
// 1. TEACHER HOME SCREEN (Tab 1: Teacher Profile, Quick Launch & Analytics)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherHomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeSession by viewModel.activeSession.collectAsState()
    val presentCount by viewModel.presentCount.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val allAttendanceRecords by viewModel.allAttendanceRecords.collectAsState()

    // 1. Extract subjects detected in sheet history + official course list
    val detectedSubjects = allAttendanceRecords.map { rec ->
        rec.sessionId.split("_").firstOrNull()?.trim()?.uppercase()?.ifBlank { "CS030601" } ?: "CS030601"
    }.filter { it.isNotBlank() && !SubjectMapping.isLegacyCode(it) }.distinct()

    val subjectChips = (SubjectMapping.OFFICIAL_SUBJECT_CODES + detectedSubjects).distinct().sorted()

    // 2. Calculate total check-ins recorded today
    val calendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val startOfToday = calendar.timeInMillis
    val todayCheckIns = allAttendanceRecords.count { it.scannedTimestamp >= startOfToday }

    // 3. Group past sessions by sessionCode (Column E e.g. "CS101_57661")
    val groupedSessions = allAttendanceRecords.groupBy { it.sessionId }

    Scaffold(
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
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = Color(0xFF281A00),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Teacher Portal",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("teacher_home_theme_toggle")
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
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            val currentDay = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                SectionDayBadge(text = currentDay)
            }

            // Teacher Profile Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("teacher_profile_card"),
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
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(GoldPillHighlight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "A",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                ),
                                color = Color(0xFF281A00)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Prof. Admin",
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 20.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Dept. of Computer Science & Engineering",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSecondaryText
                            )
                        }
                    }
                }
            }

            // ==========================================
            // FEATURE 1: QUICK LAUNCH SUBJECT CHIPS
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionDayBadge(text = "Quick Launch Subject Chips")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    subjectChips.forEach { subCode ->
                        val fullTitle = SubjectMapping.getSubjectTitle(subCode)
                        Card(
                            onClick = {
                                viewModel.selectSubject(subCode)
                                Toast.makeText(context, "Pre-filled $subCode ($fullTitle)", Toast.LENGTH_SHORT).show()
                                viewModel.navigateToScreen(AppScreen.TEACHER_START_CLASS)
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = GoldPillHighlight
                            ),
                            modifier = Modifier.testTag("quick_chip_$subCode")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFF281A00),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = subCode,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = Color(0xFF281A00)
                                    )
                                }
                                Text(
                                    text = fullTitle,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF281A00).copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // FEATURE 2: TOTAL TODAY STATS CARD
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_stats_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "Today's Attendance",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Total check-ins recorded today",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("today_checkins_count_badge")
                    ) {
                        Text(
                            text = "$todayCheckIns",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Active Session Status Banner
            if (activeSession != null && activeSession?.isActive == true) {
                val session = activeSession!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🔴 LIVE SESSION ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${session.courseName} (${session.subjectCode})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "Present: $presentCount",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.navigateToScreen(AppScreen.TEACHER_START_CLASS) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Live QR", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Button(
                                onClick = { viewModel.navigateToScreen(AppScreen.TEACHER_ROSTER) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Live Roster", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // FEATURE 3: RECENT SESSIONS LIST
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionDayBadge(text = "Recent Sessions History")

                if (groupedSessions.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = "No past sessions recorded yet. Start a class session to begin broadcasting.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSecondaryText,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                } else {
                    groupedSessions.entries.take(8).forEach { (sessionCode, recordsList) ->
                        val subCode = sessionCode.split("_").firstOrNull()?.ifBlank { "CS101" } ?: "CS101"
                        val studentCount = recordsList.size
                        val latestTime = recordsList.maxOfOrNull { it.scannedTimestamp } ?: System.currentTimeMillis()
                        val formattedDate = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(latestTime))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recent_session_card_$sessionCode"),
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
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = GoldPillHighlight,
                                        modifier = Modifier.testTag("session_badge_$sessionCode")
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

                                    Column {
                                        Text(
                                            text = sessionCode,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = formattedDate,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MutedSecondaryText
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = if (studentCount == 1) "1 Student" else "$studentCount Students",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Navigation Shortcuts
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionDayBadge(text = "Quick Actions")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        onClick = { viewModel.navigateToScreen(AppScreen.TEACHER_START_CLASS) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("teacher_quick_start_class"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Start Class",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Launch dynamic QR",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Card(
                        onClick = { viewModel.navigateToScreen(AppScreen.TEACHER_ROSTER) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("teacher_quick_live_roster"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Live Roster",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "View check-in table",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSecondaryText
                            )
                        }
                    }
                }
            }

            // System Overview Highlights
            MaterialYouContentCard(
                title = "Live Google Sheets Sync",
                subtitle = "Student scans are automatically broadcasted and recorded directly into your Google Sheets spreadsheet in real time.",
                badgeText = "Database Connection"
            )

            MaterialYouContentCard(
                title = "5-Second Dynamic Anti-Proxy QR",
                subtitle = "QR tokens automatically regenerate every 5 seconds with hardware verification to ensure zero proxy attendance.",
                badgeText = "Security Shield"
            )

            // Support & Danger Zone Section
            SupportAndDangerZoneSection(
                onLogOut = { viewModel.performLogOut() }
            )

            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}

// =========================================================================
// 2. TEACHER START CLASS SCREEN (Tab 2: Configure Class & Live Rolling QR)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherStartClassScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeSession by viewModel.activeSession.collectAsState()
    val subjectCode by viewModel.inputSubjectCode.collectAsState()
    val courseName by viewModel.inputCourseName.collectAsState()

    val liveQrBitmap by viewModel.liveQrBitmap.collectAsState()
    val countdownProgress by viewModel.countdownProgress.collectAsState()
    val presentCount by viewModel.presentCount.collectAsState()

    val isDarkMode by viewModel.isDarkMode.collectAsState()

    Scaffold(
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
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF281A00),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (activeSession?.isActive == true) "Live Rolling QR" else "Start Class",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("start_class_theme_toggle")
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
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (activeSession == null || activeSession?.isActive == false) {
                // --- FORM INPUTS TO CONFIGURE SESSION ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    val currentDay = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
                    SectionDayBadge(text = currentDay)
                }

                MaterialYouContentCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Configure Class Session",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 22.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Enter the subject name and code to start dynamic rolling QR broadcasting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSecondaryText,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = subjectCode,
                            onValueChange = { newCode ->
                                viewModel.inputSubjectCode.value = newCode
                                val mappedTitle = SubjectMapping.getSubjectTitle(newCode)
                                if (mappedTitle != newCode.trim().uppercase()) {
                                    viewModel.inputCourseName.value = mappedTitle
                                }
                            },
                            label = { Text("Class Code (e.g. CS030601)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("class_code_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Course Selector Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SubjectMapping.OFFICIAL_SUBJECT_CODES.forEach { code ->
                                Surface(
                                    onClick = { viewModel.selectSubject(code) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (subjectCode.equals(code, ignoreCase = true)) GoldPillHighlight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.testTag("form_course_chip_$code")
                                ) {
                                    Text(
                                        text = code,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (subjectCode.equals(code, ignoreCase = true)) Color(0xFF281A00) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = courseName,
                            onValueChange = { viewModel.inputCourseName.value = it },
                            label = { Text("Subject / Class Name (e.g. Data Structures)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("subject_name_input")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { viewModel.startAttendanceSession() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_session_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Session & Generate QR", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // --- DYNAMIC ROLLING QR DISPLAY SCREEN ---
                val session = activeSession!!

                // Session Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${session.courseName} (${session.subjectCode})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Session ID: ${session.sessionId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Present: $presentCount",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }

                // High Contrast QR Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "DYNAMIC ROLLING QR CODE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color(0xFF1E293B)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .size(230.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            if (liveQrBitmap != null) {
                                Image(
                                    bitmap = liveQrBitmap!!.asImageBitmap(),
                                    contentDescription = "Live Session Dynamic QR",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(80.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Countdown Progress Bar
                        Column(
                            modifier = Modifier.fillMaxWidth(0.85f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Rotates dynamic key...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Every 5s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { countdownProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.endAttendanceSession() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("end_session_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("End Session", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}

// =========================================================================
// 3. TEACHER ROSTER SCREEN (Tab 3: Real-time Check-In Table & CSV Export)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherRosterScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeSession by viewModel.activeSession.collectAsState()
    val liveRoster by viewModel.liveRoster.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val isSessionActive = activeSession != null && activeSession?.isActive == true
    val activeSessionId = activeSession?.sessionId

    // Filter live roster for active session only
    val activeRoster = if (isSessionActive && !activeSessionId.isNullOrBlank()) {
        liveRoster.filter { record ->
            record.sessionId.trim().equals(activeSessionId.trim(), ignoreCase = true) ||
            record.sessionId.trim().contains(activeSessionId.trim(), ignoreCase = true) ||
            activeSessionId.trim().contains(record.sessionId.trim(), ignoreCase = true)
        }
    } else {
        emptyList()
    }

    val presentCount = activeRoster.size

    Scaffold(
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
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = Color(0xFF281A00),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Live Roster",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("roster_theme_toggle")
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
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar with CSV Export Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Classroom Roster",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Real-time Google Sheets Sync",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSecondaryText
                    )
                }

                Button(
                    onClick = {
                        viewModel.getExportCsvIntent { shareIntent ->
                            if (shareIntent != null) {
                                context.startActivity(shareIntent)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    ),
                    modifier = Modifier.testTag("export_csv_button"),
                    enabled = isSessionActive
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export CSV", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!isSessionActive) {
                // ==========================================
                // IDLE STATE CARD (NO ACTIVE CLASS RUNNING)
                // ==========================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .testTag("idle_roster_empty_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(GoldPillHighlight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF281A00),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Text(
                            text = "No Active Class Session",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 22.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Text(
                            text = "Go to the 'Start Class' tab and tap 'Start Session' to generate a live QR code and begin tracking attendance.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSecondaryText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = { viewModel.navigateToScreen(AppScreen.TEACHER_START_CLASS) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("start_a_class_action_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start a Class",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }
                }
            } else {
                // ==========================================
                // ACTIVE SESSION LIVE ROSTER
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${activeSession?.courseName} (${activeSession?.subjectCode})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "Present: $presentCount",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Student Roster List
                if (activeRoster.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Waiting for students to scan live QR code...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                } else {
                    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = activeRoster,
                            key = { index, record ->
                                if (record.id != 0L) {
                                    "db_${record.id}_$index"
                                } else {
                                    "remote_${record.studentRollNumber}_${record.scannedTimestamp}_$index"
                                }
                            }
                        ) { _, record ->
                            StudentRosterCard(record = record, timeFormat = dateFormat)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.endAttendanceSession() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("end_session_roster_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("End Class Session", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}

@Composable
fun StudentRosterCard(
    record: AttendanceRecord,
    timeFormat: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = record.studentName.ifBlank { "Student" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Roll No: ${record.studentRollNumber.ifBlank { "N/A" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSecondaryText
                    )
                    if (record.deviceId.isNotBlank()) {
                        Text(
                            text = "Device ID: ${record.deviceId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSecondaryText.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Text(
                text = timeFormat.format(Date(record.scannedTimestamp)),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Wrapper for backward compatibility if referenced directly
@Composable
fun TeacherDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    TeacherHomeScreen(viewModel = viewModel, modifier = modifier)
}
