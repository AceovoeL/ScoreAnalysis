package com.yg.scoreanalysis.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/** 唯一数据源；所有编辑走 mutate，写完立即落盘 */
class ScoreRepository(context: Context) {

    private val store = LocalStore(context.applicationContext)

    private val _state = MutableStateFlow(store.load())
    val state: StateFlow<AppData> = _state.asStateFlow()

    val recoveredFromBackup: Boolean get() = store.lastLoadRecovered

    fun mutate(transform: (AppData) -> AppData) {
        val next = transform(_state.value)
        _state.value = next
        store.save(next)
    }

    // ---------- 科目 ----------
    fun addSubject(name: String, fullScore: Double) = mutate { d ->
        val seed = (d.subjects.size + 1) % PALETTE_SIZE
        d.copy(subjects = d.subjects + Subject(newId(), name.trim(), fullScore, seed, System.currentTimeMillis()))
    }

    fun setSubjectTarget(subjectId: String, target: Double) = mutate { d ->
        d.copy(subjects = d.subjects.map { if (it.id == subjectId) it.copy(targetScore = target) else it })
    }

    fun updateSubject(subject: Subject) = mutate { d ->
        d.copy(subjects = d.subjects.map { if (it.id == subject.id) subject else it })
    }

    /** 删除科目：连带删除其考试；考试组里的条目保留快照并标记科目已删除 */
    fun deleteSubject(subjectId: String) = mutate { d ->
        d.copy(
            subjects = d.subjects.filterNot { it.id == subjectId },
            exams = d.exams.filterNot { it.subjectId == subjectId },
        )
    }

    // ---------- 单科考试 ----------
    fun upsertExam(exam: Exam) = mutate { d ->
        val exists = d.exams.any { it.id == exam.id }
        d.copy(exams = if (exists) d.exams.map { if (it.id == exam.id) exam else it } else d.exams + exam)
    }

    fun deleteExam(examId: String) = mutate { d -> d.copy(exams = d.exams.filterNot { it.id == examId }) }

    fun newExam(subjectId: String): Exam = Exam(
        id = newId(),
        subjectId = subjectId,
        name = "",
        date = System.currentTimeMillis(),
        score = 0.0,
        rank = null,
        examFullScore = 0.0,
    )

    // ---------- 考试组 ----------
    fun upsertGroup(group: ExamGroup) = mutate { d ->
        val exists = d.groups.any { it.id == group.id }
        d.copy(groups = if (exists) d.groups.map { if (it.id == group.id) group else it } else d.groups + group)
    }

    fun deleteGroup(groupId: String) = mutate { d -> d.copy(groups = d.groups.filterNot { it.id == groupId }) }

    fun newGroup(): ExamGroup = ExamGroup(
        id = newId(),
        name = "",
        date = System.currentTimeMillis(),
        totalFullScore = 0.0,
    )

    // ---------- 设置 ----------
    fun setDarkMode(mode: Int) = mutate { d -> d.copy(settings = d.settings.copy(darkMode = mode)) }

    fun replaceAll(data: AppData) = mutate { data }

    fun clearAll() {
        store.clear()
        _state.value = AppData()
        store.save(_state.value)
    }

    private fun newId(): String = UUID.randomUUID().toString()

    companion object {
        const val PALETTE_SIZE = 8
    }
}
