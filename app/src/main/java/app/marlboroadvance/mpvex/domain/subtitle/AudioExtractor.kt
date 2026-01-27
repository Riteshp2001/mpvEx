package app.marlboroadvance.mpvex.domain.subtitle

import android.content.Context
import android.media.*
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Extracts audio from a video file and converts it to 16kHz Mono PCM WAV format
 * for WhisperKit transcription using only standard Android APIs.
 */
object AudioExtractor {
    private const val TAG = "AudioExtractor"
    private const val TARGET_SAMPLE_RATE = 16000
    private const val TARGET_CHANNELS = 1

    fun extractToWav(videoPath: String, outputFile: File): Boolean {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        val fos = FileOutputStream(outputFile)

        try {
            extractor.setDataSource(videoPath)
            val trackIndex = selectAudioTrack(extractor)
            if (trackIndex < 0) return false

            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return false
            
            extractor.selectTrack(trackIndex)
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            // Write temporary WAV header (placeholders)
            writeWavHeader(fos, 0, TARGET_SAMPLE_RATE, TARGET_CHANNELS)

            var totalPcmSize = 0
            val info = MediaCodec.BufferInfo()
            var isEOS = false

            while (!isEOS) {
                val inputIndex = codec.dequeueInputBuffer(10000)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)!!
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEOS = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                var outputIndex = codec.dequeueOutputBuffer(info, 10000)
                while (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)!!
                    val outFormat = codec.getOutputFormat(outputIndex)
                    
                    val sampleRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    val channelCount = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

                    // Process and write chunks
                    val pcmData = ByteArray(info.size)
                    outputBuffer.get(pcmData)
                    outputBuffer.clear()

                    val processedData = processAudioData(pcmData, sampleRate, channelCount)
                    fos.write(processedData)
                    totalPcmSize += processedData.size

                    codec.releaseOutputBuffer(outputIndex, false)
                    outputIndex = codec.dequeueOutputBuffer(info, 0)
                }
            }

            // Update WAV header with final size
            fos.channel.position(0)
            writeWavHeader(fos, totalPcmSize, TARGET_SAMPLE_RATE, TARGET_CHANNELS)
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Extraction failed", e)
            return false
        } finally {
            codec?.stop()
            codec?.release()
            extractor.release()
            fos.close()
        }
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) return i
        }
        return -1
    }

    private fun processAudioData(data: ByteArray, sampleRate: Int, channelCount: Int): ByteArray {
        // 1. Convert to ShortArray (16-bit PCM)
        val shorts = ShortArray(data.size / 2)
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)

        // 2. Downmix to mono if needed
        var monoShorts = if (channelCount > 1) {
            ShortArray(shorts.size / channelCount) { i ->
                var sum = 0
                for (c in 0 until channelCount) {
                    sum += shorts[i * channelCount + c]
                }
                (sum / channelCount).toShort()
            }
        } else shorts

        // 3. Resample to 16kHz if needed (Simple Decimation/Interpolation)
        if (sampleRate != TARGET_SAMPLE_RATE) {
            val ratio = sampleRate.toDouble() / TARGET_SAMPLE_RATE
            val targetSize = (monoShorts.size / ratio).toInt()
            val resampled = ShortArray(targetSize)
            for (i in 0 until targetSize) {
                val index = (i * ratio).toInt()
                if (index < monoShorts.size) {
                    resampled[i] = monoShorts[index]
                }
            }
            monoShorts = resampled
        }

        // 4. Convert back to ByteArray
        val result = ByteArray(monoShorts.size * 2)
        ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(monoShorts)
        return result
    }

    private fun writeWavHeader(fos: FileOutputStream, pcmSize: Int, sampleRate: Int, channels: Int) {
        val totalDataLen = pcmSize + 36
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44)
        header[0] = 'R'.toByte() // RIFF
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.toByte() // WAVE
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()
        header[12] = 'f'.toByte() // 'fmt ' chunk
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1 (PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte()
        header[27] = (sampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (channels * 2).toByte() // block align
        header[33] = 0
        header[34] = 16 // bits per sample
        header[35] = 0
        header[36] = 'd'.toByte() // data chunk
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()
        header[40] = (pcmSize and 0xff).toByte()
        header[41] = (pcmSize shr 8 and 0xff).toByte()
        header[42] = (pcmSize shr 16 and 0xff).toByte()
        header[43] = (pcmSize shr 24 and 0xff).toByte()

        fos.write(header)
    }
}
