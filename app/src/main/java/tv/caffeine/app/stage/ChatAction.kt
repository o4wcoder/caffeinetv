package tv.caffeine.app.stage

interface ChatActionCallback {

    fun processChatAction(type: ChatAction)
}

enum class ChatAction {
    DIGITAL_ITEM,
    MESSAGE,
    SHARE
}