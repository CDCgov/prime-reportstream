package gov.cdc.prime.router.poster;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class PortalPosterReporter implements CommandLineRunner {

    private static final String FILE_DELIMITER_REGEX = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";

    @Value("${gov.cdc.prime.router.poster.input.file}")
    private String inputFile;

    @Value("${gov.cdc.prime.router.poster.skip.first.line}")
    private boolean skipFirstLine;

    @Value("${gov.cdc.prime.router.poster.wait.time.in.seconds}")
    private int waitTimeInSeconds;

    @Value("${gov.cdc.prime.router.poster.url}")
    private String url;

    @Value("${gov.cdc.prime.router.poster.token}")
    private String token;

    @Value("${gov.cdc.prime.router.poster.organization}")
    private String organization;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting PortalPosterReporter...");
        log.info("Input file: {}", inputFile);
        BufferedWriter submissionHistoryWriter = getSubmissionHistoryFile();
        submissionHistoryWriter.write("Report ID,Min of Created At: Minute,Min of Organization - Org → Organization Name,id,submission id,destinationCount,overallStatus");
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
                    log.info("Skipping first line: {}", line);
                    continue;
                }
                Instant then = Instant.now();
                log.info("line [{}]: {}", lineCount, line);
                String[] splitLine = line.split(FILE_DELIMITER_REGEX);
                log.debug("splitLine: {}", (Object) splitLine);
                // Make the submission history call to ReportStream.
                HttpHeaders requestHeaders = new HttpHeaders();
                requestHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                requestHeaders.set("authentication-type", "okta");
                requestHeaders.set("Organization", organization);
                HttpEntity<String> entity = new HttpEntity<>(requestHeaders);
                try {
                    ResponseEntity<String> result = restTemplate.exchange(URI.create(String.format(url, splitLine[3])), HttpMethod.GET, entity, String.class);
                    log.info("result: {}", result);
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode jsonNode = mapper.readTree(result.getBody());
                    String destinationCount = jsonNode.get("destinationCount").asText();
                    String overallStatus = jsonNode.get("overallStatus").asText();
                    log.info("input id=[{}], id=[{}], submissionId=[{}], destinationCount=[{}], overallStatus=[{}].", splitLine[0], splitLine[3], splitLine[4], destinationCount, overallStatus);
                    submissionHistoryWriter.write(splitLine[0] + "," + splitLine[1] + "," + splitLine[2] + "," + splitLine[3] + "," + splitLine[4] +  "," + destinationCount + "," + overallStatus);
                    submissionHistoryWriter.newLine();
                } catch (Exception e) {
                    log.error("Error retrieving submission history for [{}]", splitLine[4], e);
                    continue;
                }
                while (Duration.between(then, Instant.now()).toSeconds() < waitTimeInSeconds) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            log.info("Number of lines: {}", lineCount);
        } catch (Exception e) {
            log.error("Error while reading data from {}", inputFile, e);
        }
        submissionHistoryWriter.flush();
        submissionHistoryWriter.close();
        log.info("Ending PortalPosterReporter.");
    }

    public BufferedWriter getSubmissionHistoryFile() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        String formattedDateTime = now.format(formatter);
        String filePath = formattedDateTime + "-portal-poster-reporter.csv";
        log.info("Submission history written to: {}", filePath);
        return new BufferedWriter(new FileWriter(filePath));
    }
}
