package edu.illinois.library.cantaloupe.source;

import com.microsoft.azure.storage.SharedAccessAccountPermissions;
import com.microsoft.azure.storage.SharedAccessAccountPolicy;
import com.microsoft.azure.storage.SharedAccessAccountResourceType;
import com.microsoft.azure.storage.SharedAccessAccountService;
import com.microsoft.azure.storage.SharedAccessProtocols;
import com.microsoft.azure.storage.StorageException;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import edu.illinois.library.cantaloupe.script.DelegateProxyService;
import edu.illinois.library.cantaloupe.test.AzureStorageTestUtil;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.security.InvalidKeyException;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;

import static org.junit.Assert.*;

/**
 * Tests AzureStorageSource against Azure Storage. (Requires an Azure
 * account.)
 */
public class AzureStorageSourceTest extends AbstractSourceTest {

    private AzureStorageSource instance;

    @BeforeClass
    public static void beforeClass() throws Exception {
        BaseTest.beforeClass();
        AzureStorageTestUtil.uploadFixtures();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        BaseTest.afterClass();
        AzureStorageTestUtil.deleteFixtures();
    }

    private static void clearConfig() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.AZURESTORAGESOURCE_CONTAINER_NAME, "");
        config.setProperty(Key.AZURESTORAGESOURCE_ACCOUNT_NAME, "");
        config.setProperty(Key.AZURESTORAGESOURCE_ACCOUNT_KEY, "");
    }

    private static String generateSAS()
            throws StorageException, InvalidKeyException {
        SharedAccessAccountPolicy policy = new SharedAccessAccountPolicy();
        policy.setPermissions(EnumSet.of(
                SharedAccessAccountPermissions.READ,
                SharedAccessAccountPermissions.WRITE,
                SharedAccessAccountPermissions.LIST));
        policy.setServices(EnumSet.of(
                SharedAccessAccountService.BLOB,
                SharedAccessAccountService.FILE));
        policy.setResourceTypes(EnumSet.of(
                SharedAccessAccountResourceType.OBJECT));

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, 365 * 100);
        policy.setSharedAccessExpiryTime(c.getTime());
        policy.setProtocols(SharedAccessProtocols.HTTPS_ONLY);

        return AzureStorageSource.getAccount()
                .generateSharedAccessSignature(policy);
    }

    private static String getAccountName() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.AZURE_ACCOUNT_NAME.getKey());
    }

    private static String getAccountKey() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.AZURE_ACCOUNT_KEY.getKey());
    }

    private static String getContainer() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.AZURE_CONTAINER.getKey());
    }

    private static String getSASURI()
            throws StorageException, InvalidKeyException {
        return String.format("https://%s.blob.core.windows.net/%s/%s?%s",
                AzureStorageTestUtil.getAccountName(),
                AzureStorageTestUtil.getContainer(),
                AzureStorageTestUtil.OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION,
                generateSAS());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @Override
    void destroyEndpoint() {
        // will be done in @AfterClass
    }

    @Override
    void initializeEndpoint() {
        // will be done in @BeforeClass
    }

    @Override
    AzureStorageSource newInstance() {
        AzureStorageSource instance = new AzureStorageSource();
        instance.setIdentifier(new Identifier(AzureStorageTestUtil.OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION));
        return instance;
    }

    @Override
    void useBasicLookupStrategy() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.AZURESTORAGESOURCE_CONTAINER_NAME,
                getContainer());
        config.setProperty(Key.AZURESTORAGESOURCE_ACCOUNT_NAME,
                getAccountName());
        config.setProperty(Key.AZURESTORAGESOURCE_ACCOUNT_KEY,
                getAccountKey());
        config.setProperty(Key.AZURESTORAGESOURCE_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
    }

    @Override
    void useScriptLookupStrategy() {
        try {
            Configuration config = Configuration.getInstance();
            config.setProperty(Key.AZURESTORAGESOURCE_LOOKUP_STRATEGY,
                    "ScriptLookupStrategy");
            config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
            config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                    TestUtil.getFixture("delegates.rb").toString());

            Identifier identifier = new Identifier(AzureStorageTestUtil.OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION);
            RequestContext context = new RequestContext();
            context.setIdentifier(identifier);
            DelegateProxyService service = DelegateProxyService.getInstance();
            DelegateProxy proxy = service.newDelegateProxy(context);
            instance.setDelegateProxy(proxy);
        } catch (Exception e) {
            fail();
        }
    }

    /* checkAccess() */

    @Test
    public void testCheckAccessUsingBasicLookupStrategyWithPresentUnreadableImage() {
        // TODO: write this
    }

    @Test
    public void testCheckAccessUsingScriptLookupStrategyWithPresentReadableImage()
            throws Exception {
        useScriptLookupStrategy();
        instance.checkAccess();
    }

    @Test
    public void testCheckAccessUsingScriptLookupStrategyWithPresentUnreadableImage() {
        useScriptLookupStrategy();
        // TODO: write this
    }

    @Test(expected = NoSuchFileException.class)
    public void testCheckAccessUsingScriptLookupStrategyWithMissingImage()
            throws Exception {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier("bogus");
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        instance.checkAccess();
    }

    @Test
    public void testCheckAccessWithSAS() throws Exception {
        instance.setIdentifier(new Identifier(getSASURI()));
        clearConfig();
        instance.checkAccess();
    }

    /* getFormat() */

    @Test
    public void testGetSourceFormatUsingBasicLookupStrategy()
            throws IOException {
        assertEquals(Format.JPG, instance.getFormat());
    }

    @Test
    public void testGetSourceFormatUsingScriptLookupStrategy()
            throws IOException {
        useScriptLookupStrategy();
        assertEquals(Format.JPG, instance.getFormat());
    }

    @Test
    public void testGetSourceFormatWithContentTypeAndRecognizedExtensionInObjectKey()
            throws IOException {
        instance.setIdentifier(new Identifier(AzureStorageTestUtil.OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION));
        assertEquals(Format.JPG, instance.getFormat());
    }

    @Test
    public void testGetSourceFormatWithContentTypeAndUnrecognizedExtensionInObjectKey()
            throws IOException {
        instance.setIdentifier(new Identifier(AzureStorageTestUtil.OBJECT_KEY_WITH_CONTENT_TYPE_AND_UNRECOGNIZED_EXTENSION));
        assertEquals(Format.JPG, instance.getFormat());
    }

    @Test
    public void testGetSourceFormatWithContentTypeAndNoExtensionInObjectKey()
            throws IOException {
        instance.setIdentifier(new Identifier(AzureStorageTestUtil.OBJECT_KEY_WITH_CONTENT_TYPE_BUT_NO_EXTENSION));
        assertEquals(Format.JPG, instance.getFormat());
    }

    @Test
    public void testGetSourceFormatWithNoContentTypeButRecognizedExtensionInObjectKey()
            throws IOException {
        instance.setIdentifier(new Identifier(AzureStorageTestUtil.OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION));
        assertEquals(Format.JPG, instance.getFormat());
    }

    @Test
    public void testGetSourceFormatWithNoContentTypeAndUnrecognizedExtensionInObjectKey()
            throws IOException {
        instance.setIdentifier(new Identifier(AzureStorageTestUtil.OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_UNRECOGNIZED_EXTENSION));
        assertEquals(Format.JPG, instance.getFormat());
    }

    @Test
    public void testGetSourceFormatWithNoContentTypeOrExtensionInObjectKey()
            throws IOException {
        instance.setIdentifier(new Identifier(AzureStorageTestUtil.OBJECT_KEY_WITH_NO_CONTENT_TYPE_OR_EXTENSION));
        assertEquals(Format.JPG, instance.getFormat());
    }

    @Test
    public void testGetSourceFormatWithNonImage() throws IOException {
        instance.setIdentifier(new Identifier(AzureStorageTestUtil.NON_IMAGE_KEY));
        assertEquals(Format.UNKNOWN, instance.getFormat());
    }

    @Test
    public void testGetSourceFormatWithSAS() throws Exception {
        instance.setIdentifier(new Identifier(getSASURI()));
        clearConfig();
        instance.getFormat();
    }

    /* newStreamFactory() */

    @Test
    public void testNewStreamFactoryUsingBasicLookupStrategy() throws Exception {
        instance.newStreamFactory();
    }

    @Test
    public void testNewStreamFactoryUsingScriptLookupStrategy() throws Exception {
        useScriptLookupStrategy();
        assertNotNull(instance.newStreamFactory());
    }

    @Test
    public void testNewStreamFactoryWithSAS() throws Exception {
        instance.setIdentifier(new Identifier(getSASURI()));
        clearConfig();
        instance.newStreamFactory();
    }

}
