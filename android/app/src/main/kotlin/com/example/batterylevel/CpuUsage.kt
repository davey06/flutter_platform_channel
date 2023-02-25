import android.os.*
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

class CpuUsage {
    private var mLastCpuTime: LongArray? = null
    private var mLastAppCpuTime: Long = 0
    private var mHandler: Handler? = null

    init {
        mLastCpuTime = LongArray(4)
        mHandler = Handler(Looper.getMainLooper())
    }

    fun getCpuUsage(): Double {
        val pid = Process.myPid()
        val cpuStatFile = File("/proc/stat")
        val cpuPidStatFile = File("/proc/$pid/stat")
        var totalCpuTime = 0L
        var appCpuTime = 0L
        var p = Runtime.getRuntime().exec("top -n 1")
        var br = BufferedReader(InputStreamReader(p.getInputStream()))
        var returnString: String = br.readLine()
        try {
            val reader = BufferedReader(InputStreamReader(FileInputStream(cpuStatFile)), 1000)
            var line: String? = reader.readLine()
            while (line != null) {
                if (line.startsWith("cpu")) {
                    val fields = line.split(" +".toRegex()).toTypedArray()
                    for (i in 1 until fields.size) {
                        totalCpuTime += fields[i].toLong()
                    }
                }
                line = reader.readLine()
            }
            reader.close()
            val pidReader = BufferedReader(InputStreamReader(FileInputStream(cpuPidStatFile)), 1000)
            val pidLine = pidReader.readLine()
            val fields = pidLine.split(" +".toRegex()).toTypedArray()
            val utime = fields[13].toLong()
            val stime = fields[14].toLong()
            val cutime = fields[15].toLong()
            val cstime = fields[16].toLong()
            appCpuTime = utime + stime + cutime + cstime
            pidReader.close()
        } catch (e: Exception) {
            Log.e("CPU_USAGE", e.toString())
        }
        val currentTime = SystemClock.elapsedRealtime()
        val elapsedTime = currentTime - mLastAppCpuTime
        mLastAppCpuTime = currentTime
        val totalElapsedTime = SystemClock.elapsedRealtime() - (mLastCpuTime?.get(3) ?: 0)
        mLastCpuTime?.set(3, currentTime)
        val usage = if (elapsedTime > 0) {
            (appCpuTime - mLastAppCpuTime) * 100.0 / elapsedTime / numCores()
        } else {
            0.0
        }
        mLastAppCpuTime = appCpuTime
        mLastCpuTime?.set(0, totalCpuTime)
        mLastCpuTime?.set(1, appCpuTime)
        mLastCpuTime?.set(2, totalElapsedTime)
        return usage
    }

    private fun numCores(): Int {
        return if (Build.VERSION.SDK_INT >= 17) {
            Runtime.getRuntime().availableProcessors()
        } else {
            1
        }
    }
}
