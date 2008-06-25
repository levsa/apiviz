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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.Tag;

/**
 * @author The APIViz Project (http://apiviz.googlecode.com/)
 * @author Trustin Lee (http://gleamynode.net/)
 *
 * @version $Rev$, $Date$
 *
 */
public class DiagramBuilder {

    private static final String NORMAL_FONT;
    private static final String ITALIC_FONT;

    static {
        if(System.getProperty("os.name").toLowerCase().contains("windows")) {
            NORMAL_FONT = "arial";
            ITALIC_FONT = "ariali";
        } else {
            NORMAL_FONT = "Helvetica";
            ITALIC_FONT = "Helvetica-Oblique";
        }
    }

    private final RootDoc root;
    private final String packagePrefix;
    private final Set<Doc> landmarks = new HashSet<Doc>();
    private final Set<Doc> dimouts = new HashSet<Doc>();
    private final Map<String, Set<Doc>> groups = new HashMap<String, Set<Doc>>();
    private final Map<Doc, Set<Doc>> generalizations = new HashMap<Doc, Set<Doc>>();
    private final Map<Doc, Set<Doc>> realizations = new HashMap<Doc, Set<Doc>>();
    private final Map<Doc, List<Relationship>> dependencies = new HashMap<Doc, List<Relationship>>();
    private final Map<Doc, List<Relationship>> navigabilities = new HashMap<Doc, List<Relationship>>();
    private final Map<Doc, List<Relationship>> aggregations = new HashMap<Doc, List<Relationship>>();
    private final Map<Doc, List<Relationship>> compositions = new HashMap<Doc, List<Relationship>>();
    private final Set<String> seeAlsoRelationships = new HashSet<String>();

    private boolean useLandscapeView = true;
    private boolean useAutoDimout = true;
    private boolean useAutoLandmark = true;
    private boolean useSeeTags = false;

    public DiagramBuilder(RootDoc root) {
        packagePrefix = "";
        this.root = root;
    }

    public DiagramBuilder(RootDoc root, PackageDoc defaultPackage) {
        packagePrefix = defaultPackage.name() + '.';
        this.root = root;
    }

    public boolean isUseLandscapeView() {
        return useLandscapeView;
    }

    public void setUseLandscapeView(boolean portrait) {
        useLandscapeView = portrait;
    }

    public boolean isUseAutoLandmark() {
        return useAutoLandmark;
    }

    public void setUseAutoLandmark(boolean autoLandmark) {
        useAutoLandmark = autoLandmark;
    }

    public boolean isUseAutoDimout() {
        return useAutoDimout;
    }

    public void setUseAutoDimout(boolean useAutoDimout) {
        this.useAutoDimout = useAutoDimout;
    }

    public boolean isUseSeeTags() {
        return useSeeTags;
    }

    public void setUseSeeTags(boolean useSeeTags) {
        this.useSeeTags = useSeeTags;
    }

    public void addPackage(PackageDoc pkg) {
        if (pkg == null) {
            throw new NullPointerException("pkg");
        }

        addNode(pkg);
    }

    public void addClass(ClassDoc type) {
        addClass(type, true);
    }

    public void addLandmark(Doc node) {
        landmarks.add(node);
    }

    public void addDimout(Doc node) {
        dimouts.add(node);
    }

    private void addClass(ClassDoc type, boolean addRelatedClasses) {
        if (type == null) {
            throw new NullPointerException("type");
        }

        addNode(type);

        if (addRelatedClasses) {
            addRelatedClasses(type);
        }
    }

    private void addNode(Doc node) {
        String groupId = groupId(node);
        Set<Doc> group = groups.get(groupId);
        if (group == null) {
            group = new HashSet<Doc>();
            groups.put(groupId, group);
        }

        group.add(node);

        if (isUseAutoLandmark()) {
            if (node.tags(TAG_LANDMARK).length > 0) {
                addLandmark(node);
                return;
            }
        }

        if (isUseAutoDimout()) {
            if (node instanceof ProgramElementDoc) {
                if (!((ProgramElementDoc) node).qualifiedName().startsWith(packagePrefix)) {
                    addDimout(node);
                }
            }
        }
    }

    private String groupId(Doc type) {
        String groupId = "default";
        if (type.tags(TAG_GROUP).length > 0) {
            groupId = type.tags(TAG_GROUP)[0].text().trim().replaceAll(
                    "(\\s|\\.|-|\\$)+", "_");
        } else if (type instanceof ProgramElementDoc) {
            if (!((ProgramElementDoc) type).qualifiedName().startsWith(packagePrefix)) {
                groupId = "external";
            }
        }

        return groupId;
    }

    private void addRelatedClasses(ClassDoc type) {
        // Generalization
        ClassDoc superType = type.superclass();
        if (superType != null &&
            !superType.qualifiedName().equals("java.lang.Object") &&
            !superType.qualifiedName().equals("java.lang.Annotation") &&
            !superType.qualifiedName().equals("java.lang.Enum")) {
            addClass(superType, false);
            addGeneralization(type, superType);
        }

        // Realization
        for (ClassDoc i: type.interfaces()) {
            if (i.qualifiedName().equals("java.lang.annotation.Annotation")) {
                continue;
            }

            addClass(i, false);
            addRealization(type, i);
        }

        // Apply custom doclet tags first.
        for (Tag t: type.tags()) {
            if (t.name().equals(TAG_USES)) {
                addDependency(type, new Relationship(root, t.text()));
            } else if (t.name().equals(TAG_HAS)) {
                addNavigability(type, new Relationship(root, t.text()));
            } else if (t.name().equals(TAG_OWNS)) {
                addAggregation(type, new Relationship(root, t.text()));
            } else if (t.name().equals(TAG_COMPOSED_OF)) {
                addComposition(type, new Relationship(root, t.text()));
            }
        }

        if (isUseSeeTags()) {
            addRelatedClassesFromSeeTags(type);
        }
    }

    private void addRelatedClassesFromSeeTags(ClassDoc type) {
        // Add an edge with '<<see also>>' label for the classes with @see
        // tags, but avoid duplication.
        for (SeeTag t: type.seeTags()) {
            try {
                if (t.referencedClass() == null) {
                    continue;
                }
            } catch (Exception e) {
                continue;
            }

            String a = type.qualifiedName();
            String b = t.referencedClass().qualifiedName();
            addClass(t.referencedClass(), false);
            if (a.compareTo(b) != 0) {
                if (a.compareTo(b) < 0) {
                    if (seeAlsoRelationships.add(a + " & " + b)) {
                        addNavigability(type, new Relationship(root, b + " - - &#171;see also&#187;"));
                    }
                } else {
                    if (seeAlsoRelationships.add(b + " & " + a)) {
                        addNavigability(t.referencedClass(), new Relationship(root, a + " - - &#171;see also&#187;"));
                    }
                }
            }
        }
    }

    public void addRealization(Doc type, Doc superType) {
        addSuperType(realizations, type, superType);
    }

    public void addGeneralization(Doc type, Doc superType) {
        addSuperType(generalizations, type, superType);
    }

    private void addSuperType(Map<Doc, Set<Doc>> target, Doc type, Doc superType) {
        Set<Doc> superTypes = target.get(type);
        if (superTypes == null) {
            superTypes = new HashSet<Doc>();
            target.put(type, superTypes);
        }
        superTypes.add(superType);
    }

    public void addDependency(Doc type, Relationship relationship) {
        addRelationship(dependencies, type, relationship);
    }

    public void addNavigability(Doc type, Relationship relationship) {
        addRelationship(navigabilities, type, relationship);
    }

    public void addAggregation(Doc type, Relationship relationship) {
        addRelationship(aggregations, type, relationship);
    }

    public void addComposition(Doc type, Relationship relationship) {
        addRelationship(compositions, type, relationship);
    }

    private void addRelationship(Map<Doc, List<Relationship>> target, Doc type, Relationship relationship) {
        addNode(relationship.getTarget());
        List<Relationship> relationships = target.get(type);
        if (relationships == null) {
            relationships = new ArrayList<Relationship>();
            target.put(type, relationships);
        }
        relationships.add(relationship);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(
                "digraph __G__ {" + NEWLINE +
                "rankdir=" + (isUseLandscapeView()? "RL" : "BT") + ";" + NEWLINE +
                "ranksep=0.3;" + NEWLINE +
                "nodesep=0.3;" + NEWLINE +
                "mclimit=10;" + NEWLINE +
                "outputorder=edgesfirst;" + NEWLINE +
                "center=1;" + NEWLINE +
                "remincross=true;" + NEWLINE +
                "searchsize=1024;" + NEWLINE +
                "edge [fontsize=10, fontname=\"" + NORMAL_FONT + "\", " +
                "style=\"setlinewidth(0.6)\"]; " + NEWLINE +
                "node [shape=box, fontsize=10, fontname=\"" + NORMAL_FONT + "\", " +
                "width=0.1, height=0.1, style=\"setlinewidth(0.6)\"]; " + NEWLINE);

        renderGroups(buf);

        renderGeneralizations(buf);
        renderRealizations(buf);

        renderDependencies(buf);
        renderNavigabilities(buf);
        renderAggregations(buf);
        renderCompositions(buf);

        buf.append("}" + NEWLINE);

        return buf.toString();
    }

    private void renderGroups(StringBuilder buf) {
        if (groups.size() == 1) {
            renderNodes(buf, groups.values().iterator().next());
            return;
        }

        for (String groupId: groups.keySet()) {
            Set<Doc> group = groups.get(groupId);
//            if (group.size() > 1) {
//                buf.append("subgraph cluster_");
//                buf.append(groupId);
//                buf.append(" {" + NEWLINE);
                renderNodes(buf, group);
//                buf.append("}" + NEWLINE);
//            } else {
//                renderNodes(buf, group);
//            }
        }
    }

    private void renderNodes(StringBuilder buf, Set<Doc> group) {
        for (Doc node: group) {
            if (node instanceof PackageDoc) {
                renderPackage(buf, (PackageDoc) node);
            } else {
                renderClass(buf, (ClassDoc) node);
            }
        }
    }

    private void renderRealizations(StringBuilder buf) {
        for (Doc type: realizations.keySet()) {
            for (Doc superType: realizations.get(type)) {
                renderRealization(buf, type, superType);
            }
        }
    }

    private void renderGeneralizations(StringBuilder buf) {
        for (Doc type: generalizations.keySet()) {
            for (Doc superType: generalizations.get(type)) {
                renderGeneralization(buf, type, superType);
            }
        }
    }

    private void renderDependencies(StringBuilder buf) {
        for (Doc e: dependencies.keySet()) {
            for (Relationship rel: dependencies.get(e)) {
                renderDependency(buf, e, rel);
            }
        }
    }

    private void renderNavigabilities(StringBuilder buf) {
        for (Doc e: navigabilities.keySet()) {
            for (Relationship rel: navigabilities.get(e)) {
                renderNavigability(buf, e, rel);
            }
        }
    }

    private void renderAggregations(StringBuilder buf) {
        for (Doc e: aggregations.keySet()) {
            for (Relationship rel: aggregations.get(e)) {
                renderAggregation(buf, e, rel);
            }
        }
    }

    private void renderCompositions(StringBuilder buf) {
        for (Doc e: compositions.keySet()) {
            for (Relationship rel: compositions.get(e)) {
                renderComposition(buf, e, rel);
            }
        }
    }

    private void renderPackage(StringBuilder buf, PackageDoc pkg) {
        String stereotype = getStereotype(pkg);
        String fillColor = fillColor(pkg);
        String lineColor = lineColor(pkg);
        String fontColor = fontColor(pkg);

        buf.append(nodeId(pkg));
        buf.append(" [label=\"");
        if (stereotype != null) {
            buf.append("&#171;");
            buf.append(stereotype);
            buf.append("&#187;\\n");
        }
        buf.append(pkg.name());
        buf.append("\", style=\"filled\", color=\"");
        buf.append(lineColor);
        buf.append("\", fontcolor=\"");
        buf.append(fontColor);
        buf.append("\", fillcolor=\"");
        buf.append(fillColor);
        buf.append("\"];");
        buf.append(NEWLINE);
    }

    private void renderClass(StringBuilder buf, ClassDoc type) {
        String stereotype = getStereotype(type);
        String fillColor = fillColor(type);
        String lineColor = lineColor(type);
        String fontColor = fontColor(type);

        buf.append(nodeId(type));
        buf.append(" [label=\"");
        if (stereotype != null) {
            buf.append("&#171;");
            buf.append(stereotype);
            buf.append("&#187;\\n");
        }
        buf.append(nodeLabel(type));
        buf.append("\"");
        if (type.isAbstract() && !type.isInterface()) {
            buf.append(", fontname=\"");
            buf.append(ITALIC_FONT);
            buf.append("\"");
        }
        buf.append(", style=\"filled\", color=\"");
        buf.append(lineColor);
        buf.append("\", fontcolor=\"");
        buf.append(fontColor);
        buf.append("\", fillcolor=\"");
        buf.append(fillColor);
        buf.append("\"];");
        buf.append(NEWLINE);
    }

    private void renderRealization(StringBuilder buf, Doc type, Doc superType) {
        buf.append(nodeId(type));
        buf.append(" -> ");
        buf.append(nodeId(superType));
        buf.append(" [arrowhead=enormal, style=dashed];");
        buf.append(NEWLINE);
    }

    private void renderGeneralization(StringBuilder buf, Doc type, Doc superType) {
        buf.append(nodeId(type));
        buf.append(" -> ");
        buf.append(nodeId(superType));
        buf.append(" [arrowhead=enormal];");
        buf.append(NEWLINE);
    }

    private void renderDependency(StringBuilder buf, Doc type, Relationship relationship) {
        buf.append(nodeId(type));
        buf.append(" -> ");
        buf.append(nodeId(relationship));
        buf.append(" [arrowhead=open, style=dashed, headlabel=\"");
        buf.append(relationship.getTargetLabel());
        buf.append("\", taillabel=\"");
        buf.append(relationship.getSourceLabel());
        buf.append("\", label=\"");
        buf.append(relationship.getEdgeLabel());
        buf.append("\"];");
        buf.append(NEWLINE);
    }

    private void renderNavigability(StringBuilder buf, Doc type, Relationship relationship) {
        buf.append(nodeId(type));
        buf.append(" -> ");
        buf.append(nodeId(relationship));
        buf.append(" [arrowhead=");
        buf.append(relationship.isOneway() ? "open": "none");
        buf.append(", headlabel=\"");
        buf.append(relationship.getTargetLabel());
        buf.append("\", taillabel=\"");
        buf.append(relationship.getSourceLabel());
        buf.append("\", label=\"");
        buf.append(relationship.getEdgeLabel());
        buf.append("\"];");
        buf.append(NEWLINE);
    }

    private void renderAggregation(StringBuilder buf, Doc type, Relationship relationship) {
        buf.append(nodeId(type));
        buf.append(" -> ");
        buf.append(nodeId(relationship));
        buf.append(" [arrowhead=open, arrowtail=ediamond, headlabel=\"");
        buf.append(relationship.getTargetLabel());
        buf.append("\", taillabel=\"");
        buf.append(relationship.getSourceLabel());
        buf.append("\", label=\"");
        buf.append(relationship.getEdgeLabel());
        buf.append("\"];");
        buf.append(NEWLINE);
    }

    private void renderComposition(StringBuilder buf, Doc type, Relationship relationship) {
        buf.append(nodeId(type));
        buf.append(" -> ");
        buf.append(nodeId(relationship));
        buf.append(" [arrowhead=open, arrowtail=diamond, headlabel=\"");
        buf.append(relationship.getTargetLabel());
        buf.append("\", taillabel=\"");
        buf.append(relationship.getSourceLabel());
        buf.append("\", label=\"");
        buf.append(relationship.getEdgeLabel());
        buf.append("\"];");
        buf.append(NEWLINE);
    }

    private String nodeId(Relationship rel) {
        return nodeId(rel.getTarget());
    }

    private String nodeId(Doc node) {
        if (node instanceof ProgramElementDoc) {
            return ((ProgramElementDoc) node).qualifiedName().replace('.', '_');
        } else {
            return node.name().replace('.', '_');
        }
    }

    private String nodeLabel(Doc node) {
        String name;
        if (node instanceof ProgramElementDoc) {
            name = ((ProgramElementDoc) node).qualifiedName();
        } else {
            name = node.name();
        }

        if (name.startsWith(packagePrefix)) {
            name = name.substring(packagePrefix.length());
        }

        int dotIndex = name.lastIndexOf('.');
        if (dotIndex < 0) {
            return name;
        } else {
            return name.substring(dotIndex + 1) + "\\n(" +
                   name.substring(0, dotIndex) + ')';
        }
    }

    private String fillColor(Doc doc) {
        String color = "white";
        if (landmarks.contains(doc)) {
            color = "khaki1";
        }
        return color;
    }

    private String lineColor(Doc doc) {
        String color = "black";
        if (dimouts.contains(doc)) {
            color = "gray";
        }
        return color;
    }

    private String fontColor(Doc doc) {
        String color = "black";
        if (dimouts.contains(doc)) {
            color = "gray30";
        }
        return color;
    }

    private String getStereotype(Doc doc) {
        String stereotype = doc.isInterface()? "interface" : null;
        if (doc.isException()) {
            stereotype = "exception";
        } else if (doc.isAnnotationType()) {
            stereotype = "annotation";
        } else if (doc.isEnum()) {
            stereotype = "enum";
        } else if (doc instanceof ClassDoc) {
            ClassDoc classDoc = (ClassDoc) doc;
            boolean staticType = true;
            int methods = 0;
            for (MethodDoc m: classDoc.methods()) {
                if (m.isConstructor()) {
                    continue;
                }
                methods ++;
                if (!m.isStatic()) {
                    staticType = false;
                    break;
                }
            }
            if (staticType && methods > 0) {
                stereotype = "static";
            }
        }

        if (doc.tags(TAG_STEREOTYPE).length > 0) {
            stereotype = doc.tags(TAG_STEREOTYPE)[0].text();
        }
        return stereotype;
    }
}
