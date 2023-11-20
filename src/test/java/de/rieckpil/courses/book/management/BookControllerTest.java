package de.rieckpil.courses.book.management;

import de.rieckpil.courses.config.WebSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
// see https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.7-Release-Notes#migrating-from-websecurityconfigureradapter-to-securityfilterchain
@Import(WebSecurityConfig.class)
class BookControllerTest {

  @MockBean
  private BookManagementService bookManagementService;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void shouldGetEmptyArrayWhenNoBooksExists() throws Exception {
    mockMvc.perform(get("/api/books")
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
      )
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.size()", is(0)))
      .andDo(print());
  }

  @Test
  void shouldNotReturnXML() throws Exception {
    mockMvc.perform(get("/api/books")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
    )
      .andExpect(status().isNotAcceptable());
  }

  @Test
  void shouldGetBooksWhenServiceReturnsBooks() throws Exception {
    Book bookOne = createBook(1L, "42", "Java 14", "Mike", "Good book",
      "Software Engineering", 200L, "Oracle", "ftp://localhost:42");

    Book bookTwo = createBook(2L, "84", "Java 15", "Duke", "Good book",
      "Software Engineering", 200L, "Oracle", "ftp://localhost:84");

    when(bookManagementService.getAllBooks()).thenReturn(List.of(bookOne, bookTwo));

    mockMvc.perform(get("/api/books")
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
      )
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.size()", is(2)))
      .andExpect(jsonPath("$[0].id").doesNotExist())
      .andExpect(jsonPath("$[0].isbn", is("42")))
      .andExpect(jsonPath("$[0].title", is("Java 14")))
      .andExpect(jsonPath("$[0].author", is("Mike")))
      .andExpect(jsonPath("$[0].description", is("Good book")))
      .andExpect(jsonPath("$[0].genre", is("Software Engineering")))
      .andExpect(jsonPath("$[0].pages", is(200)))
      .andExpect(jsonPath("$[0].publisher", is("Oracle")))
      .andExpect(jsonPath("$[0].thumbnailUrl", is("ftp://localhost:42")))
      .andExpect(jsonPath("$[1].id").doesNotExist())
      .andExpect(jsonPath("$[1].isbn", is("84")))
      .andExpect(jsonPath("$[1].title", is("Java 15")))
      .andExpect(jsonPath("$[1].author", is("Duke")))
      .andExpect(jsonPath("$[1].description", is("Good book")))
      .andExpect(jsonPath("$[1].genre", is("Software Engineering")))
      .andExpect(jsonPath("$[1].pages", is(200)))
      .andExpect(jsonPath("$[1].publisher", is("Oracle")))
      .andExpect(jsonPath("$[1].thumbnailUrl", is("ftp://localhost:84")))
      ;

    // minden property tesztelésre másik módszer a JSONAssert
    // What you would need to do is get the HTTP response body from the test as a String and then use
    // JSONAssert.assertEquals(expected, data);. A simple String comparison won’t do the trick as the order of
    // the JSOn attributes might differ.
  }

  private Book createBook(Long id, String isbn, String title, String author, String description, String genre, Long pages, String publisher, String thumbnailUrl) {
    Book result = new Book();
    result.setId(id);
    result.setIsbn(isbn);
    result.setTitle(title);
    result.setAuthor(author);
    result.setDescription(description);
    result.setGenre(genre);
    result.setPages(pages);
    result.setPublisher(publisher);
    result.setThumbnailUrl(thumbnailUrl);
    return result;
  }

}
