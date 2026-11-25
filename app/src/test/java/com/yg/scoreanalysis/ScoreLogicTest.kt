package com.yg.scoreanalysis

import com.yg.scoreanalysis.data.Aggregates
import com.yg.scoreanalysis.data.AppData
import com.yg.scoreanalysis.data.Exam
import com.yg.scoreanalysis.data.ExamGroup
import com.yg.scoreanalysis.data.GroupEntry
import com.yg.scoreanalysis.data.Normalizer
import com.yg.scoreanalysis.data.Subject
import com.yg.scoreanalysis.data.totalScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreLogicTest {

    private fun exam(id: String, score: Double, full: Double, date: Long, rank: Int? = null) =
        Exam(id = id, subjectId = "s1", name = "E" + id, date = date, score = score, rank = rank, examFullScore = full)

    @Test
    fun bestExam_prefersRawScoreWhenFullScoreSame() {
        val list = listOf(exam("1", 90.0, 150.0, 1), exam("2", 120.0, 150.0, 2))
        assertEquals("2", Aggregates.bestExam(list)?.id)
    }

    @Test
    fun bestExam_usesRatioWhenFullScoreDiffers() {
        // 100/100 = 100% 高于 120/150 = 80%
        val list = listOf(exam("a", 100.0, 100.0, 1), exam("b", 120.0, 150.0, 2))
        assertEquals("a", Aggregates.bestExam(list)?.id)
    }

    @Test
    fun scoreDistribution_countsRoundedScores() {
        val list = listOf(exam("1", 88.0, 100.0, 1), exam("2", 88.4, 100.0, 2), exam("3", 92.0, 100.0, 3))
        val dist = Aggregates.scoreDistribution(list).toMap()
        assertEquals(2, dist[88])
        assertEquals(1, dist[92])
    }

    @Test
    fun mostFrequentScore_picksHighestOnTie() {
        val list = listOf(exam("1", 80.0, 100.0, 1), exam("2", 90.0, 100.0, 2))
        assertEquals(90, Aggregates.mostFrequentScore(list))
    }

    @Test
    fun normalizeSubject_scalesToSubjectFullScore() {
        val e = exam("1", 100.0, 120.0, 1)
        assertEquals(125.0, Normalizer.scaledExamScore(e, 150.0), 1e-9)
    }

    @Test
    fun groupTotal_sumsNonAbsentEntries() {
        val g = ExamGroup(
            id = "g", name = "联考", date = 1, totalFullScore = 300.0,
            entries = listOf(
                GroupEntry("s1", "语文", 100.0, 10, 150.0),
                GroupEntry("s2", "数学", 120.0, 5, 150.0),
                GroupEntry("s3", "英语", 0.0, null, 150.0, absent = true),
            ),
        )
        assertEquals(220.0, g.totalScore, 1e-9)
    }

    @Test
    fun groupNormalize_flagsOnlyWhenFullScoresDiffer() {
        val a = ExamGroup("a", "A", 1, 300.0)
        val b = ExamGroup("b", "B", 2, 300.0)
        val c = ExamGroup("c", "C", 3, 750.0)
        assertFalse(Aggregates.groupsNeedNormalize(listOf(a, b)))
        assertTrue(Aggregates.groupsNeedNormalize(listOf(a, c)))
    }

    @Test
    fun scaledGroupTotal_isPercent() {
        val g = ExamGroup(
            id = "g", name = "联考", date = 1, totalFullScore = 750.0,
            entries = listOf(GroupEntry("s1", "语文", 600.0, 1, 150.0)),
        )
        assertEquals(80.0, Normalizer.scaledGroupTotal(g), 1e-9)
    }

    @Test
    fun subjectNeedsNormalize_detectsMismatch() {
        val subject = Subject("s1", "数学", 150.0)
        assertFalse(Aggregates.subjectNeedsNormalize(subject, listOf(exam("1", 100.0, 150.0, 1))))
        assertTrue(Aggregates.subjectNeedsNormalize(subject, listOf(exam("1", 100.0, 120.0, 1))))
    }

    @Test
    fun zeroFullScore_doesNotProduceInfinity() {
        // 本场满分缺失时回退到原始分，不产生 Infinity/NaN
        val e = exam("1", 90.0, 0.0, 1)
        assertEquals(90.0, Normalizer.scaledExamScore(e, 150.0), 1e-9)
        val g = ExamGroup("g", "G", 1, 0.0, listOf(GroupEntry("s1", "x", 10.0, 1, 100.0)))
        assertEquals(10.0, Normalizer.scaledGroupTotal(g), 1e-9)
    }

    @Test
    fun examsOf_includesScoresFromExamGroups() {
        val data = AppData(
            subjects = listOf(Subject("s1", "数学", 150.0)),
            exams = listOf(exam("own", 120.0, 150.0, 1000L, 10)),
            groups = listOf(
                ExamGroup(
                    id = "g1", name = "期末", date = 2000L, totalFullScore = 300.0,
                    entries = listOf(
                        GroupEntry("s1", "数学", 140.0, 3, 150.0),
                        GroupEntry("s2", "语文", 100.0, 20, 150.0),
                    ),
                ),
            ),
        )
        val list = Aggregates.examsOf(data, "s1")
        assertEquals(2, list.size)
        assertEquals(listOf(120.0, 140.0), list.map { it.score })
        // 派生记录带来源标记，且 id 稳定
        val derived = list.first { it.fromGroup }
        assertEquals("group:g1:s1", derived.id)
        assertEquals("g1", derived.groupId)
        assertEquals(3, derived.rank)
        assertTrue(Aggregates.examsOf(data, "s2").single().fromGroup)
    }

    @Test
    fun examsOf_ignoresAbsentEntries() {
        val data = AppData(
            subjects = listOf(Subject("s1", "数学", 150.0)),
            groups = listOf(
                ExamGroup(
                    id = "g1", name = "缺考", date = 1L, totalFullScore = 150.0,
                    entries = listOf(GroupEntry("s1", "数学", 0.0, null, 150.0, absent = true)),
                ),
            ),
        )
        assertTrue(Aggregates.examsOf(data, "s1").isEmpty())
    }

    @Test
    fun bestExam_canSelectGroupDerivedExam() {
        val data = AppData(
            subjects = listOf(Subject("s1", "数学", 150.0)),
            exams = listOf(exam("own", 120.0, 150.0, 1000L)),
            groups = listOf(
                ExamGroup(
                    id = "g1", name = "期末", date = 2000L, totalFullScore = 150.0,
                    entries = listOf(GroupEntry("s1", "数学", 145.0, 2, 150.0)),
                ),
            ),
        )
        val best = Aggregates.bestExam(Aggregates.examsOf(data, "s1"))
        assertEquals("group:g1:s1", best?.id)
        assertEquals(145.0, best?.score ?: 0.0, 1e-9)
    }
}
