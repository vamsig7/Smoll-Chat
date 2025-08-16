package com.krishna.passwordstrengthener.data

class WeakPasswordProvider {
    fun getSessionPasswords(): List<String> = StaticWeakPasswords.pickRandomTen()
}
