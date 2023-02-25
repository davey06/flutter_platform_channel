package com.example.batterylevel

import CpuUsage
import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.*
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.provider.Settings
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.*


class MainActivity : FlutterActivity() {
    private val CHANNEL = "example.flutter/platform_channel"

    @RequiresApi(VERSION_CODES.JELLY_BEAN_MR1)
    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        cpuProvider = CpuDataProvider()
        cache = hashMapOf<String, Any>()
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        ).setMethodCallHandler { call, result ->
            // This method is invoked on the main thread.
            // method name same with one on Flutter app
            when (call.method) {
                "getBatteryLevel" -> {
                    val batteryLevel = getBatteryLevel();

                    if (batteryLevel != -1) {
                        result.success(batteryLevel)
                    } else {
                        result.error("UNAVAILABLE", "Battery level not available.", null)
                    }
                }
                "changeLife" -> {
                    result.success("Hellow from Android!")
                }
                "getDeviceInfo" -> {
                    val deviceInfo: HashMap<String, String> = getDeviceInfo()
                    if (deviceInfo.isNotEmpty()) {
                        result.success(deviceInfo)
                    } else {
                        result.error("UNAVAILABLE", "Device info not available.", null)
                    }
                }
                "getCPU2" -> {
                    val cpuUsage = getCpuInfo()
                    result.success(cpuUsage)
                }
                "name" -> {
                    result.success(
                        Settings.Global.getString(
                            contentResolver,
                            Settings.Global.DEVICE_NAME
                        )
                    )
                }
                "codePath" -> result.success(activity.packageCodePath)
                "integrityCheck" -> {
                    val argument = call.arguments as Map<String, String>
                    if (argument.isEmpty()) {
                        result.success(false)
                    } else {
                        val packageName = activity.packageName
                        result.success(argument.entries.map {
                            val resId: Int =
                                activity.resources.getIdentifier(it.key, "string", packageName)
                            return@map it.value == activity.getString(resId)
                        }.all { it })
                    }
                }
                "getCPUUsage" -> {
                    val hashMap = HashMap<String, Any>()
                    val manager =
                        activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    val memoryInfo = ActivityManager.MemoryInfo()
                    manager.getMemoryInfo(memoryInfo)
                    val totalMemory = memoryInfo.totalMem
                    val freeMemory = memoryInfo.availMem
                    hashMap["freeMemory"] = freeMemory
                    hashMap["totalMemory"] = totalMemory


                    val file = Environment.getExternalStorageDirectory()
                    val statFs = StatFs(file.path)
                    val blockSizeLong =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            statFs.blockSizeLong
                        } else {
                            statFs.blockSize.toLong()
                        }
                    val blockCountLong =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            statFs.blockCountLong
                        } else {
                            statFs.blockCount.toLong()
                        }
                    val availableBlocksLong =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            statFs.availableBlocksLong
                        } else {
                            statFs.availableBlocks.toLong()
                        }
                    hashMap["freeSpace"] = availableBlocksLong * blockSizeLong
                    hashMap["totalSpace"] = blockCountLong * blockSizeLong

                    hashMap["usageCpu"] = Process.getElapsedCpuTime()
                    hashMap["totalCpu"] = Runtime.getRuntime().availableProcessors()
                    result.success(hashMap)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    private lateinit var cpuProvider: CpuDataProvider
    private lateinit var cache: HashMap<String, Any>

    private fun getCpuInfo(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val abi = cache.getOrPut("abi") { cpuProvider.getAbi() } as String
        val cores = cache.getOrPut("cores") { cpuProvider.getNumberOfCores() }
        val minMaxFrequencies = cache.getOrPut("minMaxFrequencies") {
            val minMax = mutableMapOf<Int, Map<String, Long>>()
            for (i in 0 until cores as Int) {
                val values = cpuProvider.getMinMaxFreq(i);
                val mapOfMinMax = mapOf("min" to values.first, "max" to values.second)
                minMax[i] = mapOfMinMax
            }
            minMax
        } as MutableMap<Int, Map<String, Long>>

        val currentFrequencies = mutableMapOf<Int, Long>()
        val cpuUsagePercentage = mutableMapOf<Int, Double>()
        for (i in 0 until cores as Int) {
            currentFrequencies[i] = cpuProvider.getCurrentFreq(i)
        }
        var totalPercentage = 0.0;
        for (i in 0 until cores as Int) {
            val currentFreq = currentFrequencies[i]?.toDouble() ?: 0.0
            val maxFreq = minMaxFrequencies[i]?.get("max")?.toDouble() ?: 0.0
            var cpuUsage = 0.0
            if (maxFreq > 0) {
                cpuUsage = currentFreq / maxFreq
            }
            totalPercentage += cpuUsage
            cpuUsagePercentage[i] = String.format("%.3f", cpuUsage).toDouble()
        }

        val averagePercentage = totalPercentage / cores

        map["abi"] = abi
        map["numberOfCores"] = cores
        map["minMaxFrequencies"] = minMaxFrequencies
        map["currentFrequencies"] = currentFrequencies
        map["cpuUsagePercentage"] = cpuUsagePercentage
        map["cpuUsageAverage"] = String.format("%.3f", averagePercentage)

        return map
    }


    //This is the native function to get battery level
    private fun getBatteryLevel(): Int {
        val batteryLevel: Int
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } else {
            val intent = ContextWrapper(applicationContext).registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            batteryLevel =
                intent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100 / intent.getIntExtra(
                    BatteryManager.EXTRA_SCALE,
                    -1
                )
        }
        return batteryLevel
    }

    private fun getDeviceInfo(): HashMap<String, String> {
        val deviceInfo = HashMap<String, String>()
        deviceInfo["version"] =
            System.getProperty("os.version")?.toString() ?: "Version Not found" // OS version
        deviceInfo["device"] = android.os.Build.DEVICE           // Device
        deviceInfo["model"] = android.os.Build.MODEL            // Model
        deviceInfo["product"] = android.os.Build.PRODUCT          // Product
        return deviceInfo
    }

    private fun getCpuUsageStatistic(): IntArray {
        var tempString = executeTop()

        tempString = tempString.replace(",", "")
        tempString = tempString.replace("User", "")
        tempString = tempString.replace("System", "")
        tempString = tempString.replace("IOW", "")
        tempString = tempString.replace("IRQ", "")
        tempString = tempString.replace("%", "")
        tempString = tempString.replace("  ", " ")
        tempString = tempString.trim()
        val myString = tempString.split(" ")
        val cpuUsageAsInt = IntArray(myString.size)
        for ((i, value) in myString.withIndex()) {
            cpuUsageAsInt[i] = Integer.parseInt(value.trim())
        }
        return cpuUsageAsInt;
    }

    private fun executeTop(): String {
        var returnString = ""
        try {
            val p = Runtime.getRuntime().exec("top -n 1");
            val br = BufferedReader(InputStreamReader(p.inputStream));
            while (returnString.isEmpty()) {
                returnString = br.readLine();
            }
            br.close()
            p.destroy()
        } catch (e: IOException) {
            Log.e("executeTop", "error in getting first line of top");
            e.printStackTrace();
        }
        return returnString
    }
}
