# Logging to AppInsights

## Background

All logging via Log4J is automatically captured and sent to Azure AppInsights given you are running in a deployed 
environment (or have AppInsights set up locally). We are able to query for specific logs using either the log messaging or a 
variety of properties.

## Setting custom properties on logs

Use the extension functions located at [LoggerExt.kt](../../src/main/kotlin/common/LoggerExt.kt). Alongside your 
 message, you can pass an optional context map containing additional fields. The fields in this map will populate the 
`customDimensions` column in the Azure `traces` or `exceptions` table. Anything you want to be easily queryable should go in this map. 

```kotlin
// logging message in source code with additional properties
logger.info("Hello world!", mapOf("property" to "value"))
```

```
// query to find log message in Azure Log Explorer
traces | where customDimensions.property == "value"
```

## Logging structure

Logs are now output in a JSON structure that Azure AppInsights knows how to ingest.

```kotlin
logger.info("Hello world!", mapOf("property" to "value"))
```

```json
{
  "mdc":{
    "property":"value",
    "span_id":"ed5839f999bd014f",
    "trace_flags":"01",
    "trace_id":"1a66f28df94b7dcf0eb29ee0287e54d5"
  },
  "message":"Hello world!",
  "thread":"pool-8-thread-45",
  "timestamp":"2024-01-18T20:34:37.958Z",
  "level":"INFO",
  "logger":"gov.cdc.prime.router.tokens.AuthenticatedClaims"
}
```

Notice the additional fields present in each log.

One of the more interesting fields which will be helpful to debug production issues will be `trace_id`. This field is injected by the 
AppInsights Java Agent and remains the same for all logs occuring during a particular function invocation (whether triggered
by an http request, timer, queue, etc).

```
// Show all logs during a function invocation
traces | where customDimensions.trace_id == "1a66f28df94b7dcf0eb29ee0287e54d5"
```

## Exceptions

When an exception is logged it will end up in the `exceptions` table rather than the `traces` table. It has a few additional
fields to help figure out the cause of the exception.

```kotlin
logger.error("Kaboom!", RuntimeException("Why?"), mapOf("property" to "value"))
```

```json
{
  "mdc":{
    "property":"value",
    "span_id":"8827a252c589cb42",
    "trace_flags":"01",
    "trace_id":"365d09076e60d179b807c9d0dddf86d9"
  },
  "exceptionClass":"java.lang.RuntimeException",
  "stackTrace":"java.lang.RuntimeException: Why?\n\tat gov.cdc.prime.router.tokens.AuthenticatedClaims$Companion.authenticate(AuthenticatedClaims.kt:207)\n\tat gov.cdc.prime.router.history.azure.ReportFileFunction.authSingleBlocks(ReportFileFunction.kt:160)\n\tat gov.cdc.prime.router.history.azure.ReportFileFunction.getDetailedView(ReportFileFunction.kt:124)\n\tat gov.cdc.prime.router.history.azure.SubmissionFunction.getReportDetailedHistory(SubmissionFunction.kt:127)\n\tat java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n\tat java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)\n\tat java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n\tat java.base/java.lang.reflect.Method.invoke(Method.java:568)\n\tat com.microsoft.azure.functions.worker.broker.JavaMethodInvokeInfo.invoke(JavaMethodInvokeInfo.java:22)\n\tat com.microsoft.azure.functions.worker.broker.EnhancedJavaMethodExecutorImpl.execute(EnhancedJavaMethodExecutorImpl.java:22)\n\tat com.microsoft.azure.functions.worker.chain.FunctionExecutionMiddleware.invoke(FunctionExecutionMiddleware.java:19)\n\tat com.microsoft.azure.functions.worker.chain.InvocationChain.doNext(InvocationChain.java:21)\n\tat com.microsoft.azure.functions.worker.broker.JavaFunctionBroker.invokeMethod(JavaFunctionBroker.java:125)\n\tat com.microsoft.azure.functions.worker.handler.InvocationRequestHandler.execute(InvocationRequestHandler.java:37)\n\tat com.microsoft.azure.functions.worker.handler.InvocationRequestHandler.execute(InvocationRequestHandler.java:12)\n\tat com.microsoft.azure.functions.worker.handler.MessageHandler.handle(MessageHandler.java:44)\n\tat com.microsoft.azure.functions.worker.JavaWorkerClient$StreamingMessagePeer.lambda$onNext$0(JavaWorkerClient.java:93)\n\tat java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:539)\n\tat java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)\n\tat java.base/java.lang.Thread.run(Thread.java:840)\n",
  "message":"Kaboom!",
  "thread":"pool-8-thread-45",
  "timestamp":"2024-01-18T20:44:53.893Z",
  "level":"ERROR",
  "logger":"gov.cdc.prime.router.tokens.AuthenticatedClaims"
}
```