package com.yg.scoreanalysis

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yg.scoreanalysis.data.ExamGroup
import com.yg.scoreanalysis.data.ScoreRepository
import com.yg.scoreanalysis.ui.components.NavTab
import com.yg.scoreanalysis.ui.screens.AnalysisScreen
import com.yg.scoreanalysis.ui.screens.ExamEditScreen
import com.yg.scoreanalysis.ui.screens.GroupDetailScreen
import com.yg.scoreanalysis.ui.screens.GroupEditScreen
import com.yg.scoreanalysis.ui.screens.OverviewScreen
import com.yg.scoreanalysis.ui.screens.RecordsScreen
import com.yg.scoreanalysis.ui.screens.SettingsScreen
import com.yg.scoreanalysis.ui.screens.SubjectDetailScreen
import com.yg.scoreanalysis.ui.screens.SubjectEditScreen

/** 主界面四个 Tab */
enum class MainTab(val label: String) { Overview("总览"), Records("记录"), Analysis("分析"), Settings("设置") }

/** 全屏子页面 */
sealed interface Page {
    data class SubjectDetail(val subjectId: String) : Page
    data class SubjectEdit(val subjectId: String?) : Page
    data class ExamEdit(val subjectId: String, val examId: String?) : Page
    data class GroupDetail(val groupId: String) : Page
    data class GroupEdit(val groupId: String?) : Page
}

@Composable
fun ScoreAnalysisApp(
    repository: ScoreRepository,
    updateManager: com.yg.scoreanalysis.data.UpdateManager,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    val data by repository.state.collectAsState()
    var tab by remember { mutableStateOf(MainTab.Overview) }
    var stack by remember { mutableStateOf(listOf<Page>()) }
    val current = stack.lastOrNull()
    var goingForward by remember { mutableStateOf(true) }

    fun push(page: Page) {
        goingForward = true
        stack = stack + page
    }

    fun pop() {
        if (stack.isNotEmpty()) {
            goingForward = false
            stack = stack.dropLast(1)
        }
    }

    BackHandler(enabled = stack.isNotEmpty()) { pop() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            val cfg = androidx.compose.ui.platform.LocalConfiguration.current
            val maxW = if (cfg.screenWidthDp >= 600) 720.dp else androidx.compose.ui.unit.Dp.Unspecified
            val widthMod = if (maxW != androidx.compose.ui.unit.Dp.Unspecified)
                Modifier.fillMaxSize().widthIn(max = maxW) else Modifier.fillMaxSize()

            Column(widthMod) {
                Box(Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = current,
                        transitionSpec = {
                            if (targetState == null) {
                                (fadeIn(tween(180)) + scaleIn(initialScale = 0.98f, animationSpec = tween(200)))
                                    .togetherWith(fadeOut(tween(140)))
                            } else {
                                (slideInHorizontally(tween(260)) { w -> if (goingForward) w / 5 else -w / 5 } + fadeIn(tween(200)))
                                    .togetherWith(slideOutHorizontally(tween(240)) { w -> if (goingForward) -w / 8 else w / 8 } + fadeOut(tween(160)))
                            }
                        },
                        label = "page",
                    ) { page ->
                        if (page == null) {
                            MainTabs(
                                data = data,
                                tab = tab,
                                onTab = { tab = it },
                                onOpenSubject = { push(Page.SubjectDetail(it)) },
                                onAddSubject = { push(Page.SubjectEdit(null)) },
                                onOpenGroup = { push(Page.GroupDetail(it)) },
                                onAddGroup = { push(Page.GroupEdit(null)) },
                                onExport = onExport,
                                onImport = onImport,
                                onDarkMode = { repository.setDarkMode(it) },
                                onClearAll = { repository.clearAll() },
                                updateManager = updateManager,
                            )
                        } else {
                            PageHost(
                                page = page,
                                data = data,
                                repository = repository,
                                onBack = { pop() },
                                push = { push(it) },
                                resetStack = { stack = emptyList() },
                            )
                        }
                    }
                }
                // 底部导航：仅在主界面显示
                if (current == null) {
                    BottomBar(tab = tab, onTab = { tab = it })
                }
            }
        }
    }

    // 启动检测到新版本时，在主界面之上弹出更新说明
    val updateState by updateManager.state.collectAsState()
    val ready = updateState as? com.yg.scoreanalysis.data.UpdateState.Ready
    if (ready != null) {
        com.yg.scoreanalysis.ui.update.UpdateDialog(
            state = ready,
            onDownload = { updateManager.download() },
            onDismiss = { updateManager.dismiss() },
            onOpenPage = { updateManager.openReleasePage() },
        )
    }
}

@Composable
private fun MainTabs(
    data: com.yg.scoreanalysis.data.AppData,
    tab: MainTab,
    onTab: (MainTab) -> Unit,
    onOpenSubject: (String) -> Unit,
    onAddSubject: () -> Unit,
    onOpenGroup: (String) -> Unit,
    onAddGroup: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onDarkMode: (Int) -> Unit,
    onClearAll: () -> Unit,
    updateManager: com.yg.scoreanalysis.data.UpdateManager,
) {
    AnimatedContent(targetState = tab, transitionSpec = {
        fadeIn(tween(180)).togetherWith(fadeOut(tween(120)))
    }, label = "tab") { t ->
        when (t) {
            MainTab.Overview -> OverviewScreen(
                data = data,
                onOpenGroup = onOpenGroup,
                onAddGroup = onAddGroup,
                onAddSubject = onAddSubject,
                onOpenSubject = onOpenSubject,
                onOpenSettings = { onTab(MainTab.Settings) },
                onOpenRecords = { onTab(MainTab.Records) },
            )
            MainTab.Records -> RecordsScreen(
                data = data,
                onOpenGroup = onOpenGroup,
                onOpenSubject = onOpenSubject,
            )
            MainTab.Analysis -> AnalysisScreen(data = data, onOpenSubject = onOpenSubject)
            MainTab.Settings -> SettingsScreen(
                data = data,
                onBack = { onTab(MainTab.Overview) },
                onDarkMode = onDarkMode,
                onExport = onExport,
                onImport = onImport,
                onClearAll = onClearAll,
                embedded = true,
                updateManager = updateManager,
            )
        }
    }
}

@Composable
private fun BottomBar(tab: MainTab, onTab: (MainTab) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            NavTab("总览", Icons.Rounded.SpaceDashboard, tab == MainTab.Overview, { onTab(MainTab.Overview) }, Modifier.weight(1f))
            NavTab("记录", Icons.Rounded.FactCheck, tab == MainTab.Records, { onTab(MainTab.Records) }, Modifier.weight(1f))
            NavTab("分析", Icons.Rounded.Assessment, tab == MainTab.Analysis, { onTab(MainTab.Analysis) }, Modifier.weight(1f))
            NavTab("设置", Icons.Rounded.Settings, tab == MainTab.Settings, { onTab(MainTab.Settings) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PageHost(
    page: Page,
    data: com.yg.scoreanalysis.data.AppData,
    repository: ScoreRepository,
    onBack: () -> Unit,
    push: (Page) -> Unit,
    resetStack: () -> Unit,
) {
    when (page) {
        is Page.SubjectDetail -> {
            val subject = data.subjects.firstOrNull { it.id == page.subjectId }
            if (subject == null) {
                LaunchedBack(onBack)
            } else {
                SubjectDetailScreen(
                    subject = subject,
                    exams = com.yg.scoreanalysis.data.Aggregates.examsOf(data, subject.id),
                    onBack = onBack,
                    onAddExam = { push(Page.ExamEdit(subject.id, null)) },
                    onEditExam = { exam ->
                        if (exam.fromGroup) exam.groupId?.let { push(Page.GroupDetail(it)) }
                        else push(Page.ExamEdit(subject.id, exam.id))
                    },
                    onDeleteExam = { exam -> if (!exam.fromGroup) repository.deleteExam(exam.id) },
                    onEditSubject = { push(Page.SubjectEdit(subject.id)) },
                )
            }
        }

        is Page.SubjectEdit -> {
            val editing = page.subjectId?.let { id -> data.subjects.firstOrNull { it.id == id } }
            SubjectEditScreen(
                initialName = editing?.name ?: "",
                initialFull = editing?.fullScore ?: 0.0,
                initialTarget = editing?.targetScore ?: 0.0,
                isNew = editing == null,
                onBack = onBack,
                onSave = { name, full, target ->
                    if (editing == null) {
                        repository.addSubject(name, full)
                        repository.setSubjectTarget(
                            repository.state.value.subjects.last().id, target,
                        )
                    } else {
                        repository.updateSubject(editing.copy(name = name, fullScore = full, targetScore = target))
                    }
                    onBack()
                },
            )
        }

        is Page.ExamEdit -> {
            val subject = data.subjects.firstOrNull { it.id == page.subjectId }
            if (subject == null) {
                LaunchedBack(onBack)
            } else {
                val existing = page.examId?.let { id -> data.exams.firstOrNull { it.id == id } }
                val initial = remember(page.subjectId, page.examId) {
                    existing ?: repository.newExam(subject.id).copy(examFullScore = subject.fullScore)
                }
                ExamEditScreen(
                    subject = subject,
                    initial = initial,
                    onBack = onBack,
                    onSave = {
                        repository.upsertExam(it)
                        onBack()
                    },
                )
            }
        }

        is Page.GroupDetail -> {
            val group = data.groups.firstOrNull { it.id == page.groupId }
            if (group == null) {
                LaunchedBack(onBack)
            } else {
                GroupDetailScreen(
                    groups = data.groups,
                    current = group,
                    subjectColorSeed = { sid -> data.subjects.firstOrNull { it.id == sid }?.colorSeed ?: 0 },
                    onBack = onBack,
                    onEdit = { push(Page.GroupEdit(group.id)) },
                    onDelete = {
                        repository.deleteGroup(group.id)
                        resetStack()
                    },
                )
            }
        }

        is Page.GroupEdit -> {
            val editing: ExamGroup? = page.groupId?.let { id -> data.groups.firstOrNull { it.id == id } }
            val initialGroup = remember(page.groupId) { editing ?: repository.newGroup() }
            GroupEditScreen(
                subjects = data.subjects,
                initial = initialGroup,
                onBack = onBack,
                onSave = {
                    repository.upsertGroup(it)
                    onBack()
                },
            )
        }
    }
}

@Composable
private fun LaunchedBack(onBack: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) { onBack() }
}
