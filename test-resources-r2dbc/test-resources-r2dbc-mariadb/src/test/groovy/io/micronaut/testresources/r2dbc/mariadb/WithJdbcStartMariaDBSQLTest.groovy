package io.micronaut.testresources.r2dbc.mariadb

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.jdbc.AbstractJDBCSpec
import io.micronaut.testresources.jdbc.Book
import jakarta.inject.Inject

@MicronautTest(environments = ["jdbc"] )
class WithJdbcStartMariaDBSQLTest extends AbstractJDBCSpec {

    @Inject
    ReactiveBookRepository repository

    def "starts a reactive MariaDB container"() {
        def book = new Book(title: "Micronaut for Spring developers")
        repository.save(book).block()

        when:
        def books = repository.findAll().toIterable() as List<Book>

        then:
        books.size() == 1
    }

    @Override
    String getImageName() {
        "mariadb"
    }
}
