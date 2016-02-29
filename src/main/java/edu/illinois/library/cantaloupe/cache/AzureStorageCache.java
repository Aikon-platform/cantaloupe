package edu.illinois.library.cantaloupe.cache;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.processor.ImageInfo;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.restlet.data.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.util.Calendar;
import java.util.Date;

/**
 * @see <a href="https://github.com/azure/azure-storage-java">
 *     Microsoft Azure Storage DSK for Java</a>
 */
class AzureStorageCache implements DerivativeCache {

    private static Logger logger = LoggerFactory.
            getLogger(AzureStorageCache.class);

    public static final String ACCOUNT_KEY_CONFIG_KEY =
            "AzureStorageCache.account_key";
    public static final String ACCOUNT_NAME_CONFIG_KEY =
            "AzureStorageCache.account_name";
    public static final String CONTAINER_NAME_CONFIG_KEY =
            "AzureStorageCache.container_name";
    public static final String OBJECT_KEY_PREFIX_CONFIG_KEY =
            "AzureStorageCache.object_key_prefix";
    public static final String TTL_SECONDS_CONFIG_KEY =
            "AzureStorageCache.ttl_seconds";

    private static CloudBlobClient client;

    public static synchronized CloudBlobClient getClientInstance() {
        if (client == null) {
            try {
                final Configuration config = Application.getConfiguration();
                final String accountName = config.getString(ACCOUNT_NAME_CONFIG_KEY);
                final String accountKey = config.getString(ACCOUNT_KEY_CONFIG_KEY);

                final String connectionString = String.format(
                        "DefaultEndpointsProtocol=https;" +
                                "AccountName=%s;" +
                                "AccountKey=%s", accountName, accountKey);
                final CloudStorageAccount account =
                        CloudStorageAccount.parse(connectionString);

                logger.info("Using account: {}", accountName);

                client = account.createCloudBlobClient();

                client.getContainerReference(getContainerName()).
                        createIfNotExists();
            } catch (StorageException | URISyntaxException | InvalidKeyException e) {
                logger.error(e.getMessage());
            }
        }
        return client;
    }

    public static String getContainerName() {
        final Configuration config = Application.getConfiguration();
        // All letters in a container name must be lowercase.
        return config.getString(CONTAINER_NAME_CONFIG_KEY).toLowerCase();
    }

    /**
     * Does nothing, as this cache is always clean.
     */
    @Override
    public void cleanUp() throws CacheException {}

    @Override
    public ImageInfo getImageInfo(Identifier identifier) throws CacheException {
        final String containerName = getContainerName();

        final CloudBlobClient client = getClientInstance();
        try {
            final long msec = System.currentTimeMillis();
            final CloudBlobContainer container =
                    client.getContainerReference(containerName);
            final String objectKey = getObjectKey(identifier);

            final CloudBlockBlob blob = container.getBlockBlobReference(objectKey);
            if (blob.exists()) {
                ImageInfo info = ImageInfo.fromJson(blob.openInputStream());
                logger.info("getImageInfo(): read {} from container {} in {} msec",
                        objectKey, containerName,
                        System.currentTimeMillis() - msec);
                return info;
            }
            return null;
        } catch (IOException | URISyntaxException | StorageException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

    @Override
    public InputStream getImageInputStream(OperationList opList)
            throws CacheException {
        final String containerName = getContainerName();

        final CloudBlobClient client = getClientInstance();
        try {
            final CloudBlobContainer container =
                    client.getContainerReference(containerName);
            final String objectKey = getObjectKey(opList);

            logger.info("getImageInputStream(): bucket: {}; key: {}",
                    containerName, objectKey);
            final CloudBlockBlob blob = container.getBlockBlobReference(objectKey);
            if (blob.exists()) {
                return blob.openInputStream();
            }
            return null;
        } catch (URISyntaxException | StorageException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

    @Override
    public OutputStream getImageOutputStream(OperationList opList)
            throws CacheException {
        final String containerName = getContainerName();

        final CloudBlobClient client = getClientInstance();
        final String objectKey = getObjectKey(opList);

        try {
            final CloudBlobContainer container =
                    client.getContainerReference(containerName);
            final CloudBlockBlob blob = container.getBlockBlobReference(objectKey);
            blob.getProperties().setContentType(opList.getOutputFormat().
                    getPreferredMediaType().toString());
            return blob.openOutputStream();
        } catch (URISyntaxException | StorageException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

    /**
     * @param identifier
     * @return Object key of the serialized ImageInfo associated with the given
     *         identifier.
     */
    public String getObjectKey(Identifier identifier) {
        try {
            return getObjectKeyPrefix() + "info/" +
                    URLEncoder.encode(identifier.toString(), "UTF-8") + ".json";
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * @param opList
     * @return Object key of the image associated with the given operation list.
     */
    public String getObjectKey(OperationList opList) {
        try {
            return getObjectKeyPrefix() + "image/" +
                    URLEncoder.encode(opList.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * @return Value of {@link #OBJECT_KEY_PREFIX_CONFIG_KEY} with trailing
     * slash.
     */
    public String getObjectKeyPrefix() {
        String prefix = Application.getConfiguration().
                getString(OBJECT_KEY_PREFIX_CONFIG_KEY);
        if (prefix.length() < 1 || prefix.equals("/")) {
            return "";
        }
        return StringUtils.stripEnd(prefix, "/") + "/";
    }

    @Override
    public void purge() throws CacheException {
        final String containerName = getContainerName();

        final CloudBlobClient client = getClientInstance();

        try {
            final CloudBlobContainer container =
                    client.getContainerReference(containerName);
            int count = 0;
            for (ListBlobItem item : container.listBlobs(getObjectKeyPrefix(), true)) {
                final CloudBlockBlob blob = container.
                        getBlockBlobReference(new Reference(item.getUri()).getLastSegment(false));
                if (blob.deleteIfExists()) {
                    count++;
                }
            }
            logger.info("purge(): deleted {} items", count);
        } catch (URISyntaxException | StorageException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

    @Override
    public void purge(OperationList opList) throws CacheException {
        final String containerName = getContainerName();

        final CloudBlobClient client = getClientInstance();
        final String objectKey = getObjectKey(opList);

        try {
            final CloudBlobContainer container =
                    client.getContainerReference(containerName);
            final CloudBlockBlob blob = container.getBlockBlobReference(objectKey);
            blob.deleteIfExists();
        } catch (URISyntaxException | StorageException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

    @Override
    public void purgeExpired() throws CacheException {
        final String containerName = getContainerName();

        final CloudBlobClient client = getClientInstance();

        final Calendar c = Calendar.getInstance();
        c.add(Calendar.SECOND, 0 - Application.getConfiguration().
                getInt(TTL_SECONDS_CONFIG_KEY));
        final Date cutoffDate = c.getTime();

        try {
            final CloudBlobContainer container =
                    client.getContainerReference(containerName);
            int count = 0, deletedCount = 0;

            for (ListBlobItem item : container.listBlobs(getObjectKeyPrefix(), true)) {
                CloudBlockBlob blob = container.
                        getBlockBlobReference(item.getUri().toString());
                if (blob.exists()) {
                    System.out.println("exists: " + blob.getUri());
                    count++;
                    if (blob.getProperties().getLastModified().before(cutoffDate)) {
                        System.out.println(blob.getProperties().getLastModified());
                        blob.delete();
                        deletedCount++;
                    }
                }
            }
            logger.info("purgeExpired(): deleted {} of {} items",
                    deletedCount, count);
        } catch (URISyntaxException | StorageException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

    @Override
    public void purgeImage(Identifier identifier) throws CacheException {
        final String containerName = getContainerName();

        final CloudBlobClient client = getClientInstance();
        final String objectKey = getObjectKey(identifier);

        try {
            final CloudBlobContainer container =
                    client.getContainerReference(containerName);
            final CloudBlockBlob blob = container.getBlockBlobReference(objectKey);
            blob.deleteIfExists();
        } catch (URISyntaxException | StorageException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }

    @Override
    public void putImageInfo(Identifier identifier, ImageInfo imageInfo)
            throws CacheException {
        final String containerName = getContainerName();

        final CloudBlobClient client = getClientInstance();
        final String objectKey = getObjectKey(identifier);

        try {
            final CloudBlobContainer container =
                    client.getContainerReference(containerName);
            final CloudBlockBlob blob = container.getBlockBlobReference(objectKey);
            blob.getProperties().setContentType("application/json");
            blob.getProperties().setContentEncoding("UTF-8");
            blob.uploadText(imageInfo.toJson()); // TODO: stream it
        } catch (IOException | URISyntaxException | StorageException e) {
            throw new CacheException(e.getMessage(), e);
        }
    }
}
