# Persistent app signing

`release.p12.enc` contains the AES-256-CBC/PBKDF2-encrypted project-specific PKCS#12 signing keystore. The decryption password and raw private key are **not** committed to the repository. The GitHub Actions secret `XGIMI_SIGNING_PASSPHRASE` is required for the release workflow.

Release builds use the same key across versions. The workflow intentionally refuses to overwrite an existing versioned release (avoids republishing APKs under the same tag with differing bytes). For a later version increment `versionCode`, `versionName`, `RELEASE_TAG` and release notes together.

The certificate is for app integrity/update identity. It does not prove licensing of the streamed content or trustworthiness of the website.

Certificate SHA-256 fingerprint of project release key (locally verified):
`C5:73:3E:C4:C9:AE:98:FF:54:6F:AA:D9:57:B2:31:0F:65:38:6C:41:9C:7D:12:F6:3A:64:44:E8:AD:68:B9:4B`
