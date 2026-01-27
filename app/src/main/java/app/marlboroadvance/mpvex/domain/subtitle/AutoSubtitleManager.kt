package app.marlboroadvance.mpvex.domain.subtitle

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SubtitleJob(
    val id: String,
    val videoUri: Uri,
    val videoPath: String,
    val status: JobStatus = JobStatus.Pending,
    val progress: Float = 0f // 0.0 to 1.0
)

enum class JobStatus {
    Pending,
    ExtractingAudio,
    Transcribing,
    GeneratingSrt,
    Completed,
    Failed,
    Cancelled
}

class AutoSubtitleManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _currentJob = MutableStateFlow<SubtitleJob?>(null)
    val currentJob: StateFlow<SubtitleJob?> = _currentJob.asStateFlow()

    fun addToQueue(videoUri: Uri, videoPath: String) {
        val job = SubtitleJob(
            id = java.util.UUID.randomUUID().toString(),
            videoUri = videoUri,
            videoPath = videoPath
        )
        
        val workRequest = androidx.work.OneTimeWorkRequest.Builder(SubtitleWorker::class.java)
            .setInputData(androidx.work.Data.Builder()
                .putString(SubtitleWorker.KEY_VIDEO_PATH, videoPath)
                .putString(SubtitleWorker.KEY_VIDEO_URI, videoUri.toString())
                .putString(SubtitleWorker.KEY_JOB_ID, job.id)
                .build())
            .addTag(job.id)
            .build()

        androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
        
        // Monitor status
        scope.launch(Dispatchers.Main) {
            androidx.work.WorkManager.getInstance(context)
                .getWorkInfoByIdFlow(workRequest.id)
                .collect { workInfo ->
                    if (workInfo != null) {
                        val progress = workInfo.progress.getInt(SubtitleWorker.KEY_PROGRESS, 0)
                        val state = workInfo.progress.getString(SubtitleWorker.KEY_STATE) ?: ""
                        
                        when (workInfo.state) {
                            androidx.work.WorkInfo.State.ENQUEUED -> updateJobStatus(job, JobStatus.Pending, 0f)
                            androidx.work.WorkInfo.State.RUNNING -> {
                                val status = when {
                                    state.contains("Extracting") -> JobStatus.ExtractingAudio
                                    state.contains("Transcribing") -> JobStatus.Transcribing
                                    state.contains("Generating") -> JobStatus.GeneratingSrt
                                    else -> JobStatus.Transcribing
                                }
                                updateJobStatus(job, status, progress / 100f)
                            }
                            androidx.work.WorkInfo.State.SUCCEEDED -> updateJobStatus(job, JobStatus.Completed, 1f)
                            androidx.work.WorkInfo.State.FAILED -> updateJobStatus(job, JobStatus.Failed)
                            androidx.work.WorkInfo.State.CANCELLED -> updateJobStatus(job, JobStatus.Cancelled)
                            else -> {}
                        }
                    }
                }
        }
    }
    
    private fun updateJobStatus(job: SubtitleJob, status: JobStatus, progress: Float = 0f) {
        val updatedJob = job.copy(status = status, progress = progress)
        _currentJob.value = updatedJob
    }

    companion object {
        private const val TAG = "AutoSubtitleManager"
    }
}
