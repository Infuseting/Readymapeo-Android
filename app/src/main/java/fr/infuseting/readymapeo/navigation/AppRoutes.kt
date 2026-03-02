package fr.infuseting.readymapeo.navigation

/**
 * Routes de navigation de l'application.
 */
object AppRoutes {
    const val LOGIN = "login"
    const val CLUB_LIST = "club_list"
    const val CLUB_DETAIL = "club_detail/{clubId}"
    const val MEMBER_MANAGEMENT = "member_management/{clubId}"

    fun clubDetail(clubId: Int) = "club_detail/$clubId"
    fun memberManagement(clubId: Int) = "member_management/$clubId"
}
