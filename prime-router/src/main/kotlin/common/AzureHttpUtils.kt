package gov.cdc.prime.router.common

import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.prime.router.azure.db.Tables

object AzureHttpUtils {

    fun getSenderIP(request: HttpRequestMessage<*>): String? {
        return (
            (
                request.headers["x-forwarded-for"]?.split(",")
                    ?.firstOrNull()
                )?.take(Tables.ACTION.SENDER_IP.dataType.length()) ?: request.headers["x-azure-clientip"]
            )
    }
}