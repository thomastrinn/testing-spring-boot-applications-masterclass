package de.rieckpil.courses.book.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class  BookSynchronizationListenerTest {

  private final static String VALID_ISBN = "1234567891234";

  @Mock
  private BookRepository bookRepository;

  @Mock
  private OpenLibraryApiClient openLibraryApiClient;

  @InjectMocks
  private BookSynchronizationListener cut;

  @Captor
  private ArgumentCaptor<Book> bookArgumentCaptor;

  @Test
  void shouldRejectBookWhenIsbnIsMalformed() {
    BookSynchronization bookSynchronization = new BookSynchronization("42");

    cut.consumeBookUpdates(bookSynchronization);

    verifyNoInteractions(openLibraryApiClient, bookRepository);
  }

  @Test
  void shouldNotOverrideWhenBookAlreadyExists() {
    when(bookRepository.findByIsbn(VALID_ISBN)).thenReturn(new Book());

    BookSynchronization bookSynchronization = new BookSynchronization(VALID_ISBN);

    cut.consumeBookUpdates(bookSynchronization);

    verifyNoInteractions(openLibraryApiClient);

    verify(bookRepository, times(0)).save(ArgumentMatchers.any());
  }

  @Test
  void shouldThrowExceptionWhenProcessingFails() {
    when(bookRepository.findByIsbn(VALID_ISBN)).thenReturn(null);
    when(openLibraryApiClient.fetchMetadataForBook(VALID_ISBN))
      .thenThrow(new RuntimeException("Network timeout"));

    BookSynchronization bookSynchronization = new BookSynchronization(VALID_ISBN);

    assertThrows(RuntimeException.class, () -> cut.consumeBookUpdates(bookSynchronization));
  }

  @Test
  void shouldStoreBookWhenNewAndCorrectIsbn() {
    // ARANGE
    when(bookRepository.findByIsbn(VALID_ISBN)).thenReturn(null);

    Book requestedBook = new Book();
    requestedBook.setTitle("Java book");
    requestedBook.setIsbn(VALID_ISBN);

    when(openLibraryApiClient.fetchMetadataForBook(VALID_ISBN))
      .thenReturn(requestedBook);
    when(bookRepository.save(ArgumentMatchers.any())).then(invocationOnMock -> {
      Book methodArgument = invocationOnMock.getArgument(0);
      methodArgument.setId(1L);

      return methodArgument;
    });

    BookSynchronization bookSynchronization = new BookSynchronization(VALID_ISBN);

    // ACT
    cut.consumeBookUpdates(bookSynchronization);

    // ASSERT
    verify(bookRepository).save(bookArgumentCaptor.capture());

    Book methodArgument = bookArgumentCaptor.getValue();
    assertEquals("Java book", methodArgument.getTitle());
    assertEquals(VALID_ISBN, methodArgument.getIsbn());
  }

}
