/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.apache.commons.lang3.StringUtils;

/**
 * Provides access to the registered PMD languages. These are found
 * from the classpath of the {@link ClassLoader} of this class.
 */
public final class LanguageRegistry {

    private static final LanguageRegistry INSTANCE = new LanguageRegistry();

    private final Map<String, Language> languages;

    private LanguageRegistry() {
        List<Language> languagesList = new ArrayList<>();
        // Use current class' classloader instead of the threads context classloader, see https://github.com/pmd/pmd/issues/1377
        ServiceLoader<Language> languageLoader = ServiceLoader.load(Language.class, getClass().getClassLoader());
        Iterator<Language> iterator = languageLoader.iterator();

        while (true) {
            // this loop is weird, but both hasNext and next may throw ServiceConfigurationError,
            // it's more robust that way
            try {
                if (iterator.hasNext()) {
                    Language language = iterator.next();
                    languagesList.add(language);
                } else {
                    break;
                }
            } catch (UnsupportedClassVersionError | ServiceConfigurationError e) {
                // Some languages require java8 and are therefore only available
                // if java8 or later is used as runtime.
                System.err.println("Ignoring language for PMD: " + e.toString());
            }
        }

        // sort languages by terse name. Avoiding differences in the order of languages
        // across JVM versions / OS.
        Collections.sort(languagesList, new Comparator<Language>() {
            @Override
            public int compare(Language o1, Language o2) {
                return o1.getTerseName().compareToIgnoreCase(o2.getTerseName());
            }
        });

        // using a linked hash map to maintain insertion order
        languages = new LinkedHashMap<>();
        for (Language language : languagesList) {
            languages.put(language.getName(), language);
        }
    }

    /**
     * @deprecated Use the static methods instead, will be made private
     */
    @Deprecated
    public static LanguageRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Returns a collection of all the known languages. The ordering of this
     * collection is undefined.
     */
    public static Collection<Language> getLanguages() {
        // Filter out languages, that are not fully supported by PMD yet.
        // Those languages should not have a LanguageModule then, but they have it.
        // TODO This is unnecessary, if the incomplete language modules have been removed.
        List<Language> languages = new ArrayList<>();
        for (Language language : getInstance().languages.values()) {
            LanguageVersionHandler languageVersionHandler = language.getDefaultVersion().getLanguageVersionHandler();
            boolean pmdSupported = false;

            if (languageVersionHandler != null) {
                ParserOptions defaultParserOptions = languageVersionHandler.getDefaultParserOptions();
                Parser parser = languageVersionHandler.getParser(defaultParserOptions);
                pmdSupported = parser.canParse();
            }

            if (pmdSupported) {
                languages.add(language);
            }
        }
        return languages;
    }

    /**
     * Returns a language from its {@linkplain Language#getName() full name}
     * (eg {@code "Java"}). This is case sensitive.
     *
     * @param languageName Language name
     *
     * @return A language, or null if the name is unknown
     */
    public static Language getLanguage(String languageName) {
        return getInstance().languages.get(languageName);
    }

    /**
     * Returns a "default language" known to the service loader. This
     * is the Java language if available, otherwise an arbitrary one.
     * If no languages are loaded, returns null.
     *
     * @return A language, or null if the name is unknown
     */
    public static Language getDefaultLanguage() {
        Language defaultLanguage = getLanguage("Java");
        if (defaultLanguage == null) {
            Collection<Language> allLanguages = getInstance().languages.values();
            if (!allLanguages.isEmpty()) {
                defaultLanguage = allLanguages.iterator().next();
            }
        }
        return defaultLanguage;
    }

    /**
     * Returns a language from its {@linkplain Language#getTerseName() terse name}
     * (eg {@code "java"}). This is case sensitive.
     *
     * @param terseName Language terse name
     *
     * @return A language, or null if the name is unknown
     */
    public static Language findLanguageByTerseName(String terseName) {
        for (Language language : getInstance().languages.values()) {
            if (language.getTerseName().equals(terseName)) {
                return language;
            }
        }
        return null;
    }

    /**
     * @deprecated This is not useful, will be removed with 7.0.0
     */
    @Deprecated
    public static LanguageVersion findLanguageVersionByTerseName(String terseNameAndVersion) {
        String version;
        String terseName;
        if (terseNameAndVersion.contains(" ")) {
            version = StringUtils.trimToNull(terseNameAndVersion.substring(terseNameAndVersion.lastIndexOf(' ') + 1));
            terseName = terseNameAndVersion.substring(0, terseNameAndVersion.lastIndexOf(' '));
        } else {
            version = null;
            terseName = terseNameAndVersion;
        }
        Language language = findLanguageByTerseName(terseName);
        if (language != null) {
            if (version == null) {
                return language.getDefaultVersion();
            } else {
                return language.getVersion(version);
            }
        }
        return null;
    }

    /**
     * Returns all languages that support the given extension.
     *
     * @param extensionWithoutDot A file extension (without '.' prefix)
     */
    public static List<Language> findByExtension(String extensionWithoutDot) {
        List<Language> languages = new ArrayList<>();
        for (Language language : getInstance().languages.values()) {
            if (language.hasExtension(extensionWithoutDot)) {
                languages.add(language);
            }
        }
        return languages;
    }

    /**
     * @deprecated This is not useful, will be removed with 7.0.0
     */
    @Deprecated
    public static List<LanguageVersion> findAllVersions() {
        List<LanguageVersion> versions = new ArrayList<>();
        for (Language language : getLanguages()) {
            versions.addAll(language.getVersions());
        }
        return versions;
    }

    /**
     * A utility method to find the Languages which have Rule support.
     *
     * @return A List of Languages with Rule support.
     *
     * @deprecated This method will be removed with PMD 7.0.0. Use {@link #getLanguages()} instead.
     */
    @Deprecated
    public static List<Language> findWithRuleSupport() {
        return new ArrayList<>(getLanguages());
    }

    /**
     * @deprecated This is too specific, will be removed with 7.0.0
     */
    @Deprecated
    public static String commaSeparatedTerseNamesForLanguage(List<Language> languages) {
        StringBuilder builder = new StringBuilder();
        for (Language language : languages) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(language.getTerseName());
        }
        return builder.toString();
    }

    /**
     * @deprecated This is too specific, will be removed with 7.0.0
     */
    @Deprecated
    public static String commaSeparatedTerseNamesForLanguageVersion(List<LanguageVersion> languageVersions) {
        if (languageVersions == null || languageVersions.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(languageVersions.get(0).getTerseName());
        for (int i = 1; i < languageVersions.size(); i++) {
            builder.append(", ").append(languageVersions.get(i).getTerseName());
        }
        return builder.toString();
    }

}
