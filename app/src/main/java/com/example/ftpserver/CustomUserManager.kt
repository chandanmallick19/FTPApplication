package com.example.ftpserver

import org.apache.ftpserver.usermanager.impl.AbstractUserManager
import org.apache.ftpserver.usermanager.PasswordEncryptor
import org.apache.ftpserver.ftplet.User
import org.apache.ftpserver.ftplet.FtpException

class CustomUserManager(
    private val passwordEncryptor: PasswordEncryptor? = null
) : AbstractUserManager("admin", passwordEncryptor) {

    private val users = mutableMapOf<String, User>()

    override fun save(user: User) {
        users[user.name] = user
    }

    override fun delete(username: String) {
        users.remove(username)
    }

    override fun getUserByName(username: String?): User? {
        return if (username != null) users[username] else null
    }

    override fun getAllUserNames(): Array<String> {
        return users.keys.toTypedArray()
    }

    override fun doesExist(username: String?): Boolean {
        return username != null && users.containsKey(username)
    }

    override fun authenticate(authentication: org.apache.ftpserver.ftplet.Authentication?): User {
        if (authentication is org.apache.ftpserver.usermanager.UsernamePasswordAuthentication) {
            val user = users[authentication.username]
            if (user != null && user.password == authentication.password) {
                return user
            }
        }
        throw FtpException("Authentication failed")
    }
}
