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

    internal fun dedupKey(b: Birthday): String =
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

    /**
     * 逐条标记导入记录是否与现有记录重复（含文件内自身重复，只保留首次出现为不重复），
     * 供导入预览对话框做「跳过/覆盖/导入」三选
     */
    fun classify(existing: List<Birthday>, incoming: List<Birthday>): List<ImportItem> {
        val seen = existing.mapTo(HashSet()) { dedupKey(it) }
        return incoming.map { record ->
            ImportItem(record = record, isDuplicate = !seen.add(dedupKey(record)))
        }
    }
}

/** 导入预览里的一条记录：是否与现有数据重复（重复的默认跳过） */
data class ImportItem(val record: Birthday, val isDuplicate: Boolean)
