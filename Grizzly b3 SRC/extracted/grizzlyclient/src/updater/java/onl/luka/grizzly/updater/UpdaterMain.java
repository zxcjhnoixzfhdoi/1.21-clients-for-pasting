package onl.luka.grizzly.updater;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public final class UpdaterMain {

    private static final long WAIT_TIMEOUT_MS = 120_000L;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) return;

        PrintStream log = openLog(new File(args[0]));
        String label = null;
        boolean failed = false;
        try {
            int i = 1;
            while (i < args.length) {
                String op = args[i++];
                switch (op) {
                    case "move" -> {
                        Path from = Path.of(args[i++]);
                        Path to = waitForRelease(Path.of(args[i++]), log);
                        log.println("move " + from + " -> " + to);
                        Files.createDirectories(to.toAbsolutePath().getParent());
                        Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
                    }
                    case "delete" -> {
                        Path target = waitForRelease(Path.of(args[i++]), log);
                        log.println("delete " + target);
                        Files.deleteIfExists(target);
                    }
                    case "announce" -> label = args[i++];
                    default -> {
                        log.println("unknown instruction " + op);
                        return;
                    }
                }
            }
            log.println("done");
        } catch (Throwable t) {
            failed = true;
            t.printStackTrace(log);
        } finally {
            log.flush();
            log.close();
        }

        if (label != null) showDialog(label, failed);
    }

    private static PrintStream openLog(File file) throws Exception {
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        return new PrintStream(new FileOutputStream(file, true), true);
    }

    private static Path waitForRelease(Path path, PrintStream log) throws InterruptedException {
        File file = path.toFile();
        long deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS;
        while (file.exists() && !file.renameTo(file)) {
            if (System.currentTimeMillis() > deadline) {
                log.println("gave up waiting for " + path);
                break;
            }
            Thread.sleep(500L);
        }
        return path;
    }

    private static void showDialog(String label, boolean failed) {
        if (GraphicsEnvironment.isHeadless()) return;
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Throwable ignored) {
        }
        try {
            String message = failed
                ? "Grizzly could not install " + label + ".\nYour existing install has been left alone."
                : "Grizzly has been updated to " + label + ".\nIt will be running next time you launch.";

            JFrame owner = new JFrame();
            owner.setAlwaysOnTop(true);
            owner.setUndecorated(true);
            owner.setSize(0, 0);
            owner.setLocationRelativeTo(null);
            owner.setVisible(true);
            JOptionPane.showMessageDialog(
                owner,
                message,
                "Grizzly",
                failed ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE
            );
            owner.dispose();
        } catch (Throwable ignored) {
        }
    }
}
