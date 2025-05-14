package com.nanwe.nbizframemobile_webview_kotiln

object AppConstants {

    const val APP_ID = "MOBILE"

    const val PUSH_SAVE_TOKEN_URL = "http://192.168.10.94:9090/push/api/saveToken.do"
    const val PUSH_REMOVE_TOKEN_URL = "http://192.168.10.94:9090/push/api/removeToken.do"

    const val PUSH_CHANNEL_ID = "알림"
    const val PUSH_CHANNEL_NM = "NBizFrameMobile 알림"
    const val PUSH_CHANNEL_DES = "NBizFrameMobile 알림 서비스 입니다"

    // const val APP_URL = "http://59.1.135.4:19084"
    const val APP_URL = "http://192.168.10.94:9090/login.do"

    // nexacro 이벤트 속성명
    const val UID = "uid"
    const val SVCID = "svcid"
    const val REASON = "reason"
    const val RETVAL = "returnvalue"

    const val CODE_SUCCESS = 0      // 처리결과 코드(성공)
    const val CODE_ERROR = -1       // 처리결과 코드(실패)
}
