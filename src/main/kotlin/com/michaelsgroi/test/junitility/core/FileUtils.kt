package com.michaelsgroi.test.junitility.core

import java.io.File
import kotlin.system.exitProcess

object FileUtils {
    fun copyDirectory(
        source: File,
        target: File,
    ) {
        try {
            source.copyRecursively(target, overwrite = true)
        } catch (e: Exception) {
            System.err.println("Error copying directory: ${e.message}")
            exitProcess(3)
        }
    }

    fun promptOverwrite(dir: File): Boolean {
        print("Directory ${dir.path} already exists. Delete and overwrite? (y/n): ")
        val response = readLine()?.trim()?.lowercase()
        return response == "y"
    }
}
