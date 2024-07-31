package de.rieckpil.courses.book.management;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class OpenLibraryHttpClientTest {

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

  private OpenLibraryHttpClient cut;

  private MockWebServer mockWebServer;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    HttpClient httpClient = HttpClient.create()
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2_000)
      .doOnConnected(connection ->
        connection.addHandlerLast(new ReadTimeoutHandler(2))
          .addHandlerLast(new WriteTimeoutHandler(2)));

    WebClient webClient = WebClient.builder()
      .baseUrl(mockWebServer.url("/").toString())
      .clientConnector(new ReactorClientHttpConnector(httpClient))
      .build();

    HttpServiceProxyFactory factory = HttpServiceProxyFactory.builder(WebClientAdapter.forClient(webClient)).build();
    cut =  factory.createClient(OpenLibraryHttpClient.class);
  }

  @AfterEach
  void tearDown() throws IOException {
    if (mockWebServer != null) {
      mockWebServer.shutdown();
    }
  }

  @Test
  void contextLoads() {
    assertNotNull(cut);
  }

  @Test
  void shouldReturnBookWhenResultIsSuccess() throws InterruptedException {
    MockResponse mockResponse = new MockResponse()
      .addHeader("Content-Type", "application/json; charset=utf-8")
      .setBody(VALID_RESPONSE);

    mockWebServer.enqueue(mockResponse);

    Book result = cut.fetchBookByISBN(ISBN);

    assertNull(result.getId());
    assertEquals("9780596004651", result.getIsbn());
    assertEquals("Head first Java", result.getTitle());
    assertEquals("https://covers.openlibrary.org/b/id/388761-S.jpg", result.getThumbnailUrl());
    assertEquals("Kathy Sierra", result.getAuthor());
    assertEquals("Your brain on Java--a learner's guide--Cover.Includes index.", result.getDescription());
    assertEquals("Java (Computer program language)", result.getGenre());
    assertEquals("O'Reilly", result.getPublisher());
    assertEquals(619, result.getPages());

    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals("/api/books?jscmd=data&format=json&bibkeys=" + ISBN, recordedRequest.getPath());
  }

  @Test
  void shouldReturnBookWhenResultIsSuccessButLackingAllInformation() {
    String response = """
      {
        "9780596004651": {
          "publishers": [
            {
              "name": "O'Reilly"
            }
          ],
          "title": "Head first Java",
          "authors": [
            {
              "url": "https://openlibrary.org/authors/OL1400543A/Kathy_Sierra",
              "name": "Kathy Sierra"
            }
          ],
          "number_of_pages": 42,
          "cover": {
                "small": "https://covers.openlibrary.org/b/id/388761-S.jpg",
                "large": "https://covers.openlibrary.org/b/id/388761-L.jpg",
                "medium": "https://covers.openlibrary.org/b/id/388761-M.jpg"
          }
        }
      }

      """;

    MockResponse mockResponse = new MockResponse()
      .addHeader("Content-Type", "application/json; charset=utf-8")
      .setResponseCode(200)
      .setBody(response);

    mockWebServer.enqueue(mockResponse);

    Book result = cut.fetchBookByISBN(ISBN);

    assertNull(result.getId());
    assertEquals("9780596004651", result.getIsbn());
    assertEquals("Head first Java", result.getTitle());
    assertEquals("https://covers.openlibrary.org/b/id/388761-S.jpg", result.getThumbnailUrl());
    assertEquals("Kathy Sierra", result.getAuthor());
    assertEquals("n.A", result.getDescription());
    assertEquals("n.A", result.getGenre());
    assertEquals("O'Reilly", result.getPublisher());
    assertEquals(42, result.getPages());
  }

  @Test
  void shouldPropagateExceptionWhenRemoteSystemIsDown() {
    // 3 times server error for retry 2 times
    mockWebServer.enqueue(new MockResponse()
      .setResponseCode(500)
      .setBody("Sorry, system is down :("));
    mockWebServer.enqueue(new MockResponse()
      .setResponseCode(500)
      .setBody("Sorry, system is down :("));
    mockWebServer.enqueue(new MockResponse()
      .setResponseCode(500)
      .setBody("Sorry, system is down :("));

    assertThrows(RuntimeException.class, () -> {
      cut.fetchBookByISBN(ISBN);
    });
  }
}
