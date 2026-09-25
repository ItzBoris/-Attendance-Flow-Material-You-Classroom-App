package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AttendanceRecord
import com.example.data.local.AttendanceSession
import com.example.data.local.StudentProfile
import com.example.data.repository.AttendanceRepository
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class UserRole {
    NONE, TEACHER, STUDENT
}

enum class AppScreen {
    AUTH,
    STUDENT_HOME,
    STUDENT_SCAN,
    STUDENT_CLASSES,
    TEACHER_HOME,
    TEACHER_START_CLASS,
    TEACHER_ROSTER
}

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Loading : ScanUiState()
    data class Success(
        val rollNumber: String,
        val name: String,
        val timestampMs: Long,
        val sessionId: String = "",
        val message: String = "✅ Attendance Recorded Successfully!"
    ) : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AttendanceRepository(
        dao = AppDatabase.getInstance(application).attendanceDao(),
        context = application
    )

    // Current Navigation / Screen State
    private val _currentScreen = MutableStateFlow(AppScreen.AUTH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = _currentScreen
        .map { it != AppScreen.AUTH }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _currentRole = MutableStateFlow(UserRole.NONE)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    fun navigateToScreen(screen: AppScreen) {
        _currentScreen.value = screen
        when (screen) {
            AppScreen.AUTH -> _currentRole.value = UserRole.NONE
            AppScreen.STUDENT_HOME, AppScreen.STUDENT_SCAN, AppScreen.STUDENT_CLASSES -> _currentRole.value = UserRole.STUDENT
            AppScreen.TEACHER_HOME, AppScreen.TEACHER_START_CLASS, AppScreen.TEACHER_ROSTER -> _currentRole.value = UserRole.TEACHER
        }
    }

    fun switchRole(role: UserRole) {
        _currentRole.value = role
        when (role) {
            UserRole.NONE -> _currentScreen.value = AppScreen.AUTH
            UserRole.STUDENT -> _currentScreen.value = AppScreen.STUDENT_HOME
            UserRole.TEACHER -> _currentScreen.value = AppScreen.TEACHER_HOME
        }
    }

    // Dynamic M3 Theme Mode State
    val isDarkMode = MutableStateFlow(true)

    fun toggleDarkMode() {
        isDarkMode.value = !isDarkMode.value
    }

    // Active Student Profile from DB
    val activeStudentProfile: StateFlow<StudentProfile?> = repository.activeStudentProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active Teacher Session from DB
    val activeSession: StateFlow<AttendanceSession?> = repository.activeSessionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Teacher Login Form State
    var teacherUsername = MutableStateFlow("")
    var teacherPassword = MutableStateFlow("")
    val teacherLoginError = MutableStateFlow<String?>(null)

    // Student Reg Form State (No OTP)
    var studentFullName = MutableStateFlow("")
    var studentRollNumber = MutableStateFlow("")
    var studentPhoneNumber = MutableStateFlow("")
    val studentRegError = MutableStateFlow<String?>(null)

    // Teacher Dashboard Session Inputs
    var inputSubjectCode = MutableStateFlow("CS030601")
    var inputCourseName = MutableStateFlow("Data Structures")

    fun selectSubject(code: String) {
        val cleanCode = code.trim().uppercase()
        inputSubjectCode.value = cleanCode
        inputCourseName.value = com.example.util.SubjectMapping.getSubjectTitle(cleanCode)
    }

    // Dynamic QR Generator State
    private val _liveQrBitmap = MutableStateFlow<Bitmap?>(null)
    val liveQrBitmap: StateFlow<Bitmap?> = _liveQrBitmap.asStateFlow()

    private val _currentQrToken = MutableStateFlow<String>("")
    val currentQrToken: StateFlow<String> = _currentQrToken.asStateFlow()

    private val _countdownProgress = MutableStateFlow(1.0f) // 1.0 down to 0.0 every 3s
    val countdownProgress: StateFlow<Float> = _countdownProgress.asStateFlow()

    // Live Roster and Present Count
    private val _liveRoster = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val liveRoster: StateFlow<List<AttendanceRecord>> = _liveRoster.asStateFlow()

    private val _presentCount = MutableStateFlow(0)
    val presentCount: StateFlow<Int> = _presentCount.asStateFlow()

    // All Attendance Records across all sessions (for Home Screen stats)
    private val _allAttendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val allAttendanceRecords: StateFlow<List<AttendanceRecord>> = _allAttendanceRecords.asStateFlow()

    private var historyFetchJob: Job? = null

    // Student Scanner UI State
    private val _scanUiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanUiState: StateFlow<ScanUiState> = _scanUiState.asStateFlow()

    private var qrLoopJob: Job? = null
    private var rosterJob: Job? = null

    init {
        // Observe student profile changes to prefill state and set initial role if registered
        viewModelScope.launch {
            activeStudentProfile.collect { profile ->
                if (profile != null) {
                    studentFullName.value = profile.fullName
                    studentRollNumber.value = profile.rollNumber
                    studentPhoneNumber.value = profile.phoneNumber
                    if (_currentScreen.value == AppScreen.AUTH) {
                        _currentScreen.value = AppScreen.STUDENT_HOME
                        _currentRole.value = UserRole.STUDENT
                    }
                }
            }
        }

        // Observe active session changes to manage dynamic QR generator and live roster
        viewModelScope.launch {
            activeSession.collectLatest { session ->
                if (session != null && session.isActive) {
                    startDynamicQrTimer(session.sessionId)
                    observeRosterForSession(session.sessionId)
                } else {
                    stopDynamicQrTimer()
                }
            }
        }

        // Periodic background fetch for global attendance history (Google Sheets + Local DB)
        viewModelScope.launch {
            while (true) {
                if (isLoggedIn.value) {
                    refreshAllHistoryRecords()
                }
                delay(8000)
            }
        }
    }

    fun refreshAllHistoryRecords() {
        historyFetchJob?.cancel()
        historyFetchJob = viewModelScope.launch {
            // Fetch remote records from Google Sheets (passing empty string fetches all records)
            val remoteResult = repository.fetchRecordsFromGoogleSheet("")
            val remoteList = remoteResult.getOrDefault(emptyList())

            // Fetch local records from Room Database
            val localList = repository.getAllLocalRecords()

            // Merge and deduplicate by Roll Number + Session ID + Timestamp
            val mergedMap = mutableMapOf<String, AttendanceRecord>()

            for (rec in localList) {
                val key = "${rec.studentRollNumber.trim().uppercase()}_${rec.sessionId.trim().uppercase()}_${rec.scannedTimestamp}"
                mergedMap[key] = rec
            }

            for (rec in remoteList) {
                val key = "${rec.studentRollNumber.trim().uppercase()}_${rec.sessionId.trim().uppercase()}_${rec.scannedTimestamp}"
                mergedMap[key] = rec
            }

            _allAttendanceRecords.value = mergedMap.values.sortedByDescending { it.scannedTimestamp }
        }
    }

    // --- Teacher Actions ---
    fun loginTeacher() {
        if (repository.verifyTeacherCredentials(teacherUsername.value, teacherPassword.value)) {
            teacherLoginError.value = null
            _currentScreen.value = AppScreen.TEACHER_HOME
            _currentRole.value = UserRole.TEACHER
        } else {
            teacherLoginError.value = "Invalid Username or Password"
        }
    }

    fun startAttendanceSession() {
        viewModelScope.launch {
            repository.startSession(
                subjectCode = inputSubjectCode.value,
                courseName = inputCourseName.value
            )
        }
    }

    fun endAttendanceSession() {
        val session = activeSession.value ?: return
        viewModelScope.launch {
            repository.endSession(session.sessionId)
            _liveQrBitmap.value = null
        }
    }

    fun getExportCsvIntent(onIntentReady: (android.content.Intent?) -> Unit) {
        val session = activeSession.value ?: return
        viewModelScope.launch {
            val records = _liveRoster.value.ifEmpty { repository.getSessionRecords(session.sessionId) }
            val intent = com.example.util.CsvExporter.exportAndShareCsv(
                context = getApplication(),
                session = session,
                records = records
            )
            onIntentReady(intent)
        }
    }

    // --- Dynamic QR Timer (5-Second Cycle) ---
    private fun startDynamicQrTimer(sessionId: String) {
        stopDynamicQrTimer()
        qrLoopJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val token = QrCodeGenerator.generateTokenPayload(sessionId, now)
                _currentQrToken.value = token
                _liveQrBitmap.value = QrCodeGenerator.createQrBitmap(token, sizePx = 600)

                // Smooth 5-second countdown (updates progress every 100ms)
                val durationMs = 5000L
                val steps = 50
                for (i in steps downTo 0) {
                    _countdownProgress.value = i.toFloat() / steps.toFloat()
                    delay(durationMs / steps)
                }
            }
        }
    }

    private fun stopDynamicQrTimer() {
        qrLoopJob?.cancel()
        qrLoopJob = null
    }

    private var lastConnectionErrorToastTime = 0L

    private fun observeRosterForSession(sessionId: String) {
        rosterJob?.cancel()
        rosterJob = viewModelScope.launch {
            while (true) {
                // Fetch remote records from Google Sheets endpoint
                val remoteResult = repository.fetchRecordsFromGoogleSheet(sessionId)
                val remoteRecords = remoteResult.getOrElse {
                    val now = System.currentTimeMillis()
                    if (now - lastConnectionErrorToastTime > 15000L) {
                        lastConnectionErrorToastTime = now
                        viewModelScope.launch(Dispatchers.Main) {
                            Toast.makeText(
                                getApplication(),
                                "⚠️ Session connection failed. Syncing locally...",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    emptyList()
                }

                // Fetch local records from Room Database
                val localRecords = repository.getSessionRecords(sessionId)

                // Combine both and deduplicate by Roll Number (case-insensitive)
                val combinedMap = mutableMapOf<String, AttendanceRecord>()

                // Insert local records first
                for (rec in localRecords) {
                    val key = rec.studentRollNumber.trim().uppercase()
                    if (key.isNotBlank()) {
                        combinedMap[key] = rec
                    }
                }

                // Override / add with remote records
                for (rec in remoteRecords) {
                    val key = rec.studentRollNumber.trim().uppercase()
                    if (key.isNotBlank()) {
                        combinedMap[key] = rec
                    }
                }

                val finalRoster = combinedMap.values.sortedByDescending { it.scannedTimestamp }
                _liveRoster.value = finalRoster
                _presentCount.value = finalRoster.size

                delay(3000) // Poll every 3 seconds for live sync
            }
        }
    }

    // --- Student Direct Registration Actions ---
    fun registerAndContinueStudent() {
        val name = studentFullName.value.trim()
        val roll = studentRollNumber.value.trim()
        val phone = studentPhoneNumber.value.trim()

        if (name.isEmpty() || roll.isEmpty() || phone.isEmpty()) {
            studentRegError.value = "Please enter your Full Name, Roll Number, and Phone Number."
            return
        }

        studentRegError.value = null
        viewModelScope.launch {
            repository.registerStudent(
                fullName = name,
                rollNumber = roll,
                phoneNumber = phone
            )
            _currentScreen.value = AppScreen.STUDENT_HOME
            _currentRole.value = UserRole.STUDENT
        }
    }

    fun editProfile() {
        _currentScreen.value = AppScreen.AUTH
        _currentRole.value = UserRole.NONE
    }

    fun signOutStudent() {
        performLogOut()
    }

    fun switchRoleToAuthHub() {
        performLogOut()
    }

    fun performLogOut() {
        viewModelScope.launch {
            repository.signOutStudent()
            studentFullName.value = ""
            studentRollNumber.value = ""
            studentPhoneNumber.value = ""
            _currentScreen.value = AppScreen.AUTH
            _currentRole.value = UserRole.NONE
            _scanUiState.value = ScanUiState.Idle
        }
    }

    // --- Student QR Scanner Logic ---
    fun onQrScanned(scannedTokenUrl: String) {
        // Prevent duplicate processing if already loading or succeeded
        if (_scanUiState.value is ScanUiState.Loading || _scanUiState.value is ScanUiState.Success) {
            return
        }

        _scanUiState.value = ScanUiState.Loading

        val profile = activeStudentProfile.value ?: StudentProfile(
            rollNumber = studentRollNumber.value.ifBlank { "2506028" },
            fullName = studentFullName.value.ifBlank { "Piyush Kumar" },
            phoneNumber = studentPhoneNumber.value.ifBlank { "8210777536" },
            deviceId = "ANDROID_DEVICE_ID",
            registeredAtTimestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            val result = repository.processScannedQrToken(scannedTokenUrl, profile)
            when (result) {
                is AttendanceRepository.AttendanceScanResult.Success -> {
                    _scanUiState.value = ScanUiState.Success(
                        rollNumber = result.rollNumber,
                        name = result.studentName,
                        timestampMs = result.timestampMs,
                        sessionId = result.sessionId,
                        message = result.message
                    )
                    if (result.message.contains("Saved Locally", ignoreCase = true)) {
                        viewModelScope.launch(Dispatchers.Main) {
                            Toast.makeText(
                                getApplication(),
                                "⚠️ Session connection failed. Attendance saved locally.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                is AttendanceRepository.AttendanceScanResult.Error -> {
                    if (result.message.contains("Invalid QR", ignoreCase = true) ||
                        result.message.contains("Invalid Payload", ignoreCase = true)) {
                        // Return to idle silently so camera can continue scanning valid codes
                        _scanUiState.value = ScanUiState.Idle
                        viewModelScope.launch(Dispatchers.Main) {
                            Toast.makeText(
                                getApplication(),
                                "⚠️ Invalid QR Code detected for session",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        _scanUiState.value = ScanUiState.Error(result.message)
                        viewModelScope.launch(Dispatchers.Main) {
                            Toast.makeText(
                                getApplication(),
                                "⚠️ Session connection issue: ${result.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    /**
     * Mark attendance directly for the current active classroom session
     */
    fun markAttendanceNow() {
        val token = _currentQrToken.value
        if (token.isNotEmpty()) {
            onQrScanned(token)
        } else {
            val active = activeSession.value
            val sessionId = active?.sessionId ?: "CS101_${System.currentTimeMillis() % 100000}"
            val validToken = QrCodeGenerator.generateTokenPayload(sessionId)
            onQrScanned(validToken)
        }
    }

    fun simulateScanCurrentToken() {
        markAttendanceNow()
    }

    fun resetScanUiState() {
        _scanUiState.value = ScanUiState.Idle
    }
}
