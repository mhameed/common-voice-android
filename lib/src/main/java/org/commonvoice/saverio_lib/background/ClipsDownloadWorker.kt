package org.commonvoice.saverio_lib.background

import android.content.Context
import androidx.work.*
import org.commonvoice.saverio_lib.api.RetrofitFactory
import org.commonvoice.saverio_lib.db.AppDB
import org.commonvoice.saverio_lib.repositories.ClipsRepository
import org.commonvoice.saverio_lib.utils.PrefManager
import org.commonvoice.saverio_lib.utils.getTimestampOfNowPlus
import java.util.concurrent.TimeUnit

class ClipsDownloadWorker(
    appContext: Context,
    private val workerParams: WorkerParameters
): CoroutineWorker(appContext, workerParams) {

    private val db = AppDB.build(appContext)
    private val prefManager = PrefManager(appContext)
    private val retrofitFactory = RetrofitFactory(prefManager)

    private val requiredClips: Int = prefManager.requiredClipsCount

    private val currentLanguage = prefManager.language

    private val clipsRepository = ClipsRepository(db, retrofitFactory)

    override suspend fun doWork(): Result {

        clipsRepository.deleteOldClips(getTimestampOfNowPlus(seconds = 0))

        val numberDifference = requiredClips - clipsRepository.getClipsCount()

        return when {
            numberDifference < 0 -> {
                db.close()
                Result.failure()
            }
            numberDifference == 0 -> {
                db.close()
                Result.success()
            }
            else -> {
                val newClips = clipsRepository.getNewClips(numberDifference)
                if (newClips == null) {
                    db.close()

                    if (workerParams.runAttemptCount > 5) {
                        Result.failure()
                    } else {
                        Result.retry()
                    }
                } else {
                    clipsRepository.insertClips(newClips.map { it.also {
                        it.sentence.setLanguage(currentLanguage)
                    }})

                    db.close()
                    Result.success()
                }
            }
        }

    }

    companion object {

        private const val TAG = "clipsDownloadWorker"

        private val periodicWorkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .build()

        private val periodicWorkRequest = PeriodicWorkRequestBuilder<ClipsDownloadWorker>(
            1, TimeUnit.DAYS, 8, TimeUnit.HOURS
        ).setConstraints(periodicWorkConstraint).build()

        private val oneTimeWorkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        private val oneTimeWorkRequest = OneTimeWorkRequestBuilder<ClipsDownloadWorker>()
            .setConstraints(oneTimeWorkConstraints)
            .build()

        fun attachOneTimeJobToWorkManager(wm: WorkManager) {
            wm.enqueueUniqueWork(
                "oneTimeClipsDownloadWorker",
                ExistingWorkPolicy.KEEP,
                oneTimeWorkRequest
            )
        }

        fun attachPeriodicJobToWorkManager(wm: WorkManager) {
            wm.enqueueUniquePeriodicWork(
                "periodicClipsDownloadWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
        }

    }

}