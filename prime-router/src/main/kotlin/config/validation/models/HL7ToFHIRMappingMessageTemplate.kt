package gov.cdc.prime.router.config.validation.models

import io.github.linuxforhealth.api.Condition
import io.github.linuxforhealth.hl7.expression.ExpressionAttributes
import io.github.linuxforhealth.hl7.message.HL7Segment

/**
 * Much more simple model strictly for validation purposes
 *
 * Original could not be used as it had to be loaded directly from the directory structure and
 * is deserialized into one compressed model, breaking the ability for us to pinpoint specific files
 *
 * @see io.github.linuxforhealth.hl7.message.HL7MessageModel
 * @see io.github.linuxforhealth.hl7.message.HL7FHIRResourceTemplateAttributes
 * @see io.github.linuxforhealth.hl7.message.HL7Segment
 */
data class HL7ToFHIRMappingMessageTemplate(val resources: List<MappingTemplateResource>)

data class MappingTemplateResource(
    val resourceName: String,
    val segment: HL7Segment,
    val resourcePath: String,
    val repeats: Boolean?,
    val isReferenced: Boolean?,
    val group: String?,
    val additionalSegments: List<HL7Segment>?,
)

/**
 * Model with conditions extracted from the expression tree for easier validation
 */
data class HL7ToFHIRMappingResourceTemplate(
    val resourceType: String?,
    val expressions: Map<String, ExpressionAttributes>,
    val flatConditions: List<Condition>,
)