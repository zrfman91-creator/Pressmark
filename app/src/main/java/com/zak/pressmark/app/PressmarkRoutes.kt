// FILE: app/src/main/java/com/zak/pressmark/app/PressmarkRoutes.kt
package com.zak.pressmark.app

object PressmarkRoutes {
    const val LIBRARY = "library"

    const val ADD_WORK = "add_work"
    const val ADD_BARCODE = "add_barcode"
    const val BARCODE_SCANNER = "barcode_scanner"

    const val WORK_DETAILS = "work_details"
    const val ARG_WORK_ID = "workId"
    const val WORK_DETAILS_PATTERN = "$WORK_DETAILS/{$ARG_WORK_ID}"

    fun workDetails(workId: String): String = "$WORK_DETAILS/$workId"
}
