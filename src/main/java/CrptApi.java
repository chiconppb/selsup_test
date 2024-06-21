import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {
    private final int requestLimit;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ReentrantLock lock = new ReentrantLock();


    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        scheduler.scheduleAtFixedRate(() -> counter.set(0), 0, 1, timeUnit);
    }

    public void createDocument(Document document) throws Exception {
        lock.lock();
        try {
            if (counter.incrementAndGet() > requestLimit) {
                counter.decrementAndGet();
                throw new RuntimeException("Too many requests");
            }
            sendRequest(document);
        } finally {
            lock.unlock();
        }
    }

    private void sendRequest(Document document) throws Exception {
        var url = URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String jsonInputString = document.toJson();
            try (OutputStream outputStream = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                outputStream.write(input, 0, input.length);
            }
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("RESPONSE CODE: " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }

    public class Document {
        @JsonProperty("description")
        public String description;

        @JsonProperty("doc_id")
        public String docId;

        @JsonProperty("doc_status")
        public String docStatus;

        @JsonProperty("doc_type")
        public String docType;

        @JsonProperty("importRequest")
        public boolean importRequest;

        @JsonProperty("owner_inn")
        public String ownerInn;

        @JsonProperty("participant_inn")
        public String participantInn;

        @JsonProperty("producer_inn")
        public String producerInn;

        @JsonProperty("production_date")
        public String productionDate;

        @JsonProperty("production_type")
        public String productionType;

        @JsonProperty("products")
        public List<Product> products;

        public static class Product {
            @JsonProperty("certificate_document")
            public String certificateDocument;

            @JsonProperty("certificate_document_date")
            public String certificateDocumentDate;

            @JsonProperty("certificate_document_number")
            public String certificateDocumentNumber;

            @JsonProperty("owner_inn")
            public String ownerInn;

            @JsonProperty("producer_inn")
            public String producerInn;

            @JsonProperty("production_date")
            public String productionDate;

            @JsonProperty("tnved_code")
            public String tnvedCode;

            @JsonProperty("uit_code")
            public String uitCode;

            @JsonProperty("uitu_code")
            public String uituCode;

            @JsonProperty("reg_date")
            public String regDate;

            @JsonProperty("reg_number")
            public String regNumber;
        }

        public String toJson() throws Exception {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(this);
        }
    }

}
