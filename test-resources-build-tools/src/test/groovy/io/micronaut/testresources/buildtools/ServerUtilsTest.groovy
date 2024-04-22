package io.micronaut.testresources.buildtools

import io.micronaut.context.ApplicationContext
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Inject
import spock.lang.Specification
import spock.lang.TempDir
import spock.util.environment.RestoreSystemProperties

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

class ServerUtilsTest extends Specification {
    @TempDir
    Path tmpDir

    def "writes and reads server settings"() {
        def settings = new ServerSettings(
                1234,
                token,
                timeout,
                null
        )
        def settingsDir = tmpDir.resolve("settings")

        when:
        ServerUtils.writeServerSettings(settingsDir, settings)
        def read = ServerUtils.readServerSettings(settingsDir)

        then:
        read.present
        def actual = read.get()
        actual == settings

        where:
        token | timeout
        null  | null
        'abc' | null
        null  | 60
        'abc' | 98
    }

    def "requires new server"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def factory = Mock(ServerFactory)
        def applicationContext = ApplicationContext.builder().start()
        def embeddedServer = applicationContext.getBean(EmbeddedServer)
        embeddedServer.start()

        when:
        def settings = ServerUtils.startOrConnectToExistingServer(null, portFile, settingsDir, token, classpath, timeout, null, factory)

        then:
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->
            assert params.mainClass == 'io.micronaut.testresources.server.TestResourcesService'
            assert params.classpath == classpath
            def sysProps = ['com.sun.management.jmxremote': null]
            if (token != null) {
                sysProps["server.access-token"] = token
            }
            assert params.systemProperties == sysProps
            assert params.arguments == [
                    "--port-file=${portFile.toAbsolutePath()}".toString()
            ]
        }
        1 * factory.waitFor(_) >> {
            portFile.toFile().text = "${embeddedServer.port}"
        }

        and:
        Files.exists(settingsDir.resolve(ServerUtils.PROPERTIES_FILE_NAME))
        settings.port == embeddedServer.port
        settings.accessToken == Optional.ofNullable(token)
        settings.clientTimeout == Optional.ofNullable(timeout)

        when:
        ServerUtils.stopServer(settingsDir)

        then:
        !Files.exists(settingsDir.resolve(ServerUtils.PROPERTIES_FILE_NAME))

        cleanup:
        applicationContext.stop()

        where:
        token | classpath         | timeout
        null  | []                | null
        'abc' | []                | null
        null  | [new File('abc')] | null
        'abc' | [new File('def')] | 98
    }

    @RestoreSystemProperties
    def "can set the docker check timeout"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def factory = Mock(ServerFactory)
        System.setProperty('docker.check.timeout.seconds', "100")

        when:
        ServerUtils.startOrConnectToExistingServer(9999, portFile, settingsDir, null, null, null, null, factory)

        then:
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->
            assert params.mainClass == 'io.micronaut.testresources.server.TestResourcesService'
            assert params.systemProperties['docker.check.timeout.seconds'] == '100'
        }
    }

    @RestoreSystemProperties
    def "waits for the server to be available when using an explicit port"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def factory = Mock(ServerFactory)
        def iterations = new AtomicInteger(0);
        System.setProperty(ServerUtils.SERVER_TEST_PROPERTY, "false")

        when:
        def settings = ServerUtils.startOrConnectToExistingServer(9999, portFile, settingsDir, null, null, null, null, factory)

        then:
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->
            assert params.mainClass == 'io.micronaut.testresources.server.TestResourcesService'
        }
        _ * factory.waitFor(_) >> {
            if (iterations.incrementAndGet() == 4) {
                // start the service
                System.setProperty(ServerUtils.SERVER_TEST_PROPERTY, "true")
            } else {
                Thread.sleep(10)
            }
        }

        and:
        Files.exists(settingsDir.resolve(ServerUtils.PROPERTIES_FILE_NAME))
        iterations.get() == 4


    }

    def "reuses existing server"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def factory = Mock(ServerFactory)
        def applicationContext = ApplicationContext.builder().start()
        def embeddedServer = applicationContext.getBean(EmbeddedServer)
        embeddedServer.start()
        ServerUtils.writeServerSettings(settingsDir, new ServerSettings(embeddedServer.port, null, null, null))

        when: "no explicit port"
        ServerUtils.startOrConnectToExistingServer(null, portFile, settingsDir, null, [], null, null, factory)

        then:
        0 * factory.startServer(_)
        0 * factory.waitFor(_)

        when: "explicit port"
        ServerUtils.startOrConnectToExistingServer(embeddedServer.port, portFile, settingsDir, null, [], null, null, factory)

        then:
        0 * factory.startServer(_)
        0 * factory.waitFor(_)

        cleanup:
        applicationContext.stop()
    }

    def "supports class data sharing"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def cdsDir = tmpDir.resolve("cds-dir")
        def cdsClasspathDir = tmpDir.resolve("some-classes")
        Files.createDirectory(cdsClasspathDir)
        Files.write(cdsClasspathDir.resolve("some.class"), [1, 2, 3] as byte[])
        def cdsClassList = cdsDir.resolve("cds.classlist")
        def cdsArchiveFile = cdsDir.resolve("cds.jsa")
        def cdsFlatJar = cdsDir.resolve("flat.jar")
        String cdsClassListOption = "-XX:DumpLoadedClassList=${cdsClassList.toAbsolutePath()}"
        String cdsSharedClassListOption = "-XX:SharedClassListFile=${cdsClassList.toAbsolutePath()}"
        String cdsSharedArchiveFileOption = "-XX:SharedArchiveFile=${cdsArchiveFile.toAbsolutePath()}"
        def factory = Mock(ServerFactory)
        def applicationContext = ApplicationContext.builder().start()
        def embeddedServer = applicationContext.getBean(EmbeddedServer)
        embeddedServer.start()

        when: "first call with CDS support enabled"
        ServerUtils.startOrConnectToExistingServer(null, portFile, settingsDir, null, cdsDir, [cdsClasspathDir.toFile()], null, null, factory)

        then:
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->
            def jvmArgs = params.jvmArguments
            assert jvmArgs.contains("-Xshare:off")
            assert jvmArgs.contains(cdsClassListOption)
            assert params.classpath.contains(cdsFlatJar.toFile())
            assert Files.exists(cdsFlatJar)
            Files.write(cdsClassList, "test".getBytes())
        }
        1 * factory.waitFor(_) >> {
            portFile.toFile().text = "${embeddedServer.port}"
        }
        settingsDir.toFile().deleteDir()

        when: "second call dumps CDS then starts server"
        ServerUtils.startOrConnectToExistingServer(null, portFile, settingsDir, null, cdsDir, [cdsClasspathDir.toFile()], null, null, factory)

        then:
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->
            def jvmArgs = params.jvmArguments
            assert jvmArgs.contains("-Xshare:dump")
            assert jvmArgs.contains(cdsSharedClassListOption)
            assert jvmArgs.contains(cdsSharedArchiveFileOption)
            Files.write(cdsArchiveFile, "test".getBytes())
        }
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->
            def jvmArgs = params.jvmArguments
            assert !jvmArgs.contains("-Xshare:dump")
            assert !jvmArgs.contains(cdsSharedClassListOption)
            assert jvmArgs.contains(cdsSharedArchiveFileOption)
        }
        1 * factory.waitFor(_) >> {
            portFile.toFile().text = "${embeddedServer.port}"
        }

        settingsDir.toFile().deleteDir()

        when: "third call starts server with CDS"
        ServerUtils.startOrConnectToExistingServer(null, portFile, settingsDir, null, cdsDir, [cdsClasspathDir.toFile()], null, null, factory)

        then:
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->
            def jvmArgs = params.jvmArguments
            assert !jvmArgs.contains("-Xshare:dump")
            assert !jvmArgs.contains(cdsSharedClassListOption)
            assert jvmArgs.contains(cdsSharedArchiveFileOption)
        }
        1 * factory.waitFor(_) >> {
            portFile.toFile().text = "${embeddedServer.port}"
        }

        settingsDir.toFile().deleteDir()

        when: "removes CDS files if classpath changes"
        ServerUtils.startOrConnectToExistingServer(null, portFile, settingsDir, null, cdsDir, [], null, null, factory)

        then:
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->
            def jvmArgs = params.jvmArguments
            assert jvmArgs.contains("-Xshare:off")
            assert jvmArgs.contains(cdsClassListOption)
            assert params.classpath == []
            Files.write(cdsClassList, "test".getBytes())
        }
        1 * factory.waitFor(_) >> {
            portFile.toFile().text = "${embeddedServer.port}"
        }

        cleanup:
        applicationContext.stop()
    }

    def "waits for port file to have contents"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def factory = Mock(ServerFactory)
        def applicationContext = ApplicationContext.builder().start()
        def embeddedServer = applicationContext.getBean(EmbeddedServer)
        embeddedServer.start()

        when:
        def settings = ServerUtils.startOrConnectToExistingServer(null, portFile, settingsDir, null, [], null, null, factory)

        then:
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->

        }
        1 * factory.waitFor(_) >> {
            portFile.toFile().text = ""
            println "Waiting to write the contents of the port file"
        }
        1 * factory.waitFor(_) >> {
            portFile.toFile().text = "${embeddedServer.port}"
        }

        cleanup:
        applicationContext.stop()
    }

    def "reasonable error message if port file can never be read"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def factory = Mock(ServerFactory)
        def applicationContext = ApplicationContext.builder().start()
        def embeddedServer = applicationContext.getBean(EmbeddedServer)
        embeddedServer.start()

        when:
        def settings = ServerUtils.startOrConnectToExistingServer(null, portFile, settingsDir, null, [], null, null, factory)

        then:
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->

        }
        10 * factory.waitFor(_) >> {
            portFile.toFile().text = ""
            println "Waiting to write the contents of the port file"
        }
        IllegalStateException ex = thrown()
        ex.message == "Unable to read port file ${portFile}: file is empty"

        cleanup:
        applicationContext.stop()
    }

    def "can configure a namespace for the default shared settings"() {
        def withoutNamespace = ServerUtils.getDefaultSharedSettingsPath()
        def withNamespace = ServerUtils.getDefaultSharedSettingsPath("custom")

        expect:
        withoutNamespace == ServerUtils.getDefaultSharedSettingsPath(null)
        withNamespace.parent == withoutNamespace.parent
        withNamespace.parent.resolve("test-resources-custom") == withNamespace
    }


    @Controller
    static class ServerMock {

        @Inject
        ApplicationContext ctx

        @Post("/stop")
        void close() {
            ctx.close()
        }
    }
}
