package io.micronaut.testresources.r2dbc.pool

import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.jdbc.AbstractJDBCSpec
import io.micronaut.testresources.jdbc.Book
import jakarta.inject.Inject

@MicronautTest(environments = ["jdbc"] )
class WithJdbcStartPostgreSQLTest extends AbstractJDBCSpec {

    @Inject
    ReactiveBookRepository repository

    @Value('${r2dbc.datasources.default.options.protocol}')
    String protocol

    @Value('${r2dbc.datasources.default.options.driver}')
    String driver

    def "starts a reactive PostgreSQL container"() {
        def book = new Book(title: "Micronaut for Spring developers")
        repository.save(book).block()

        when:
        def books = repository.findAll().toIterable() as List<Book>

        then:
        protocol == 'postgres'
        driver == 'pool'
        books.size() == 1
    }

    @Override
    String getImageName() {
        "postgres"
    }
}
