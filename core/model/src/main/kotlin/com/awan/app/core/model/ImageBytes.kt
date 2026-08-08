package com.awan.app.core.model

/**
 * A decoded, already-downscaled image ready to attach to a request. Not a `data class`: the array
 * would give it reference equality while the generated `equals` promises value semantics.
 */
class ImageBytes(val bytes: ByteArray, val mimeType: String) {
    companion object {
        /** What `POST /v1/ai/image-to-tasks` enforces; reads compress down to it. */
        const val MAX_BYTES = 10 * 1024 * 1024
    }
}
