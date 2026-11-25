package com.yg.scoreanalysis.data

/**
 * 按比例换算：仅影响图表显示，不修改原始数据。
 * - 科目考试：score / examFullScore * subject.fullScore（统一到科目满分基准）
 * - 考试组：total / totalFullScore * 100（统一到百分制）
 */
object Normalizer {

    fun scaledExamScore(exam: Exam, subjectFullScore: Double): Double =
        if (exam.examFullScore > 0.0) exam.score / exam.examFullScore * subjectFullScore else exam.score

    fun scaledGroupTotal(group: ExamGroup): Double =
        if (group.totalFullScore > 0.0) group.totalScore / group.totalFullScore * 100.0 else group.totalScore

    /** 科目图表的分数序列 */
    fun examScores(exams: List<Exam>, subjectFullScore: Double, scaled: Boolean): List<Double?> =
        exams.map { if (scaled) scaledExamScore(it, subjectFullScore) else it.score }

    /** 考试组总分序列 */
    fun groupTotals(groups: List<ExamGroup>, scaled: Boolean): List<Double?> =
        groups.map { if (scaled) scaledGroupTotal(it) else it.totalScore }

    /** 换算后统一基准的最大值（用于 y 轴） */
    fun subjectAxisMax(subjectFullScore: Double, scaled: Boolean, exams: List<Exam>): Double {
        val rawMax = exams.maxOfOrNull { it.score } ?: 0.0
        return if (scaled) maxOf(subjectFullScore, rawMax) else maxOf(rawMax, subjectFullScore)
    }
}
