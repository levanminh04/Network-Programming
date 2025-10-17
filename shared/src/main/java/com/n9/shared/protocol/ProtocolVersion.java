//package com.n9.shared.protocol;
//
//import com.n9.shared.constants.ProtocolConstants;
//
///**
// * Protocol Version Utility
// *
// * Handles protocol versioning and compatibility checking between
// * different versions of the client and server.
// *
// * Version format: MAJOR.MINOR.PATCH (Semantic Versioning)
// * - MAJOR: Breaking changes (incompatible)
// * - MINOR: New features (backward compatible)
// * - PATCH: Bug fixes (backward compatible)
// *
// * @author N9 Team
// * @version 1.0.0
// * @since 2025-01-05
// */
//public final class ProtocolVersion {
//
//    // Prevent instantiation
//    private ProtocolVersion() {
//        throw new AssertionError("Cannot instantiate utility class");
//    }
//
//    /**
//     * Current protocol version (from ProtocolConstants)
//     */
//    public static final String CURRENT = ProtocolConstants.PROTOCOL_VERSION;
//
//    /**
//     * Minimum supported client version
//     * Clients with version below this will be rejected
//     */
//    public static final String MIN_SUPPORTED_CLIENT = "1.0.0";
//
//    /**
//     * Minimum supported server version
//     * Servers with version below this are incompatible
//     */
//    public static final String MIN_SUPPORTED_SERVER = "1.0.0";
//
//    // ============================================================================
//    // VERSION PARSING
//    // ============================================================================
//
//    /**
//     * Parse version string into components
//     *
//     * @param version Version string (e.g., "1.2.3")
//     * @return Version components [major, minor, patch]
//     * @throws IllegalArgumentException if version format is invalid
//     */
//    public static int[] parseVersion(String version) {
//        if (version == null || version.trim().isEmpty()) {
//            throw new IllegalArgumentException("Version cannot be null or empty");
//        }
//
//        String[] parts = version.trim().split("\\.");
//        if (parts.length != 3) {
//            throw new IllegalArgumentException(
//                "Invalid version format. Expected: MAJOR.MINOR.PATCH, got: " + version
//            );
//        }
//
//        try {
//            int major = Integer.parseInt(parts[0]);
//            int minor = Integer.parseInt(parts[1]);
//            int patch = Integer.parseInt(parts[2]);
//
//            if (major < 0 || minor < 0 || patch < 0) {
//                throw new IllegalArgumentException(
//                    "Version components must be non-negative"
//                );
//            }
//
//            return new int[] { major, minor, patch };
//
//        } catch (NumberFormatException e) {
//            throw new IllegalArgumentException(
//                "Invalid version format. All components must be integers: " + version, e
//            );
//        }
//    }
//
//    /**
//     * Check if version string is valid
//     *
//     * @param version Version to validate
//     * @return true if valid, false otherwise
//     */
//    public static boolean isValidVersion(String version) {
//        try {
//            parseVersion(version);
//            return true;
//        } catch (IllegalArgumentException e) {
//            return false;
//        }
//    }
//
//    // ============================================================================
//    // VERSION COMPARISON
//    // ============================================================================
//
//    /**
//     * Compare two versions
//     *
//     * @param version1 First version
//     * @param version2 Second version
//     * @return Negative if version1 < version2, 0 if equal, positive if version1 > version2
//     */
//    public static int compareVersions(String version1, String version2) {
//        int[] v1 = parseVersion(version1);
//        int[] v2 = parseVersion(version2);
//
//        // Compare major
//        if (v1[0] != v2[0]) {
//            return Integer.compare(v1[0], v2[0]);
//        }
//
//        // Compare minor
//        if (v1[1] != v2[1]) {
//            return Integer.compare(v1[1], v2[1]);
//        }
//
//        // Compare patch
//        return Integer.compare(v1[2], v2[2]);
//    }
//
//    /**
//     * Check if version1 is greater than version2
//     *
//     * @param version1 First version
//     * @param version2 Second version
//     * @return true if version1 > version2
//     */
//    public static boolean isGreaterThan(String version1, String version2) {
//        return compareVersions(version1, version2) > 0;
//    }
//
//    /**
//     * Check if version1 is less than version2
//     *
//     * @param version1 First version
//     * @param version2 Second version
//     * @return true if version1 < version2
//     */
//    public static boolean isLessThan(String version1, String version2) {
//        return compareVersions(version1, version2) < 0;
//    }
//
//    /**
//     * Check if version1 equals version2
//     *
//     * @param version1 First version
//     * @param version2 Second version
//     * @return true if versions are equal
//     */
//    public static boolean isEqual(String version1, String version2) {
//        return compareVersions(version1, version2) == 0;
//    }
//
//    // ============================================================================
//    // COMPATIBILITY CHECKING
//    // ============================================================================
//
//    /**
//     * Check if client version is compatible with server
//     *
//     * Compatible if:
//     * - Major versions match (breaking changes)
//     * - Client version >= minimum supported version
//     *
//     * @param clientVersion Client version
//     * @return true if compatible
//     */
//    public static boolean isClientCompatible(String clientVersion) {
//        if (!isValidVersion(clientVersion)) {
//            return false;
//        }
//
//        int[] client = parseVersion(clientVersion);
//        int[] server = parseVersion(CURRENT);
//        int[] minSupported = parseVersion(MIN_SUPPORTED_CLIENT);
//
//        // Major version must match (breaking changes)
//        if (client[0] != server[0]) {
//            return false;
//        }
//
//        // Client must be >= minimum supported version
//        return compareVersions(clientVersion, MIN_SUPPORTED_CLIENT) >= 0;
//    }
//
//    /**
//     * Check if server version is compatible with client
//     *
//     * @param serverVersion Server version
//     * @return true if compatible
//     */
//    public static boolean isServerCompatible(String serverVersion) {
//        if (!isValidVersion(serverVersion)) {
//            return false;
//        }
//
//        int[] server = parseVersion(serverVersion);
//        int[] client = parseVersion(CURRENT);
//
//        // Major version must match
//        if (server[0] != client[0]) {
//            return false;
//        }
//
//        // Server must be >= minimum supported version
//        return compareVersions(serverVersion, MIN_SUPPORTED_SERVER) >= 0;
//    }
//
//    /**
//     * Check if two versions are compatible with each other
//     *
//     * @param version1 First version
//     * @param version2 Second version
//     * @return true if compatible (major versions match)
//     */
//    public static boolean areCompatible(String version1, String version2) {
//        if (!isValidVersion(version1) || !isValidVersion(version2)) {
//            return false;
//        }
//
//        int[] v1 = parseVersion(version1);
//        int[] v2 = parseVersion(version2);
//
//        // Compatible if major versions match
//        return v1[0] == v2[0];
//    }
//
//    // ============================================================================
//    // VERSION INFO
//    // ============================================================================
//
//    /**
//     * Get major version number
//     *
//     * @param version Version string
//     * @return Major version number
//     */
//    public static int getMajorVersion(String version) {
//        return parseVersion(version)[0];
//    }
//
//    /**
//     * Get minor version number
//     *
//     * @param version Version string
//     * @return Minor version number
//     */
//    public static int getMinorVersion(String version) {
//        return parseVersion(version)[1];
//    }
//
//    /**
//     * Get patch version number
//     *
//     * @param version Version string
//     * @return Patch version number
//     */
//    public static int getPatchVersion(String version) {
//        return parseVersion(version)[2];
//    }
//
//    /**
//     * Get version info as string
//     *
//     * @param version Version string
//     * @return Human-readable version info
//     */
//    public static String getVersionInfo(String version) {
//        int[] v = parseVersion(version);
//        return String.format(
//            "Version %s (Major: %d, Minor: %d, Patch: %d)",
//            version, v[0], v[1], v[2]
//        );
//    }
//
//    // ============================================================================
//    // COMPATIBILITY MESSAGES
//    // ============================================================================
//
//    /**
//     * Get compatibility error message for incompatible client
//     *
//     * @param clientVersion Client version
//     * @return Error message
//     */
//    public static String getIncompatibilityMessage(String clientVersion) {
//        if (!isValidVersion(clientVersion)) {
//            return String.format(
//                "Invalid client version format: '%s'. Expected format: MAJOR.MINOR.PATCH",
//                clientVersion
//            );
//        }
//
//        int[] client = parseVersion(clientVersion);
//        int[] server = parseVersion(CURRENT);
//
//        if (client[0] != server[0]) {
//            return String.format(
//                "Incompatible protocol version. Client: %s, Server: %s. " +
//                "Major version mismatch (breaking changes). Please update your client.",
//                clientVersion, CURRENT
//            );
//        }
//
//        if (compareVersions(clientVersion, MIN_SUPPORTED_CLIENT) < 0) {
//            return String.format(
//                "Client version %s is too old. Minimum supported version: %s. " +
//                "Please update your client.",
//                clientVersion, MIN_SUPPORTED_CLIENT
//            );
//        }
//
//        return "Unknown compatibility issue";
//    }
//
//    /**
//     * Get upgrade recommendation message
//     *
//     * @param clientVersion Client version
//     * @return Upgrade message if needed, null if up to date
//     */
//    public static String getUpgradeRecommendation(String clientVersion) {
//        if (!isValidVersion(clientVersion)) {
//            return null;
//        }
//
//        int comparison = compareVersions(clientVersion, CURRENT);
//
//        if (comparison < 0) {
//            int[] client = parseVersion(clientVersion);
//            int[] server = parseVersion(CURRENT);
//
//            if (client[1] < server[1]) {
//                return String.format(
//                    "New features available! Your version: %s, Latest: %s. " +
//                    "Consider upgrading for new features.",
//                    clientVersion, CURRENT
//                );
//            } else if (client[2] < server[2]) {
//                return String.format(
//                    "Bug fixes available! Your version: %s, Latest: %s. " +
//                    "Consider upgrading for bug fixes.",
//                    clientVersion, CURRENT
//                );
//            }
//        }
//
//        return null; // Up to date
//    }
//}
