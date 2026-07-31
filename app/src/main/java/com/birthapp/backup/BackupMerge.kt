package com.birthapp.backup

import com.birthapp.data.Birthday

/**
 * 导入时的合并去重。
 *
 * 判重口径：姓名 + 历法 + 出生年月日 + 闰月标记 + 事件类型完全一致才算
 * 同一条。提醒时间、备注这些不参与判重——同一个人在两台手机上设了
 * 不同的提醒时间，仍然是同一条记录，跳过而不是重复导入。
 */
object BackupMerge {

    private fun dedupKey(b: Birthday): String =
        listOf(
            b.name, b.calendarType, b.birthYear, b.birthMonth, b.birthDay,
            b.isLeapMonth, b.eventType
        ).joinToString("|")

    /**
     * 从 [incoming] 里挑出 [existing] 中没有的记录。
     * 备份文件内部自身的重复也一并去掉（同一条只留第一次出现的）。
     */
    fun filterNew(existing: List<Birthday>, incoming: List<Birthday>): List<Birthday> {
        val seen = existing.mapTo(HashSet()) { dedupKey(it) }
        return incoming.filter { seen.add(dedupKey(it)) }
    }
}
