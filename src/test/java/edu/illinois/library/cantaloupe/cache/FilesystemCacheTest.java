package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.ConcurrentReaderWriter;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.DeletingFileVisitor;
import edu.illinois.library.cantaloupe.util.StringUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static edu.illinois.library.cantaloupe.cache.FilesystemCache.*;
import static edu.illinois.library.cantaloupe.test.Assert.PathAssert.assertRecursiveFileCount;
import static org.junit.Assert.*;

public class FilesystemCacheTest extends AbstractCacheTest {

    private Path fixturePath;
    private Path sourceImagePath;
    private Path derivativeImagePath;
    private FilesystemCache instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        fixturePath = Files.createTempDirectory("test").resolve("cache");
        sourceImagePath = fixturePath.resolve("source");
        derivativeImagePath = fixturePath.resolve("image");

        instance = newInstance();
    }

    @After
    public void tearDown() throws IOException {
        Files.walkFileTree(fixturePath, new DeletingFileVisitor());
    }

    @Override
    FilesystemCache newInstance() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMCACHE_DIRECTORY_DEPTH, 3);
        config.setProperty(Key.FILESYSTEMCACHE_DIRECTORY_NAME_LENGTH, 2);
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                fixturePath.toString());

        return new FilesystemCache();
    }

    private void createEmptyFile(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.createFile(path);
    }

    private void writeStringToFile(Path path,
                                   String contents) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, contents.getBytes("UTF-8"));
    }

    @Test
    public void testHashedPathFragment() {
        // depth = 2, length = 3
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMCACHE_DIRECTORY_DEPTH, 2);
        config.setProperty(Key.FILESYSTEMCACHE_DIRECTORY_NAME_LENGTH, 3);
        assertEquals(
                String.format("083%s2c1", File.separator),
                FilesystemCache.hashedPathFragment("cats"));

        // depth = 0
        config.setProperty(Key.FILESYSTEMCACHE_DIRECTORY_DEPTH, 0);
        assertEquals("", hashedPathFragment("cats"));
    }

    @Test
    public void testDerivativeImageFile() {
        String pathname = Configuration.getInstance().
                getString(Key.FILESYSTEMCACHE_PATHNAME);

        Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setPercent(0.905f);
        Format format = Format.TIF;

        OperationList ops = new OperationList(identifier, format, scale);

        final Path expected = Paths.get(
                pathname,
                "image",
                hashedPathFragment(identifier.toString()),
                ops.toFilename());
        assertEquals(expected, derivativeImageFile(ops));
    }

    @Test
    public void testDerivativeImageTempFile() {
        String pathname = Configuration.getInstance().
                getString(Key.FILESYSTEMCACHE_PATHNAME);

        Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        Crop crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(50f);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setPercent(0.905f);
        Rotate rotate = new Rotate(10);
        ColorTransform transform = ColorTransform.BITONAL;
        Format format = Format.TIF;

        OperationList ops = new OperationList(identifier, format,
                crop, scale, rotate, transform);

        final Path expected = Paths.get(
                pathname,
                "image",
                FilesystemCache.hashedPathFragment(identifier.toString()),
                ops.toFilename() + FilesystemCache.tempFileSuffix());
        assertEquals(expected, FilesystemCache.derivativeImageTempFile(ops));
    }

    @Test
    public void testInfoFile() {
        final String pathname = Configuration.getInstance().
                getString(Key.FILESYSTEMCACHE_PATHNAME);
        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        final Path expected = Paths.get(
                pathname,
                "info",
                FilesystemCache.hashedPathFragment(identifier.toString()),
                StringUtil.filesystemSafe(identifier.toString()) + ".json");
        assertEquals(expected, infoFile(identifier));
    }

    @Test
    public void testInfoTempFile() {
        final String pathname = Configuration.getInstance().
                getString(Key.FILESYSTEMCACHE_PATHNAME);
        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        final Path expected = Paths.get(
                pathname,
                "info",
                FilesystemCache.hashedPathFragment(identifier.toString()),
                StringUtil.filesystemSafe(identifier.toString()) + ".json"
                        + FilesystemCache.tempFileSuffix());
        assertEquals(expected, infoTempFile(identifier));
    }

    @Test
    public void testSourceImageFile() {
        final String pathname = Configuration.getInstance().
                getString(Key.FILESYSTEMCACHE_PATHNAME);
        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        final Path expected = Paths.get(
                pathname,
                "source",
                FilesystemCache.hashedPathFragment(identifier.toString()),
                StringUtil.filesystemSafe(identifier.toString()));
        assertEquals(expected, sourceImageFile(identifier));
    }

    @Test
    public void testSourceImageTempFile() {
        final String pathname = Configuration.getInstance().
                getString(Key.FILESYSTEMCACHE_PATHNAME);
        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        final Path expected = Paths.get(
                pathname,
                "source",
                FilesystemCache.hashedPathFragment(identifier.toString()),
                StringUtil.filesystemSafe(identifier.toString())
                        + FilesystemCache.tempFileSuffix());
        assertEquals(expected, sourceImageTempFile(identifier));
    }

    @Test
    public void testTempFileSuffix() {
        assertEquals("_" + Thread.currentThread().getName() + ".tmp",
                FilesystemCache.tempFileSuffix());
    }

    /* cleanUp() */

    @Test
    public void testCleanUpDoesNotDeleteValidFiles() throws Exception {
        OperationList ops = TestUtil.newOperationList();

        // create a new source image file
        Path sourceImageFile = sourceImageFile(ops.getIdentifier());
        writeStringToFile(sourceImageFile, "not empty");

        // create a new derivative image file
        Path derivativeImageFile = derivativeImageFile(ops);
        Files.createDirectories(derivativeImageFile.getParent());
        writeStringToFile(derivativeImageFile, "not empty");

        // create a new info file
        Path infoFile = infoFile(ops.getIdentifier());
        Files.createDirectories(infoFile.getParent());
        writeStringToFile(infoFile, "not empty");

        // create some temp files
        Path sourceImageTempFile = sourceImageTempFile(ops.getIdentifier());
        writeStringToFile(sourceImageTempFile, "not empty");

        Path derivativeImageTempFile = derivativeImageTempFile(ops);
        writeStringToFile(derivativeImageTempFile, "not empty");

        Path infoTempFile = infoTempFile(ops.getIdentifier());
        writeStringToFile(infoTempFile, "not empty");

        // create some empty files
        Path root = FilesystemCache.rootSourceImagePath();
        Path subdir = root.resolve("bogus");
        Files.createDirectories(subdir);
        Files.createFile(subdir.resolve("empty"));
        Files.createFile(subdir.resolve("empty2"));

        root = FilesystemCache.rootDerivativeImagePath();
        subdir = root.resolve("bogus");
        Files.createDirectories(subdir);
        Files.createFile(subdir.resolve("empty"));
        Files.createFile(subdir.resolve("empty2"));

        root = FilesystemCache.rootInfoPath();
        subdir = root.resolve("bogus");
        Files.createDirectories(subdir);
        Files.createFile(subdir.resolve("empty"));
        Files.createFile(subdir.resolve("empty2"));

        instance.setMinCleanableAge(10000);
        instance.cleanUp();

        assertRecursiveFileCount(fixturePath, 12);
    }

    @Test
    public void testCleanUpDeletesInvalidFiles() throws Exception {
        OperationList ops = TestUtil.newOperationList();

        // create a new source image file
        Path sourceImageFile = sourceImageFile(ops.getIdentifier());
        writeStringToFile(sourceImageFile, "not empty");

        // create a new derivative image file
        Path derivativeImageFile = derivativeImageFile(ops);
        writeStringToFile(derivativeImageFile, "not empty");

        // create a new info file
        Path infoFile = infoFile(ops.getIdentifier());
        writeStringToFile(infoFile, "not empty");

        // create some temp files
        Path sourceImageTempFile = sourceImageTempFile(ops.getIdentifier());
        writeStringToFile(sourceImageTempFile, "not empty");

        Path derivativeImageTempFile = derivativeImageTempFile(ops);
        writeStringToFile(derivativeImageTempFile, "not empty");

        Path infoTempFile = infoTempFile(ops.getIdentifier());
        writeStringToFile(infoTempFile, "not empty");

        // create some empty files
        Path root = FilesystemCache.rootSourceImagePath();
        Path subdir = root.resolve("bogus");
        Files.createDirectories(subdir);
        Files.createFile(subdir.resolve("empty"));
        Files.createFile(subdir.resolve("empty2"));

        root = FilesystemCache.rootDerivativeImagePath();
        subdir = root.resolve("bogus");
        Files.createDirectories(subdir);
        Files.createFile(subdir.resolve("empty"));
        Files.createFile(subdir.resolve("empty2"));

        root = FilesystemCache.rootInfoPath();
        subdir = root.resolve("bogus");
        Files.createDirectories(subdir);
        Files.createFile(subdir.resolve("empty"));
        Files.createFile(subdir.resolve("empty2"));

        instance.setMinCleanableAge(10);

        Thread.sleep(1000);

        instance.cleanUp();

        assertRecursiveFileCount(fixturePath, 3);
    }

    @Test
    public void testGetDerivativeImageFiles() throws Exception {
        Identifier identifier = new Identifier("dogs");
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(identifier);

        Path imageFile = derivativeImageFile(ops);
        createEmptyFile(imageFile);

        ops.add(new Rotate(15));
        imageFile = derivativeImageFile(ops);
        createEmptyFile(imageFile);

        ops.add(ColorTransform.GRAY);
        imageFile = derivativeImageFile(ops);
        createEmptyFile(imageFile);

        assertEquals(3, instance.getDerivativeImageFiles(identifier).size());
    }

    /* getSourceImageFile(Identifier) */

    @Test
    public void testGetSourceImageFileWithZeroTTL() throws Exception {
        Configuration.getInstance().setProperty(Key.CACHE_SERVER_TTL, 0);

        Identifier identifier = new Identifier("cats");
        assertNull(instance.getSourceImageFile(identifier));

        Path imageFile = sourceImageFile(identifier);
        Files.createDirectories(imageFile.getParent());
        Files.createFile(imageFile);
        assertNotNull(instance.getSourceImageFile(identifier));
    }

    @Test
    public void testGetSourceImageFileWithNonzeroTTL() throws Exception {
        Configuration.getInstance().setProperty(Key.CACHE_SERVER_TTL, 1);

        Identifier identifier = new Identifier("cats");
        Path cacheFile = sourceImageFile(identifier);
        Files.createDirectories(cacheFile.getParent());
        Files.createFile(cacheFile);
        assertNotNull(instance.getSourceImageFile(identifier));

        Thread.sleep(1100);

        assertNull(instance.getSourceImageFile(identifier));

        Thread.sleep(1000);

        assertFalse(Files.exists(cacheFile));
    }

    @Test
    public void testGetSourceImageFileConcurrently() throws Exception {
        final Identifier identifier = new Identifier("monkeys");

        new ConcurrentReaderWriter(() -> {
            try (OutputStream os =
                         instance.newSourceImageOutputStream(identifier)) {
                Files.copy(TestUtil.getImage("jpg"), os);
            }
            return null;
        }, () -> {
            instance.getSourceImageFile(identifier);
            return null;
        }).run();
    }

    /* newDerivativeImageOutputStream(OperationList) */

    @Test
    public void testNewDerivativeImageOutputStreamCreatesFolder()
            throws Exception {
        Files.walkFileTree(derivativeImagePath, new DeletingFileVisitor());
        assertFalse(Files.exists(derivativeImagePath));

        OperationList ops = TestUtil.newOperationList();
        try (OutputStream os = instance.newDerivativeImageOutputStream(ops)) {
            assertTrue(Files.exists(derivativeImagePath));
        }
    }

    /* newSourceImageOutputStream(Identifier) */

    @Test
    public void testNewSourceImageOutputStream() throws Exception {
        try (OutputStream os = instance.newSourceImageOutputStream(new Identifier("cats"))) {
            assertNotNull(os);
        }
    }

    @Test
    public void testNewSourceImageOutputStreamCreatesFolder() throws Exception {
        Files.walkFileTree(sourceImagePath, new DeletingFileVisitor());
        assertFalse(Files.exists(sourceImagePath));

        Identifier identifier = new Identifier("cats");
        try (OutputStream os = instance.newSourceImageOutputStream(identifier)) {
            assertTrue(Files.exists(sourceImagePath));
        }
    }

    @Test
    public void testNewSourceImageOutputStreamConcurrently() {
        // Tested in testGetSourceImageFileConcurrently()
    }

}
