package com.example.batterylevel

import CpuUsage
import android.os.Build
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.io.RandomAccessFile
import java.util.regex.Pattern

/**
 * This class is responsible for providing CPU specific information
 * such as ABI, number of cores, temperature and frequencies
 */
class CpuDataProvider constructor() {
    /**
    Read Android Binary Interface information from the device
     */
    fun getAbi(): String {
        return if (Build.VERSION.SDK_INT >= 21) {
            Build.SUPPORTED_ABIS[0]
        } else {
            @Suppress("DEPRECATION")
            Build.CPU_ABI
        }
    }

    fun getNumberOfCores(): Int {
        return if (Build.VERSION.SDK_INT >= 17) {
            Runtime.getRuntime().availableProcessors()
        } else {
            getNumCoresLegacy()
        }
    }

    /**
     * Checking frequencies directories and return current value if exists (otherwise we can
     * assume that core is stopped - value -1)
     */
    fun getCurrentFreq(coreNumber: Int): Long {
        val currentFreqPath = "${CPU_INFO_DIR}cpu$coreNumber/cpufreq/scaling_cur_freq"
        return try {
            RandomAccessFile(currentFreqPath, "r").use { it.readLine().toLong() / 1000 }
        } catch (e: Exception) {
            e.printStackTrace();
            -1
        }
    }

    /**
     * Read max/min frequencies for specific [coreNumber]. Return [Pair] with min and max frequency
     * or [Pair] with -1.
     */
    fun getMinMaxFreq(coreNumber: Int): Pair<Long, Long> {
        val minPath = "${CPU_INFO_DIR}cpu$coreNumber/cpufreq/cpuinfo_min_freq"
        val maxPath = "${CPU_INFO_DIR}cpu$coreNumber/cpufreq/cpuinfo_max_freq"
        return try {
            val minMhz = RandomAccessFile(minPath, "r").use { it.readLine().toLong() / 1000 }
            val maxMhz = RandomAccessFile(maxPath, "r").use { it.readLine().toLong() / 1000 }
            Pair(minMhz, maxMhz)
        } catch (e: Exception) {
            e.printStackTrace();
            Pair(-1, -1)
        }
    }

    /**
     * Gets the number of cores available in this device, across all processors.
     * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
     *
     * @return The number of cores, or 1 if check fails
     */
    private fun getNumCoresLegacy(): Int {
        class CpuFilter : FileFilter {
            override fun accept(pathname: File): Boolean {
                // Check if filename is "cpu", followed by a single digit number
                if (Pattern.matches("cpu[0-9]+", pathname.name)) {
                    return true
                }
                return false
            }
        }
        return try {
            val dir = File(CPU_INFO_DIR)
            val files = dir.listFiles(CpuFilter())
            files.size
        } catch (e: Exception) {
            1
        }
    }

    /**
     * Retrieves the current overall thermal temperature for all the CPUs
     */
    fun getCpuTemperature(): Double {
        val tempPath = "sys/class/thermal/thermal_zone0/temp"
        return try {
            RandomAccessFile(tempPath, "r").use { it.readLine().toDouble() / 1000 }
        } catch (e: Exception) {
            e.printStackTrace()
            -1.0
        }
    }

    /**
     * Get cpu Usage % from current_freq / max_freq.
     */
    fun getCpuUsagePercentage(coreNumber: Int): Double {
        val currentFreq = getCurrentFreq(coreNumber).toDouble()
        val maxFreq = getMinMaxFreq(coreNumber).second.toDouble()
        val cpuUsage = currentFreq / maxFreq

        return String.format("%.2f", cpuUsage).toDouble()
    }

    companion object {
        private const val CPU_INFO_DIR = "/sys/devices/system/cpu/"
    }
}