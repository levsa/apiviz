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

import com.sun.javadoc.Doc;
import com.sun.javadoc.RootDoc;

/**
 * @author The APIViz Project (http://apiviz.googlecode.com/)
 * @author Trustin Lee (http://gleamynode.net/)
 *
 * @version $Rev$, $Date$
 *
 */
public class Relationship {
    private final Doc target;
    private final String sourceLabel;
    private final String targetLabel;
    private final String edgeLabel;
    private final boolean oneway;

    public Relationship(RootDoc rootDoc, String spec) {
        if (spec == null) {
            spec = "";
        }

        String[] args = spec.replaceAll("\\s+", " ").trim().split(" ");
        for (int i = 1; i < Math.min(4, args.length); i ++) {
            if (args[i].equals("-")) {
                args[i] = "";
            }
        }

        if (args.length == 1) {
            target = rootDoc.classNamed(args[0]);
            sourceLabel = "";
            targetLabel = "";
            edgeLabel = "";
            oneway = true;
        } else if (args.length >= 3) {
            target = rootDoc.classNamed(args[0]);
            sourceLabel = args[1];
            targetLabel = args[2];
            if (args.length > 3 && args[args.length -1].equalsIgnoreCase("oneway")) {
                oneway = true;
                StringBuilder buf = new StringBuilder();
                for (int i = 3; i < args.length - 1; i ++) {
                    buf.append(' ');
                    buf.append(args[i]);
                }
                edgeLabel = buf.substring(1);
            } else {
                oneway = false;
                StringBuilder buf = new StringBuilder();
                for (int i = 3; i < args.length; i ++) {
                    buf.append(' ');
                    buf.append(args[i]);
                }
                edgeLabel = buf.substring(1);
            }
        } else {
            throw new IllegalArgumentException("Invalid relationship syntax: " + spec);
        }

        if (target == null) {
            throw new IllegalArgumentException(
                    "Invalid relationship syntax: " + spec +
                    " (Unknown package or class name)");
        }
    }

    public Doc getTarget() {
        return target;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public String getTargetLabel() {
        return targetLabel;
    }

    public String getEdgeLabel() {
        return edgeLabel;
    }

    public boolean isOneway() {
        return oneway;
    }
}
