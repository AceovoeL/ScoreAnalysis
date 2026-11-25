package com.yg.scoreanalysis.data

import kotlinx.serialization.Serializable

/** 单科目：名称 + 该科满分 */
@Serializable
data class Subject(
    val id: String,
    val name: String,
    val fullScore: Double,
    val colorSeed: Int = 0,
    val createdAt: Long = 0L,
    /** 该科目标分；0 表示未设置 */
    val targetScore: Double = 0.0,
)

/** 单科目下的一场考试 */
@Serializable
data class Exam(
    val id: String,
    val subjectId: String,
    val name: String,
    val date: Long,
    val score: Double,
    val rank: Int? = null,
    /** 本场该科满分；若与科目满分不同，则触发"按比例换算" */
    val examFullScore: Double,
    val note: String = "",
    /** 来源考试组 id；为空表示在科目里单独录入 */
    val groupId: String? = null,
    /** 是否来自考试组（只读记录，需去考试组里修改） */
    val fromGroup: Boolean = false,
)

/** 考试组中的一个科目条目 */
@Serializable
data class GroupEntry(
    val subjectId: String,
    val subjectName: String,
    val score: Double,
    val rank: Int? = null,
    val fullScore: Double,
    val absent: Boolean = false,
)

/** 多科目考试组（联考、期末考等） */
@Serializable
data class ExamGroup(
    val id: String,
    val name: String,
    val date: Long,
    val totalFullScore: Double,
    val entries: List<GroupEntry> = emptyList(),
    /** 总分班级排名 */
    val classRank: Int? = null,
    /** 总分年级排名 */
    val gradeRank: Int? = null,
    /** 旧版本的单一总分排名，仅用于迁移旧数据（读取时回填到 classRank） */
    val totalRank: Int? = null,
)

@Serializable
data class Settings(
    /** 0=跟随系统 1=浅色 2=深色（默认深色，贴合整体设计） */
    val darkMode: Int = 2,
)

@Serializable
data class AppData(
    val version: Int = 1,
    val subjects: List<Subject> = emptyList(),
    val exams: List<Exam> = emptyList(),
    val groups: List<ExamGroup> = emptyList(),
    val settings: Settings = Settings(),
)

/** 班级排名：优先新字段，旧数据回填 */
val ExamGroup.classRankOrLegacy: Int?
    get() = classRank ?: totalRank

/** 组总分 = 未缺考科目成绩之和 */
val ExamGroup.totalScore: Double
    get() = entries.filter { !it.absent }.sumOf { it.score }

/** 组内已录入科目数（非缺考） */
val ExamGroup.enteredCount: Int
    get() = entries.count { !it.absent }
