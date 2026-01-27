package app.marlboroadvance.mpvex.domain.subtitle

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.argmaxinc.whisperkit.android.WhisperKit
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.floor
import app.marlboroadvance.mpvex.R
import androidx.core.app.NotificationCompat

class SubtitleWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private var whisperKit: WhisperKit? = null
    private val collectedSegments = java.util.Collections.synchronizedList(mutableListOf<MySegment>())

    override suspend fun doWork(): Result {
        val videoPath = inputData.getString(KEY_VIDEO_PATH) ?: return Result.failure()
        val videoUriString = inputData.getString(KEY_VIDEO_URI) ?: return Result.failure()
        val videoUri = Uri.parse(videoUriString)
        val jobId = inputData.getString(KEY_JOB_ID) ?: java.util.UUID.randomUUID().toString()

        setForeground(createForegroundInfo(videoPath))

        return try {
            processTranscription(videoPath, jobId)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in SubtitleWorker", e)
            Result.failure()
        }
    }

    private suspend fun processTranscription(videoPath: String, jobId: String) {
        val audioFile = File(context.cacheDir, "$jobId.wav")
        if (audioFile.exists()) audioFile.delete()

        try {
            // 1. Extract Audio
            updateProgress("Extracting audio...", 0)
            val ffmpegCommand = "-i \"$videoPath\" -ar 16000 -ac 1 -c:a pcm_s16le \"${audioFile.absolutePath}\""
            val session = FFmpegKit.execute(ffmpegCommand)
            
            if (!ReturnCode.isSuccess(session.returnCode)) {
                throw Exception("FFmpeg extraction failed: ${session.failStackTrace}")
            }

            // 2. Initialize WhisperKit
            updateProgress("Initializing model...", 20)
            initializeWhisperKit()

            // 3. Transcribe
            updateProgress("Transcribing...", 40)
            val segments = transcribeAudio(audioFile)

            // 4. Generate SRT
            updateProgress("Generating subtitles...", 90)
            val srtFile = File(File(videoPath).parentFile, "${File(videoPath).nameWithoutExtension}.srt")
            generateSrtFile(segments, srtFile)

            updateProgress("Completed", 100)
            
        } finally {
            if (audioFile.exists()) audioFile.delete()
        }
    }

    private suspend fun initializeWhisperKit() {
        if (whisperKit == null) {
            whisperKit = WhisperKit.Builder()
                .setModel(WhisperKit.Builder.OPENAI_TINY_EN)
                .setApplicationContext(context)
                .setCallback { what, result ->
                    if (what == WhisperKit.TextOutputCallback.MSG_TEXT_OUT && result != null) {
                        result.segments?.forEach { segment ->
                            if (segment.text.isNotBlank()) {
                                collectedSegments.add(MySegment(segment.start, segment.end, segment.text))
                            }
                        }
                    }
                }
                .build()
            
            whisperKit?.loadModel()?.collect {}
            whisperKit?.init(frequency = 16000, channels = 1, duration = 0)
        }
    }

    private suspend fun transcribeAudio(audioFile: File): List<MySegment> {
        collectedSegments.clear()
        
        val bufferSize = 16000 * 2 * 30 
        val buffer = ByteArray(bufferSize)
        
        withContext(Dispatchers.IO) {
            audioFile.inputStream().use { input ->
                var bytesRead = input.read(buffer)
                while (bytesRead != -1) {
                    val chunk = if (bytesRead == buffer.size) buffer else buffer.copyOf(bytesRead)
                    whisperKit?.transcribe(chunk)
                    bytesRead = input.read(buffer)
                }
            }
        }
        
        kotlinx.coroutines.delay(500)
        return collectedSegments.sortedBy { it.start }
    }

    private fun generateSrtFile(segments: List<MySegment>, outputFile: File) {
        val sb = StringBuilder()
        segments.forEachIndexed { index, segment ->
            sb.append("${index + 1}\n")
            sb.append("${formatTime(segment.start)} --> ${formatTime(segment.end)}\n")
            sb.append("${segment.text.trim()}\n\n")
        }
        outputFile.writeText(sb.toString())
    }

    private fun formatTime(seconds: Float): String {
        val h = floor(seconds / 3600).toInt()
        val m = floor((seconds % 3600) / 60).toInt()
        val s = floor(seconds % 60).toInt()
        val ms = floor((seconds * 1000) % 1000).toInt()
        return String.format("%02d:%02d:%02d,%03d", h, m, s, ms)
    }

    private suspend fun updateProgress(state: String, progressPercent: Int) {
        setProgress(workDataOf(KEY_PROGRESS to progressPercent, KEY_STATE to state))
        setForeground(createForegroundInfo(state))
    }

    private fun createForegroundInfo(content: String): ForegroundInfo {
        val id = "auto_subtitle_channel"
        val title = "Auto Subtitle Generation"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, title, NotificationManager.IMPORTANCE_LOW)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, id)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher) // Use app icon
            .setOngoing(true)
            .setProgress(100, 0, true) // Indeterminate for now as we don't have fine-grained progress from WhisperKit easily
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    // Duplicate MySegment to be self-contained or import
    data class MySegment(val start: Float, val end: Float, val text: String)

    companion object {
        const val TAG = "SubtitleWorker"
        const val KEY_VIDEO_PATH = "video_path"
        const val KEY_VIDEO_URI = "video_uri"
        const val KEY_JOB_ID = "job_id"
        const val KEY_PROGRESS = "progress"
        const val KEY_STATE = "state"
        const val NOTIFICATION_ID = 1001
    }
}
