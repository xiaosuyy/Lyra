import org.gradle.api.Project
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Common {
    fun getBuildVersionCode(): Int {
        val appVerCode: Int by lazy {
            val versionCode = SimpleDateFormat("yyMMddHH", Locale.ENGLISH).format(Date())
            versionCode.toInt()
        }
        return appVerCode
    }

    private fun getCurrentDate(project: Project): String {
        val override = project.findProperty("buildVersionTimestamp") as String?
        if (!override.isNullOrBlank()) {
            return override
        }

        val sdf = SimpleDateFormat("MMddHHmm", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Taipei")
        return sdf.format(Date())
    }


    private fun getShortGitRevision(): String {
        // 没有安装 git 或者源码目录不是仓库时也可以正常配置构建
        return runCatching {
            val command = "git rev-parse --short HEAD"
            val processBuilder = ProcessBuilder(*command.split(" ").toTypedArray())
            val process = processBuilder.start()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                output.trim()
            } else {
                "no_commit"
            }
        }.getOrDefault("no_commit")
    }

    fun getBuildVersionName(project: Project): String {
        return "${getShortGitRevision()}.${getCurrentDate(project)}"
    }
}
