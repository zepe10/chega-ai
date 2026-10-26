package br.com.habil.carona.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"

    const val RIDES_GRAPH = "rides_graph"
    const val RIDE_LIST = "ride_list"
    const val RIDE_DETAIL = "ride_detail/{rideId}"
    const val RIDE_FORM = "ride_form?rideId={rideId}"

    const val ACTIVITY_GRAPH = "activity_graph"
    const val ACTIVITY = "activity"
    const val CHAT = "chat/{requestId}"

    const val CONTACTS_GRAPH = "contacts_graph"
    const val CONTACTS = "contacts"

    const val PROFILE_GRAPH = "profile_graph"
    const val PROFILE = "profile"

    const val ARG_RIDE_ID = "rideId"
    const val ARG_REQUEST_ID = "requestId"

    fun rideDetail(rideId: String) = "ride_detail/$rideId"
    fun rideForm(rideId: String? = null) = if (rideId == null) "ride_form" else "ride_form?rideId=$rideId"
    fun chat(requestId: Long) = "chat/$requestId"
}
