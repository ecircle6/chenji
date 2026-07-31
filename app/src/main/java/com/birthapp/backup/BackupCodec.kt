package com.birthapp.backup

import com.birthapp.data.Birthday
import com.birthapp.data.EventType
import org.json.JSONArray
import org.json.JSONObject

/**
 * 备份文件的编解码。
 *
 * 格式是带标记头的 JSON 文本：`app` 字段用来认门牌（防止用户误选了
 * 别的 App 的 JSON 文件），`format` 是格式版本号，以后字段有变化时
 * 靠它做兼容。记录里只存"用户输入过的东西"；id、下次提醒日期这类
 * 运行时状态不进备份——导入的手机上会重新生成，硬带过去反而会
 * 跟目标手机上的数据打架。
 */
object BackupCodec {

    /** 格式版本。字段增删时 +1，decode 里按版本做兼容 */
    const val FORMAT_VERSION = 1

    /** 门牌标记，认文件用，跟包名保持一致 */
    private const val APP_MARK = "com.birthapp"

    /** 单个文件最多允许的记录数，防止喂进来一个超大文件把内存撑爆 */
    private const val MAX_RECORDS = 5000

    fun encode(records: List<Birthday>): String {
        val arr = JSONArray()
        for (b in records) {
            arr.put(JSONObject().apply {
                put("name", b.name)
                put("birthYear", b.birthYear)
                put("birthMonth", b.birthMonth)
                put("birthDay", b.birthDay)
                put("calendarType", b.calendarType)
                put("isLeapMonth", b.isLeapMonth)
                put("advanceDays", b.advanceDays)
                put("reminderHour", b.reminderHour)
                put("reminderMinute", b.reminderMinute)
                put("relation", b.relation)
                put("eventType", b.eventType)
                put("notes", b.notes)
                put("isActive", b.isActive)
            })
        }
        val root = JSONObject().apply {
            put("app", APP_MARK)
            put("format", FORMAT_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("records", arr)
        }
        // 缩进输出：备份文件用户可能会自己打开看，排好版比压成一行友好
        return root.toString(2)
    }

    /**
     * 解析备份文本。不是本 App 的备份、结构不完整时抛 [IllegalArgumentException]，
     * 由调用方转成用户能看懂的提示。
     */
    fun decode(text: String): List<Birthday> {
        val root = runCatching { JSONObject(text) }
            .getOrElse { throw IllegalArgumentException("不是有效的备份文件", it) }
        require(root.optString("app") == APP_MARK) { "不是辰记导出的备份文件" }
        require(root.optInt("format", 0) in 1..FORMAT_VERSION) { "备份文件版本不支持" }

        val arr = root.optJSONArray("records")
            ?: throw IllegalArgumentException("备份文件里没有记录数据")
        require(arr.length() <= MAX_RECORDS) { "备份文件里的记录数超出上限" }

        val result = ArrayList<Birthday>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val name = o.optString("name").trim()
            val year = o.optInt("birthYear", -1)
            val month = o.optInt("birthMonth", -1)
            val day = o.optInt("birthDay", -1)
            // 核心四项缺一不可，其余字段都有安全的默认值可兜底
            require(name.isNotEmpty() && year > 0 && month in 1..12 && day in 1..31) {
                "备份文件里第 ${i + 1} 条记录不完整"
            }
            result.add(
                Birthday(
                    // id 交给数据库自增；导入方重新排提醒，所以运行时字段全部用默认值
                    name = name,
                    birthYear = year,
                    birthMonth = month,
                    birthDay = day,
                    calendarType = if (o.optString("calendarType") == "lunar") "lunar" else "solar",
                    isLeapMonth = o.optBoolean("isLeapMonth", false),
                    advanceDays = o.optInt("advanceDays", 0).coerceIn(0, 365),
                    reminderHour = o.optInt("reminderHour", 8).coerceIn(0, 23),
                    reminderMinute = o.optInt("reminderMinute", 0).coerceIn(0, 59),
                    relation = o.optString("relation").ifEmpty { "family" },
                    eventType = o.optString("eventType").ifEmpty { EventType.BIRTHDAY },
                    notes = o.optString("notes"),
                    isActive = o.optBoolean("isActive", true)
                )
            )
        }
        return result
    }
}
