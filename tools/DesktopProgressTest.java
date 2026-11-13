package com.bargeboss.game;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

/** Native file-storage checks with opaque snapshots and a temporary directory. */
public final class DesktopProgressTest {
    public static void main(String[] args) throws Exception {
        Files.createDirectories(Paths.get("build"));
        Path root=Files.createTempDirectory(Paths.get("build"),"progress-test-");
        try {
            Path file=root.resolve("profile").resolve("progress.dat");
            DesktopProgress storage=new DesktopProgress(file);
            check(storage.read()==null,"first launch has no snapshot");
            check(storage.write("first snapshot"),"create progress directory and file");
            check("first snapshot".equals(new DesktopProgress(file).read()),"fresh host restores opaque snapshot");
            check(storage.write("new snapshot\nwith unicode \u2693"),"replace previous snapshot atomically");
            check("new snapshot\nwith unicode \u2693".equals(new DesktopProgress(file).read()),"UTF-8 snapshot survives restart");
            try(Stream<Path> entries=Files.list(file.getParent())) {
                check(entries.count()==1,"atomic save leaves no temporary files");
            }
            Path blocked=root.resolve("blocked"); Files.write(blocked,"file".getBytes(StandardCharsets.UTF_8));
            check(!new DesktopProgress(blocked.resolve("progress.dat")).write("cannot write"),"storage failure returns without throwing");
            check("new snapshot\nwith unicode \u2693".equals(storage.read()),"failed write cannot damage a separate saved profile");
            Files.write(file,new byte[65537]);
            check(storage.read()==null,"oversized file is rejected before allocation");
            System.out.println("Desktop progress passed: restore, atomic replacement, UTF-8, failure isolation, bounded read");
        } finally {
            try(Stream<Path> paths=Files.walk(root)) {
                for(Path path:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator) Files.deleteIfExists(path);
            }
        }
    }
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
