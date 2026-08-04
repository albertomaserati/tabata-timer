package com.amasesoft.tabatatimer

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform