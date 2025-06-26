package gov.cdc.prime.router.poster;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class PortalPoster implements CommandLineRunner {

    private static final String FILE_DELIMITER_REGEX = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";

    @Value("${gov.cdc.prime.router.poster.input.file}")
    private String inputFile;

    @Value("${gov.cdc.prime.router.poster.payload.directory}")
    private String payloadFileDirectory;

    @Value("${gov.cdc.prime.router.poster.skip.first.line}")
    private boolean skipFirstLine;

    @Value("${gov.cdc.prime.router.poster.wait.time.in.seconds}")
    private int waitTimeInSeconds;

    @Value("${gov.cdc.prime.router.poster.url}")
    private String url;

    @Value("${gov.cdc.prime.router.poster.client}")
    private String client;

    @Value("${gov.cdc.prime.router.poster.x.functions.key}")
    private String xFunctionsKey;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting PortalPoster...");
        log.info("Input file: {}", inputFile);
        log.info("Payload file directory: {}", payloadFileDirectory);
        // Verify that payloadFileDirectory exists.
        File directory = new File(payloadFileDirectory);
        if (!directory.isDirectory()) {
            log.error("Payload file directory does not exist or is not a directory at [{}].", payloadFileDirectory);
            return;
        }
        BufferedWriter historyWriter = getPortalHistoryFile();
        historyWriter.write("Report ID,Min of Created At: Minute,Min of Organization - Org → Organization Name,id,submission id");
        historyWriter.newLine();
        // Loop through input CSV file.
        // Send one file every waitTimeInSeconds seconds.
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            RestTemplate restTemplate = new RestTemplate();
            URI uri = new URI(url);
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
                // For each report id in the input file find the corresponding payload file.
                String payloadFile = payloadFileDirectory + FileSystems.getDefault().getSeparator() + splitLine[0] + ".csv";
                log.debug("Payload file: {}", payloadFile);
                File f = new File(payloadFile);
                if (f.exists()) {
                    String content = Files.readString(f.toPath());
                    log.debug("content: {}", content);
                    // POST the contents of the payload file to ReportStream.
                    HttpHeaders requestHeaders = new HttpHeaders();
                    requestHeaders.setContentType(new MediaType("text", "csv"));
                    requestHeaders.set("client", client);
                    requestHeaders.set("x-functions-key", xFunctionsKey);
                    HttpEntity<String> requestEntity = new HttpEntity<>(content, requestHeaders);
                    try {
                        ResponseEntity<String> result = restTemplate.postForEntity(uri, requestEntity, String.class);
                        log.info("result: {}", result);
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode jsonNode = mapper.readTree(result.getBody());
                        String id = jsonNode.get("id").asText();
                        String submissionId = jsonNode.get("submissionId").asText();
                        log.info("input id=[{}], id=[{}], submissionId=[{}].", splitLine[0], id, submissionId);
                        historyWriter.write(splitLine[0] + "," + splitLine[1] + "," + splitLine[2] + "," + id + "," + submissionId);
                        historyWriter.newLine();
                    } catch (Exception e) {
                        log.error("Error while posting data from {}", payloadFile, e);
                        continue;
                    }
                } else {
                    log.error("Payload file [{}] does not exist.", payloadFile);
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
        historyWriter.flush();
        historyWriter.close();
        log.info("Ending PortalPoster.");
    }

    public BufferedWriter getPortalHistoryFile() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        String formattedDateTime = now.format(formatter);
        String filePath = formattedDateTime + "-portal-poster-history.csv";
        return new BufferedWriter(new FileWriter(filePath));
    }
}
