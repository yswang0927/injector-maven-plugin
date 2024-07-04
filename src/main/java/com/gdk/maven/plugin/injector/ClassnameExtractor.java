package com.gdk.maven.plugin.injector;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Provides methods for extract class name from file name.
 */
public class ClassnameExtractor {

    private ClassnameExtractor() {
    }

    public static Iterator<File> iterateFiles(File dir, String[] extensions) {
        List<File> fileList = new ArrayList<>();
        if (dir != null && dir.isDirectory()) {
            iterateFiles(dir, extensions, fileList);
        }
        return fileList.iterator();
    }

    private static void iterateFiles(File dir, String[] extensions, List<File> fileList) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    iterateFiles(file, extensions, fileList);
                } else {
                    if (extensions == null || extensions.length == 0) {
                        fileList.add(file);
                    } else {
                        for (String extension : extensions) {
                            if (file.getName().endsWith(extension)) {
                                fileList.add(file);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public static String removeExtension(String filename) {
        if (filename == null) {
            return null;
        }

        final int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return filename;
        }

        final int lastUnixPos = filename.lastIndexOf('/');
        final int lastWindowsPos = filename.lastIndexOf('\\');
        final int lastSeparator = Math.max(lastUnixPos, lastWindowsPos);
        if (lastSeparator > lastDotIndex) {
            return filename;
        }

        return filename.substring(0, lastDotIndex);
    }

    /**
     * Remove parent directory from file name and replace directory separator with dots.
     *
     * <ul>
     * <li>parentDirectory: {@code /tmp/my/parent/src/}</li>
     * <li>classFile: {@code /tmp/my/parent/src/foo/bar/MyApp.class}</li>
     * </ul>
     * returns: {@code foo.bar.MyApp}
     *
     * @param parentDirectory to remove from {@code classFile} and maybe {@code null}.
     * @param classFile to extract the name from. Maybe {@code null}
     * @return class name extracted from file name or {@code null}
     * @throws IOException by file operations
     */
    public static String extractClassNameFromFile(final File parentDirectory,
                                                  final File classFile) throws IOException {
        if (null == classFile) {
            return null;
        }

        final String qualifiedFileName = parentDirectory != null
                ? classFile.getCanonicalPath()
                .substring(parentDirectory.getCanonicalPath().length() + 1)
                : classFile.getCanonicalPath();

        return removeExtension(qualifiedFileName.replace(File.separator, "."));
    }

    /**
     * Iterate over the class files and remove the parent directory from file name and replace
     * directory separator with dots.
     *
     * @param parentDirectory to remove from {@code classFile} and maybe {@code null}.
     * @param classFiles array to extract the names from. Must not be {@code null}.
     * @return iterator of full qualified class names based on passed classFiles
     * @see #iterateClassnames(File, Iterator)
     */
    public static Iterator<String> iterateClassnames(final File parentDirectory,
                                                     final File... classFiles) {
        return iterateClassnames(parentDirectory, Arrays.asList(classFiles).iterator());
    }

    /**
     * Wrapping the iterator (as reference) of class file names and extract full qualified class name
     * on
     * {@link Iterator#next()}.
     * <p>
     * It is possible that {@link Iterator#hasNext()} returns {@code true} and {@link Iterator#next()}
     * returns {@code null}.
     * </p>
     *
     * @param parentDirectory to remove from {@code classFile} and maybe {@code null}.
     * @param classFileIterator to extract the names from. Must not be {@code null}.
     * @return iterator of full qualified class names based on passed classFiles or {@code null}
     * @see #extractClassNameFromFile(File, File)
     */
    // DANGEROUS call by reference
    public static Iterator<String> iterateClassnames(final File parentDirectory,
                                                     final Iterator<File> classFileIterator) {
        return new Iterator<String>() {

            @Override
            public boolean hasNext() {
                return classFileIterator == null ? false : classFileIterator.hasNext();
            }

            @Override
            public String next() {
                final File classFile = classFileIterator.next();
                try {
                    // possible returns null
                    return extractClassNameFromFile(parentDirectory, classFile);
                } catch (final IOException e) {
                    throw new RuntimeException(e.getMessage());
                }
            }

            @Override
            public void remove() {
                classFileIterator.remove();
            }
        };
    }

    /**
     * Wrapping the list (as reference) of class file names and extract full qualified class name on
     * {@link Iterator#next()}.
     * <p>
     * It is possible that {@link Iterator#hasNext()} returns <code>true</code> and
     * {@link Iterator#next()} returns {@code null}.
     * </p>
     *
     * @param parentDirectory to remove from {@code classFile} and maybe {@code null}.
     * @param classFileList to extract the names from. Maybe {@code null}
     * @return list of full qualified class names based on passed classFiles or {@code null}
     * @throws IOException by file operations
     * @see #extractClassNameFromFile(File, File)
     */
    // DANGEROUS call by reference
    public static List<String> listClassnames(final File parentDirectory,
                                              final List<File> classFileList) throws IOException {
        if (null == classFileList || classFileList.isEmpty()) {
            return Collections.emptyList();
        }
        final List<String> list = new ArrayList<String>(classFileList.size());
        for (final File file : classFileList) {
            list.add(extractClassNameFromFile(parentDirectory, file));
        }
        return list;
    }

    /**
     * Wrapping the array of class file names and extract full qualified class name on
     * {@link Iterator#next()}.
     * <p>
     * It is possible that {@link Iterator#hasNext()} returns <code>true</code> and
     * {@link Iterator#next()} returns {@code null}.
     * </p>
     *
     * @param parentDirectory to remove from {@code classFile} and maybe {@code null}.
     * @param classFileList to extract the names from. Maybe {@code null}
     * @return list of full qualified class names based on passed classFiles or {@code null}
     * @throws IOException by file operations
     * @see #extractClassNameFromFile(File, File)
     */
    public static List<String> listClassnames(final File parentDirectory,
                                              final String... classFileList) throws IOException {
        if (null == classFileList || classFileList.length <= 0) {
            return Collections.emptyList();
        }
        final List<String> list = new ArrayList<String>(classFileList.length);
        for (final String file : classFileList) {
            list.add(extractClassNameFromFile(parentDirectory, new File(parentDirectory, file)));
        }
        return list;
    }

}
