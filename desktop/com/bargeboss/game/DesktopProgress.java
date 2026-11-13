package com.bargeboss.game;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Opaque native storage; only shared Game interprets captain progress. */
final class DesktopProgress {
    private final Path file;
    DesktopProgress(Path file) { this.file=file; }
    String read() {
        try {
            if(!Files.exists(file)) return null;
            if(Files.size(file)>65536) return null;
            return new String(Files.readAllBytes(file),StandardCharsets.UTF_8);
        } catch(IOException | SecurityException e) {
            System.err.println("Could not read captain progress: "+e); return null;
        }
    }
    boolean write(String snapshot) {
        Path temporary=null;
        try {
            Files.createDirectories(file.getParent());
            temporary=Files.createTempFile(file.getParent(),"progress-",".tmp");
            Files.write(temporary,snapshot.getBytes(StandardCharsets.UTF_8));
            Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch(IOException | SecurityException e) {
            System.err.println("Could not save captain progress: "+e); return false;
        } finally {
            if(temporary!=null) try { Files.deleteIfExists(temporary); } catch(IOException | SecurityException ignored) {}
        }
    }
}
