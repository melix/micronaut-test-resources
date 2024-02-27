package io.micronaut.testresources.r2dbc.oracle

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.jdbc.AbstractJDBCSpec
import io.micronaut.testresources.jdbc.Book
import jakarta.inject.Inject
import spock.lang.IgnoreIf

@MicronautTest(environments = ["standalone"], transactional = false )
@IgnoreIf({ !jvm.java11Compatible })
class StandaloneStartOracleTest extends AbstractJDBCSpec {
    @Inject
    ReactiveBookRepository repository

    def "starts a reactive Oracle container"() {
        def book = new Book(title: "Micronaut for Spring developers")
        repository.save(book).block()

        when:
        def books = repository.findAll().toIterable() as List<Book>

        then:
        books.size() == 1
    }

    @Override
    String getImageName() {
        "oracle"
    }
}
