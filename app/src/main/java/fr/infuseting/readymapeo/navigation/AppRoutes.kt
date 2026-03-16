package fr.infuseting.readymapeo.navigation

/**
 * Routes de navigation de l'application.
 */
object AppRoutes {
    const val LOGIN = "login"
    const val CLUB_LIST = "club_list"
    const val CLUB_DETAIL = "club_detail/{clubId}"
    // isManager est un paramètre optionnel passé en query
    const val MEMBER_MANAGEMENT = "member_management/{clubId}?isManager={isManager}"

    fun clubDetail(clubId: Int) = "club_detail/$clubId"
    fun memberManagement(clubId: Int, isManager: Boolean) = "member_management/$clubId?isManager=$isManager"
}
