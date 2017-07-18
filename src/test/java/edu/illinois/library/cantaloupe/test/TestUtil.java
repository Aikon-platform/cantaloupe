package edu.illinois.library.cantaloupe.test;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.image.Format;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public abstract class TestUtil {

    public static int countFiles(File dir) {
        List<File> allFiles = new ArrayList<>();
        Queue<File> dirs = new LinkedList<>();
        dirs.add(dir);
        while (!dirs.isEmpty()) {
            for (File f : dirs.poll().listFiles()) {
                if (f.isDirectory()) {
                    dirs.add(f);
                } else if (f.isFile()) {
                    allFiles.add(f);
                }
            }
        }
        return allFiles.size();
    }

    public static File getCurrentWorkingDirectory() throws IOException {
        File directory = new File(".");
        return directory.getCanonicalFile();
    }

    public static Collection<File> getImageFixtures(Format format)
            throws IOException {
        return FileUtils.listFiles(getFixture("images"), null, false);
    }

    public static File getFixture(String filename) throws IOException {
        return new File(getFixturePath() + File.separator + filename);
    }

    public static Path getFixturePath() throws IOException {
        return Paths.get(getCurrentWorkingDirectory().getAbsolutePath(),
                "src", "test", "resources");
    }

    public static File getImage(String name) throws IOException {
        return getFixture("images/" + name);
    }

    public static Integer getOpenPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static File getTempFolder() throws IOException {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    public static Configuration getTestConfig() {
        try {
            return new PropertiesConfiguration("./test.properties");
        } catch (ConfigurationException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        return new BaseConfiguration();
    }

    /**
     * @return Dummy OperationList object with no operations added.
     */
    public static OperationList newOperationList() {
        OperationList ops = new OperationList(new Identifier("dummy"), Format.JPG);
        ops.add(new Encode(Format.JPG));
        return ops;
    }

}
