package com.github.hiyuuu.guimaker

import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

class GUIMakerSession {

    companion object {

        private val sessions = HashMap<String, Inventory>()

        fun saveLocal(player: Player, sessionName: String, inventory: Inventory)
                = sessions.set(localKey(player, sessionName), inventory)

        fun restoreLocal(player: Player, sessionName: String): Inventory?
                = sessions[localKey(player, sessionName)]

        fun clearLocal(player: Player, sessionName: String) : Boolean {
            val key = localKey(player, sessionName)
            if (key !in sessions) return false
            sessions.remove(key)
            return true
        }

        fun clearLocal(sessionName: String) : Boolean {
            val localSessions = sessions.filter { it.key.contains("-$sessionName") }
            if (localSessions.isEmpty()) return false
            localSessions.forEach { session, _ ->
                sessions.remove(session)
            }
            return true
        }

        private fun localKey(player: Player, sessionName: String)
            = "${player.uniqueId}-$sessionName"

        fun saveGlobal(sessionName: String, inventory: Inventory)
                = sessions.set(sessionName, inventory)

        fun restoreGlobal(sessionName: String): Inventory?
                = sessions[sessionName]

        fun clearGlobal(sessionName: String) : Boolean {
            if (sessionName !in sessions) return false
            sessions.remove(sessionName)
            return true
        }
    }

}