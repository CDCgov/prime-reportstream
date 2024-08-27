package gov.cdc.prime.reportstream.auth.model

sealed interface AuthenticationFailure
data object InvalidAuthHeader : AuthenticationFailure
data object OktaFailure : AuthenticationFailure
data object InactiveToken : AuthenticationFailure
data object InvalidClientId : AuthenticationFailure