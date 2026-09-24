package com.lonx.lyrico.worker.processor

import com.lonx.lyrico.data.model.ExportDestination
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatchExportTaskConfigTest {

    @Test
    fun legacyConfigDefaultsToSelectedDirectory() {
        val config = Json.decodeFromString<BatchExportTaskConfig>(
            """{"destinationTreeUri":"content://documents/tree/music","concurrency":3}"""
        )

        assertEquals(ExportDestination.SELECTED_DIRECTORY, config.destination)
        assertEquals("content://documents/tree/music", config.destinationTreeUri)
    }

    @Test
    fun audioDirectoryConfigDoesNotRequireOneSharedTreeUri() {
        val encoded = Json.encodeToString(
            BatchExportTaskConfig.serializer(),
            BatchExportTaskConfig(destination = ExportDestination.AUDIO_DIRECTORY)
        )

        val decoded = Json.decodeFromString<BatchExportTaskConfig>(encoded)
        assertEquals(ExportDestination.AUDIO_DIRECTORY, decoded.destination)
        assertNull(decoded.destinationTreeUri)
    }
}
