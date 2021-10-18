package co.sunnyapp.flutter_phone_state

data class PhoneCallEvent(val phoneNumber: String? = null, val type: PhoneEventType) {
    fun toMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (phoneNumber?.isNotBlank() == true) {
            map["phoneNumber"] = phoneNumber
            map["id"] = phoneNumber
        }
        map["type"] = type.name
        return map
    }
}
