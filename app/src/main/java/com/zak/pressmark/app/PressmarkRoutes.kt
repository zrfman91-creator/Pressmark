// FILE: app/src/main/java/com/zak/pressmark/app/PressmarkRoutes.kt
package com.zak.pressmark.app

object PressmarkRoutes {
    const val LIBRARY = "library"

    const val ADD_WORK = "add_work"
    const val BARCODE_SCANNER = "barcode_scanner"

    const val WORK_DETAILS = "work_details"
    const val ARG_WORK_ID = "workId"
    const val WORK_DETAILS_PATTERN = "$WORK_DETAILS/{$ARG_WORK_ID}"

    fun workDetails(workId: String): String = "$WORK_DETAILS/$workId"

    const val REFINE_PRESSING = "refine_pressing"
    const val REFINE_PRESSING_PATTERN = "$REFINE_PRESSING/{$ARG_WORK_ID}"

    fun refinePressing(workId: String): String = "$REFINE_PRESSING/$workId"
}
