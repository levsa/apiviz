/*
 * Copyright (C) 2008  Trustin Heuiseung Lee
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, 5th Floor, Boston, MA 02110-1301 USA
 */
package net.gleamynode.apiviz;

import static net.gleamynode.apiviz.Constant.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.TreeMap;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.RootDoc;
import com.sun.tools.doclets.standard.Standard;

/**
 * @author The APIViz Project (netty@googlegroups.com)
 * @author Trustin Lee (trustin@gmail.com)
 *
 * @version $Rev$, $Date$
 *
 */
public class ApiVizDoclet {

    public static boolean start(RootDoc root) {
        Standard.start(root);
        try {
            File outputDirectory = getOutputDirectory(root.options());

            generateOverviewSummary(root, outputDirectory);
            generatePackageSummaries(root, outputDirectory);
            generateClassDiagrams(root, outputDirectory);
        } catch(Throwable t) {
            root.printError("An error occurred during diagram generation:" + t.toString());
            t.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean validOptions(String[][] options, DocErrorReporter errorReporter) {
        return Standard.validOptions(options, errorReporter);
    }

    public static int optionLength(String option) {
        return Standard.optionLength(option);
    }

    public static LanguageVersion languageVersion() {
        return Standard.languageVersion();
    }

    private static void generateOverviewSummary(RootDoc root, File outputDirectory) throws IOException {
        DiagramBuilder builder = new DiagramBuilder(root);
        for (PackageDoc p: getPackages(root).values()) {
            builder.addPackage(p);
        }
        instrumentDiagram(root, outputDirectory, "overview-summary", builder.toString());
    }

    private static void generatePackageSummaries(RootDoc root, File outputDirectory) throws IOException {
        for (PackageDoc p: getPackages(root).values()) {
            generatePackageSummary(root, p, outputDirectory);
        }
    }

    private static void generatePackageSummary(RootDoc root, PackageDoc p, File outputDirectory) throws IOException {
        DiagramBuilder builder = new DiagramBuilder(root, p);
        for (ClassDoc c: p.allClasses()) {
            if (c.tags(TAG_HIDDEN).length < 0) {
                continue;
            }

            builder.addClass(c);
        }

        instrumentDiagram(root, outputDirectory, p.name().replace('.', File.separatorChar) + File.separatorChar + "package-summary", builder.toString());
    }

    private static void generateClassDiagrams(RootDoc root, File outputDirectory) throws IOException {
        for (PackageDoc p: getPackages(root).values()) {
            for (ClassDoc c: p.allClasses()) {
                generateClassDiagram(root, p, c, outputDirectory);
            }
        }
    }

    private static void generateClassDiagram(RootDoc root, PackageDoc p, ClassDoc c, File outputDirectory) throws IOException {
        DiagramBuilder builder = new DiagramBuilder(root, p);
        builder.setUseLandscapeView(false);
        builder.setUseAutoLandmark(false);
        builder.setUseSeeTags(true);
        builder.addClass(c);
        builder.addLandmark(c);
        instrumentDiagram(root, outputDirectory, c.qualifiedName().replace('.', '/'), builder.toString());
    }

    private static Map<String, PackageDoc> getPackages(RootDoc root) {
        Map<String, PackageDoc> packages = new TreeMap<String, PackageDoc>();
        for (ClassDoc c: root.classes()) {
            PackageDoc p = c.containingPackage();
            if(!packages.containsKey(p.name())) {
                packages.put(p.name(), p);
            }
        }

        return packages;
    }

    private static void instrumentDiagram(RootDoc root, File outputDirectory, String filename, String diagram) throws IOException {
        File outFile = new File(outputDirectory, filename + ".png");

        root.printNotice("Generating " + outFile + "...");
        Process p = Runtime.getRuntime().exec(new String [] {
            "dot",
            "-Tpng",
            "-o",
            outFile.getAbsolutePath(),
        });

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), "UTF-8"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));

        try {
            writer.write(diagram);
            writer.write(Constant.NEWLINE);
            writer.flush();
            writer.close();
            writer = null;

            String line = null;
            while((line = reader.readLine()) != null) {
                root.printWarning(line);
            }
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    // Shouldn't happen.
                }
            }

            try {
                reader.close();
            } catch (IOException e) {
                // Shouldn't happen.
            }

            try {
                p.getInputStream().close();
            } catch (IOException e) {
                // Shouldn't happen.
            }

            for (;;) {
                try {
                    int result = p.waitFor();
                    if (result != 0) {
                        root.printWarning("Graphviz exited with a non-zero return value: " + result);
                    }
                    break;
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
    }

    private static File getOutputDirectory(String[][] options) {
        for (String[] o: options) {
            if (o[0].equals("-d")) {
                return new File(o[1]);
            }
        }

        // Fall back to the current working directory.
        return new File(System.getProperty("user.dir", "."));
    }
}
