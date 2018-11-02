package io.fnproject.kafkaconnect.sink;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;

public final class FnInvocationConfig {

    static final String TENANT_OCID_CONFIG = "tenant_ocid";
    static final String TENANT_OCID_CONFIG_DESC = "OCI Root Tenant OCID";

    static final String USER_OCID_CONFIG = "user_ocid";
    static final String USER_OCID_CONFIG_DESC = "User OCID";

    static final String PUBLIC_KEY_FINGERPRINT_CONFIG = "public_fingerprint";
    static final String PUBLIC_KEY_FINGERPRINT_CONFIG_DESC = "Public Key Fingerprint";

    static final String PRIVATE_KEY_CONFIG = "private_key_location";
    static final String PRIVATE_KEY_CONFIG_DESC = "Private Key (.pem) location";

    static final String FUNCTION_URL_CONFIG = "function_url";
    static final String FUNCTION_URL_CONFIG_DESC = "Function endpoint URL";

    public static ConfigDef getConfigDef() {
        return new ConfigDef()
                .define(TENANT_OCID_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, TENANT_OCID_CONFIG_DESC)
                .define(USER_OCID_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, USER_OCID_CONFIG_DESC)
                .define(PUBLIC_KEY_FINGERPRINT_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, PUBLIC_KEY_FINGERPRINT_CONFIG_DESC)
                .define(PRIVATE_KEY_CONFIG, ConfigDef.Type.STRING, ConfigDef.NO_DEFAULT_VALUE, new PrivateKeyFileTypeValidator(), ConfigDef.Importance.HIGH, PRIVATE_KEY_CONFIG_DESC)
                .define(FUNCTION_URL_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, FUNCTION_URL_CONFIG_DESC);
    }

    private static class PrivateKeyFileTypeValidator implements ConfigDef.Validator {

        @Override
        public void ensureValid(String configName, Object privateKeyLocation) {
            if (!((String)privateKeyLocation).endsWith(".pem")) {
                throw new ConfigException(configName, privateKeyLocation, "Private key should be of type PEM with a .pem extension");
            }
        }

    }
}
