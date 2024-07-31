package de.rieckpil.courses.config;

import de.rieckpil.courses.book.management.OpenLibraryHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class OpenLibraryHttpClientConfig {

  @Bean
  public OpenLibraryHttpClient bookClient(WebClient openLibraryWebClient) {
    HttpServiceProxyFactory factory = HttpServiceProxyFactory.builder(WebClientAdapter.forClient(openLibraryWebClient)).build();
    return factory.createClient(OpenLibraryHttpClient.class);
  }
}
