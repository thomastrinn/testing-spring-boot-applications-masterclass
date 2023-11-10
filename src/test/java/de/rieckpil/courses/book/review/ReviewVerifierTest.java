package de.rieckpil.courses.book.review;

import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static de.rieckpil.courses.book.review.RandomReviewParameterResolverExtension.RandomReview;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(RandomReviewParameterResolverExtension.class)
class ReviewVerifierTest {

  private ReviewVerifier reviewVerifier;

  @BeforeEach
  void setup() {
    reviewVerifier = new ReviewVerifier();
  }

  @Test
  void shouldFailWhenReviewContainsSwearWord() {
    String review = "This book is shit";
    System.out.println("Testing a review");

    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertFalse(result, "ReviewVerifier did not detect swear word");
  }

  @Test
  @DisplayName("Should fail when review contains 'lorem ipsum'")
  void testLoremIpsum() {
    String review = "Lorem ipsum is simply dummy text of the printing and typesetting industry. " +
      "Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer " +
      "took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, " +
      "but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the " +
      "1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop " +
      "publishing software like Aldus PageMaker including versions of Lorem Ipsum.";

    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertFalse(result, "ReviewVerifier did not detect lorem ipsum");
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/badReview.csv")
  void shouldFailWhenReviewIsOfBadQuality(String review) {
    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertFalse(result, "ReviewVerifier did not detect bad review");
  }

  @RepeatedTest(5)
  void shouldFailWhenRandomReviewQualityIsBad(@RandomReview String review) {
    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertFalse(result, "ReviewVerifier did not detect random bad review");
  }

  @Test
  void shouldPassWhenReviewIsGood() {
    String review = "I can totally recommend this book who is interested in learning how to write Java code!";
    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertTrue(result, "ReviewVerifier did not detect good review");
  }

  @Test
  void shouldPassWhenReviewIsGoodHamcrest() {
    String review = "I can totally recommend this book who is interested in learning how to write Java code!";
    boolean result = reviewVerifier.doesMeetQualityStandards(review);

    MatcherAssert.assertThat("ReviewVerifier did not detect good review", result, Matchers.equalTo(true));
  }

  @Test
  void shouldPassWhenReviewIsGoodAssertJ() {
    String review = "I can totally recommend this book who is interested in learning how to write Java code!";
    boolean result = reviewVerifier.doesMeetQualityStandards(review);

    Assertions.assertThat(result)
      .withFailMessage("ReviewVerifier did not detect good review")
      .isTrue();
  }
}
