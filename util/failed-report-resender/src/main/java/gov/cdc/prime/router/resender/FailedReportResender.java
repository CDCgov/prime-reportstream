package gov.cdc.prime.router.resender;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class FailedReportResender implements CommandLineRunner {

  @Value("${gov.cdc.prime.router.resender.input.file}")
  private String inputFile;

  @Value("${gov.cdc.prime.router.resender.skip.first.line}")
  private boolean skipFirstLine;

  @Value("${gov.cdc.prime.router.resender.wait.time.in.seconds}")
  private int waitTimeInSeconds;

  @Value("${gov.cdc.prime.router.resender.baseUrl}")
  private String baseUrl;

  @Value("${gov.cdc.prime.router.resender.token}")
  private String token;

  @Value("${gov.cdc.prime.router.resender.receiver}")
  private String receiver;

  @Override
  public void run(String... args) throws Exception {
    log.info("Starting FailedReportResender...");
    log.info("Input file: [{}]", inputFile);
    BufferedWriter submissionHistoryWriter = getSubmissionHistoryFile();
    submissionHistoryWriter.write("Report ID,Result");
    submissionHistoryWriter.newLine();
    // Loop through input CSV file.
    // Send one file every waitTimeInSeconds seconds.
    try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
      RestTemplate restTemplate = new RestTemplate();
      int lineCount = 0;
      String line;
      while ((line = br.readLine()) != null) {
        lineCount++;
        if (lineCount == 1 && skipFirstLine) {
          log.info("Skipping first line: [{}]", line);
          continue;
        }
        Instant then = Instant.now();
        log.info("line [{}]: {}", lineCount, line);
        // Make the resend call to ReportStream.
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        requestHeaders.set("authentication-type", "okta");
        HttpEntity<String> requestEntity = new HttpEntity<>(requestHeaders);
        try {
          UriComponentsBuilder builder = UriComponentsBuilder.fromUri(URI.create(baseUrl))
            .queryParam("reportId", line)
            .queryParam("receiver", receiver);

          String url = builder.toUriString();
          log.info("url=[{}]", url);

          // Make the POST request
          ResponseEntity<String> result = restTemplate.postForEntity(url, requestEntity, String.class);
          log.info("result: [{}]", result);
          submissionHistoryWriter.write(line + "," + StringEscapeUtils.escapeCsv(result.toString()));
          submissionHistoryWriter.newLine();
          submissionHistoryWriter.flush();
        } catch (Exception e) {
          log.error("Error resending report id: [{}]", line, e);
          continue;
        }
        while (Duration.between(then, Instant.now()).toSeconds() < waitTimeInSeconds) {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException ignored) {
          }
        }
      }
      log.info("Number of lines: [{}]", lineCount);
    } catch (Exception e) {
      log.error("Error while reading data from [{}]", inputFile, e);
    }
    submissionHistoryWriter.flush();
    submissionHistoryWriter.close();
    log.info("Ending FailedReportResender.");
  }

  public BufferedWriter getSubmissionHistoryFile() throws IOException {
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    String formattedDateTime = now.format(formatter);
    String filePath = formattedDateTime + "-failed-report-resender.csv";
    log.info("Resent failed reports logged to: [{}]", filePath);
    return new BufferedWriter(new FileWriter(filePath));
  }
}
