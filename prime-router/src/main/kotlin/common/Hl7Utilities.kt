package gov.cdc.prime.router.common

class Hl7Utilities {
    companion object {
        /**
         * Extract a set of messages from the many HL7 messages in [blob].
         * [indices] is zero-based. The resultant HL7 will have the batch FHS heading and trailing segments.
         */
        fun cut(blob: String, indices: List<Int>): String {
            if (blob.isBlank()) {
                if (indices.isEmpty()) return "" else error("Blank content with non-empty indices")
            }
            return when {
                blob.startsWith("MSH") -> cutSingle(blob, indices)
                blob.startsWith("FHS") -> cutMultiple(blob, indices)
                else -> error("Expected HL7 file to start with MSH or FSH segment")
            }
        }

        private fun cutSingle(blob: String, indices: List<Int>): String {
            if (indices.size != 1 || indices[0] != 0) error("Indices are out of bounds")
            return blob
        }

        private fun cutMultiple(blob: String, indices: List<Int>): String {
            val allSegments = blob.split('\r')

            // Find the message breaks
            val messageStartIndices = mutableListOf<Int>()
            allSegments.forEachIndexed { index, segment ->
                if (segment.isBlank()) return@forEachIndexed
                val segmentType = segment.substring(0..2)
                if (segmentType == "MSH" || segmentType == "BTS") {
                    messageStartIndices.add(index)
                }
            }

            // build out segments
            val outSegments = mutableListOf<String>()
            outSegments.add(allSegments[0])
            outSegments.add(allSegments[1])
            indices.forEach { index ->
                if (index >= messageStartIndices.size - 1) error("Index $index is out of bounds of the content")
                val slice = allSegments.slice(messageStartIndices[index] until messageStartIndices[index + 1])
                outSegments.addAll(slice)
            }
            outSegments.add("BTS|${indices.size}")
            outSegments.add("FTS|1")
            return outSegments.joinToString("\r")
        }
    }
}