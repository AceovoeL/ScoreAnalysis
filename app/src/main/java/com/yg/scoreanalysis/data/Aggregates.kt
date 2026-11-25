package com.yg.scoreanalysis.data

/** 纯函数聚合，便于单元测试 */
object Aggregates {

    /**
     * 把考试组里录入的科目成绩派生为科目考试记录。
     * 不落盘：唯一数据源仍是 groups，改考试组后科目详情自动同步。
     */
    fun derivedExams(data: AppData, subjectId: String): List<Exam> =
        data.groups
            .filter { g -> g.entries.any { it.subjectId == subjectId && !it.absent } }
            .flatMap { g ->
                g.entries
                    .filter { it.subjectId == subjectId && !it.absent }
                    .map { e ->
                        Exam(
                            id = "group:" + g.id + ":" + subjectId,
                            subjectId = subjectId,
                            name = g.name.ifBlank { "考试组" },
                            date = g.date,
                            score = e.score,
                            rank = e.rank,
                            examFullScore = e.fullScore,
                            note = "",
                            groupId = g.id,
                            fromGroup = true,
                        )
                    }
            }

    /** 某科目的全部考试：单独录入的 + 从考试组派生的，按时间排序 */
    fun examsOf(data: AppData, subjectId: String): List<Exam> =
        (data.exams.filter { it.subjectId == subjectId } + derivedExams(data, subjectId))
            .sortedBy { it.date }

    /** 该场成绩占本场满分的比例，用于跨满分比较 */
    fun ratio(exam: Exam): Double =
        if (exam.examFullScore > 0.0) exam.score / exam.examFullScore else 0.0

    /**
     * 最高分那一次：
     * 若该科目所有考试满分一致，直接比原始分；否则按得分率比较（避免不同满分下误判）。
     */
    fun bestExam(exams: List<Exam>): Exam? {
        if (exams.isEmpty()) return null
        val sameFull = exams.map { it.examFullScore }.distinct().size <= 1
        return exams.maxWithOrNull(
            if (sameFull) compareBy<Exam> { it.score }.thenBy { it.date }
            else compareBy<Exam> { ratio(it) }.thenBy { it.score }.thenBy { it.date }
        )
    }

    fun averageScore(exams: List<Exam>): Double =
        if (exams.isEmpty()) 0.0 else exams.sumOf { it.score } / exams.size

    /** 分数出现次数：四舍五入到整数分 → 出现次数，按分数升序 */
    fun scoreDistribution(exams: List<Exam>): List<Pair<Int, Int>> =
        exams.groupingBy { Math.round(it.score).toInt() }.eachCount().toList().sortedBy { it.first }

    /** 出现次数最多的分数；并列时取较高分 */
    fun mostFrequentScore(exams: List<Exam>): Int? =
        scoreDistribution(exams).maxWithOrNull(compareBy<Pair<Int, Int>> { it.second }.thenBy { it.first })?.first

    /** 排名序列：排名为空的位置用 null 断线 */
    fun rankSeries(exams: List<Exam>): List<Double?> = exams.map { it.rank?.toDouble() }

    /** 该科目是否存在"某场满分≠科目满分"的情况（决定是否显示换算按钮） */
    fun subjectNeedsNormalize(subject: Subject, exams: List<Exam>): Boolean =
        exams.any { it.examFullScore > 0.0 && !nearlyEqual(it.examFullScore, subject.fullScore) }

    /** 组之间总分满分是否不一致 */
    fun groupsNeedNormalize(groups: List<ExamGroup>): Boolean {
        if (groups.size < 2) return false
        val first = groups.first().totalFullScore
        return groups.any { !nearlyEqual(it.totalFullScore, first) }
    }

    fun nearlyEqual(a: Double, b: Double): Boolean = kotlin.math.abs(a - b) < 1e-6

    fun groupsOf(data: AppData): List<ExamGroup> = data.groups.sortedBy { it.date }

    /**
     * 与上一次相比的变化：正数=上升（分数变高/名次变好）
     * 排名是"越小越好"，所以用 上次排名 - 本次排名
     */
    fun deltaScore(previous: Exam?, current: Exam?): Double? {
        if (previous == null || current == null) return null
        if (previous.examFullScore <= 0.0 || current.examFullScore <= 0.0) return null
        // 满分不同时按得分率比较，避免 120/150 与 90/100 误判
        val sameFull = nearlyEqual(previous.examFullScore, current.examFullScore)
        return if (sameFull) current.score - previous.score
        else (current.score / current.examFullScore - previous.score / previous.examFullScore) * 100.0
    }

    fun deltaRank(previous: Exam?, current: Exam?): Int? {
        val p = previous?.rank ?: return null
        val c = current?.rank ?: return null
        return p - c
    }

    /** 该科目最近一次考试与上一次考试（按时间） */
    fun lastTwo(exams: List<Exam>): Pair<Exam?, Exam?> {
        val sorted = exams.sortedBy { it.date }
        val last = sorted.lastOrNull()
        val prev = if (sorted.size >= 2) sorted[sorted.size - 2] else null
        return prev to last
    }

    /** 组考的变化：总分按得分率比较，排名按名次差 */
    fun groupDeltaTotal(previous: ExamGroup?, current: ExamGroup?): Double? {
        if (previous == null || current == null) return null
        if (previous.totalFullScore <= 0.0 || current.totalFullScore <= 0.0) return null
        return if (nearlyEqual(previous.totalFullScore, current.totalFullScore))
            current.totalScore - previous.totalScore
        else (current.totalScore / current.totalFullScore - previous.totalScore / previous.totalFullScore) * 100.0
    }

    /** 某考试组内参与过的科目 id 并集（按出现顺序） */
    fun subjectsInGroup(group: ExamGroup): List<String> = group.entries.map { it.subjectId }.distinct()
}
