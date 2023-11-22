package de.rieckpil.courses.book.management;

import com.nimbusds.jose.JOSEException;
import de.rieckpil.courses.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

import static org.testcontainers.shaded.org.awaitility.Awaitility.given;

class BookSynchronizationListenerRefactoredIT extends AbstractIntegrationTest {

  private static final String ISBN = "9780596004651";
  private static String VALID_RESPONSE;

  static {
    try {
      VALID_RESPONSE = new String(Objects.requireNonNull(
          OpenLibraryApiClientTest.class.getClassLoader()
            .getResourceAsStream("stubs/openlibrary/success-" + ISBN + ".json"))
        .readAllBytes()
      );
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private SqsAsyncClient sqsClient;

  @Autowired
  private BookRepository bookRepository;

  @Test
  void shouldGetSuccessWhenClientIsAuthenticated() throws JOSEException {
    webTestClient
      .get()
      .uri("/api/books/reviews/statistics")
      .header(HttpHeaders.AUTHORIZATION, "Bearer " + getSignedJWT())
      .exchange()
      .expectStatus().is2xxSuccessful();
  }

  @Test
  void shouldReturnBookFromAPIWhenApplicationConsumesNewSyncRequest() {
    // VALIDATE no books are present
    webTestClient
      .get()
      .uri("/api/books")
      .exchange()
      .expectStatus().isOk()
      .expectBody().jsonPath("$.size()").isEqualTo(0);

    // ARANGE
    openLibraryStubs.stubForSuccessfulBookResponse(ISBN, VALID_RESPONSE);

    // ACT
    sqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(this.sqsClient.getQueueUrl(r -> r.queueName(QUEUE_NAME)).join().queueUrl())
        .messageBody(
          """
            {
              "isbn": "%s"
            }
          """.formatted(ISBN))
        .build());

    // ASSERT
    given()
      .atMost(Duration.ofSeconds(5))
      .await()
      .untilAsserted(
        () -> {
          this.webTestClient
            .get()
            .uri("/api/books")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.size()").isEqualTo(1)
            .jsonPath("$[0].isbn").isEqualTo(ISBN);
        });
  }
}
