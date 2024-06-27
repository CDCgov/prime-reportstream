package gov.cdc.prime.reportstream.submissions

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.util.UUID


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonTypeName("convert")
data class FhirConvertQueueMessage(
    val reportId: UUID,
    val blobURL: String,
    val digest: String,
    val blobSubFolderName: String,
)