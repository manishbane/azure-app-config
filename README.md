# azure-app-config
Utility for Azure Web Application Configuration

1. Allows to connect to an Azure App Config endpoint and retrieve entries, including rerouting to Azure Key Vault
2. Decode a URI retrieved from a JSON string. Used to decode app config values that reference a Key Vault.
3. Allows to retrieve token credentials depending whether you are on Azure cloud or on-premise, based on the MANAGED_IDENTITY_CLIENT_ID environment variable or system property being defined or not.
