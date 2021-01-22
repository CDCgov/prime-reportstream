package gov.cdc.prime.router.credentials

data class UserPassConnectionCredential(val user: String, val pass: String) : ConnectionCredential