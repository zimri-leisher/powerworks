package main

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*


enum class OperatingSystem {
    WIN, MAC, UNIX, SUNOS;

    fun getUUID(): UUID {
        if(this == WIN) {
            val process = Runtime.getRuntime().exec("wmic csproduct get UUID")
            val stdInput = BufferedReader(InputStreamReader(process.inputStream))
            val stdError = BufferedReader(InputStreamReader(process.errorStream))
            val error = stdError.readLine()
            if (error != null) {
                throw Exception("Exception while getting UUID of machine: $error")
            }
            val uuid = stdInput.lines().skip(2).findFirst().get().trim()
            return UUID.fromString(uuid)
        } else if(this == MAC) {
            val process = Runtime.getRuntime().exec("system_profiler SPHardwareDataType | awk '/UUID/ { print \$3; }'")
            val stdInput = BufferedReader(InputStreamReader(process.inputStream))
            val stdError = BufferedReader(InputStreamReader(process.errorStream))
            val error = stdError.readLine()
            if (error != null) {
                throw Exception("Exception while getting UUID of machine: $error")
            }
            // TODO test for macs
            val uuid = stdInput.lines().skip(2).findFirst().get().trim()
            return UUID.fromString(uuid)
        } else {
            return UUID.randomUUID()
        }
    }

    companion object {
        fun get(): OperatingSystem {
            val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
            return when {
                osName.indexOf("win") >= 0 -> WIN
                osName.indexOf("mac") >= 0 -> MAC
                osName.indexOf("sunos") >= 0 -> SUNOS
                osName.indexOf("nix") >= 0 || osName.indexOf("nux") >= 0 || osName.indexOf("aix") > 0 -> UNIX
                else -> throw UnsupportedOperationException("Unsupported operating system")
            }
        }
    }
}