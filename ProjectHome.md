# What is APIviz? #

APIviz is a JavaDoc doclet which extends the Java standard doclet.  It generates comprehensive UML-like class and package diagrams for quick understanding of the overall API structure.

  * [Samples](#Sample.md)
  * [Basic Usage](#Basic_Usage.md)
  * [Doclet Tags](#Doclet_Tags.md)
  * [Prerequisites](#Prerequisites.md)
  * [Ant Integration](#Ant_Integration.md)
  * [Maven 2 Integration](#Maven_2_Integration.md)
  * [Using in Eclipse](#Using_in_Eclipse.md)
  * [Feed Back](#Feed_Back.md)
  * [Changes](#Changes.md)

# Samples #

  * [The Netty project](http://docs.jboss.org/netty/3.0/api/) - with APIviz 1.2.4.GA
  * [The Netty project](http://docs.jboss.org/netty/3.2/api/) - with APIviz 1.3.1.GA
  * Please [e-mail me](mailto:trustin@gmail.com) if you got a URL to share here (don't forget to tell me the APIviz version you used.)

# Basic Usage #

  * Set the '`-doclet`' option to: `org.jboss.apiviz.APIviz`
  * Set the '`-docletpath`' option to: `path/to/apiviz-<version>.jar`
  * Make sure that the '`-classpath`' option includes the path to the directory which contains the compiled class files (e.g. '`-classpath target/classes`' or '`-classpath build/classes`').  If your tool doesn't allow you to override the '`-classpath`' option, please use the '`-sourceclasspath`' option instead:
```
javadoc ... -sourceclasspath build/classes
            -sourceclasspath build/test-classes ...
```
  * Please note that the '`-sourceclasspath`' option can be specified more than once.
  * Set the '`-nopackagediagram`' to disable package diagram generation.  '`-sourceclasspath`' option is no longer necessary if '`-nopackagediagram`' option is specified.

If you are using Ant or Maven 2, please follow the instructions below.

# Doclet Tags #

APIviz automatically discovers the relationship between packages and classes and their stereotype by default.  Additionally, you can use the following doclet tags to add more relationships or to make the generated diagram look cleaner.

| **Tag** | **Target** | **Description** |
|:--------|:-----------|:----------------|
| `@apiviz.stereotype <stereotype>` | Class / `package-info.java` | Assigns a stereotype to the target |
| `@apiviz.landmark` | Class / `package-info.java` | Highlights the target by filling it with a bright color|
| `@apiviz.category <categoryname>` | Class / `package-info.java` | (Since 1.3) Assigns a category to the target.  Each category is automatically colored with pre-defined fill- and line- color.  Alternatively, you can specify the color of a category as a Javadoc command line option. (e.g. `javadoc ... -category beginner:white:green -category advanced:#CCCCCC:red ...`) |
| `@apiviz.exclude` | Class / `package-info.java` | Hides the target from the package summary page or the overview summary page |
| `@apiviz.exclude <regex>` | Class / `package-info.java` | Hides the classes that matches the regular expression from the target class diagram or the target package summary page |
| `@apiviz.excludeSubtypes` | Class only | Hides the subtypes of the target from the class diagram |
| `@apiviz.inherit` | Class only | Hides the classes which matches the exclusion regular expression of the package where the target belongs from the class diagram |
| `@apiviz.uses       <FQCN>` | Class only | Adds a dependency relationship |
| `@apiviz.has        <FQCN>` | Class only | Adds a navigability relationship |
| `@apiviz.owns       <FQCN>` | Class only | Adds an aggregation relationship |
| `@apiviz.composedOf <FQCN>` | Class only | Adds a composition relationship |

`@see` and `@deprecated` tags are also recognized.  `@apiviz.landmark`, `@deprecated`, `@apiviz.exclude` and  `@apiviz.exclude <regex>` tags can be specified in `package-info.java`, too.  'FQCN' stands for 'Fully Qualified Class Name'.

## Deprecated Tags ##

  * `@apiviz.hidden` - replaced by `@apiviz.exclude` without an argument

# Prerequisites #

You must have JDK 1.5+ and [Graphviz 2.20+](http://www.graphviz.org) installed in your system.  If Graphviz is not found in your system path, it will automatically fall back to the standard doclet with a warning message.  Alternatively, you can specify the directory where `dot` or `dot.exe` is located with either the Java system property - `graphviz.home` (e.g. `-J-Dgraphviz.home=C:\Program Files\Graphviz-2.20.2\bin`) or the system environment variable `GRAPHVIZ_HOME` (e.g. `set GRAPHVIZ_HOME=C:\Program Files\Graphviz-2.20.2\bin`)

# Ant Integration #

Modify your `build.xml`'s `javadoc` task:

```
  <javadoc ...
           doclet="org.jboss.apiviz.APIviz"
           docletpath="apiviz-1.3.0.GA.jar"
           additionalparam="-author -version ...">
    ...
  </javadoc>
```

Please note that `additionalparam` attribute is used instead of the standard doclet attributes.  It is because some Ant versions don't pass the standard doclet attributes to a custom doclet even if it extends the standard doclet.  Also, you must specify the build output directory which contains the compiled class files in either the `-classpath` attribute or the `-sourceclasspath` attribute.

# Maven 2 Integration #

In the `reporting` section, add the `maven-javadoc-plugin`:

```
  <reporting>
    ...
    <plugins>
      ...
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-javadoc-plugin</artifactId>
        <version>2.5</version>
        <configuration>
          <doclet>org.jboss.apiviz.APIviz</doclet>
          <docletArtifact>
            <groupId>org.jboss.apiviz</groupId>
            <artifactId>apiviz</artifactId>
            <version>1.3.2.GA</version>
          </docletArtifact>
          <useStandardDocletOptions>true</useStandardDocletOptions>
          <charset>UTF-8</charset>
          <encoding>UTF-8</encoding>
          <docencoding>UTF-8</docencoding>
          <breakiterator>true</breakiterator>
          <version>true</version>
          <author>true</author>
          <keywords>true</keywords>
          <additionalparam>
            -sourceclasspath ${project.build.outputDirectory}
          </additionalparam>
        </configuration>
      </plugin>
      ...
    </plugins>
    ...
  </reporting>
```

Please note that you must compile the project first before running the `maven-javadoc-plugin` (e.g. `mvn compile javadoc:javadoc`), and specify the `-sourceclasspath` option.

# Using in Eclipse #

  1. Right-click on a Java project.
  1. Select the 'Export ...' menu item in the context menu.
  1. Select 'Java > Javadoc' as an export destination.
  1. Click the 'Next' button.
  1. Choose the 'Use custom doclet' radio button.
  1. Enter '`org.jboss.apiviz.APIviz`' in the 'Doclet name' field.
  1. Enter the location of the APIviz JAR in the 'Doclet class path' field.
  1. Click the 'Next' button.  (Do not click the 'Finish' button yet.)
  1. Specify '`-d`' and '`-sourceclasspath`' option explicitly in the 'Extra Javadoc options' area:
```
-d /home/trustin/projectX/build/apidoc
-sourceclasspath /home/trustin/projectX/build/classes
-sourceclasspath /home/trustin/projectX/build/test-classes
```
  1. Click the 'Finish' button.

# Feed Back #

Do you have a good idea to improve APIviz or a problem to report?  Please e-mail me: trustin@gmail.com (Trustin Lee)

# Changes #

  * 1.3.2.GA - 17 October 2012
    * Now APIViz works with java7.
  * 1.3.1.GA - 4 March 2010
    * Fixed a bug where arrowheads and arrowtails are not rendered when used with Graphviz 2.26.3.
  * 1.3.0.GA - 9 April 2009
    * Added '`-category`' option (by Brad Sneade)
    * Slight tweak on diagram look
    * Better explanation when Graphviz is not found
    * Added support for an environment variable '`GRAPHVIZ_HOME`'
    * Fixed a bug where a certain Graphviz version is not recognized
  * 1.2.5.GA - 3 December 2008
    * Added '`-nopackagediagram`' option
  * 1.2.4.GA - 7 October 2008
    * Fixed a problem in the published `pom.xml` file
  * 1.2.3.GA - 10 September 2008
    * Fixed issues related with inner classes (reported by Steven Buroff)
  * 1.2.2.GA - 9 September 2008
    * Added '-sourceclasspath' option to help a user to specify the class path of the output directories which contains the compiled classes.  APIviz should work fine with Eclipse now with the '-sourceclasspath' option.
  * 1.2.1.GA - 6 September 2008
    * Fixed a `NullPointerException` which is thrown when the `-classpath` option is specified incorrectly - now skips the package dependency diagram generation with a warning
    * Fixed a `NoSuchElementException` which is thrown when there's no class to process
    * Fixed a bug where packages are laid out in an alphabeticaly descending order
  * 1.2.0.GA - 5 September 2008
    * Moved the source code repository to JBoss.org - the web site will move, too.
    * Added `@apiviz.excludeSubtypes` tag
    * Fixed incorrect package dependency analysis - now uses [JDepend](http://clarkware.com/software/JDepend.html)
    * A class diagram is automatically rotated if it's too wide
    * Put some bottom margin between the diagram and the table
    * `@apiviz.exclude` deprecates `@apiviz.hidden`
  * 1.1.3 - 21 July 2008
    * Fixed a 'ugly output' warning in Microsoft Windows.
    * Fixed a bug where JavaDoc generation fails when there's only one package.
  * 1.1.2 - 16 July 2008
    * Contributed by Heiko Feldker <`heiko/dot/feldker/at/googlemail/dot/com`>
      * Fixed a bug where Graphviz is not detected in Microsoft Windows.
      * Added `graphviz.home` system property to specify the directory where the Graphviz executable is located.
  * 1.1.1 - 12 July 2008
    * Fixed a bug where `@apiviz.hidden` tag doesn't hide a class in a certain case.
    * Fixed a bug where `@apiviz.exclude` tag excludes even a central class in a diagram.
  * 1.1.0 - 4 July 2008
    * Edges are drawn more smartly so that the resulting diagram is more compact.
  * 1.0.5 - 2 July 2008
    * Fixed a bug where an escape character (e.g. '`\n`') is displayed in a tooltip.
  * 1.0.4 - 27 June 2008
    * Fixed a bug where an arrow tail is not drawn.
  * 1.0.3 - 26 June 2008
    * Applied a workaround for a `NullPointerException` raised from inside JDK
  * 1.0.2 - 26 June 2008
    * Fixed a bug where package dependency graph is calculated incorrected.
    * Added support for `@deprecated` tag - a deprecated class or package will be represented as a dotted box.
  * 1.0.1 - 26 June 2008
    * Fixed a bug where a nested class is not handled properly.
  * 1.0.0 - 26 June 2008
    * Initial release