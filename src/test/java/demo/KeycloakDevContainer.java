package demo;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.SelinuxContext;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.shaded.com.google.common.cache.Cache;
import org.testcontainers.shaded.com.google.common.cache.CacheBuilder;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class KeycloakDevContainer extends KeycloakContainer {

    private boolean classFolderChangeTrackingEnabled;

    public KeycloakDevContainer(String image) {
        super(image);
    }

    public KeycloakContainer withFixedExposedPort(int hostPort, int containerPort) {
        super.addFixedExposedPort(hostPort, containerPort);
        return this.self();
    }

    @Override
    protected void configure() {
        super.configure();
        this.withExposedPorts(8080, 8443, 8787);

        // TODO externalize commands
        this.withCommand("-c standalone.xml", "-Dkeycloak.profile.feature.upload_scripts=enabled", "-Dwildfly.statistics-enabled=true", "--debug *:8787");
        String explodedFolderExtensionsJar = "/opt/jboss/keycloak/standalone/deployments/extensions.jar";
        String deploymentTriggerFile = explodedFolderExtensionsJar + ".dodeploy";

        String classesLocation = MountableFile.forClasspathResource(".").getResolvedPath() + "../classes";

        addFileSystemBind(classesLocation, explodedFolderExtensionsJar, BindMode.READ_WRITE, SelinuxContext.SINGLE);
        withClasspathResourceMapping("dodeploy", deploymentTriggerFile, BindMode.READ_ONLY);

        if (isClassFolderChangeTrackingEnabled()) {
            registerClassFolderWatcher(Paths.get(
                    classesLocation
            ).normalize(), Set.of(new File(deploymentTriggerFile).getName()), (watchEvent) -> {
                System.out.println("Detected change... trigger redeployment. changed file: " + watchEvent.context());
                copyFileToContainer(Transferable.of("true".getBytes(StandardCharsets.UTF_8)), deploymentTriggerFile);
                System.out.println("Redeployment triggered");
            });
        }
    }

    private void registerClassFolderWatcher(Path classPath, Set<String> excludes, Consumer<WatchEvent<Path>> onChange) {

        Set<String> watchList = Collections.newSetFromMap(new ConcurrentHashMap<>());

        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();

            Cache<String, Boolean> seen = CacheBuilder.newBuilder().expireAfterWrite(500, TimeUnit.MILLISECONDS).build();

            Executors.newSingleThreadExecutor().execute(() -> {
                for (; ; ) {
                    try {
//                        System.out.println("Waiting for changes...");

                        registerFileWatcherRecursively(watchList, watcher, classPath);

                        WatchKey key = watcher.take();

                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();

                            if (kind == OVERFLOW) {
                                continue;
                            }

                            WatchEvent<Path> ev = (WatchEvent<Path>) event;
                            Path contextPath = ev.context();

                            String filename = contextPath.getName(0).toString();
                            if (excludes.contains(filename)) {
                                continue;
                            }

                            if (!filename.endsWith(".class")) {
                                continue;
                            }

                            if (seen.getIfPresent(filename) != null) {
                                continue;
                            } else {
                                seen.put(filename, Boolean.TRUE);
                            }

                            onChange.accept(ev);
                            break;
                        }

                        boolean valid = key.reset();
                        if (!valid) {
                            System.out.println("Watch key no longer valid, exiting...");
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void registerFileWatcherRecursively(Set<String> watchList, WatchService watcher, Path classesPath) throws IOException {

        Files.walkFileTree(classesPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                String absolutePath = dir.toFile().getAbsolutePath();
                if (watchList.contains(absolutePath)) {
                    return FileVisitResult.CONTINUE;
                }

//                System.out.println("Add dir to watchlist " + absolutePath);
                dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public boolean isClassFolderChangeTrackingEnabled() {
        return classFolderChangeTrackingEnabled;
    }

    public KeycloakDevContainer withClassFolderChangeTrackingEnabled(boolean classFolderChangeTrackingEnabled) {
        this.classFolderChangeTrackingEnabled = classFolderChangeTrackingEnabled;
        return this;
    }
}
