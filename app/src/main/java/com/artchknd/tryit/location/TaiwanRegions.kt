package com.artchknd.tryit.location

object TaiwanRegions {

    private val WEEKLY_DATASET_IDS: Map<String, String> = mapOf(
        "宜蘭縣" to "F-D0047-003",
        "桃園市" to "F-D0047-007",
        "新竹縣" to "F-D0047-011",
        "苗栗縣" to "F-D0047-015",
        "彰化縣" to "F-D0047-019",
        "南投縣" to "F-D0047-023",
        "雲林縣" to "F-D0047-027",
        "嘉義縣" to "F-D0047-031",
        "屏東縣" to "F-D0047-035",
        "臺東縣" to "F-D0047-039",
        "花蓮縣" to "F-D0047-043",
        "澎湖縣" to "F-D0047-047",
        "基隆市" to "F-D0047-051",
        "新竹市" to "F-D0047-055",
        "嘉義市" to "F-D0047-059",
        "臺北市" to "F-D0047-063",
        "高雄市" to "F-D0047-067",
        "新北市" to "F-D0047-071",
        "臺中市" to "F-D0047-075",
        "臺南市" to "F-D0047-079",
        "連江縣" to "F-D0047-083",
        "金門縣" to "F-D0047-087"
    )

    fun normalizeCounty(raw: String): String? {
        val name = raw.trim().replace('台', '臺')
        return name.takeIf { it in WEEKLY_DATASET_IDS }
    }

    fun normalizeDistrict(raw: String): String = raw.trim().replace('台', '臺')

    fun weeklyDatasetId(county: String): String? = WEEKLY_DATASET_IDS[county]

    /** 全部縣市名稱（官方寫法，依上方宣告順序）。 */
    fun allCounties(): List<String> = WEEKLY_DATASET_IDS.keys.toList()
}
